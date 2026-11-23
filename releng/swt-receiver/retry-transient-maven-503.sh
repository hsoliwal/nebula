#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compatibility entrypoint: retry only genuine upstream HTTP 429/503 resolution failures.
# Never retry compiler/test failures, change the invoked command, or turn exhaustion into success.
set -euo pipefail
if (( $# == 0 )); then
  echo 'usage: retry-transient-maven-503.sh command [arguments...]' >&2
  exit 64
fi
attempts=${M3_MAVEN_TRANSIENT_ATTEMPTS:-${M3_HTTP503_ATTEMPTS:-3}}
delay=${M3_MAVEN_TRANSIENT_RETRY_DELAY_SECONDS:-${M3_HTTP503_RETRY_DELAY_SECONDS:-10}}
if ! [[ "$attempts" =~ ^[1-5]$ && "$delay" =~ ^(0|[1-9][0-9]?)$ ]] || (( delay > 60 )); then
  echo 'invalid retry configuration' >&2
  exit 64
fi
log=$(mktemp)
trap 'rm -f "$log"' EXIT
for (( attempt = 1; attempt <= attempts; attempt++ )); do
  if "$@" 2>&1 | tee "$log"; then
    exit 0
  else
    rc=$?
  fi
  # A transient HTTP warning must never mask an independent fatal compilation or test result.
  if grep -Eiq 'Compilation failure|COMPILATION ERROR|There are test failures|Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]|AssertionError|\[ERROR\] .*cannot find symbol' "$log"; then
    echo "M3: compiler/test failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  if ! grep -Eiq 'status([[:space:]]+code:|[[:space:]]+)?[[:space:]]*(429|503)([^0-9]|$)|HTTP/[0-9.]+[[:space:]]+(429|503)([^0-9]|$)|Unexpected HTTP response:[[:space:]]*(429|503)([^0-9]|$)|Too Many Requests[[:space:]]*\(429\)|Service Temporarily Unavailable[[:space:]]*\(503\)' "$log"; then
    echo "M3: non-transient failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  if (( attempt == attempts )); then
    echo "M3: upstream HTTP 429/503 persists after $attempts attempts; failing closed" >&2
    exit "$rc"
  fi
  echo "M3: upstream HTTP 429/503 (attempt $attempt/$attempts); retrying original command" >&2
  sleep "$delay"
done
exit 1
