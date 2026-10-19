# GridRowExtents failed-rebuild product oracle (2026-10-17)

**Promotion: HOLD / expected RED before the canonical recipe is executed.** No old source is overwritten.

- Source under review: `widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridRowExtents.java`, exact master Git blob `ec2aab7b1bc4c7c4e96e602d0645309f779af58f`.
- Source path introduced here: `widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridRowExtentsRebuildFailure_Test.java` (only new test, never edits an existing Java owner).
- Root cause: `rebuild(count, extentAt)` clears and mutates Fenwick arrays, but if `extentAt` throws or returns a negative value the method exits before resetting the historical `valid=true` bit from an earlier build. `isValid(count)` then lies about partially mutated arrays.
- Oracle: after a prior valid index, inject the callback failure at each of the four row positions. Assert the original exception propagates, `isValid(4)` becomes false, the next successful rebuild restores exact rank and extent answers, and a negative row extent is also fail-closed.
- Only approved implementation is the existing Synexia OpenRewrite atom `M3InsertStatements`, composed into `NebulaGridExtentRebuildFailClosed` from [Synexia PR #12079](https://github.com/hsoliwal/com.synexia/pull/12079), stacked on its existing viewport catalogue recipe owner. It inserts `valid = false;` at the start of `GridRowExtents.rebuild`. **No direct production-file edits** are authorized.

## Qualification (must actually run before the implementation is promoted)
1. Verify incoming product blob SHA and canonical recipe branch/head; fail on drift.
2. Maven/OpenRewrite source recipe test and two passes (first CHANGED, second UNCHANGED), exact one-file diff.
3. Run `GridRowExtentsRebuildFailure_Test` and existing `GridRowExtents_Test` under Nebula's original Tycho/test profiles.
4. Full Grid native original/viewport, SetData/reentry, variable height, million-row, screen-image pixel acceptance and resource-gate checks against the official SWT reference, with exact-head receipts and original fallback untouched.
5. Existing API, row identity, native event ordering, disposal, attribution and failure exception must be identical except internal index validity after an aborted rebuild.

This draft contributes the **product oracle only**. It will initially be RED against the current master code; test execution and native builds were not run at authoring.
