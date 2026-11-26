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
# Tolerated [WARNING] 429/503 lines (the first fixture line is one) next to a failure
# whose cause is something else.
missing='[ERROR] Failed to execute goal on project demo: Could not resolve dependencies for project org.example:demo:jar:1.0: Could not find artifact org.example:missing:jar:1.0 in central (https://repo.maven.apache.org/maven2) -> [Help 1]'
case "$mode" in
  # Real Tycho target-definition failure: central's primary 429 and a single
  # eclipse-hosted fallback 404 for the *same* artifact.
  m310target429transient) if (( count == 1 )); then cat "$M310_TYCHO_TARGET429"; exit 1; fi ;;
  m310target429persistent) cat "$M310_TYCHO_TARGET429"; exit 1 ;;
  m310target429different)
    sed 's/Could not find artifact org\.mortbay\.jasper:mortbay-apache-el:pom:9\.0\.124/Could not find artifact org.other:missing:pom:9.0.124/' "$M310_TYCHO_TARGET429"
    exit 1 ;;
  m310target429extra)
    cat "$M310_TYCHO_TARGET429"
    echo '[ERROR] Could not find artifact org.other:missing:pom:1.0 in central (https://repo.maven.apache.org/maven2)'
    exit 1 ;;
  m310target429404)
    sed 's/HTTP Status: 429 (additional failures:/HTTP Status: 404 (additional failures:/' "$M310_TYCHO_TARGET429"
    exit 1 ;;
  m310target429notTycho)
    sed 's/Failed to resolve target definition/Failed to execute an unrelated task/' "$M310_TYCHO_TARGET429"
    exit 1 ;;
  # Verbatim Maven 3.10.1 outputs (the runner's preinstalled Maven in maven.yaml).
  m310extension) if (( count == 1 )); then cat "$M310_EXTENSION"; exit 1; fi ;;
  m310extensionpersistent) cat "$M310_EXTENSION"; exit 1 ;;
  m310parent) if (( count == 1 )); then cat "$M310_PARENT"; exit 1; fi ;;
  m310otherartifact) head -n 8 "$M310_EXTENSION"
    echo '[ERROR] Plugin org.example:other:1.0 or one of its dependencies could not be resolved:'
    printf '\tFailed to read artifact descriptor for org.example:other:jar:1.0\n'
    exit 1 ;;
  m310code13) head -n 8 "$M310_EXTENSION"
    echo '[ERROR] Failed to execute goal org.eclipse.tycho:tycho-surefire-plugin:5.0.4:test (default-test) on project org.eclipse.swt.tests: An unexpected error occured while launching the test runtime (process returned error code 13). Please check the log for details. -> [Help 1]'
    exit 1 ;;
  # The plugin POM had a 429 that Maven's own retry recovered; another artifact then fails
  # without any status code (the review's case A).
  m310recoveredplugin|m310unrecoveredplugin|m310recoveredclause)
    pom='https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-dependency-plugin/3.8.1/maven-dependency-plugin-3.8.1.pom'
    echo "[WARNING] GET request to $pom failed (attempt 1 of 4) due to status 429. Retrying in 5000 ms (due to retryInterval 5000 ms multiplied by execution count 1)..."
    [[ "$mode" == m310unrecoveredplugin ]] || echo "[INFO] Downloaded from central: $pom (11 kB at 52 kB/s)"
    if [[ "$mode" == m310recoveredclause ]]; then
      echo '[ERROR] Plugin org.apache.maven.plugins:maven-dependency-plugin:3.8.1 or one of its dependencies could not be resolved: The following artifacts could not be resolved: org.foo:bar:jar:1.0 (absent): Could not transfer artifact org.foo:bar:jar:1.0 from/to maven-default-http-blocker (http://0.0.0.0/): Blocked mirror for repositories: [legacy (http://legacy.example/repo, default, releases)] -> [Help 1]'
    else
      echo '[ERROR] Failed to execute goal org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath (default-cli) on project demo: Could not resolve dependencies for project org.example:demo:jar:1.0: The following artifacts could not be resolved: org.foo:bar:jar:1.0 (absent): Could not transfer artifact org.foo:bar:jar:1.0 from/to maven-default-http-blocker (http://0.0.0.0/): Blocked mirror for repositories: [legacy (http://legacy.example/repo, default, releases)] -> [Help 1]'
    fi
    exit 1 ;;
  # 429 transport warnings for the artifact, but a failure line reports HTTP 500 for it.
  m310status500) sed -n 6,8p "$M310_EXTENSION"
    echo '[ERROR] Plugin org.eclipse.tycho:tycho-build:5.0.4 or one of its dependencies could not be resolved:'
    printf '\tFailed to read artifact descriptor for org.eclipse.tycho:tycho-build:jar:5.0.4\n'
    echo '[ERROR] Could not transfer artifact org.eclipse.tycho:tycho-build:pom:5.0.4 from/to central (https://repo.maven.apache.org/maven2): HTTP Status: 500'
    exit 1 ;;
  # The 429 is for com.x.org.foo:bar:1.0, whose path contains /org/foo/bar/1.0/; the failure is org.foo:bar:1.0.
  m310collision)
    echo '[WARNING] GET request to https://repo.maven.apache.org/maven2/com/x/org/foo/bar/1.0/bar-1.0.pom failed (attempt 3 of 4) due to status 429. Retrying in 15000 ms (due to retryInterval 5000 ms multiplied by execution count 3)...'
    echo '[ERROR] Plugin org.foo:bar:1.0 or one of its dependencies could not be resolved:'
    printf '\tFailed to read artifact descriptor for org.foo:bar:jar:1.0\n'
    exit 1 ;;
  m310status404) echo '[ERROR] Could not transfer artifact a:b:pom:1 from/to central (https://repo.maven.apache.org/maven2): HTTP Status: 404'; exit 1 ;;
  real429testerrors) cat "$REAL429"; echo '[ERROR] Tests run: 5, Failures: 0, Errors: 2, Skipped: 0, Time elapsed: 1.2 s <<< FAILURE!'; exit 1 ;;
  warn429code13) head -n 1 "$REAL429"
    echo '[ERROR] Failed to execute goal org.eclipse.tycho:tycho-surefire-plugin:5.0.4:test (default-test) on project org.eclipse.swt.tests: An unexpected error occured while launching the test runtime (process returned error code 13). Please check the log for details. -> [Help 1]'
    exit 1 ;;
  warn429forktimeout) head -n 1 "$REAL429"
    echo '[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.6.1:test (test) on project org.eclipse.swt.tests: There was a timeout in the fork -> [Help 1]'
    exit 1 ;;
  warn503other) echo '[WARNING] Could not transfer metadata a:b/maven-metadata.xml from/to central (https://repo.maven.apache.org/maven2): status code: 503, reason phrase: Service Temporarily Unavailable (503)'
    echo '[ERROR] Failed to execute goal org.apache.maven.plugins:maven-antrun-plugin:3.2.0:run (build-native-binaries) on project x: An Ant BuildException has occured -> [Help 1]'
    exit 1 ;;
  missingartifact) echo "$missing"; exit 1 ;;
  real429missingartifact) cat "$REAL429"; echo "$missing"; exit 1 ;;
  tychomissingother) echo '[ERROR] Could not find artifact org.eclipse.tycho:tycho-maven-plugin:jar:4.0.12 in central (https://repo.maven.apache.org/maven2)'
    echo "$missing"; exit 1 ;;
esac
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
# Verbatim Maven 3.10.1 outputs from maven.yaml's SWT build step: run 44992024856 (HTTP 429
# while resolving the tycho-build extension; no [ERROR] line carries a status code) and
# run 45108815443 (HTTP 503 on the [ERROR] line as "HTTP Status: 503", Retry-After 60).
export M310_EXTENSION="$selfdir/tests/data/run-44992024856-maven-3.10.1-429-extension.log"
export M310_PARENT="$selfdir/tests/data/run-45108815443-maven-3.10.1-503-parent.log"
# Actual primary Central HTTP 429 with an optional eclipse-hosted fallback 404.
export M310_TYCHO_TARGET429="$selfdir/tests/data/run-45610458196-maven-3.10.1-429-tycho-target.txt"
grep -Fq 'Failed to resolve target definition' "$M310_TYCHO_TARGET429"
grep -Fq 'HTTP Status: 429 (additional failures:' "$M310_TYCHO_TARGET429"
grep -Fq 'Could not find artifact org.mortbay.jasper:mortbay-apache-el:pom:9.0.124 in eclipse-hosted' "$M310_TYCHO_TARGET429"
grep -q 'due to status 429' "$M310_EXTENSION"
grep -q 'HTTP Status: 503' "$M310_PARENT"
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
# 401, 403, 404, checksum and Maven's "Could not find artifact" stay non-transient,
# also next to a 429 that is reported as an [ERROR].
for mode in fatal401 fatal403 fatal404 fatalchecksum real429with404 missingartifact real429missingartifact \
            tychomissingother; do
  fails "$tmp/m-$mode" "$mode"; calls "$tmp/m-$mode" 1
  grep -q 'non-transient repository failure' "$tmp/stderr"
done
# Only an [ERROR] line counts: a tolerated [WARNING] 429 or 503 does not make another
# failure retryable (Tycho test runtime error code 13, surefire fork timeout, Ant).
for mode in warn429code13 warn429forktimeout warn503other; do
  fails "$tmp/w-$mode" "$mode"; calls "$tmp/w-$mode" 1
  grep -q 'M3: non-transient failure; no retry (exit 1)' "$tmp/stderr"
done
# Maven 3.10.1: both real outputs retry. The extension case is tied to the artifact in
# the failure; a 3.10 transport warning for another artifact, or next to an unrelated
# failure, does not count. "HTTP Status: 404" stays non-transient, and test errors win.
wrapper "$tmp/t1" m310extension >/dev/null 2>"$tmp/stderr"; calls "$tmp/t1" 2
grep -q 'upstream HTTP 429 for an artifact the failure names (Maven 3.10 transport) (attempt 1/3)' "$tmp/stderr"
fails "$tmp/t2" m310extensionpersistent; calls "$tmp/t2" 3
# Its Retry-After of 60 s is recorded, not slept (checked below).
M3_RETRY_SLEEP_RECORD="$tmp/ignored-waits" wrapper "$tmp/t3" m310parent >/dev/null 2>"$tmp/stderr"; calls "$tmp/t3" 2
grep -q 'upstream HTTP 503 (attempt 1/3)' "$tmp/stderr"
# Tycho's exact run-45610458196 target-definition primary Central 429 has a
# single *secondary* eclipse-hosted 404 for that same POM. This is safe to retry,
# but never suppress genuinely missing or unrelated primary artifacts.
wrapper "$tmp/tycho429ok" m310target429transient >/dev/null 2>"$tmp/stderr"
calls "$tmp/tycho429ok" 2
grep -q 'upstream HTTP 429 (rate limited) (attempt 1/3); retrying original command' "$tmp/stderr"
fails "$tmp/tycho429persistent" m310target429persistent
calls "$tmp/tycho429persistent" 3
grep -q 'upstream HTTP 429 (rate limited) persists after 3 attempts; failing closed' "$tmp/stderr"
for mode in m310target429different m310target429extra m310target429404 m310target429notTycho; do
  fails "$tmp/tycho429-$mode" "$mode"
  calls "$tmp/tycho429-$mode" 1
  grep -q 'non-transient repository failure' "$tmp/stderr"
done
# Only artifacts named by the resolution clause count, a warning that Maven's own retry
# recovered does not, a non-transient status for the artifact wins, and the URL must be
# that artifact's exact repository path.
for mode in m310otherartifact m310code13 m310recoveredplugin m310unrecoveredplugin m310recoveredclause \
            m310status500 m310collision; do
  fails "$tmp/t-$mode" "$mode"; calls "$tmp/t-$mode" 1
  grep -q 'M3: non-transient failure; no retry (exit 1)' "$tmp/stderr"
done
fails "$tmp/t4" m310status404; calls "$tmp/t4" 1
grep -q 'non-transient repository failure' "$tmp/stderr"
fails "$tmp/t5" real429testerrors; calls "$tmp/t5" 1
grep -q 'product compilation/test failure' "$tmp/stderr"
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
# Maven 3.10's wording, from the real run 45108815443 output, is honoured too.
rm -f "$tmp/waits"
M3_HTTP503_ATTEMPTS=2 wrapper "$tmp/p3" m310parent >/dev/null 2>"$tmp/stderr"; calls "$tmp/p3" 2
[[ $(<"$tmp/waits") == 60 ]] || { echo "Maven 3.10 Retry-After not honoured: $(<"$tmp/waits")" >&2; exit 1; }
grep -q 'retrying original command in 60s (Retry-After)' "$tmp/stderr"
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
# Invalid configuration, including a leading zero that bash would read as octal, is
# refused before running anything, with the usage exit status.
for setting in M3_RETRY_MAX_TOTAL_SECONDS=abc M3_HTTP503_RETRY_DELAY_SECONDS=08 M3_RETRY_MAX_DELAY_SECONDS=09 \
               M3_RETRY_MAX_TOTAL_SECONDS=010 M3_HTTP503_ATTEMPTS=0 M3_HTTP503_RETRY_DELAY_SECONDS=10000; do
  rc=0
  env "$setting" bash "$selfdir/retry-transient-maven-503.sh" "$tmp/fake" "$tmp/s" transient >/dev/null 2>&1 || rc=$?
  [[ $rc -eq 64 && ! -e "$tmp/s" ]] || { echo "$setting: expected refusal (64) before running, got $rc" >&2; exit 1; }
done
printf 'PASS: Maven 3.9 and 3.10 [ERROR]/[FATAL] HTTP 429/502/503/504, Maven 3.10 transport status for the failing artifact, exact Tycho target-definition Central 429 with same-artifact auxiliary eclipse-hosted 404 (unrelated/foreign/missing/primary-404 refused), Eclipse locks, Tycho 4.0.12 Central transient retry=2; [WARNING]-only codes, product, 401/403/404, missing-artifact and checksum failures run once; persistent failures=attempts; exponential backoff with jitter, cap, Retry-After and total budget; no leading-zero settings\n'
