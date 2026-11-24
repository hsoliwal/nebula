#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Retry bounded transient upstream failures: HTTP 429 (rate limiting), 502, 503
# and 504 from a Maven repository, Eclipse metadata locks and the specific
# published Tycho 4.0.12 extension's transient Maven Central lookup failure.
# Never mask compiler, test or application failures, and never retry an
# authentication (401/403), not-found (404) or checksum failure.
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
if ! [[ "$attempts" =~ ^[1-5]$ && "$delay" =~ ^[0-9]{1,4}$ && "$max_delay" =~ ^[0-9]{1,4}$ \
        && "$max_total" =~ ^[0-9]{1,4}$ ]]; then
  echo 'invalid retry configuration' >&2
  exit 64
fi
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
  # Authentication, a missing artifact or a corrupt download is not transient,
  # even when a rate-limit or gateway response appears in the same output.
  if grep -Eqi 'status code: (401|403|404)\b|reason phrase: (Unauthorized|Forbidden|Not Found)\b|HTTP/[0-9.]+ (401|403|404)\b|Checksum validation failed|ChecksumFailureException|checksum (verification|validation) fail' "$log"; then
    echo "M3: non-transient repository failure (authentication, not found or checksum); no retry (exit $rc)" >&2
    exit "$rc"
  fi
  reason=''
  if grep -Eq 'status code: 429\b|Too Many Requests \(429\)|HTTP/[0-9.]+ 429\b' "$log"; then
    reason='upstream HTTP 429 (rate limited)'
  elif grep -Eq 'status code: 503|Service Temporarily Unavailable \(503\)|HTTP/[0-9.]+ 503' "$log"; then
    reason='upstream HTTP 503'
  elif grep -Eq 'status code: (502|504)\b|Bad Gateway \(502\)|Gateway Time-?out \(504\)|HTTP/[0-9.]+ (502|504)\b' "$log"; then
    reason='upstream HTTP 502/504'
  elif grep -Eq "Could not acquire (read|write) lock for 'metadata:org\.eclipse:" "$log"; then
    # Parallel Tycho metadata resolution can report only a lock timeout.
    reason='Eclipse Maven metadata lock timeout'
  elif grep -Eq 'Could not find artifact org\.eclipse\.tycho:tycho-maven-plugin:jar:4\.0\.12 in central' "$log"; then
    # 4.0.12 is a confirmed Maven Central release; retry only this precise
    # extension-resolution failure, never an arbitrary 404 or missing source.
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
