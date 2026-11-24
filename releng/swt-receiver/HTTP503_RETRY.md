<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Fail-closed SWT receiver transient-failure retry

`retry-transient-maven-503.sh` runs the exact supplied command. It retries the
whole command, a bounded number of times, only for a recognized transient
condition. Everything else fails on the first attempt, and so does a transient
condition that persists through every attempt. (The file name predates the
429/502/504 support and is kept because the workflows call it.)

## What is detected

A repository response counts only on a line that reports the failure, that is
a line starting with `[ERROR]`. Maven also logs many tolerated responses and
continues, for example `[WARNING] Could not transfer metadata ... status code:
429, reason phrase: Too Many Requests (429)`. Run 45457730531 has 44 such
warnings per attempt, some in a step that ended in BUILD SUCCESS. Those lines
never make a failure retryable, so a Tycho "process returned error code 13" or
a surefire "There was a timeout in the fork" next to them still runs once.

**Transient: retried.** These checks run in this order, after the non-transient
checks below:

| Condition | Detected strings |
|---|---|
| HTTP 429 | an `[ERROR]` line with `status code: 429`, `Too Many Requests (429)` or `HTTP/<version> 429` |
| HTTP 503 | an `[ERROR]` line with `status code: 503`, `Service Temporarily Unavailable (503)` or `HTTP/<version> 503` |
| HTTP 502/504 | an `[ERROR]` line with `status code: 502`, `status code: 504`, `Bad Gateway (502)`, `Gateway Timeout (504)` (or `Gateway Time-out (504)`), or `HTTP/<version> 502` or `504` |
| Eclipse metadata lock | anywhere in the output: `Could not acquire read lock for 'metadata:org.eclipse:` or `... write lock ...` |
| Tycho 4.0.12 lookup | anywhere in the output: `Could not find artifact org.eclipse.tycho:tycho-maven-plugin:jar:4.0.12 in central` |

**Non-transient: never retried, even next to a transient line.**

| Condition | Detected strings |
|---|---|
| Product failure (checked first) | anywhere in the output: `Compilation failure`, `There are test failures`, `Tests run: N, Failures: M` with M > 0, `The method ... is undefined for the type` |
| Authentication, not found, checksum | an `[ERROR]` line with `status code: 401`, `403` or `404`; `reason phrase: Unauthorized`, `Forbidden` or `Not Found`; `HTTP/<version> 401`, `403` or `404`; `Checksum validation failed`; `ChecksumFailureException`; `checksum verification fail` or `checksum validation fail` |
| Missing artifact | an `[ERROR]` line with Maven 3.9's `Could not find artifact <coordinates> in <repository> (<url>)`, for anything but the Tycho 4.0.12 case above |

**Anything else** is not retried.

## Backoff

`M3_HTTP503_ATTEMPTS` sets the number of attempts (1-5, default 3).
`M3_HTTP503_RETRY_DELAY_SECONDS` sets the base delay (default 10).

Between attempts the wrapper waits:
- the numeric `Retry-After` value, if the captured output contains one. Maven
  3.9 does not log that header today, so in practice the next rule applies;
- otherwise `base x 2^(attempt-1)` plus a random jitter of up to half that,
  capped at `M3_RETRY_MAX_DELAY_SECONDS` (default 300).

The total sleep is bounded by `M3_RETRY_MAX_TOTAL_SECONDS` (default 900). A wait
that would exceed the remaining budget fails closed instead of sleeping.

The three seconds settings must be decimal numbers from 0 to 9999 without a
leading zero; bash arithmetic would read `08` as invalid octal. Any other value
is refused with exit status 64 before the command runs.

Waits with the current settings:

| Call site | Settings | Waits |
|---|---|---|
| SWT assembly and AllTests steps in `terminal-vnc-contracts.yml` | 5 attempts, base 15 s | 15-22, 30-45, 60-90, 120-180 s (at most about 5.6 minutes) |
| Terminal tests in `terminal-vnc-contracts.yml` | 3 attempts, base 12 s | 12-18, 24-36 s (at most 54 s) |
| `maven.yaml`, `codeql.yml` | defaults: 3 attempts, base 10 s | 10-15, 20-30 s |

Each retry re-runs the original command. Every wrapped call passes `mvn -U`.
That matters for HTTP 429: Maven caches a failed transfer ("resolution is not
reattempted until the update interval ... has elapsed or updates are forced"),
and `-U` forces the next attempt to contact the repository again. The wrapper
does not bypass artifact resolution, skip any test, change native binaries or
alter result reports.

## Tests

Run `bash releng/swt-receiver/test-retry-transient-maven-503.sh`. It feeds the
wrapper the verbatim Maven Central 429 output of nebula run 45457730531, attempt
2 (`tests/data/run-45457730531-attempt2-maven-429.log`). That output is where
`maven-dependency-plugin:3.8.1` could not be resolved and the previous wrapper
logged "non-transient failure; no retry". Its first line is a tolerated
`[WARNING]` 429.

It checks:
- **Retried, success on the second attempt:** an `[ERROR]` 429, 502, 503 or 504,
  metadata locks and the Tycho 4.0.12 lookup.
- **Retried, then failed closed after every attempt:** persistent 429, 503,
  locks and Tycho lookup.
- **Exactly one run:**
  - compile failures;
  - 401, 403, 404 and checksum failures;
  - Maven's "Could not find artifact" wording, alone, next to the real `[ERROR]`
    429, and next to the Tycho 4.0.12 line;
  - a `[WARNING]`-only 429 next to Tycho's "error code 13" or surefire's "timeout
    in the fork";
  - a `[WARNING]`-only 503 next to an Ant failure.
- **Backoff:** exponential growth with jitter, the per-wait cap, an exactly
  honoured `Retry-After`, and the total budget. A test hook
  (`M3_RETRY_SLEEP_RECORD`) records each wait instead of sleeping.
- **Configuration:** invalid values, including `08`, `09` and `010`, are refused
  with exit status 64 before the command runs.

This does **not** fix the separate Tycho Guice `MavenTargetLocationFactory`
binding problem in the native Grid workflow.
