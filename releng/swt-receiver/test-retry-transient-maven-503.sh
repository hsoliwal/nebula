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
printf '%s\\n' "$count" > "$state"
if [[ "$mode" == transient && "$count" -eq 1 || "$mode" == persistent ]]; then
  echo '[ERROR] status code: 503, reason phrase: Service Temporarily Unavailable (503)'
  exit 22
fi
if [[ "$mode" == fatal ]]; then
  echo '[ERROR] compilation failure: missing method'
  exit 17
fi
echo 'BUILD SUCCESS'
FAKE
chmod +x "$tmp/fake"
export M3_HTTP503_ATTEMPTS=3 M3_HTTP503_RETRY_DELAY_SECONDS=0
bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/a" transient >/dev/null
[[ $(<"$tmp/a") == 2 ]] || exit 1
if bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/b" fatal >/dev/null 2>&1; then exit 1; fi
[[ $(<"$tmp/b") == 1 ]] || exit 1
if bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/c" persistent >/dev/null 2>&1; then exit 1; fi
[[ $(<"$tmp/c") == 3 ]] || exit 1
printf 'PASS: HTTP503 transient retry=2, compile failure=1 (fatal), persistent503=3 (fatal)\\n'
