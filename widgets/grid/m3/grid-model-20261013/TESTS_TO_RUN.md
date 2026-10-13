<!-- SPDX-License-Identifier: EPL-2.0 -->
# Tests to run (queued while test runs are paused, 2026-10-13)

Runner: `C:/m3-work-c/nebula-grid-build/scripts/` (`compile.sh`, `junit.sh`, `model-test.sh`, `bench.sh`),
JDK 21, SWT 3.135.0 win32, JUnit 4.13.2, Mockito 1.8.4 + Objenesis 3.2, `--add-opens java.base/java.lang=ALL-UNNAMED`,
memory gate (free >= 4 GB, commit headroom >= 8 GB), `-Xmx` <= 2 GB, one JVM at a time.

| # | test | branch / PR | why queued | last result |
|---|---|---|---|---|
| 1 | `GridIssue700_Test` | `m3/grid-issue-700-20261013` (#153), merged into #151 | new, written during the pause | compile-checked only |
| 2 | `GridPinnedRows_Test` (5) | #151 | plane background fill changed after the last run | before the fill: 4 pass, 1 fail (`unscrolledPinnedTopRowsPaintLikeTheUnpinnedGrid`, overlay text pixels) |
| 3 | `model.RowStateLanes_Test` (3) | #151 | new (containers mirrored from SWT #122) | compile-checked only |
| 4 | `model.HeapGrowth_Test` (4) | #151 | slot width widened to 1..64 | compile-checked only |
| 5 | `model.RowPermutation_Test`, `RowBitLanes_Test`, `RowGrouping_Test` | #151 | regression after the ports | 14 + 2 pass (before the ports) |
| 6 | `GridRowPermutation_Test` (7), `GridColumnWindow_Test` (5), `GridSetDataRedraw_Test` (2) | #151 | regression after pinned plane + #700 merge | 7 / 5 / 2 pass before |
| 7 | existing suite: 11 classes, 509 tests, pre (eab2a65c) vs post, failure-set diff | #151, #153 | after all changes | pre 134 failures, dev 134, identical set (before pinned plane) |
| 8 | snippets compile (`examples/org.eclipse.nebula.snippets/.../grid`) pre and post | #151 | harness item 4 | compiled: 35 sources, pre rc=0 / post rc=0, 0 errors, 128 classes each; running the snippets (windows) NOT_RUN |
| 9 | bench `GridModelBench` (sources moved to the test fragment `org.eclipse.nebula.widgets.grid.bench`) 1M x 100: pre+Classic, post+Classic, post+Packed, two rounds each, wall + thread CPU | #151 | before/after tables (sort, filter, group, first paint, v/h scroll p50/p95/max, heap, private bytes) | round 1 done for wave 1 (machine load 67-99 %); CPU-time columns added after |
| 11 | `GridModelJobs_Test` (3), `RowPermutation_Test.staticSortedOrderEqualsTheInstanceSort` | #151 | new (batch threshold, worker + UI-thread apply) | compile-checked only |
| 12 | `GridCheckLanes_Test` (1) + bench check-all on 1M rows | #151 | new (packed check lanes keyed by model row, filter-aware check-all/count) | compile-checked only |
| 13 | `GridResidency_Test` (1): reproduction round trip (states set, scroll away so rows are released, scroll back: identical API values, identical print hash, SetData once per row) | #151 | new (opt-in RESIDENCY_EVICT_COLD) | compile-checked only |
| 14 | `GridBodyDefer_Test` (2): one body paint per sort/filter/setItemCount; thumb drag moves the bar at once, body paints <= frames | #151 | new (viewport-only redraw suppression) | compile-checked only |
| 15 | bench `busy_sort` (1M rows, PackedDriver): BusyIndicator.showWhile(Future) worker sort; event-loop lateness p50/max, thumb handle p50/max, body paints | #151 | new | compile-checked only |
| 16 | `GridPrefetch_Test` (2): idle prefetch of rows below the viewport, no SetData when they scroll in | #151 | new (setPrefetchRows) | compile-checked only |
| 17 | `GridVirtualHeights_Test` (1): computeSize of a virtual Grid with differing heights equals the fully materialized Grid and creates <= 2 rows | #151 | new | compile-checked only |
| 18 | `GridBlitScroll_Test` (1): on-screen pixels after blit scrolling equal a full repaint (capture mode DISPLAY_COPY, shell ON_TOP and active) | #151 | new | compile-checked only |
| 19 | `GridScrollbarSettle_Test` (1): arithmetic scroll bar settling equals the measured two-pass algorithm over a 300-step resize sweep (visibility, max, thumb, selection) | #151 | new | compile-checked only |
| 10 | flicker / pixel-exact edge counts at 100/125/150/200 % zoom; resize sweep; box model | #151 | not implemented yet | - |
