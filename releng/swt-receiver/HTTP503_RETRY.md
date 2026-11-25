<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Fail-closed SWT receiver transient-failure retry

`retry-transient-maven-503.sh` runs the exact supplied command. It retries the
whole command, a bounded number of times, only for a recognized transient
condition. Everything else fails on the first attempt, and so does a transient
condition that persists through every attempt. (The file name predates the
429/502/504 support and is kept because the workflows call it.)

## Call sites and Maven versions

| Call site | Maven | Attempts, base delay | Waits |
|---|---|---|---|
| `terminal-vnc-contracts.yml`: SWT assembly, classpath guard, SWT AllTests (3) | pinned 3.9.11 | 5, 15 s | 15-22, 30-45, 60-90, 120-180 s (at most about 5.6 minutes) |
| `terminal-vnc-contracts.yml`: Terminal tests (1) | pinned 3.9.11 | 3, 12 s | 12-18, 24-36 s (at most 54 s) |
| `codeql.yml`: SWT build (1) | pinned 3.9.11 | defaults: 3, 10 s | 10-15, 20-30 s |
| `maven.yaml`: "Build and bind the pinned SWT GTK3 distribution": SWT build, receiver-P2 `verify` (2) | the runner's preinstalled 3.9 or 3.10 (3.10.1 in runs 44992024856 and 45108815443) | defaults: 3, 10 s | 10-15, 20-30 s, or a logged Retry-After |
| `maven.yaml`: DrawnBrowser reactor, "Build All Widgets" Nebula reactor (2) | pinned 3.9.11 | defaults: 3, 10 s | 10-15, 20-30 s |

The two `maven.yaml` call sites in the SWT step use the runner's Maven on
purpose. Commit 9f46414a stopped downloading Maven there after the archive
download hit HTTP 429. The job sets up Maven 3.9.11 only after the SWT build,
for Nebula's Tycho 4.0.12 reactors (eaa4a05a), which the DrawnBrowser and
"Build All Widgets" call sites use. The wrapper therefore also reads Maven
3.10's output.

The two reactor call sites re-run the whole reactor, tests included, on each
retry; "Build All Widgets" took 13:02 min in run 45488864191. A product
compilation or test failure is never retried, so this applies only to a
transient failure.

## What is detected

A repository response counts only when it reports the failure, on one of these:
- a line starting with `[ERROR]` or `[FATAL]`;
- an indented continuation line of such a line (Maven puts "Failed to read
  artifact descriptor for ..." on a tab-indented line).

Maven also logs many tolerated responses and continues, and these never count on
their own:
- `[WARNING] Could not transfer metadata ... status code: 429, reason phrase:
  Too Many Requests (429)` (Maven 3.9);
- `[WARNING] ... HTTP Status: 429` (Maven 3.10);
- `[WARNING] GET request to <url> failed (attempt N of 4) due to status 429`
  (Maven 3.10).

Run 45457730531 has 44 Maven 3.9 warnings per attempt, some in a step that ended
in BUILD SUCCESS. So a Tycho "process returned error code 13" or a surefire
"There was a timeout in the fork" next to such warnings still runs once.

**Transient: retried.** These checks run in this order, after the non-transient
checks below. NNN stands for 429, 502, 503 or 504.

| Condition | Detected strings |
|---|---|
| HTTP 429, 503, 502/504 on a failure line | `status code: NNN` (Maven 3.9), `HTTP Status: NNN` (Maven 3.10), `HTTP/<version> NNN`, `Unexpected HTTP response: NNN`, `Too Many Requests (429)`, `Service Temporarily Unavailable (503)`, `Bad Gateway (502)`, `Gateway Timeout (504)` or `Gateway Time-out (504)` |
| Maven 3.10 transport failure for the failing artifact | all of the conditions listed below the table; this is the `tycho-build` extension case of run 44992024856 |
| Eclipse metadata lock | anywhere in the output: `Could not acquire read lock for 'metadata:org.eclipse:` or `... write lock ...` |
| Tycho 4.0.12 lookup | anywhere in the output: `Could not find artifact org.eclipse.tycho:tycho-maven-plugin:jar:4.0.12 in central` |

**The Maven 3.10 transport rule.** It applies only when all four conditions
hold:

1. **The artifact is named by a resolution clause on the failure lines.** The
   accepted clauses are:
   - `<coordinates> or one of its dependencies could not be resolved`
   - `Failed to read artifact descriptor for <coordinates>`
   - `The following artifacts could not be resolved: <coordinates> (absent),
     ...` (or `(present, ...)`)

   `<coordinates>` is `<group>:<artifact>[:<type>]:<version>`. Other
   coordinates on the same lines do not count, for example the plugin in
   `Failed to execute goal <g:a:v>:<goal>`.
2. **No failure line that names the artifact carries any other status code.**
   Any `status code:`, `HTTP Status:`, `HTTP/<version>` or `Unexpected HTTP
   response:` code other than 429, 502, 503 or 504 (for example 500, 407 or
   410) disables the rule for that artifact.
3. **The output has a transport warning at that artifact's exact repository
   path.** The warning is `GET request to <url> failed (attempt N of M) due to
   status NNN`, and `<url>` must be exactly `<root>/<group path>/<artifact>/
   <version>/<artifact>-<version>` followed by `.` or `-`. `<root>` is a known
   repository root: Maven Central (`https://repo.maven.apache.org/maven2`), a
   root printed as `from/to <id> (<root>)`, or `<root>/.meta/prefixes.txt`. A
   group such as `com.x.org.foo` therefore does not match `org.foo`.
4. **Maven's own retry did not recover that request.** There must be no later
   `Downloaded from <id>: <url>` line for the same URL.

**Non-transient: never retried, even next to a transient line.**

| Condition | Detected strings |
|---|---|
| Product failure (checked first) | anywhere in the output: `Compilation failure`, `COMPILATION ERROR`, `There are test failures`, `Tests run: N, Failures: M` with M > 0, `Tests run: N, Failures: M, Errors: E` with E > 0, `[ERROR] ... cannot find symbol`, `The method ... is undefined for the type` |
| Authentication, not found, checksum | a failure line with `status code:`, `HTTP Status:`, `HTTP/<version>` or `Unexpected HTTP response:` followed by 401, 403 or 404; `reason phrase: Unauthorized`, `Forbidden` or `Not Found`; `Checksum validation failed`; `ChecksumFailureException`; `checksum verification fail` or `checksum validation fail` |
| Missing artifact | a failure line with `Could not find artifact <coordinates> in <repository> (<url>)`, for anything but the Tycho 4.0.12 case above |

**Anything else** is not retried.

## Backoff

`M3_HTTP503_ATTEMPTS` sets the number of attempts (1-5, default 3).
`M3_HTTP503_RETRY_DELAY_SECONDS` sets the base delay (default 10).

Between attempts the wrapper waits:
- the largest Retry-After value in the captured output, if it fits the remaining
  budget. Two forms are read: `Retry-After: N`, and Maven 3.10's `due to
  Retry-After header with seconds value N` (60 in run 45108815443). Maven 3.9
  does not log the header;
- otherwise `base x 2^(attempt-1)` plus a random jitter of up to half that,
  capped at `M3_RETRY_MAX_DELAY_SECONDS` (default 300).

The total sleep is bounded by `M3_RETRY_MAX_TOTAL_SECONDS` (default 900). A wait
that would exceed the remaining budget fails closed instead of sleeping.

The three seconds settings must be decimal numbers from 0 to 9999 without a
leading zero; bash arithmetic would read `08` as invalid octal. Any other value
is refused with exit status 64 before the command runs.

Each retry re-runs the original command. Every wrapped call passes `mvn -U`.
That matters for HTTP 429: Maven caches a failed transfer ("resolution is not
reattempted until the update interval ... has elapsed or updates are forced"),
and `-U` forces the next attempt to contact the repository again. The wrapper
does not bypass artifact resolution, skip any test, change native binaries or
alter result reports.

## Tests

Run `bash releng/swt-receiver/test-retry-transient-maven-503.sh`. It feeds the
wrapper three verbatim outputs from `tests/data/`:

| Fixture | Maven | What happened |
|---|---|---|
| `run-45457730531-attempt2-maven-429.log` | 3.9.11 | `maven-dependency-plugin:3.8.1` could not be resolved after HTTP 429 on an `[ERROR]` line. The fixture's first line is a tolerated `[WARNING]` 429 |
| `run-44992024856-maven-3.10.1-429-extension.log` | 3.10.1 | the `tycho-build` extension could not be resolved after `GET request ... failed (attempt 3 of 4) due to status 429` for its POM; no `[ERROR]` line carries a status code |
| `run-45108815443-maven-3.10.1-503-parent.log` | 3.10.1 | `[ERROR] ... HTTP Status: 503` for the Eclipse parent POM, with `Retry-After header with seconds value 60` |

Replayed through master's wrapper, each runs once and logs "non-transient
failure; no retry". This PR's first two heads (3cbccf35 and 50fe6de6) did the
same for the two Maven 3.10.1 outputs.

It checks:
- **Retried, success on the second attempt:** all three real outputs, the
  `[ERROR]` 502 and 504, metadata locks and the Tycho 4.0.12 lookup.
- **Retried, then failed closed after every attempt:** persistent 429 (Maven 3.9
  and 3.10 extension), 503, locks and Tycho lookup.
- **Exactly one run:**
  - compile failures and test errors;
  - 401, 403, 404 (including Maven 3.10's `HTTP Status: 404`) and checksum
    failures;
  - Maven's "Could not find artifact" wording, alone, next to the real `[ERROR]`
    429, and next to the Tycho 4.0.12 line;
  - a `[WARNING]`-only 429 next to Tycho's "error code 13" or surefire's
    "timeout in the fork";
  - a `[WARNING]`-only 503 next to an Ant failure;
  - Maven 3.10 transport warnings for `tycho-build` next to a failure that names
    a different artifact, or next to Tycho's "error code 13";
  - a 429 on the `maven-dependency-plugin` POM that Maven recovered ("Downloaded
    from central"), then a "Blocked mirror" resolution failure of
    `org.foo:bar:jar:1.0` (the review's case A);
  - the same without the recovery line, where only the clause rule applies;
  - the same with a clause that names the plugin, where only the recovery rule
    applies;
  - 429 warnings for `tycho-build`'s POM next to a failure line with `HTTP
    Status: 500` for it;
  - a 429 for `com.x.org.foo:bar:1.0` next to a resolution failure of
    `org.foo:bar:1.0`.
- **Backoff:** exponential growth with jitter, the per-wait cap, an exactly
  honoured `Retry-After: 7` and Maven 3.10's 60 s Retry-After, and the total
  budget. A test hook (`M3_RETRY_SLEEP_RECORD`) records each wait instead of
  sleeping.
- **Configuration:** invalid values, including `08`, `09` and `010`, are refused
  with exit status 64 before the command runs.

This does **not** fix the separate Tycho Guice `MavenTargetLocationFactory`
binding problem in the native Grid workflow.
