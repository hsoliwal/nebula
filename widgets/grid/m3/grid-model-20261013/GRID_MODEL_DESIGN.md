<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula Grid model: sort, filter, grouping, pinning, column window (2026-10-13)

Ledger rows (Synexia #10864, `docs/M3-SCALE/runs/20261013-all-widget-viewport/WIDGET_IMPLEMENTATION_LEDGER.tsv`):

| wave | concept | target | this leaf |
|---|---|---|---|
| 1 | grid_filter_sort (score 20) | `org.eclipse.nebula.widgets.grid.model.RowPermutation` | packed sort permutation + `long[]` filter bitset + compacted visible array |
| 2 | grid_grouping_aggregation (16) | `org.eclipse.nebula.widgets.grid.model.RowGrouping` | packed group index over the permutation, packed aggregates |
| 3 | grid_pinning (12) | `Grid` fixed plane | pinned rows/columns painted as a fixed plane of the same Grid |
| 3 | table_header (12, Grid part) | `GridColumn`, `GridColumnGroup` | column window (column virtualization) + recycled per-frame scratch |

Order of work: the four rows serially, each one committed, measured and recipe-sealed before the next.
Donor-first evidence: `FOSS_REUSE_DECISION.tsv` (25 rows), `DONOR_FILES.sha256.tsv`, `THIRD_PARTY_NOTICES.md`.

## 1. Inventory of the receiver (hsoliwal/nebula master eab2a65c)

| fact | where |
|---|---|
| Grid item storage is `List<GridItem> items`; with `SWT.VIRTUAL` and no child items it is the sparse `GridVirtualItemList` (sorted `int[] indices` + `GridItem[] values`, only materialized rows hold objects) | `Grid.java` field `items`; `GridVirtualItemList.java`; `Grid.usesSparseVirtualItems()` |
| A virtual row is materialized by `GridVirtualItemList.get(i)` -> `Grid.materializeVirtualItem(i)`; `SWT.SetData` fires once per item with `event.index = row index` | `GridItem.handleVirtual()` |
| `setItemCount` on the sparse list changes the logical size and disposes only materialized rows past the end | `Grid.setItemCount(int)` |
| Grid has no sort or filter; `GridColumn.setSort(int)` only draws the indicator. Applications sort their data, then recreate items or call `clearAll` | `GridColumn.setSort`, snippets `GridSnippet*` |
| Paint: `onPaint` -> `paintBody` -> `paintRows(cols, fixed, ...)` walks every display-ordered column from the first one for every painted row, then breaks when `x > clientArea.width` | `Grid.paintRows` (the `for (final GridColumn column : cols)` loop) |
| Per painted cell: `item.getCellSize(col, extraFill)` scans `getColumnOrder()` to find the visual index (O(columns) per cell) and returns a new `Point`; `selectedCells.contains(new Point(col, row))` allocates a `Point` | `GridItem.getCellSize(int,int)`; `Grid.paintRows` |
| Left fixed (frozen) columns already exist as the Z2 plane painted over the scrolling body | `GridColumn.setFixed`, `Grid.getFixedGridColumns()`, `FixedGridColumns`, `paintBody` Z2 |
| Visible column index cache `startColumnIndex/endColumnIndex` exists for `GridVisibleRangeSupport`, computed by a linear scan | `Grid.getStartColumnIndex()`, `GridViewportProjection.startColumnIndex` |
| Paint planes and damage: `GridPaintDAG.plan(...)` (BODY, FIXED, HEADER, FOOTER, OVERLAY), clip per plane through `GridGCProxy` | `Grid.onPaint` |
| Deferred scroll metrics: `scrollValuesObsolete` dirty bit consumed by the next `onPaint` | `Grid.onPaint`, `Grid.setScrollValuesObsolete()` |

## 2. Laws applied (user directives of 2026-10-13)

1. Bits, ints and bitwise masks; reusable arrays; objects only when a caller's API asks.
2. The viewport and the widget are one control; any helper surface is created `SWT.NO_FOCUS` (this leaf adds none).
3. Internals may change; public and protected API results may not. New API is additive.
4. SWT and Java/JNI primary; the web is RAP + GWT. The model package is pure Java, has no SWT import and follows the GWT rules (no `int` overflow reliance, no `String[].clone()`, no `Objects.checkIndex`, `int` preferred; the filter bitset is `long[]` as the ledger specifies, which GWT emulates correctly).
5. No work while the widget is not effectively visible: dirty bits, one catch-up.
6. Transient items (law of 2026-10-13, see section 8): `GridItem` rows are created lazily by the Grid and are thin handles; state lives in the Grid's packed arrays; cold payloads may be destroyed and recreated; identity is pinned only when the caller declares it retains items; behaviour is chosen by construction flags.

## 3. RowPermutation (wave 1, grid_filter_sort)

Package `org.eclipse.nebula.widgets.grid.model` (new, exported). Pure Java; usable by Grid now and by Table/Tree later (it knows nothing about widgets).

State (all reused across operations):

| field | meaning |
|---|---|
| `int modelCount` | number of model rows (the caller's data rows) |
| `int[] order` | sort order, view position -> model row; identity is not stored (`SORTED` flag clear) |
| `long[] bits` | filter: bit `m` set = model row `m` passes; absent when unfiltered (`FILTERED` flag clear) |
| `int[] visible`, `int viewCount` | compacted view: `visible[v]` = model row at view position `v` (order, filtered) |
| `int[] inverse` | model row -> view position or -1, built lazily on the first `viewIndex(m)` after a change (`INVERSE_VALID` flag) |
| `int flags` | `SORTED`, `FILTERED`, `INVERSE_VALID`, `VIEW_VALID` bits |
| `int version` | bumped (masked to 31 bits, no overflow reliance) on every change |

Operations:

- `sort(RowComparator)`: fills `order` with 0..n-1 and sorts it in place with `IntPermutationSort` (fastutil `IntArrays.quickSort` port, allocation-free). The comparator is extended with the model-row tie-break (TanStack `rowA.index - rowB.index`), so the order is total and the result is unique: it equals a stable sort, and any correct implementation (Java, JNI, GWT) must produce the identical array.
- `sortByIntKeys(int[] keys, boolean descending)`: packed key column; same tie-break.
- `setOrder(int[] order, int count)`: caller-supplied permutation (server-side sort), validated (each row once).
- `setFilter(RowPredicate)`: one predicate call per model row, writes the bitset words (64 rows per `long`).
- `setFilterBits(long[] bits)`: caller-supplied bitset (copied into the reused array).
- Compaction: walks `order` (or the identity) and appends rows whose bit is set into `visible`. With no sort it walks the bitset words with `Long.numberOfTrailingZeros`, so the cost is proportional to the passing rows plus `n/64` words.
- Allocation budget: the first sort allocates `order` (and `visible`) once; every later sort/filter of the same model size allocates nothing. Measured with `ThreadMXBean.getThreadAllocatedBytes` in the scale test.

JNI (optional): `RowPermutationNative` may sort `int` keys and scan the bitset in C. It loads only when the library is present and its self-check equals the Java twin on a fixed corpus; on any failure it is disabled (fail closed). The Java twin is the authority and is what GWT compiles. Status is reported per measurement run (`JNI=ON|OFF|NOT_BUILT`).

### Grid integration

Additive Grid API:

- `setRowPermutation(RowPermutation)` / `getRowPermutation()`
- `refreshRowPermutation()`: call after changing the permutation.

Contract:

- Supported on flat `SWT.VIRTUAL` Grids (the sparse item list). A Grid with child items, or without `SWT.VIRTUAL`, rejects it with `SWT.ERROR_INVALID_ARGUMENT` (the non-virtual Grid holds data inside items; reordering item objects is a separate leaf).
- Item index = view index. `getItemCount()` = `viewCount`; `getItem(i)`, `indexOf`, `getTopIndex`, selection indices and `SWT.SetData`'s `event.index` are all view positions, exactly as if the application had sorted/filtered its data and called `setItemCount` + `clearAll` (the classic SWT virtual pattern). The model row of a view index is `getRowPermutation().modelRow(index)`.
- Row recycling: materialized `GridItem`s stay bound to their view slot and are cleared (`SetData` fires again on next use) instead of being disposed and re-created; slots past the new count are disposed as `setItemCount` does.
- Selection and focus follow model rows across a refresh (TanStack/AG Grid behaviour). The model row of every materialized slot is kept in a packed `int[]` beside the sparse list (state in the Grid, not in `GridItem`), so the mapping survives the caller mutating the permutation before `refreshRowPermutation()`. A selected model row that is filtered out leaves the selection.
- No permutation installed (the default) = the unmodified code path; differential tests prove identical results.
- Not visible: the synchronous part of a refresh is O(materialized + selected) (needed so API reads are correct immediately); scroll metrics, viewport events and repaint are dirty bits caught up once on the next paint.

## 4. RowGrouping (wave 2, grid_grouping_aggregation)

`RowGrouping` reads a `RowPermutation` and an `int[]` group key per model row. Groups are numbered by first appearance in the current view order, so leaf order inside a group is the view order (sorted, filtered) and, when sorted by the group key, groups appear in key order (TanStack `createGroupedRowModel`). A counting pass over dense group ids builds:

| array | meaning |
|---|---|
| `int[] groupKey`, `int[] groupStart`, `int[] groupSize` | per group |
| `long[] collapsed` | bit per group |
| `int[] display`, `int displayCount` | display row -> model row (`>= 0`) or `~group` (group header) |
| `long[] sum`, `int[] min`, `int[] max`, `int[] count` per aggregated column | aggregates over packed `int[]` columns (`aggregate(int[] column)`) |

Grid: `setRowGrouping(RowGrouping)`; item count = `displayCount`; the application fills header rows in `SetData` from `isGroupHeader(i)`, `groupOf(i)` and the aggregates. Same visibility/dirty-bit rules.

## 5. Pinning (wave 3, grid_pinning)

One Grid, one more fixed plane, no second control:

- Pinned rows: `setPinnedRowCount(int top, int bottom)` freezes the first `top` and last `bottom` view rows (Excel freeze panes, Glide `freezeTrailingRows`, TanStack/AG Grid pinned top/bottom). They stay ordinary items (index, selection, hit testing unchanged); `RowPermutation.setPinned(int[] topModelRows, int[] bottomModelRows)` puts chosen model rows at the ends of the view order independent of sort and filter. Paint adds a PINNED_ROWS plane between body (Z1) and the fixed-column plane (Z2).
- Pinned columns: left pinning is the existing `GridColumn.setFixed` plane; right pinning adds the mirror plane at the right edge.

## 6. Column window (table_header, Grid part)

- `GridColumnWindow` (package-private): `int[] left` prefix sums of display-ordered visible column widths, rebuilt when a width, visibility, order or the column set changes (dirty bit set from `GridColumn.setWidth/setVisible`, `Grid.setColumnOrder`, column add/remove).
- `paintRows` and `paintHeader` start at the first column whose right edge is past the horizontal scroll (binary search) and stop at the right edge. Grids with cell spanning keep the full walk (the span manager must see every cell; AG Grid widens the window for spans the same way).
- Recycled scratch: one reused `Point` for the `selectedCells` probe; the visual column index comes from the window instead of a per-cell `getColumnOrder()` scan. `GridItem.getCellSize` keeps its result contract.
- Counters (package-private, read by tests): columns visited and cells painted per frame, materialized rows. Scale test bounds: cells painted per frame <= visible rows x visible columns (+1 overscan each side).

## 7. Tests and measurements

- Existing Grid JUnit suite (`org.eclipse.nebula.widgets.grid.test`) before and after, same classpath (SWT 3.135.0 win32, JFace 3.40.0, JUnit 4.13.2, Mockito 1.8.4).
- Nebula Grid snippets (`examples/org.eclipse.nebula.snippets/.../grid`) compiled against before and after; run under `timeout` with an auto-close timer.
- Differential tests: the unmodified Grid driven the classic way (application-side sort/filter + `setItemCount` + `clearAll`) versus the new Grid driven through `RowPermutation`; compared on item count, every cell text in the visible window, `getItem(Point)`, top index, selection indices, focus, and the painted pixels of the client area.
- `RowPermutation` model tests: against `Integer[]` + `Arrays.sort` (stable) and against a `java.util.BitSet` reference filter, on random keys with duplicates, all orders.
- Scale: 1M rows x 100 columns, `SWT.VIRTUAL`; bounds on materialized items, cells painted per frame, allocation per re-sort.
- Measurements before/after: sort, filter, group time; first paint; vertical and horizontal scroll frame p50/p95/max; Java heap used after GC; process private bytes. The shared `run-matrix.sh` of the SWT showcase is used once it lands; until then the leaf's own bench prints the same columns.
- Platforms: win32 x86_64 run here; GTK and Cocoa NOT_RUN.

## 8. Item residency (mirror of the SWT `ItemResidency` pattern)

Law of 2026-10-13: transient `GridItem` rows are created lazily by the Grid and are thin handles; their state lives in the Grid's bits and packed arrays; under memory pressure cold payloads are LRU-swapped to secondary memory or destroyed and recreated; identity is pinned only when compile-time metadata shows callers retain the object; behaviour is chosen by construction flags.

SWT is building an `ItemResidency` engine; Nebula cannot import SWT internals, so the Grid mirrors the pattern in its own package-private class `GridItemResidency` with the same vocabulary:

| SWT pattern | Grid mirror in this leaf |
|---|---|
| lazy creation by the parent | exists: `GridVirtualItemList` + `Grid.materializeVirtualItem` |
| thin handle, state in parent packed arrays | step taken here: the model row, selection-follow and recycling state of each materialized slot live in packed `int[]`/`long[]` arrays of the Grid (`GridVirtualItemList` parallel arrays, `RowPermutation`), not in new `GridItem` fields. Moving the remaining `GridItem` fields (texts, colours, fonts, images held by `GridItemDataVisualizer`) into packed stores is recorded as the next residency leaf |
| cold payload destroyed and recreated | opt-in: with residency flag `EVICT_COLD` and a capacity, materialized rows that are not visible, selected, focused, pinned or edited are evicted least-recently-used first; they come back through `SWT.SetData` |
| cold payload swapped to secondary memory | NOT_IMPLEMENTED in this leaf (needs the SWT engine's swap contract); virtual rows are recreated from the caller's `SetData` instead |
| identity pinned only when callers retain items | default flag `PIN_IDENTITY` keeps today's behaviour (no eviction, `getItem(i)` returns the same object). A caller whose code does not retain items (compile-time knowledge of its own code, for example a viewer that resolves rows by index) selects `EVICT_COLD` |
| behaviour by construction flags | `Grid.setItemResidency(int flags, int capacity)`, valid only before the first item exists, i.e. at construction time; Nebula cannot mint new `SWT` style bits, so the flags are Grid constants |

## 9. Further laws relayed on 2026-10-13 and their place in this leaf

| law (user, via coordinator) | Grid mirror | status in this PR |
|---|---|---|
| Per-row booleans are bit lanes in the Grid (expanded, has-children, selected, focused, hovered, enabled, checked/grayed, dirty, filtered-visible, group-collapsed); group expand/collapse are range bit operations | `model.RowBitLanes`: one `long[]` per lane, word-wise range set/clear, OR/AND/ANDNOT with a mask, `bitCount`, row insert/remove shifting. The filter lane is `RowPermutation`'s bitset; group-collapsed is a lane of `RowGrouping` | engine IMPLEMENTED with a `BitSet` differential test; Grid lanes listed per wave below |
| Check state captured in bits; check-all, filter-aware check-all and counts are word-wise ops; 1M-row check-all timing | checked/grayed/checkable lanes per column, owned by the Grid for `SWT.VIRTUAL` rows | planned after wave 3 (needs the `GridItemDataVisualizer` storage swap) |
| Selection packed: row ranges + `long[]` switched by density, per-row column bitsets for cells, filter-aware by AND | `RowBitLanes` selected lane + range pairs | planned after wave 3; this PR keeps `List<GridItem>` selection and makes it follow model rows across sort/filter |
| All states reproducible after a row scrolls out and is released (lanes, cell text/font/colour/image ids, spans, heights, group state, open editor text/caret/selection); round-trip test with identical API values and pixel hash | needs the residency eviction (section 8) plus packed per-cell payload stores | planned; the round-trip test is the acceptance gate of that leaf |
| Scrollbars managed for viewport peak performance: range scaling for huge counts with a logical anchor, one scroll per frame (wheel/thumb coalescing), blit scroll + exposed strip, damage-only paint, optional overlay scrollbar plane chosen by a bit; flicker count during resize/scroll storms | Grid already routes through `IScrollBarProxy` and paints only damage planes (`GridPaintDAG`); this leaf removes the redundant `redraw()` that `GridItem.setText` issues during a `SetData` callback inside paint | planned as the viewport-chrome leaf |
| Pixel-exact edges at 100/125/150/200 % zoom (floor start, ceil end, shared rounding for adjacent cells), blit clipped to the body, scrollbars composited in the same frame; zero changed border/grid-line/scrollbar-edge pixels during scroll and resize | one box model (next row) drives every rectangle | planned as the viewport-chrome leaf |
| One box model: outer bounds, border, scrollbar tracks, inset, header/pinned planes, body viewport; one packed int per side, computed once per change, used by hit-testing, `getClientArea`, scroll clip, damage and scrollbar rectangles; rings repaint only on their own state change (mirror of the SWT `Scrollable` ordering) | `GridBoxModel` (package-private) | planned as the viewport-chrome leaf |
| Layout: auto-size and row-height measurement over visible rows + overscan only, the rest dirty by bits; resize re-lays out visible cells and anchors the top row; `computeSize` never measures every row; scrollbar oscillation hysteresis; resize-sweep metrics (cells measured per frame, ms per frame, scrollbar toggles) | measurement window from the row window and the column window (section 6) | planned as the viewport-chrome leaf |
