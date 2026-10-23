# Nebula Grid viewport primitive item-window API — product receiver

**Source authority:** [Synexia #12105](https://github.com/hsoliwal/com.synexia/pull/12105) uses the canonical `M3AddMemberIfMissing` OpenRewrite leaf to generate one new method in the original `Grid`; this receiver PR does **not** hand-patch `Grid.java`.

## Public API under qualification
```java
int[] indices = new int[2];             // reuse across paint/scroll operations
grid.getViewportItemRange(indices, 0); // [inclusive, exclusive)
int first = indices[0], end = indices[1];
```
This is the **item-list position window**, not a guarantee that every position is a visible tree node. No returned `GridItem[]` allocation or new native widget. Caller-supplied array and offset support M3 dynamic arrays, index windows and offsets without new facade or event hierarchy.

It complements, not replaces, `Grid.getVisibleRange()` and `GridVisibleRangeSupport`. Input validation must occur before touching either output slot. Empty list / zero body height produce `[0, 0]`. The method inherits `Grid.getTopIndex()/getBottomIndex()`, original-mode behavior, widget thread/disposal checks, and existing row geometry state.

## Native product oracle

The new `GridViewportItemRange_Test` is JUnit4 for Nebula's Tycho grid-test bundle. It covers empty/zero-height geometry, caller offset and sentinel preservation, identity with existing `getVisibleRange()` on a small Grid, mid-list scrolling, 1,000,000 virtual rows without broad materialization, invalid argument failure atomicity.

**Status:** expected compile RED on existing `master`; `Grid.getViewportItemRange` is intentionally absent until the canonical recipe is executed. No native UI test, full Tycho test, Java/JNI or pixel proof is claimed from authoring.

## Promotion checklist
1. Verify exact current `Grid.java` preimage `161ed50912df44440281dc09a6eb2377c28334f2`. Reject source drift and recover a new receipt if HEAD changed.
2. Run the canonical Synexia Maven/OpenRewrite recipe; first pass CHANGED, second pass UNCHANGED. Inspect one-method-only generated changes and source hash.
3. Compile Grid and its tests with Tycho and run `GridViewportItemRange_Test` twice in fresh processes (viewport/default, original). Neither test should be skipped.
4. Run all existing Grid/row-span/original/native viewport differential plus screens/GC/resource checks. Preserve `GridVisibleRangeSupport` callbacks and original look.
5. Promote to product only on a green exact-head execution. No merges, force pushes, rebases or rewriting old PRs.
