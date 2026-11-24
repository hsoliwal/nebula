#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Retry bounded transient upstream failures and nothing else. Never mask
# compiler, test or application failures.
#
# Repository responses count only on a line that reports the failure, that is a
# line starting with "[ERROR]". Maven logs many tolerated responses as
# "[WARNING] Could not transfer metadata ... status code: 429" and continues;
# those never make a failure retryable.
#
# Transient (retried):
#   [ERROR] lines with "status code: 429", "Too Many Requests (429)" or
#   "HTTP/<v> 429"; "status code: 503", "Service Temporarily Unavailable (503)"
#   or "HTTP/<v> 503"; "status code: 502", "status code: 504",
#   "Bad Gateway (502)", "Gateway Timeout (504)" or "HTTP/<v> 502|504";
#   anywhere in the output, as before: "Could not acquire read|write lock for
#   'metadata:org.eclipse:" (Tycho) and "Could not find artifact
#   org.eclipse.tycho:tycho-maven-plugin:jar:4.0.12 in central".
# Non-transient (never retried, even next to a transient line):
#   "Compilation failure", "There are test failures", "Tests run: N, Failures: >0",
#   "The method ... is undefined for the type" (anywhere in the output);
#   [ERROR] lines with "status code: 401|403|404", "reason phrase: Unauthorized|
#   Forbidden|Not Found", "HTTP/<v> 401|403|404", "Checksum validation failed",
#   "ChecksumFailureException", "checksum verification|validation fail", or
#   "Could not find artifact" for anything but the Tycho 4.0.12 case above.
# Anything else is not retried.
#
# Backoff: a numeric Retry-After value in the captured output is honoured when
# it fits the remaining budget (Maven 3.9 does not log the header today);
# otherwise the delay is exponential with jitter, capped per sleep. Attempts and
# total sleep time are both bounded; an exhausted budget fails closed.
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
log=$(mktemp)
trap 'rm -f "$log"' EXIT
slept=0
for (( attempt = 1; attempt <= attempts; attempt++ )); do
  if "$@" 2>&1 | tee "$log"; then
    exit 0
  else
    rc=$?
  fi
  # A transient warning can precede a real compiler/test failure. Never mask it.
  if grep -Eqi 'Compilation failure|There are test failures|Tests run: [0-9]+, Failures: [1-9][0-9]*|The method .+ is undefined for the type' "$log"; then
    echo "M3: product compilation/test failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  errors=$(grep -E '^\[ERROR\]' "$log" || true)
  # Authentication, a missing artifact or a corrupt download is not transient,
  # even when a rate-limit or gateway response is reported next to it.
  missing=$(grep 'Could not find artifact' <<< "$errors" | grep -Ev "$tycho_4012" || true)
  if [[ -n "$missing" ]] || grep -Eqi 'status code: (401|403|404)\b|reason phrase: (Unauthorized|Forbidden|Not Found)\b|HTTP/[0-9.]+ (401|403|404)\b|Checksum validation failed|ChecksumFailureException|checksum (verification|validation) fail' <<< "$errors"; then
    echo "M3: non-transient repository failure (authentication, missing artifact or checksum); no retry (exit $rc)" >&2
    exit "$rc"
  fi
  reason=''
  if grep -Eq 'status code: 429\b|Too Many Requests \(429\)|HTTP/[0-9.]+ 429\b' <<< "$errors"; then
    reason='upstream HTTP 429 (rate limited)'
  elif grep -Eq 'status code: 503\b|Service Temporarily Unavailable \(503\)|HTTP/[0-9.]+ 503\b' <<< "$errors"; then
    reason='upstream HTTP 503'
  elif grep -Eq 'status code: (502|504)\b|Bad Gateway \(502\)|Gateway Time-?out \(504\)|HTTP/[0-9.]+ (502|504)\b' <<< "$errors"; then
    reason='upstream HTTP 502/504'
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
  retry_after=$(grep -Eio 'Retry-After:?[[:space:]]*[0-9]{1,5}\b' "$log" | grep -Eo '[0-9]+$' | sort -n | tail -n 1 || true)
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
