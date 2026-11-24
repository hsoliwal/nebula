#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Retry bounded transient upstream failures and nothing else. Never mask
# compiler, test or application failures. Understands the output of the pinned
# Maven 3.9.11 and of the runner's preinstalled Maven 3.10.
#
# A repository response counts only when it reports the failure: on a line
# starting with "[ERROR]" or "[FATAL]", or on an indented continuation line of
# such a line. Maven logs many tolerated responses ("[WARNING] Could not transfer
# metadata ... status code: 429", "[WARNING] GET request to <url> failed (attempt
# N of 4) due to status 429") and continues; those alone never make a failure
# retryable.
#
# Transient (retried):
#   failure lines with "status code: NNN", "HTTP Status: NNN" (Maven 3.10),
#   "HTTP/<v> NNN" or "Unexpected HTTP response: NNN" for NNN = 429, 502, 503 or
#   504, or with "Too Many Requests (429)", "Service Temporarily Unavailable
#   (503)", "Bad Gateway (502)" or "Gateway Timeout (504)";
#   a Maven 3.10 transport failure: a resolution clause on the failure lines names
#   an artifact ("<g:a[:type]:v> or one of its dependencies could not be resolved",
#   "Failed to read artifact descriptor for <g:a[:type]:v>", "The following
#   artifacts could not be resolved: <g:a[:type]:v> (absent), ..."); no failure
#   line naming it carries a status code other than 429/502/503/504; and the
#   output has "GET request to <url> failed (attempt N of M) due to status NNN"
#   (NNN as above) where <url> is exactly <root>/<group path>/<artifact>/
#   <version>/<artifact>-<version>... for a known repository root, with no later
#   "Downloaded from <id>: <url>" (Maven's own retry recovered it);
#   anywhere in the output, as before: "Could not acquire read|write lock for
#   'metadata:org.eclipse:" (Tycho) and "Could not find artifact
#   org.eclipse.tycho:tycho-maven-plugin:jar:4.0.12 in central".
# Non-transient (never retried, even next to a transient line):
#   anywhere in the output: "Compilation failure", "COMPILATION ERROR", "There are
#   test failures", "Tests run: N, Failures: >0", "Tests run: N, Failures: N,
#   Errors: >0", "[ERROR] ... cannot find symbol", "The method ... is undefined for
#   the type";
#   failure lines with "status code: 401|403|404", "HTTP Status: 401|403|404",
#   "HTTP/<v> 401|403|404", "Unexpected HTTP response: 401|403|404", "reason
#   phrase: Unauthorized|Forbidden|Not Found", "Checksum validation failed",
#   "ChecksumFailureException", "checksum verification|validation fail", or
#   "Could not find artifact" for anything but the Tycho 4.0.12 case above.
# Anything else is not retried.
#
# Backoff: a Retry-After value in the captured output ("Retry-After: N", or Maven
# 3.10's "due to Retry-After header with seconds value N") is honoured when it
# fits the remaining budget; otherwise the delay is exponential with jitter,
# capped per sleep. Attempts and total sleep time are both bounded; an exhausted
# budget fails closed.
set -euo pipefail
if (( $# == 0 )); then
  echo 'usage: retry-transient-maven-503.sh command [arguments...]' >&2
  exit 64
fi
attempts=${M3_HTTP503_ATTEMPTS:-3}
delay=${M3_HTTP503_RETRY_DELAY_SECONDS:-10}
max_delay=${M3_RETRY_MAX_DELAY_SECONDS:-300}
max_total=${M3_RETRY_MAX_TOTAL_SECONDS:-900}
# No leading zeros: bash arithmetic would read 08 or 09 as invalid octal.
number='^(0|[1-9][0-9]{0,3})$'
if ! [[ "$attempts" =~ ^[1-5]$ && "$delay" =~ $number && "$max_delay" =~ $number && "$max_total" =~ $number ]]; then
  echo 'invalid retry configuration' >&2
  exit 64
fi
tycho_4012='Could not find artifact org\.eclipse\.tycho:tycho-maven-plugin:jar:4\.0\.12 in central'
code='(status code:|HTTP Status:|HTTP/[0-9.]+|Unexpected HTTP response:)[[:space:]]*'
log=$(mktemp)
trap 'rm -f "$log"' EXIT

coordinate='[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+(:[a-z-]+)?:[0-9][A-Za-z0-9_.-]*'

# Artifacts that a resolution clause on the failure lines names itself:
# "<coordinates> or one of its dependencies could not be resolved", "Failed to read
# artifact descriptor for <coordinates>" and "The following artifacts could not be
# resolved: <coordinates> (absent|present...), ...". Other coordinates on the same
# lines, such as the plugin in "Failed to execute goal <g:a:v>:<goal>", do not count.
named_artifacts() {
  {
    grep -Eo "${coordinate} or one of its dependencies could not be resolved" <<< "$failure" \
      | grep -Eo "^${coordinate}" || true
    grep -Eo "Failed to read artifact descriptor for ${coordinate}" <<< "$failure" \
      | grep -Eo "${coordinate}\$" || true
    grep -Eo "The following artifacts could not be resolved: (${coordinate} \\([^)]*\\)(, )?)+" <<< "$failure" \
      | sed 's/^The following artifacts could not be resolved: //' | grep -Eo "${coordinate} \\(" \
      | grep -Eo "^${coordinate}" || true
  } | sort -u
}

# Repository roots: Maven Central, every root printed as "from/to <id> (<root>)", and
# every Maven 3.10 prefix-file URL "<root>/.meta/prefixes.txt".
repository_roots() {
  {
    printf '%s\n' 'https://repo.maven.apache.org/maven2'
    grep -Eo 'from/to [^[:space:]]+ \(https?://[^)[:space:]]+\)' "$log" | sed -E 's/^.*\((.*)\)$/\1/' || true
    grep -Eo 'https?://[^[:space:]]+/\.meta/prefixes\.txt' "$log" | sed -E 's#/\.meta/prefixes\.txt$##' || true
  } | sed -E 's#/+$##' | sort -u
}

# Status (429, 502, 503 or 504) of a Maven 3.10 transport warning for an artifact
# that a resolution clause names; empty when there is none. Such a warning counts
# only when its URL is exactly <root>/<group path>/<artifact>/<version>/<artifact>-
# <version>(-<classifier>).<extension> for a known repository root, Maven's own retry
# did not recover it (no later "Downloaded from <id>: <same URL>"), and no failure
# line naming the artifact carries any status code other than 429/502/503/504.
transport_status() {
  local artifacts roots coords group rest artifact version gesc vesc named bad
  local entry number url status root prefix matched recovered
  artifacts=$(named_artifacts)
  [[ -n "$artifacts" ]] || return 0
  roots=$(repository_roots)
  while IFS= read -r coords; do
    group=${coords%%:*}
    rest=${coords#*:}
    artifact=${rest%%:*}
    version=${coords##*:}
    gesc=$(sed 's/[.]/\\./g' <<< "$group:$artifact")
    vesc=$(sed 's/[.]/\\./g' <<< "$version")
    named=$(grep -E "(^|[^A-Za-z0-9_.-])${gesc}(:[A-Za-z0-9_-]+)*:${vesc}([^A-Za-z0-9_.-]|\$)" <<< "$failure" || true)
    bad=$(grep -Eo "${code}[0-9]{3}" <<< "$named" | grep -Ev '(429|502|503|504)$' || true)
    [[ -z "$bad" ]] || continue
    while IFS= read -r entry; do
      [[ -n "$entry" ]] || continue
      number=${entry%%:*}
      url=$(sed -E 's/^.*GET request to ([^[:space:]]+) failed .*$/\1/' <<< "$entry")
      status=$(sed -E 's/^.*due to status ([0-9]{3}).*$/\1/' <<< "$entry")
      matched=''
      while IFS= read -r root; do
        prefix="$root/${group//.//}/$artifact/$version/$artifact-$version"
        if [[ "$url" == "$prefix".* || "$url" == "$prefix"-* ]]; then
          matched=1
          break
        fi
      done <<< "$roots"
      [[ -n "$matched" ]] || continue
      recovered=$(awk -v url="$url" -v after="$number" 'NR > after && index($0, "Downloaded from ") {
          at = index($0, ": " url)
          if (at && (length($0) == at + length(url) + 1 || substr($0, at + length(url) + 2, 1) == " ")) { print NR; exit }
        }' "$log")
      [[ -z "$recovered" ]] || continue
      printf '%s' "$status"
      return 0
    done < <(grep -nE 'GET request to [^[:space:]]+ failed \(attempt [0-9]+ of [0-9]+\) due to status (429|502|503|504)\b' "$log" || true)
  done <<< "$artifacts"
}

slept=0
for (( attempt = 1; attempt <= attempts; attempt++ )); do
  if "$@" 2>&1 | tee "$log"; then
    exit 0
  else
    rc=$?
  fi
  # A transient warning can precede a real compiler/test failure. Never mask it.
  if grep -Eqi 'Compilation failure|COMPILATION ERROR|There are test failures|Tests run: [0-9]+, Failures: [1-9][0-9]*|Tests run: [0-9]+, Failures: [0-9]+, Errors: [1-9]|\[ERROR\] .*cannot find symbol|The method .+ is undefined for the type' "$log"; then
    echo "M3: product compilation/test failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  # The lines that report the failure: [ERROR]/[FATAL] and their indented continuations.
  failure=$(awk '/^\[(ERROR|FATAL)\]/ { keep = 1; print; next } keep && /^[[:space:]]+[^[:space:]]/ { print; next } { keep = 0 }' "$log")
  # Authentication, a missing artifact or a corrupt download is not transient,
  # even when a rate-limit or gateway response is reported next to it.
  missing=$(grep 'Could not find artifact' <<< "$failure" | grep -Ev "$tycho_4012" || true)
  if [[ -n "$missing" ]] || grep -Eqi "${code}(401|403|404)\\b|reason phrase: (Unauthorized|Forbidden|Not Found)\\b|Checksum validation failed|ChecksumFailureException|checksum (verification|validation) fail" <<< "$failure"; then
    echo "M3: non-transient repository failure (authentication, missing artifact or checksum); no retry (exit $rc)" >&2
    exit "$rc"
  fi
  reason=''
  if grep -Eq "${code}429\\b|Too Many Requests \\(429\\)" <<< "$failure"; then
    reason='upstream HTTP 429 (rate limited)'
  elif grep -Eq "${code}503\\b|Service Temporarily Unavailable \\(503\\)" <<< "$failure"; then
    reason='upstream HTTP 503'
  elif grep -Eq "${code}(502|504)\\b|Bad Gateway \\(502\\)|Gateway Time-?out \\(504\\)" <<< "$failure"; then
    reason='upstream HTTP 502/504'
  elif status=$(transport_status) && [[ -n "$status" ]]; then
    reason="upstream HTTP $status for an artifact the failure names (Maven 3.10 transport)"
  elif grep -Eq "Could not acquire (read|write) lock for 'metadata:org\.eclipse:" "$log"; then
    # Parallel Tycho metadata resolution can report only a lock timeout.
    reason='Eclipse Maven metadata lock timeout'
  elif grep -Eq "$tycho_4012" "$log"; then
    # 4.0.12 is a confirmed Maven Central release; retry only this precise
    # extension-resolution failure, never an arbitrary missing artifact.
    reason='published Tycho 4.0.12 Maven Central lookup failure'
  else
    echo "M3: non-transient failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  if (( attempt == attempts )); then
    echo "M3: $reason persists after $attempts attempts; failing closed" >&2
    exit "$rc"
  fi
  retry_after=$( { grep -Eio 'Retry-After:?[[:space:]]*[0-9]{1,5}\b' "$log" || true
                   grep -Eo 'Retry-After header with seconds value [0-9]{1,5}\b' "$log" || true; } \
                 | grep -Eo '[0-9]+$' | sort -n | tail -n 1 || true)
  if [[ -n "$retry_after" ]]; then
    wait=$((10#$retry_after))
    source='Retry-After'
  else
    base=$(( delay * (1 << (attempt - 1)) ))
    wait=$(( base + RANDOM % (base / 2 + 1) ))
    (( wait <= max_delay )) || wait=$max_delay
    source='exponential backoff with jitter'
  fi
  if (( slept + wait > max_total )); then
    echo "M3: $reason; a ${wait}s wait would exceed the ${max_total}s retry budget after $attempt attempts; failing closed" >&2
    exit "$rc"
  fi
  echo "M3: $reason (attempt $attempt/$attempts); retrying original command in ${wait}s ($source)" >&2
  if [[ -n "${M3_RETRY_SLEEP_RECORD:-}" ]]; then
    # Test hook: record the computed wait instead of sleeping.
    printf '%s\n' "$wait" >> "$M3_RETRY_SLEEP_RECORD"
  else
    sleep "$wait"
  fi
  slept=$(( slept + wait ))
done
exit 1
