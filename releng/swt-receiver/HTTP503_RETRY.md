<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Fail-closed SWT receiver transient-failure retry

`retry-transient-maven-503.sh` runs the exact supplied command. It retries the
whole command, a bounded number of times, only when the captured Maven output
shows a recognized transient condition:

- an upstream HTTP **429** (rate limited), **502**, **503** or **504** response
  from a Maven repository;
- an Eclipse Maven metadata lock timeout;
- the specific published Tycho 4.0.12 extension's transient Maven Central lookup
  failure.

It never retries:
- a compiler or test failure;
- an authentication (401/403), not-found (404) or checksum failure, even when a
  429/5xx line appears in the same output;
- any other unrecognized error.

Those fail the job on the first attempt, and so does a transient condition that
persists through every attempt. (The file name predates the 429/502/504 support
and is kept because the workflows call it.)

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

With the receiver workflow's settings (5 attempts, base 15 s), the waits are
15-22 s, 30-45 s, 60-90 s and 120-180 s. That is at most about 5.6 minutes in
total.

Each retry re-runs the original command, which passes `mvn -U`. That matters
for HTTP 429: Maven caches a failed transfer ("resolution is not reattempted
until the update interval ... has elapsed or updates are forced"), and `-U` forces
the next attempt to contact the repository again. The wrapper does not bypass
artifact resolution, skip any test, change native binaries or alter result
reports.

## Tests

Run `bash releng/swt-receiver/test-retry-transient-maven-503.sh`. It feeds the
wrapper the verbatim Maven Central 429 output of nebula run 45457730531, attempt
2 (`tests/data/run-45457730531-attempt2-maven-429.log`). That output is where
`maven-dependency-plugin:3.8.1` could not be resolved and the previous wrapper
logged "non-transient failure; no retry".

It checks:
- **Retried, success on the second attempt:** 429, 502, 503 and 504, metadata
  locks and the Tycho 4.0.12 lookup.
- **Retried, then failed closed after every attempt:** persistent 429, 503,
  locks and Tycho lookup.
- **Never retried:** compile failures and 401/403/404/checksum failures,
  including a 404 next to a 429.
- **Backoff:** exponential growth with jitter, the per-wait cap, an exactly
  honoured `Retry-After`, and the total budget. A test hook
  (`M3_RETRY_SLEEP_RECORD`) records each wait instead of sleeping.
- **Configuration:** invalid values are refused before the command runs.

This does **not** fix the separate Tycho Guice `MavenTargetLocationFactory`
binding problem in the native Grid workflow.
