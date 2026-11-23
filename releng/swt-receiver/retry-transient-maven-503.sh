#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Retry bounded upstream HTTP 503, Eclipse metadata locks and the specific
# published Tycho 4.0.12 extension's transient Maven Central lookup failure.
# Never mask compiler, test, or application failures.
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
  # A 503 warning can precede a real compiler/test failure. Never mask it.
  if grep -Eqi 'Compilation failure|There are test failures|Tests run: [0-9]+, Failures: [1-9][0-9]*|The method .+ is undefined for the type' "$log"; then
    echo "M3: product compilation/test failure; no retry (exit $rc)" >&2
    exit "$rc"
  fi
  reason=''
  if grep -Eq 'status code: 503|Service Temporarily Unavailable \(503\)|HTTP/[0-9.]+ 503' "$log"; then
    reason='upstream HTTP 503'
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
  echo "M3: $reason (attempt $attempt/$attempts); retrying original command" >&2
  sleep "$delay"
done
exit 1
