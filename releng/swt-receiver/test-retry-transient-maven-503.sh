#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
selfdir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
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
case "$mode" in
  transient503) if (( count == 1 )); then echo '[ERROR] status code: 503, reason phrase: Service Temporarily Unavailable (503)'; exit 22; fi ;;
  transient429) if (( count == 1 )); then echo '[WARNING] GET request failed (attempt 4 of 4) due to status 429'; exit 23; fi ;;
  transientHttp429) if (( count == 1 )); then echo 'HTTP/1.1 429 Too Many Requests'; exit 24; fi ;;
  persistent429) echo '[ERROR] status code: 429'; exit 23 ;;
  persistent503) echo '[ERROR] status code: 503'; exit 22 ;;
  fatal) echo '[ERROR] compilation failure: missing method'; exit 17 ;;
  mixedFatal) echo 'HTTP/1.1 429 Too Many Requests'; echo '[ERROR] Compilation failure: missing method'; exit 19 ;;
  status404) echo '[ERROR] status code: 404'; exit 20 ;;
  success) ;;
  *) echo 'unknown test mode' >&2; exit 97 ;;
esac
echo 'BUILD SUCCESS'
FAKE
chmod +x "$tmp/fake"
export M3_HTTP503_ATTEMPTS=3 M3_HTTP503_RETRY_DELAY_SECONDS=0
check() {
  local label=$1 mode=$2 expected=$3 count=$4 actual
  local file="$tmp/$label"
  local rc=0
  bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$file" "$mode" >"$tmp/log" 2>&1 || rc=$?
  [[ "$rc" == "$expected" ]] || { echo "bad exit for $label: $rc expected $expected" >&2; cat "$tmp/log" >&2; exit 1; }
  actual=$(<"$file")
  [[ "$actual" == "$count" ]] || { echo "bad attempts for $label: $actual expected $count" >&2; exit 1; }
}
check native503 transient503 0 2
check central429 transient429 0 2
check proxy429 transientHttp429 0 2
check exhausted429 persistent429 23 3
check exhausted503 persistent503 22 3
check compilerFatal fatal 17 1
check mixedCompilerFatal mixedFatal 19 1
check unexpected404 status404 20 1
check alreadySuccess success 0 1
if bash "$selfdir/retry-transient-maven-503.sh" >"$tmp/noargs" 2>&1; then exit 1; else [[ $? == 64 ]] || exit 1; fi
for config in 0 6 abc; do
  if M3_MAVEN_TRANSIENT_ATTEMPTS="$config" bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/never" success >/dev/null 2>&1; then exit 1; else [[ $? == 64 ]] || exit 1; fi
done
for config in -1 61 99 abc; do
  if M3_MAVEN_TRANSIENT_RETRY_DELAY_SECONDS="$config" bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/never" success >/dev/null 2>&1; then exit 1; else [[ $? == 64 ]] || exit 1; fi
done
[[ ! -e "$tmp/never" ]] || exit 1
printf 'PASS: 9 executable command cases, 7 invalid configs, no-args refusal; HTTP 429/503 retries bounded; compiler/test and 404 fail closed\n'
