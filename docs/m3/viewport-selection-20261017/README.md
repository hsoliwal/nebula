# Grid selection occurrence correction

This change preserves the original Grid's selection list behavior while keeping the viewport's constant-time membership check. On master `91c474935eb4a06e46716a0188b9082f0af0eda9`, selecting `[a, b, a, c, a]` and deselecting `a` removed its last occurrence, cleared its membership marker and incorrectly removed the row highlight even though other occurrences remained selected. The stock list removes the first occurrence. Read-triggered compaction could also move a live iterator's physical cursor, and its iterator did not support removal.

`GridSelectionList` now owns two primitive occurrence-link arrays. The item's existing marker points at its first occurrence, and removing one occurrence preserves the remaining occurrences. Appending and membership stay O(1). Indexed insertion/replacement and compaction reconstruct the links without changing list order. The JDK `AbstractList` iterator tracks logical positions and provides its tested removal/fail-fast behavior. No public method signature changes.

The canonical transformation is `com.synexia.nebula.SelectionOccurrence20261017` in `hsoliwal/com.synexia`, crate `synexia-openrewrite-recipes/recipe-crates/m3-nebula-selection-occurrence-20261017`. Its five Maven/OpenRewrite SDK tests passed before target application. The receiver installed only SDK-generated results in deterministic per-file order: the existing list, a new native JUnit regression class, and two default Tycho test includes. The second receiver pass changed zero files. The exact preimage/postimage identities and existing engine revision are in [QUALIFICATION.json](QUALIFICATION.json); the new crate's publication revision is supplied by the associated canonical PR and is not implied to exist at the engine revision.

## Executed evidence

| Gate | Result |
| --- | --- |
| Canonical Maven/OpenRewrite SDK tests | 5 passed, no failures/errors/skips |
| Grid widget source compile, Java 21 | 105 source files compiled against official SWT GTK 3.135.0 |
| Untouched master, new regressions | 8 tests, 7 expected failures |
| Corrected viewport, new + existing row/cell selection tests | 11 passed |
| Original mode, same native tests | 11 passed |
| Duplicate-inclusive ArrayList differential | 20,000 seeded operations passed |
| Actual visible selected-row screen comparison | 8,021 pixel differences before; 0 after; original mode 0 |
| 73 existing `Grid_Test` selection methods | 72 passed, one identical baseline/candidate failure, no skipped tests |

The screen test captures twice through the existing `GridSwtScreenshotCapture` using `GC(Display).copyArea`. It checks dimensions, rejects a flat frame, and compares every pixel with the same widget holding the correct remaining selection. The negative control proves that the comparison detects the lost highlight. Reference and corrected images are expected to be identical; their identical hashes are not independent scene evidence.

The additional existing failure is `testShowSelection`, which expects top index 4 and sometimes obtains 0 in the batch. `Grid_Test` leaves shells undisposed, while its redraw helper drains events once; the existing `GridScrollbarSettle_Test` documents that a covered Grid may retain stale bars until it paints. An isolated native state probe on untouched master produced `setTopIndex(12) -> 12`, followed by `showSelection() -> 4`, both before and after explicit GTK settlement. This observation is recorded separately and does not turn the failed legacy batch green. Follow-up investigation must distinguish stale-scrollbar setter behavior from test settlement.

The corrected native run used a 512 MiB maximum Java heap and observed 1.05 seconds wall time, 1.96 CPU seconds and 150,956 KiB maximum resident memory. These are correctness-test observations. Heap usage and native allocation were not measured independently; this is not a speed or memory improvement claim.

The complete Tycho reactor, optional JFace viewer package, Win32/Cocoa, DPI/theme matrix and all other Nebula widgets were not executed in this correction. The broader historical audit is in [../viewport-history-20261017](../viewport-history-20261017).

## Reproduce

Build and test the canonical crate with Java 21/Maven, then run its `receive.py --target <nebula-checkout> --check`. A clean corrected checkout must report `targets=3 changes=0`. The normal Grid test POM now includes both `GridSelectionList_Test` and `GridSelectionOccurrence_Test`; `full-grid-contracts` continues to select all existing Grid tests. For standalone native qualification, compile the Grid widget package and the three selection test classes with SWT 3.135.0, JUnit 4.13.2 and the existing screenshot helper, then run JUnitCore on an actual display or Xvfb. Repeat in a new JVM with `-Dswt.widget.mode.Grid=original`. Set `-Dnebula.selection.evidence.dir=<directory>` to retain native PNGs.

## Canonical recipe and portable source mirror

The exact authoring source is pinned to [Synexia PR #12108](https://github.com/hsoliwal/com.synexia/pull/12108), commit `a14a26e92fb65d0fbcd7f71e7ae1653222ccc439`. The [portable mirror handoff](recipe/SYNEXIA_HANDOFF.json) records every copied file hash and its original licence. The mirror is for Maven/OpenRewrite authoring and verification and adds no product runtime dependency. Local proof commits and remote source identities are recorded in [PUBLICATION.json](PUBLICATION.json).

From the `recipe` directory, qualify with Java 21 and `mvn -B -ntp -Drecipe.owner.root="$PWD/sdk-owner" verify`, then run `python3 receive.py --target /absolute/path/to/checkout --check`. Use `-o` only when the Maven dependencies are already cached.
