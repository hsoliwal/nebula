<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula startup scroll requests

A populated Grid can ignore `setTopIndex` before its first paint because its native vertical scrollbar still reports hidden. Requests following row growth or resize can similarly observe stale ranges. The earlier diagnostic PR retained both that defect and a rejected simple refresh: refreshing native visibility can call application listeners that dispose the Grid or its scrollbar before control returns.

This packet corrects the existing Grid owner in the default viewport mode. It retains a pending request as both the original `GridItem` facade and its index, refreshes stale scrollbar geometry synchronously when allowed, and applies the request only while that same row remains valid and visible. A newer callback request replaces the older one. Insertions, disposal, visibility changes and frozen scrollbar planes cannot silently redirect a request to a different row at the same index.

The shared scrollbar update guards reentry and native callback boundaries. Geometry can settle through at most three bounded attempts; if callbacks keep invalidating it, the owner retains the obsolete state for repaint. Native scrollbar disposal retires only its matching original proxy, preserving an application-supplied replacement. A final scrollbar thaw admits the latest retained request. The original-mode branch remains a compatibility path with its existing behavior.

## Exact source and dependencies

The three receiving files are `Grid.java`, the new twenty-method `GridTopIndexStartup_Test.java`, and the existing Grid test POM. The POM registers the new class alongside the earlier selection tests and requests deterministic GTK scrollbars for its full native image comparison. No previous source history, recipe, test or receipt is replaced.

The declared target revision is the accessible diagnostic PR #246 commit `7c2c52dde4226265b5492b3904f188371ec39201`. Local qualification started at `d2e08231f221f2023ead1b2fbaa6d9d3f0c57229`. Existing Grid/POM Git blobs and the new test's absence were independently verified at the immutable remote commit; no whole-tree equivalence is inferred. The exact preimage/postimage SHA-256 values and source identities are in `crate.json` and the SDK manifests.

This canonical successor stacks on [Synexia PR #12129](https://github.com/hsoliwal/com.synexia/pull/12129), commit `893118ced3ba18f97633c24c8090d1aa83b4113c`. It reuses the unchanged canonical Java/text snapshot owners, five-method SDK harness and thin Results receiver. Their original engine source custody is `892e3f6c51d78bc0948e8a8cf736055b4c70eafa`.

The native GTK disposal lane requires the separately qualified SWT ScrollBar callback-lifetime correction from that preceding canonical packet. Proof compiles real Nebula sources against the admitted SWT owner overlay and records the exact class/resource/native-library hashes. This source-authoring dependency introduces no Synexia library into the product runtime.

## Executed proof

| Lane | Exact scope | Result |
| --- | --- | --- |
| Canonical SDK | Named OpenRewrite recipes and all five existing qualification tests | 5 pass; zero failures, errors or skips |
| Product receiver | Three source/POM postimages from actual bound SDK Results | 3 changes, then 0 at fixed point |
| Native negative control | Same twenty new tests on the unchanged Grid source, default mode | 19 failures; 1 passing control |
| Native corrected default mode | Twenty startup, callback, mutation, freeze, identity and sparse-virtual tests | 20 pass; zero skipped methods |
| Native original-mode compatibility | Same twenty methods with explicit original-mode expectations | 20 pass; preserves original startup behavior |
| Adjacent selection contract | Received Grid with earlier occurrence, selection-list and cell-selection tests | 11 pass; zero skipped methods |

The default lane proves that a ten-million-row virtual Grid can admit a request for row nine million while retaining at most three materialized facades, preserving the requested facade and invoking no SetData callback for geometry. This is a bounded regression assertion, not an independent memory or throughput budget.

The image regression uses real `GC(display).copyArea` captures of the complete 390 by 280 control under GTK3/X11 with `GTK_OVERLAY_SCROLLING=0`. The unchanged default source differs from its equivalent settled request by 11,463 pixels. The received default source has zero differing pixels. PNG pairs remain in the canonical evidence. The original-mode pair separately checks its unchanged behavior and must not be described as implementing the default-mode correction.

The source-bound driver compiles all 105 core Grid widget Java sources before and after and checks unchanged public Grid JVM descriptors. Ten JFace adapter sources and the full reactor are outside that compile. Each native method has one terminal record; duplicate, missing, ignored and assumption-skipped methods fail the proof. `verification/run_native_proof.py` and its adjacent Java runner retain the exact canonical proof source. Earlier executed tooling and evidence remain separate under `evidence/target`.

Exploratory adjacent-suite comparisons are retained separately: 98 legacy methods had thirteen baseline failures and six after the correction; fourteen adjacent plane/paint methods retained the same two failing methods. These are explicit remaining failures, not a clean full-suite result. They are not substituted for the source-bound twenty-method acceptance proof.

## Replay

Use Java 21 and the declared Maven dependencies. From the canonical crate:

```sh
mvn -B -o -ntp verify
python3 receive.py --target /absolute/path/to/nebula --check
python3 receive.py --target /absolute/path/to/nebula
python3 receive.py --target /absolute/path/to/nebula --check
```

In an exported authoring mirror, add `-Drecipe.owner.root="$PWD/sdk-owner"` to Maven; the receiver discovers that local owner directory. Every target is admitted before serial writes. Only the actual SDK Results with complete current source bindings and a successful five-test report can be received. Unrelated source drift fails closed.

Run the native driver's `--help` for the before/received target roots, admitted SWT overlay and classes, native libraries, Java home, JUnit and Hamcrest paths. Launch it under a caller-owned Xvfb display with `GTK_OVERLAY_SCROLLING=0` and use a new output directory. The driver refuses to overwrite prior evidence. SDK qualification and real widget acceptance are separate gates.

## Rights and remaining gates

Original Eclipse source notices remain in all exact preimages and postimages. The new target regression offers Apache-2.0 OR EPL-2.0 under its own header. Canonical recipe, receiver, proof tooling and metadata use Apache-2.0. Both relevant licence texts and the scoped notice travel with the authoring mirror.

The executed native platform is GTK3/X11. Full Maven/Tycho, JFace adapters, Windows, Cocoa, GTK4, all-widget correctness and combined SWT/Nebula CPU, heap, native-memory and image acceptance budgets remain unqualified. The six legacy and two adjacent failures remain visible for subsequent work.
