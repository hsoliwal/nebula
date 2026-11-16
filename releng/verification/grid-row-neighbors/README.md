<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Native GridRowTree — independent headless compatibility lane

No UI replacement exists here. The runner compiles the fork's actual
`GridRowTree.java`, `GridRowIndex.java`, `HeapGrowth.java`, and
`HeapSampler.java` source files using `javac --release 17 -Xlint:all -Werror`.
Only the SWT-dependent `GridItem` is supplied as a test-only structural stub.
The stub is not packaged with any production widget. The test runs 169,397
deterministic reference comparisons including hidden rows, insertions,
removals, batch updates and boundary indices.

To execute: `bash releng/verification/grid-row-neighbors/verify.sh`.

This is a **headless algorithm check**, not full SWT/GTK3, Windows/macOS,
OSGi/Tycho, pixel, selection or accessibility qualification. Those existing
native workflows must remain required. The earlier Tycho workflow failed
before tests due to an unbound `MavenTargetLocationFactory`; the SWT
build workflow failed retrieving an upstream snapshot POM (HTTP 503).
Do not merge or publish a universal compatibility claim based solely on
this lane.
