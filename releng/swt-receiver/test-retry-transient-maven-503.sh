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
if [[ "$mode" == fatal || "$mode" == mixedfatal ]]; then
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
bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/d" locktransient >/dev/null
[[ $(<"$tmp/d") == 2 ]] || exit 1
if bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/e" lockpersistent >/dev/null 2>&1; then exit 1; fi
[[ $(<"$tmp/e") == 3 ]] || exit 1
if bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/f" mixedfatal >/dev/null 2>&1; then exit 1; fi
[[ $(<"$tmp/f") == 1 ]] || exit 1
bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/g" tychotransient >/dev/null
[[ $(<"$tmp/g") == 2 ]] || exit 1
if bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/h" tychopersistent >/dev/null 2>&1; then exit 1; fi
[[ $(<"$tmp/h") == 3 ]] || exit 1
printf 'PASS: HTTP503, Eclipse locks, Tycho 4.0.12 Central transient retry=2; product failures fatal; persistent failures=3\n'
