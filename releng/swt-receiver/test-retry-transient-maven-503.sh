#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
selfdir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# Verbatim lines from nebula run 45457730531 attempt 2: Maven Central answered HTTP 429
# for maven-dependency-plugin:3.8.1 in the classpath-guard pass, and the previous
# wrapper logged "non-transient failure; no retry".
real429="$selfdir/tests/data/run-45457730531-attempt2-maven-429.log"
grep -q 'status code: 429, reason phrase: Too Many Requests (429)' "$real429"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
cat > "$tmp/fake" <<'FAKE'
#!/usr/bin/env bash
set -euo pipefail
state=$1
mode=$2
count=0
[[ ! -f "$state" ]] || count=$(<"$state")
count=$((count + 1))
printf '%s\n' "$count" > "$state"
if [[ "$mode" == transient && "$count" -eq 1 || "$mode" == persistent || "$mode" == mixedfatal ]]; then
  echo '[ERROR] status code: 503, reason phrase: Service Temporarily Unavailable (503)'
  if [[ "$mode" != mixedfatal ]]; then exit 22; fi
fi
if [[ "$mode" == locktransient && "$count" -eq 1 || "$mode" == lockpersistent ]]; then
  echo "Suppressed: java.lang.IllegalStateException: Attempt 1: Could not acquire write lock for 'metadata:org.eclipse:eclipse-sdk-prereqs:4.42.0-SNAPSHOT' in 30 SECONDS"
  exit 19
fi
if [[ "$mode" == tychotransient && "$count" -eq 1 || "$mode" == tychopersistent ]]; then
  echo '[ERROR] Could not find artifact org.eclipse.tycho:tycho-maven-plugin:jar:4.0.12 in central (https://repo.maven.apache.org/maven2)'
  exit 18
fi
if [[ "$mode" == real429transient && "$count" -eq 1 || "$mode" == real429persistent \
      || "$mode" == real429with404 || "$mode" == real429retryafter || "$mode" == real429retryafterhuge ]]; then
  cat "$REAL429"
  [[ "$mode" != real429with404 ]] || echo '[ERROR] Could not transfer artifact org.example:missing:pom:1.0 from/to central (https://repo.maven.apache.org/maven2): status code: 404, reason phrase: Not Found (404)'
  [[ "$mode" != real429retryafter ]] || echo '[WARNING] Retry-After: 7'
  [[ "$mode" != real429retryafterhuge ]] || echo '[WARNING] Retry-After: 999'
  exit 1
fi
if [[ "$mode" == gateway502 && "$count" -eq 1 ]]; then
  echo '[ERROR] Could not transfer artifact a:b:pom:1 from/to central (https://repo.maven.apache.org/maven2): status code: 502, reason phrase: Bad Gateway (502)'
  exit 1
fi
if [[ "$mode" == gateway504 && "$count" -eq 1 ]]; then
  echo '[ERROR] Could not transfer artifact a:b:pom:1 from/to central (https://repo.maven.apache.org/maven2): status code: 504, reason phrase: Gateway Timeout (504)'
  exit 1
fi
case "$mode" in
  fatal401) echo '[ERROR] Could not transfer artifact a:b:pom:1 from/to central (https://repo.maven.apache.org/maven2): status code: 401, reason phrase: Unauthorized (401)'; exit 1 ;;
  fatal403) echo '[ERROR] Could not transfer artifact a:b:pom:1 from/to central (https://repo.maven.apache.org/maven2): status code: 403, reason phrase: Forbidden (403)'; exit 1 ;;
  fatal404) echo '[ERROR] Could not transfer artifact a:b:pom:1 from/to central (https://repo.maven.apache.org/maven2): status code: 404, reason phrase: Not Found (404)'; exit 1 ;;
  fatalchecksum) echo '[ERROR] Checksum validation failed, expected 0123abcd (REMOTE_EXTERNAL) but is actually 4567ef01'; exit 1 ;;
esac
if [[ "$mode" == fatal || "$mode" == mixedfatal ]]; then
  echo '[ERROR] compilation failure: missing method'
  exit 17
fi
echo 'BUILD SUCCESS'
FAKE
chmod +x "$tmp/fake"
export REAL429="$real429"
wrapper() { bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$@"; }
fails() { if wrapper "$@" >/dev/null 2>"$tmp/stderr"; then echo "unexpected success: $*" >&2; exit 1; fi; }
calls() { [[ $(<"$1") == "$2" ]] || { echo "$1: expected $2 invocations, got $(<"$1")" >&2; exit 1; }; }
export M3_HTTP503_ATTEMPTS=3 M3_HTTP503_RETRY_DELAY_SECONDS=0
wrapper "$tmp/a" transient >/dev/null 2>&1; calls "$tmp/a" 2
fails "$tmp/b" fatal; calls "$tmp/b" 1
fails "$tmp/c" persistent; calls "$tmp/c" 3
wrapper "$tmp/d" locktransient >/dev/null 2>&1; calls "$tmp/d" 2
fails "$tmp/e" lockpersistent; calls "$tmp/e" 3
fails "$tmp/f" mixedfatal; calls "$tmp/f" 1
wrapper "$tmp/g" tychotransient >/dev/null 2>&1; calls "$tmp/g" 2
fails "$tmp/h" tychopersistent; calls "$tmp/h" 3
# HTTP 429 (the real run 45457730531 output), 502 and 504 are transient.
wrapper "$tmp/i" real429transient >/dev/null 2>"$tmp/stderr"; calls "$tmp/i" 2
grep -q 'upstream HTTP 429 (rate limited) (attempt 1/3); retrying original command' "$tmp/stderr"
fails "$tmp/j" real429persistent; calls "$tmp/j" 3
grep -q 'upstream HTTP 429 (rate limited) persists after 3 attempts; failing closed' "$tmp/stderr"
wrapper "$tmp/k" gateway502 >/dev/null 2>&1; calls "$tmp/k" 2
wrapper "$tmp/l" gateway504 >/dev/null 2>&1; calls "$tmp/l" 2
# 401, 403, 404 and checksum failures stay non-transient, also next to a 429.
for mode in fatal401 fatal403 fatal404 fatalchecksum real429with404; do
  fails "$tmp/m-$mode" "$mode"; calls "$tmp/m-$mode" 1
  grep -q 'non-transient repository failure' "$tmp/stderr"
done
# Backoff, recorded instead of slept: exponential with jitter in [base, 1.5 base].
export M3_RETRY_SLEEP_RECORD="$tmp/waits"
M3_HTTP503_ATTEMPTS=4 M3_HTTP503_RETRY_DELAY_SECONDS=4 fails "$tmp/n" real429persistent; calls "$tmp/n" 4
mapfile -t waits < "$tmp/waits"
(( ${#waits[@]} == 3 && waits[0] >= 4 && waits[0] <= 6 && waits[1] >= 8 && waits[1] <= 12 \
   && waits[2] >= 16 && waits[2] <= 24 )) || { echo "unexpected backoff: ${waits[*]}" >&2; exit 1; }
# Each wait is capped.
rm -f "$tmp/waits"
M3_HTTP503_ATTEMPTS=3 M3_HTTP503_RETRY_DELAY_SECONDS=100 M3_RETRY_MAX_DELAY_SECONDS=120 \
  fails "$tmp/o" real429persistent; calls "$tmp/o" 3
mapfile -t waits < "$tmp/waits"
(( ${#waits[@]} == 2 && waits[0] >= 100 && waits[0] <= 120 && waits[1] == 120 )) || { echo "uncapped: ${waits[*]}" >&2; exit 1; }
# A numeric Retry-After is honoured exactly.
rm -f "$tmp/waits"
M3_HTTP503_ATTEMPTS=2 M3_HTTP503_RETRY_DELAY_SECONDS=30 fails "$tmp/p" real429retryafter; calls "$tmp/p" 2
[[ $(<"$tmp/waits") == 7 ]] || { echo "Retry-After not honoured: $(<"$tmp/waits")" >&2; exit 1; }
grep -q 'retrying original command in 7s (Retry-After)' "$tmp/stderr"
# The total sleep budget is bounded and fails closed, without sleeping past it.
rm -f "$tmp/waits"
M3_HTTP503_ATTEMPTS=5 M3_HTTP503_RETRY_DELAY_SECONDS=10 M3_RETRY_MAX_TOTAL_SECONDS=15 \
  fails "$tmp/q" real429persistent; calls "$tmp/q" 2
grep -q 'would exceed the 15s retry budget after 2 attempts; failing closed' "$tmp/stderr"
[[ $(wc -l < "$tmp/waits") -eq 1 ]]
rm -f "$tmp/waits"
fails "$tmp/r" real429retryafterhuge; calls "$tmp/r" 1
grep -q 'a 999s wait would exceed the 900s retry budget after 1 attempts; failing closed' "$tmp/stderr"
[[ ! -e "$tmp/waits" ]]
unset M3_RETRY_SLEEP_RECORD
# Invalid configuration is refused before running anything.
if M3_RETRY_MAX_TOTAL_SECONDS=abc wrapper "$tmp/s" transient >/dev/null 2>&1; then exit 1; fi
[[ ! -e "$tmp/s" ]]
printf 'PASS: HTTP 429/502/503/504, Eclipse locks, Tycho 4.0.12 Central transient retry=2; product, 401/403/404 and checksum failures fatal; persistent failures=attempts; exponential backoff with jitter, cap, Retry-After and total budget\n'
