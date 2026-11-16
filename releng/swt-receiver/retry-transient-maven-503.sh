#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Only retry documented upstream HTTP 503 resolution failures; never mask product defects.
set -euo pipefail
if (( $# == 0 )); then
  echo 'usage: retry-transient-maven-503.sh command [arguments...]' >&2
  exit 64
fi
attempts=${M3_HTTP503_ATTEMPTS:-3}
delay=${M3_HTTP503_RETRY_DELAY_SECONDS:-10}
if ! [[ "$attempts" =~ ^[1-5]$ && "$delay" =~ ^[0-9]+$ ]]; then
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
  if ! grep -Eq 'status code: 503|Service Temporarily Unavailable \\(503\\)|HTTP/[0-9.]+ 503' "$log"; then
    echo "M3: non-503 failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  if (( attempt == attempts )); then
    echo "M3: HTTP 503 persists after $attempts attempts; failing closed" >&2
    exit "$rc"
  fi
  echo "M3: upstream HTTP 503 (attempt $attempt/$attempts); retrying original command" >&2
  sleep "$delay"
done
exit 1
