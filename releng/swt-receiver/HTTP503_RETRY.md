<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Fail-closed SWT receiver HTTP 503 retry

`retry-transient-maven-503.sh` runs the exact supplied command and retries
up to three times only when the captured Maven output contains a recognized
upstream HTTP 503 response. A compiler/test failure, a missing artifact
(404), a different transport error or persistent 503 still **fails** the job.

The original SWT receiver source revision, Tycho native/JUnit tests, product
API and fail-if-no-tests gates remain unchanged. The wrapper adds `mvn -U`
only to refresh metadata after a transient 503; it does not bypass artifact
resolution, skip any test, change native binaries, or alter result reports.

Test with `bash releng/swt-receiver/test-retry-transient-maven-503.sh`.
It covers one transient 503 followed by success (2 invocations), a hard
compile failure (1 invocation, fails), and persistent 503 (3 invocations,
fails). This does **not** fix the separate Tycho Guice
`MavenTargetLocationFactory` binding problem in the native Grid workflow.
