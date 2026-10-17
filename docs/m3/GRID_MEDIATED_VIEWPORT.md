# Nebula Grid as a mediated viewport

Status: leaf G1 (row extent index) and leaf N1 (native look) implemented; recorded differential, acceptance, native-look pixel differential and scroll-paint check pass on the hidden test desktop. See "Status" below.

## Goal

`org.eclipse.nebula.widgets.grid.Grid` keeps its public contract (`Grid`, `GridItem`, `GridColumn`,
`GridColumnGroup`, events, `SWT.VIRTUAL` / `SWT.SetData`) and paints the same pixels as the stock Grid, while:

- every question about rows (top row, rows on screen, row at a point, row origin, scroll bar values, next and
  previous visible row) is answered by integer index structures in O(log n), not by walking `GridItem`s;
- only the rows on screen are visited and painted (O(window) per paint, also when a collapsed subtree with a
  million hidden rows sits on screen);
- row state moves from `GridItem` fields to parent-owned lanes (bit flags, packed ints, columnar arrays), so that
  a `GridItem` becomes a facade (later leaves);
- O(n) work (index rebuilds after structural changes, bulk selection, sort) leaves the UI thread (later leaves).

Grid is custom drawn. There is no native control to mediate, so the viewport lives in Grid's own layout and
paint code: the mediator is `GridViewport` (package-private), shaped like SWT's win32 `TreeViewport`.

## Startup modes

Grid reads the SWT-wide switch through its own copy of `WidgetMode`
(`org.eclipse.nebula.widgets.grid.internal.WidgetMode`, identical to SWT's class except for the package line
and the logger name; Nebula cannot import SWT internals):

| Property | Effect on Grid |
|---|---|
| none, or `-Dswt.widget.mode=viewport` | viewport (default) |
| `-Dswt.widget.mode=original` | stock Grid code paths for every widget |
| `-Dswt.widget.mode.Grid=original` | stock Grid code paths for Grid only |
| `custom`, `skija` | not implemented by Grid (it is already custom drawn); fall back to `viewport` |

The mode is read once, when `GridViewport` initializes (`GridViewport.ENABLED`). In `original` mode no
`GridViewport` is created and every hook is skipped (`viewport == null`).

## Receiver inventory (hsoliwal/nebula master 7cf7a59f)

The fork's Grid already carries viewport work from earlier leaves; G1 builds on it and does not replace it.

| Exists | Where |
|---|---|
| flat `SWT.VIRTUAL` rows are sparse; only touched rows have a `GridItem`; heights answered by `heightDelta` | `GridVirtualItemList` (PRs #85, #98) |
| row-span index for distant viewports | `GridRowSpanIndex` (PRs #148, #232) |
| sort/filter/grouping/pinning model, column window, item residency, check-state lanes | `model.*`, PR #151, `widgets/grid/m3/grid-model-20261013` |
| blit scroll through `Canvas.scroll` for uniform flat rows; rows outside the paint clip skipped | `Grid.blitBody`, `Grid.paintRows` |

O(n) walks that remain on the UI thread for **tree Grids and Grids with differing row heights** (full item
list, `items`), measured by reading the code at 7cf7a59f:

| Method | Walk | Cost |
|---|---|---|
| `getTopIndex` (tree) | counts visible items from row 0 up to the scroll selection | O(top) per scroll step |
| `setTopIndex` | counts visible items before the target | O(index), also for flat Grids |
| `getRowRange (start, end)` | sums heights of visible items | O(n) for `getGridHeight`, i.e. every `updateScrollbars` / `computeSize` |
| `getRowRange (start, height, …)` | steps item by item, skipping hidden items | O(window + hidden) |
| `getItem (Point)` | steps from the top row, skipping hidden items | O(window + hidden) |
| `getOrigin` (cell bounds, editors) | steps from the top row to the item | O(distance) |
| `paintRows` | skips hidden items one by one | O(hidden) per paint when a collapsed subtree is on screen |
| `getNextVisibleItem` / `getPreviousVisibleItem` | skip hidden items one by one | O(hidden) |

## Leaf G1: row extent index

`GridRowExtents` (package-private) is a dense index over item positions `0 … items.size () - 1` with two
Fenwick (binary indexed) trees:

- count lane: 1 when the item is visible (all ancestors expanded), else 0;
- extent lane: `height + 1` (row plus the 1-pixel separator) when visible, else 0.

With `P (k)` = total extent of the first `k` visible rows, every row question is one or two O(log n)
descents:

| Question | Answer |
|---|---|
| scroll selection of item `i` | `rank (i)` = visible items before `i` |
| item at scroll selection `k` | `select (k)` (Fenwick descent on the count lane) |
| height of rows `s … e` | `P (rank (e) + 1) - P (rank (s)) - 1` |
| rows that fit `h` pixels from row `s` downwards | smallest `m` with `P (rank (s) + m) - P (rank (s)) >= h` (descent on the extent lane; `forceEndCompletelyInside` steps back one row when the last row would overflow by more than its separator) |
| the same upwards (`inverse`) | largest rank `j` with `P (j) <= P (rank (s) + 1) - h` |
| row at pixel `y` below the top row | largest rank `j` with `P (j) <= P (rank (top)) + y` |
| origin of item `i` relative to the top row | `extentBefore (i) - extentBefore (top)` |
| next / previous visible item | `select (rank (i))` / `select (rank (i + 1) - 1)` |

The answers reproduce the stock loops exactly, including their edge cases (a scroll selection past the last
visible row keeps the selection as the index; a first row taller than the available height is still taken;
`getRowRange` errors on hidden start or end rows).

Maintenance:

- built lazily, in O(n), the first time a question needs it (a flat Grid with uniform rows never builds it);
- `GridItem.setVisible` and `GridItem.setHeight` update one position in O(log n) (expand/collapse is
  O(k log n) for k changed rows, as stock is O(k));
- an item appended at the end (how trees are normally built) is O(log n);
- an insertion in the middle or a removal marks the index stale; it is rebuilt on the next question. The index
  also checks its size against `items` on every use, so a structural change that bypasses the hooks can never
  produce a stale answer.

Hooks (one delegation each, inserted by `M3InsertStatements`, recipe in `grid-viewport/recipe`):
`Grid (DataVisualizer, Composite, int)`, `getTopIndex`, `setTopIndex`, both `getRowRange`, `getItem (Point)`,
`getOrigin`, `paintRows`, `getNextVisibleItem`, `getPreviousVisibleItem`, `newItem`, `removeItem`,
`setItemHeight`, `GridItem.setVisible`, `GridItem.setHeight`. Flat `SWT.VIRTUAL` Grids keep their existing
sparse path unchanged in G1.

Rule kept and rule not yet kept:

- O(window) per paint and per scroll step on the UI thread: kept for every walk listed above.
- "No O(n) on the UI thread": the lazy rebuild after a structural change in the middle is O(n) on the UI
  thread (stock Grid already shifts every following item, `increaseRow`, on such an insert). Moving the rebuild
  to a worker needs the visible flags and heights as parent-owned lanes (a snapshot the worker can read), which
  is leaf G2.

## Leaf N1: native look (user requirement 2026-10-16)

In the viewport mode Grid looks like the platform's own list and tree (SWT `Table` and `Tree`) on the same
platform. `-Dswt.widget.mode.Grid=original` keeps the stock Grid look; in the viewport mode
`-Dorg.eclipse.nebula.widgets.grid.look=classic` keeps Grid's classic renderers, and every renderer stays
selectable per column (`GridColumn.setCellRenderer`, `setHeaderRenderer`) and per Grid
(`setEmptyCellRenderer`, `setEmptyColumnHeaderRenderer`, `setFocusRenderer`).

- `GridNativeLook`: metrics measured once per display and font from hidden native controls through their public
  API (`Table.getItemHeight`, `getHeaderHeight`, `TableItem.getTextBounds`, the same for `Tree`, check-box
  tables and child rows): row height (list 19, list with check boxes 20, tree 18 for Segoe UI 9 at 100 %),
  header height 24, text insets, tree indent and expander width. A Grid row plus its one-pixel separator is
  the native row. Text is centred like the native controls (`(row - font height) / 2`).
- `GridNativeTheme`: the platform's visual styles. On Windows the same `uxtheme` calls SWT's own `Table` and
  `Tree` use (`OpenThemeData` for the monitor DPI, `DrawThemeBackground`, `DrawThemeText`,
  `GetThemePartSize`), reached reflectively because the Grid bundle is platform independent: Explorer list and
  tree items (selected, selected without focus), the list's column lines (`LVP_COLUMNDETAIL`), header items and
  header text, tree glyphs, check boxes. Without visual styles (other platforms, or the internals missing) the
  renderers fall back to system colours.
- Measured native rules (win32, Windows 11, 100 %): the list draws its column lines under the selection; the
  selection spans the columns in the list and the whole client width in the tree; the list draws a dotted focus
  rectangle one pixel inside the focused row, the tree only when keyboard cues are shown (`WM_QUERYUISTATE`);
  a disabled list keeps its header text black and paints its rows on the widget background; check boxes are
  SWT image-list images, so their anti-aliased edges are blended twice (emulated from renderings on white and
  black); the end ellipsis leaves two pixels of the reported text area.
- Renderers: `GridNativeCellRenderer`, `GridNativeHeaderRenderer`, and in `GridNativeLook` the empty header,
  empty cell and focus renderers. They are installed by two more hooks (Grid constructor, `GridColumn.init`).

Proof: `GridNativeLook_Test`, pixel differential "Grid vs native Table" and "Grid (tree rows) vs native Tree":
same rows, column widths, selection and focus, captured with `Control.print` on the hidden test desktop, both
PNGs saved on a mismatch.

| Scene | Differing pixels (300 x 260) |
|---|---|
| rows (header, text, column lines) | 0 |
| selected row, focused | 0 |
| selected row, not focused | 0 |
| focused row without selection | 0 |
| check boxes (checked and unchecked) | 40: the vertical scroll bar thumb (x 291-292, y 190-209); Grid's bar counts one more row than the list's, and the Grid's bar values are its API |
| disabled, selected row | 0 |
| tree rows, expanded and collapsed glyphs | 0 |
| tree, selected child, focused | 0 |
| tree, selected collapsed root, not focused | 0 |

Not covered yet: hot-tracking (needs real mouse input, not available on the hidden desktop), sort indicators,
grid lines (`setLinesVisible (true)` draws Grid's line colour; Grid shows lines by default, the native list
does not, so the scenes set both explicitly), images, column groups, row headers, footers, other DPI scales,
GTK and Cocoa.

## Tests

- `GridViewportDifferential_Test` (recorded differential): seeded scripts of public API operations run against
  a stock Grid and a viewport Grid side by side in one shell. Both Grids are created from the same compiled
  classes, loaded twice by isolating class loaders, once with `swt.widget.mode.Grid=original` and once with
  `viewport` (the mode is read at class initialization). With `-Dgrid.differential.oracle=<classes dir>` the
  oracle is any other Grid build instead, for example upstream Nebula. Every step records
  answer | vertical and horizontal bar | selection | cell selection | focus item and cell | events, and each seed
  ends with a pixel comparison (`Control.print`). Any difference fails with the operation log, which is the
  reproduction.
- `GridViewport_Test` (acceptance, skipped when the viewport is not active): 1,000,000-row tree, bounded paint
  and scroll cost with a collapsed million-row subtree on screen, index answers against a brute-force walk.
- `GridRowExtents_Test` (pure index): random visibility, heights, appends, inserts against a brute-force model.

## Status (win32, JDK 21, SWT 3.135.0, javac + JUnitCore, 2026-10-16)

Build: the Grid bundle, its tests and upstream Nebula Grid `5d0bcbb9` (oracle) compiled with javac against the
official SWT 3.135.0 win32 jar (`grid-viewport/build_grid.sh`); a full Tycho build was not run (memory).
GUI runs: `ui_lock.sh` + `run_guarded.sh` (`grid-viewport/run_tests.sh`).

| Gate | Result |
|---|---|
| `GridRowExtents_Test` (4 tests, brute-force walk, 1,000,000 positions) | 4/4 pass |
| `GridViewportDifferential_Test`, oracle = same build in `original` mode, 8 seeds x 160 steps, 4 configurations (row/cell selection, uniform/differing heights) | pass: every step and the printed pixels of every seed identical; the index was rebuilt 235 times (DEBUG log) |
| same, candidate = mutant (the `forceEndCompletelyInside` step-back removed, `build_mutant.sh`) | detected: seed 0 step 22, vertical bar maximum 106 (stock) vs 105 |
| same, oracle = upstream Nebula `5d0bcbb9` (`-Dgrid.differential.oracle`, pixels advisory) | seeds 0-3: all 160 steps identical; pixels differ only in the column header text row (172-224 pixels, y 8..16), a difference between this fork and upstream in both modes. Seed 4 step 148: the fork shows an empty text for item `r66` where upstream shows `r66`, in both modes (the original-mode run of the same seed agrees with the viewport); a pre-existing fork defect, not a viewport one, recorded for a separate fix |
| `GridViewport_Test` (acceptance), 1,000,000-item tree (10,000 roots x 99 children, per-item data visualizer) | pass: built in 6.7-10.6 s; median scroll step near the top / bottom: row geometry 359 / 341 us, paint 10.6 / 8.9 ms (position independent); bar maximum = visible rows; geometry answers after expanding a distant root exact |
| same, 100,000-item tree vs stock Grid at distant rows (expand, row height, showItem, selection, bar drag, Page Up) | pass: answers, state and printed pixels identical |
| `ScrollProbe` (out-of-tree probe), 1,000,000 items, 60 scroll steps near the bottom | viewport: geometry 0.378 ms, paint 10.1 ms per step; original: geometry 29.1 ms, paint 8.9 ms per step (77x) |
| Grid suite (34 classes, 590 tests), `original` mode | 151 failures: the baseline of this harness (the suite expects the Tycho/OSGi test runtime) |
| Grid suite, `viewport` mode | 152 failures: the same 151 plus `GridCheckLanes_Test.checkStateFollowsModelRowsAndCheckAllIsFilterAware`, a flat `SWT.VIRTUAL` pixel test that never reaches the index (every hook is off for the sparse list); rerun 3 times per mode it failed 3/3 (viewport) and 2/3 (original) with alternating hashes |

### Hidden test desktop (user requirement 2026-10-16: no mouse moves, no focus theft)

From this point every UI run is on the invisible desktop `WinSta0\m3-ui-tests` (`hidden_java.sh`,
`grid-viewport/run_hidden.sh`) with the official SWT 3.135.0 jar from Maven Central (md5
`ff30a6787dbca882392b48299274eb88`, the same bytes as the jar used before). No test moves the cursor; captures use
`Control.print`.

| Gate (hidden desktop) | Result |
|---|---|
| `GridViewportDifferential_Test`, oracle `original` mode, 8 seeds x 160 steps (classic look on both sides) | pass: every step and the printed pixels identical |
| `GridViewport_Test`, 1,000,000 items | pass: built in 9.2 s; median scroll step near the top / bottom: geometry 437 / 463 us, paint 5.0 / 4.1 ms; 100,000-item stock-vs-viewport pixels identical |
| `GridViewportDifferential_Test`, oracle = stock Nebula Grid `5d0bcbb9` on the official jar (pixels advisory) | seeds 0-3: all 160 steps identical; printed pixels differ in 173, 174, 172 and 2,697 pixels (seeds 0-2: the header text row; seed 3: header and body); seed 4 step 148: the fork's item-text defect (see Findings) |
| `GridNativeLook_Test` (Grid vs native Table and Tree, 9 scenes) | pass: 0 differing pixels in 8 scenes, 40 (documented scroll-bar thumb) in the check-box scene |
| `GridScrollPaint_Test` (right after a scroll, before the event loop: the window shows the old or the new rows, never a mix or a row without data), 22 steps each on a 20,000-row `SWT.VIRTUAL` Grid and a tree Grid | viewport: pass (new rows at once in 21 and 15 steps, the old rows until the event loop in 1 and 7); original: pass (14 and 11 / 8 and 11). No blank or mixed window was observed |
| Grid suite (34 classes, 590 tests): `original` / viewport with `look=classic` / viewport with the native look | 152 / 152 (the same set) / 153 failures. The extra one is `GridColumnWindow_Test.columnGroupsKeepTheFullHeaderWalkAndPaintIdentically` (column window on vs off with column groups paint differently); an out-of-tree probe (`ColumnWindowProbe`) shows the same on/off mismatch also with the classic look at other scroll positions, so it is recorded as a column-window issue to fix in its own leaf |

Findings (pre-existing, both modes, recorded for their own fixes):

- The fork's default data visualizer (`GridRowAddressedItemDataVisualizer`) re-maps its whole
  `HashMap<Integer, List>` on every row insert or removal (`RowStore.remap`), so building a tree is quadratic:
  40,000 items take 65 s in `original` mode (JFR: `RowStore.remap` and `HashMap.put` are the hot frames). The
  acceptance test therefore keeps data in the items (`GridItemDataVisualizer`, the upstream default). Columnar
  lanes for item data are leaf G2.
- Against upstream `5d0bcbb9`, seed 4 step 148 shows an item text lost by the fork in both modes (see above).
- Grid reads the real cursor position after a scroll (`refreshHoverState`), so a differential must show both
  Grids at the same screen position: the harness gives each Grid its own shell at the same bounds, and parks
  the cursor before pixel captures. Side by side, seed 2 diverged once on a click (hover state).

## Nebula bug harvest (lead directive 2026-10-17: every Nebula bug gets fixed)

[`grid-viewport/NEBULA_BUGS.tsv`](grid-viewport/NEBULA_BUGS.tsv) (working copy K:/m3-work/runs/bugs/nebula-BUGS.tsv) lists every reachable Nebula Grid report: the open GitHub tracker
(13 open issues, one about Grid) and the archived Eclipse Bugzilla (181 Grid-matching reports, closed in bulk
2021-07-05; the 46 that were not FIXED/DUPLICATE triaged one by one). Per row: source, fork status, priority
(P1 crashes, P2 behaviour, P3 cosmetic/platform, E enhancements), plan. P1 state, verified by
`GridArchivedBugs_Test` and `GridIssue700_Test` on both modes and on stock `5d0bcbb9` classes:

| Report | State |
|---|---|
| GH #700 (getCellSize crash via GridEditor during removal) | fixed earlier, both modes; kept unconditional, since the stock crash needs a real `WM_SIZE` editor layout the hidden harness cannot deliver (no observable mode difference) |
| Bugzilla 455474 (disposing an item colour kills the whole paint with `ERROR_INVALID_ARGUMENT`) | fixed, default mode (D23): `GridItem` reads a disposed colour or font as unset, so renderers fall back to the defaults, as the native Table keeps painting; the original mode keeps stock's crash |
| Bugzilla 218708 (header click on an empty Grid crashed the focus renderer) | not reproducible on stock `5d0bcbb9`; the test documents it and guards the Selection event |
| Bugzilla 454575 (`disposeAllItems` kept stale state) | verified fixed on the fork and on stock: selection, focus and scroll reset like `removeAll` |

P2 next: 377617 (0-width wrapping header), 259087 (pack ignores the group text), 382376 (pack ignores the
column span), 238601/218713 (no column Selection event with cell selection), 202517, 360798, 330224, 273923,
385351 (verify).

## Regression triage (coordinator item 1, 2026-10-16)

`grid-viewport/REGRESSION_TRIAGE.tsv` lists every failing test of the Grid suite (34 classes plus the
viewport tests, 597 tests) with its failure, root cause category and action; all runs on the hidden test
desktop with the official SWT 3.135.0 jar.

| | `original` mode | viewport mode |
|---|---|---|
| before (first hidden-desktop run) | 152 failures | 153 |
| after item 1 | 133 failures, all in the four RAP-derived classes | 139: the same 133, plus `GridCheckLanes_Test` and five `GridColumnWindow_Test` cases (capture artifacts, see items 2 and 3) |
| after items 2 and 3 | 112 failures, all in the four RAP-derived classes | the same 112 |

Method: the four legacy classes (`Grid_Test`, `GridItem_Test`, `GridColumn_Test`, `GridColumnGroup_Test`,
455 tests, taken from the RAP project in Nebula bug 477992 and excluded from upstream's build) were run
against the stock Nebula Grid `5d0bcbb9` and against this fork: both fail exactly the same 135 tests, so none
of them is a fork regression.

Fixed in this leaf:

| Category | Test | Root cause | Fix |
|---|---|---|---|
| environment | 6 image tests, 2 mockito tests | test images were not copied next to the test classes; mockito 1.8.4 needs objenesis and, on JDK 21, `--add-opens java.base/java.lang` | harness (`build_grid.sh`, `run_hidden.sh`) |
| fork defect | `GridBlitScroll_Test` | the blit scroll invalidated only the uncovered part of its source rectangle; a move of more than half the body left stale rows on screen until another repaint (a likely cause of the "blank rows" the user saw) | the exposed strip is invalidated and painted at once (`update`), as native list views do after scrolling their bits |
| fork defect | `GridGCProxy_Test` | `GridGCStateDAG` composed pending affine operations in the wrong order (SWT's `Transform.multiply` prepends) | composition order |
| fork defect | `GridPinnedRows_Test` | `GridGCProxy.close` left the caller's GC in advanced (GDI+) mode after restoring the line attributes, so the planes painted after the body (pinned rows, header, footer) drew text with GDI+ pixels | `close` ends in the caller's GC mode |
| upstream defect | (`GridColumnWindow_Test` flakes, differential seeds) | `refreshHoverState` read the global cursor position after every scroll even when the cursor was not over the Grid (another window on top, another desktop), giving hover feedback that depended on where the user's mouse was | hover follows the cursor only while it is over the Grid |
| test bug | 7 `GridVisibleRangeSupport_Test` | `setUp` declared a local `support` that shadowed the field under test | renamed the local |
| test bug | `GridPaintPreparation_Test` | expected no span owner while the owner still spanned rows in a hidden column | the test clears both spans |
| test bug | `GridScrollbarSettle_Test` | the two compared Grids overlapped; the covered one never painted and kept the bars of its previous size | one shell per Grid |
| test robustness | pixel helper (`GridRowPermutation_Test.pixels`) | `Control.print` sometimes returns an all-black capture on the hidden desktop | capture retried, then the Grid's own paint; the hash covers the client area |
| viewport | `GridScrollPaint_Test` | now copies the window's pixels right after a scroll (no paint) and after settling, with steps of more than half a page | catches the blit defect above (verified against the build before the fix) |

Open (next leaves): the 133 legacy failures (105 expect RAP theme metrics or RAP semantics, 19 argument
checks to settle per Javadoc, 9 `clear` cases where Nebula keeps the text although its Javadoc says the
attributes are reset); `GridColumnWindow_Test` (column-window paint differs from the full walk by one
column at some positions, intermittently; coordinator item 2); `GridCheckLanes_Test` (intermittent
capture on the hidden desktop).

## Fork defects of coordinator items 2 and 3 (2026-10-16)

| Defect | Root cause | Fix | Measured |
|---|---|---|---|
| quadratic build with the default data visualizer | `GridRowAddressedItemDataVisualizer` kept one `HashMap<Integer, List>` per kind of value (11) and re-keyed every stored row on each row insert or removal | one `RowIndex` per Grid (sorted row positions with a stable id each) and id-addressed `IdStore`s: an insert or removal shifts the positions after it by one int increment, an appended row is an O(log n) search, no rehash and no boxing | 40,000-item tree (`BuildProbe 400`): 65 s before, 0.34 s after (`original`), 0.60 s (viewport); 1,000,000 items: 8.0 s / 8.3 s |
| lost item text (differential seed 4 step 148 against stock) | `GridItem.remove(child)` cleared the child's data by its old row index after `Grid.removeItem` had already removed the row, so it cleared the row that moved into its place (reproduced in 12 of 12 seeds by `TextLossProbe`: the item after a disposed child lost its text) | a row-addressed visualizer is not cleared again there | no text lost in seeds 0, 2, 4, 9; the differential against stock Nebula `5d0bcbb9` answers identically in all 8 seeds x 160 steps |
| column-window paint mismatch (`GridColumnWindow_Test`) | not a Grid defect: `Control.print` (PrintWindow) can return the window's last presented surface instead of a fresh paint; the two "differing" prints were one fresh and one stale capture. Tracing both paint paths showed identical column positions | the Grid pixel tests capture the Grid's own paint (its paint listener into an image) | 3 probe runs x 26 positions: 2 to 4 mismatches with `Control.print`, 0 with the Grid's own paint; the suite passes the class in both modes in every run since |
| header text differs from stock (172 to 2,697 pixels) | `GridGCProxy.close` left the GC in GDI+ mode, so the header (painted after the body) drew its text with GDI+ (fixed in item 1) | as item 1 | against stock Nebula `5d0bcbb9` (client area, the Grid's own paint): 7 of 8 seeds pixel-identical; seed 5: one 244-pixel line under the empty header area in cell-selection mode, where stock paints the empty header one pixel taller than the fork's header plane clip (PR #128) |

Contract fixes in the same leaf (Javadoc-promised or SWT-convention argument handling, both modes):
`Grid.clear*` reset the visualizer's per-column data; `showItem`/`showColumn` reject null, disposed and foreign
arguments; `isSelected (GridItem)` and `setSelection (int[])` reject null as documented;
`getNextVisibleItem (null)` and `getPreviousVisibleColumn (null)` answer the first item and the last column
(the counterparts already accepted null); `setItemCount` on a tree Grid detaches removed rows from their own
parent list (it threw an `IndexOutOfBoundsException`); `GridItem.setImage (Image)` and the header and footer
font setters reject disposed resources. Legacy failures: 133 -> 112 in both modes.

## Stock pixel parity of the header overflow (2026-10-16)

| Defect | Root cause | Fix | Measured |
|---|---|---|---|
| seed 5 against stock `5d0bcbb9`: one 244-pixel line at `y = headerHeight` right of the last column | the default empty column header renderer fills `height + 1`. Stock paints the header first and the rows over it, so that pixel row stays wherever the first row paints nothing (an unselected empty cell of an enabled Grid). The fork paints the body first and the header clipped to its plane (PR #128), which cut that row | plane Z0 in `Grid.onPaint`: when the damage reaches the first body pixel row (`Grid.HEADER_OVERFLOW` = 1), only the empty column header renderer is painted there, under the body; column header renderers stay out of body damage (the PR #128 contracts in `GridPaintPreparation_Test` hold) | stock differential: 8 of 8 seeds answer and paint identically, candidate `viewport` and candidate `original` (`-Dgrid.differential.candidate`), with pixels strict (no advisory) |
| blit scroll and the overflow row | a move down carries the overflow row into the body; a move up covers it with row pixels | `Grid.blitBody` repaints that row too (a move up repaints it in its own paint, so the damage does not span the body) | `GridScrollPaint_Test.blitScrollKeepsTheHeaderOverflowInPlace`: the window right after every blit and once settled equals the Grid's paint; a build without the repaint fails it (3 tests fail in `original` mode) |
| blit scroll and the classic tree branch lines (`original` mode, classic look) | `BranchRenderer` takes the phase of its dotted lines from the absolute pixel row (`bounds.y % 2`); a blit by an odd number of pixels shifted the dots of every moved row (329 pixels) | no blit for an odd move while a visible tree column paints the classic dotted lines | `GridScrollPaint_Test.blitScrollKeepsTheBranchLinePhase`: before the fix it failed at the first step, after it 2 of 9 steps blit (the even moves) in `original` mode, 8 of 9 with the native look |

Suite after this leaf (598 tests, both modes): 112 failures, the same legacy list as before, 0 elsewhere.
Non-claim: a custom header renderer other than the empty column header renderer that paints below the header
stays clipped to the header (stock shows such pixels wherever the rows paint nothing).

## Upstream defects and the legacy-test port (rule 12, 2026-10-17)

The `original` mode stays behaviourally identical to stock Nebula Grid `5d0bcbb9`; every other mode
(`viewport`, the default) fixes genuine stock defects: behaviour that contradicts the API's own Javadoc,
crashes, data loss, clipped or wrong rendering. `GridViewport.UPSTREAM_FIXES` gates each fix; each one is a
row of [`grid-viewport/UPSTREAM_DEFECTS.tsv`](grid-viewport/UPSTREAM_DEFECTS.tsv) with the stock file:line and
the Javadoc it contradicts (D1-D18). The fixes of items 1-3 that changed stock behaviour (argument checks,
clear, setItemCount on a tree, disposed resources, hover from the global cursor) now apply in the default
mode only.

Header and footer heights (D14, D15): a header or footer content change (text, image, font, word wrap,
renderer, the Grid font, auto height, a removed column) only marks the header or footer; the heights are
measured once before their next use (the next paint or height query), so many changes cost one measure.
`recalculateHeader` measures every column again (stock reused the cached measures); while the Grid paints,
the wrapping renderers' own calls to it are not needed and return at once. A group header is measured at its
columns' width (stock: the width it was last painted with, zero before the first paint), and a wrapping
header counts its wrapped lines only with auto height, as `setAutoHeight` documents.

The native look measures what the native controls grow with: a taller header font or image makes the header
taller, a taller cell font or image the rows; a word-wrapping header wraps (the native header has one line).

The legacy RAP-derived classes (`Grid_Test`, `GridItem_Test`, `GridColumn_Test`, `GridColumnGroup_Test`) are
ported: expectations that were RAP theme metrics or RAP semantics assert the Nebula contract (row pitch
`getItemHeight() + 1`, the renderers' measures, scroll bars sized at paint, explicit tree columns, typed
listeners instead of Mockito 1.8.4 mocks); where the default mode fixes a listed defect the test asserts the
fix, and the stock behaviour when `GridTestUtil.fixesUpstreamDefects()` is false (the original mode, and stock
Nebula, which has no `GridViewport`).

| Gate (hidden desktop, 2026-10-17) | Result |
|---|---|
| legacy classes (455 tests): `original` mode / `viewport` mode / stock Nebula `5d0bcbb9` classes | 455 / 455 / 454 pass (stock lacks the fork-only `virtualMaterializedItemCount` of one test) |
| Grid suite (599 tests, legacy classes included) | 0 failures in both modes (before: 112 legacy failures) |
| `GridViewportDifferential_Test`, oracle stock `5d0bcbb9`, pixels strict | 8 / 8 seeds answer and paint identically, candidate `viewport` and candidate `original` |
| same, header/footer scene (11 steps: header font and image, footer visibility, font and text, set and undone) | candidate `viewport`: exactly the 5 listed steps differ (D15 x3, D17 x2) and each does; candidate `original`: 0 steps differ |
| `GridNativeLook_Test` | unchanged: 0 pixels in 8 scenes, 40 (scroll-bar thumb) in the check scene |

Lead decisions of 2026-10-17, default mode (D19-D21): after `Grid.setFont` the default item height is measured
again with the new font and applied to the rows (rows sized one by one keep their heights); `GridColumn.pack`
fits a visible footer (stock's footer renderer even measured the header text); the Dispose event of an item comes
first, while the item still has its row and data, as SWT's `Widget.dispose` sends it before releasing (stock
removed both first). `removeAll` keeps disposing first to last (ordering, not a defect). Verified: legacy
classes 457 / 457 / 456 (original / viewport / stock classes); Grid suite 603 tests, 0 failures in both modes;
stock differential 8/8 seeds and both scenes unchanged.

## Header renderers that paint below their bounds (2026-10-17)

Stock Nebula paints the header first and the rows over it, so whatever a header renderer paints below its
bounds shows wherever a row paints nothing (right of the columns, below the last row, under every column once
the Grid has no rows). The fork paints the rows first and the header clipped to its plane (PR #128).
`IRenderer.getOverflowBelow()` (a default method answering 0) lets a renderer declare the pixel rows it paints
below its bounds; the default empty column header and top-left renderers and the native look's empty header
declare 1 (they fill `height + 1`). Plane Z0 paints, for damage that reaches those rows, exactly the renderers
that declare rows (in header order, clipped to the strip), under the rows; renderers that declare none stay out
of body damage (the PR #128 contracts of `GridPaintPreparation_Test` hold). A blit scroll repaints the declared
strip. Both modes (stock parity, not a defect fix).

| Gate | Result |
|---|---|
| `GridViewportDifferential_Test.customHeaderOverflowPaintsAsStock`: a custom empty column header and a custom column header painting 3 rows below their bounds; a column narrowed, scrolls, an expand, every item removed; answers and pixels per step against stock `5d0bcbb9` | identical in all 7 steps, candidate `viewport` and candidate `original`; the build before this leaf differs at step 1 (48 pixels, empty header rows 2-3) and step 6 (222 pixels under the column) |
| `GridScrollPaint_Test.blitScrollKeepsACustomHeaderOverflowInPlace` | the window right after every blit and once settled equals the Grid's paint |

Test robustness on the multi-slot hidden desktops: `GridPinnedRows_Test` captures the Grid's own paint (it used
`Control.print`, which can return a stale surface); `GridNativeLook_Test` waits until the control really has
the focus and prints until two prints agree; `GridScrollPaint_Test` settles only after the Grid's drag frame
ran (`Grid.dragFramePending`). Before these, the build of the previous leaf failed the same three tests on the
new desktop slot.

## Leaf G2: the item list is a counted tree (2026-10-17)

In the viewport mode a non-`SWT.VIRTUAL` Grid keeps its items in `GridRowTree`, a height-balanced tree ordered
by row whose nodes are lane slots (one per item: left, right, parent, subtree size, subtree visible rows,
subtree pixel extent, own extent, height, item). It is the Grid's `items` list and also answers every row
question of G1 (`GridRowIndex`, also implemented by `GridRowExtents`):

| Operation | Stock / G1 | G2 |
|---|---|---|
| insert or remove a row anywhere | `ArrayList` shift plus `increaseRow`/`decreaseRow` over every later item, O(n); index rebuilt on the next question, O(n) | O(log n): one path rebalanced, no other item touched |
| row of an item (`getRowIndex`, `indexOf`) | a field kept by those loops (`indexOf`: O(n) scan) | O(log n): the slot's path to the root |
| item of a row | O(1) | O(log n) |
| rank, select, pixel prefix (G1 questions) | Fenwick O(log n) after an O(n) rebuild | O(log n), never rebuilt |
| visibility or height of one item | O(log n) | O(log n) |

So no structural change costs O(n) on the UI thread any more (the plan to rebuild the G1 index on a worker
over a snapshot is not needed). The original mode keeps the stock `ArrayList`. Donors (FOSS_REUSE_DECISION.tsv):
the AVL lanes of the SWT fork's `VirtualTreeSiblingIndex` (PR #197, itself from Synexia's
`LongOrderStatisticMultiset`) are adapted; the position descent, the parent lane and the two aggregates are
new. Fenwick sidecars, frozen rank/select, sorted maps, `TreeList` and `FourColorTree` were checked and do
not fit.

| Gate (hidden desktop) | Result |
|---|---|
| `GridRowTree_Test`: a tree Grid, 1,500 random inserts (roots and children), removals, expand/collapse, item and default heights; every item's row, the item of every row, extents, rank, select, pixel prefix and row-at-pixel against a linear walk | pass |
| `InsertProbe`, 1,000,000 items (10,000 roots x 99 children), a child inserted and disposed in the middle, per step | viewport before G2: insert 27.0 ms, first row question after it 32.2 ms, dispose 54.8 ms, paint 37.3 ms; after: 1.7 ms, 0.7 ms, 0.4 ms, 9.1 ms (the original mode on the same run: 50.8, 31.3, 60.7, 38.7 ms; the machine was loaded, 60 % CPU) |
| Grid suite (604 tests) | 0 failures in both modes |
| stock differential (`5d0bcbb9`, strict pixels) | 8 / 8 seeds, both scenes, candidate `viewport` and `original` |

What is left of an insertion (1.7 ms at 1,000,000 items) is stock work per new item: a sizing GC for the item
height (`Grid.estimate`), the parent's child list and the data visualizer.

## Leaf G3: selection membership in O(1) (2026-10-17)

Stock Nebula keeps the selected rows in an `ArrayList<GridItem>` and the selected cells in an `ArrayList<Point>`;
every membership test scans them: the paint asks per row (`selectedItems.contains`) and per cell
(`selectedCells.contains`), a range selection asks per added row or cell, and deselecting removes by scan. A
large selection makes painting O(window x k) and a range selection O(k^2) (`selectAllCells` over 20,000 rows x
4 columns: 3.5 s).

In the viewport mode:

- `GridSelectionList` (rows): the stock list semantics and order (getSelection returns items in selection
  order), but every item marks its own position in the list (a field of `GridItem`), so `contains`, `add` and
  `remove` are O(1); a removal leaves a hole closed when holes outnumber items or a position other than the first
  is read (O(1) amortized). `getSelectionIndices` reads the rows from the row tree (G2): O(k log n) for few
  items, one O(n) walk for many.
- `GridCellSelectionList` (cells): the stock list plus a count per cell value, so `contains` is O(1); it keeps
  its own points (added points are copied, `getCellSelection` hands out copies) and the column removal that
  shifts cells in place reindexes. `addToCellSelection` asks `contains` (the stock equality test, one scan in the
  original mode) instead of its own scan.
- The column selection keeps one entry per column (stock added one per selected cell and scanned them per
  painted header).
- Focus stays an item reference (already O(1) and stable under inserts); no positional focus is needed.

The native look draws no focus rectangle over a selected focused tree row, as the native tree (Explorer theme)
does with keyboard cues shown or hidden; `GridNativeLook_Test` now sets the cues explicitly (WM_CHANGEUISTATE),
which made the scene deterministic (before, it failed or passed with the test desktop's keyboard history). The
change is a recipe (`recipe/META-INF/rewrite/m3-nebula-grid-native-focus.yml`, second pass UNCHANGED).

| Gate (hidden desktop) | Result |
|---|---|
| `GridSelectionList_Test` (20,000 random adds, removals by item and index, reads, clears, bulk adds vs `ArrayList`) and `GridCellSelectionList_Test` (30,000 steps incl. duplicates, removeAll, removeIf, in-place shifts) | pass |
| `CellSelectProbe`, `selectAllCells`, n rows x 4 columns | 5,000 rows: original 264 ms, viewport 29 ms; 20,000 rows: original 3,510 ms, viewport 219 ms; 100,000 rows (400,000 cells): viewport 213 ms (original quadratic, not run) |
| `SelectProbe`, 200,000 rows all selected, wall median | deselect + select one row: original 1.48 ms, viewport before G3 0.59 ms, after 0.05 ms; getSelectionIndices: 11.4 / 14.3 / 6.4 ms; selectAll: 0.48 / 4.3 / 2.6 ms (the list marks each item) |
| Grid suite | 607 tests (viewport run listed three classes more than once: 609), 0 failures in both modes |
| stock differential (`5d0bcbb9`, strict pixels) | 8 / 8 seeds, both scenes, candidate `viewport` and `original` |
| `GridNativeLook_Test` | 3 / 3 runs pass (all scenes 0 pixels but the documented check-box residual) |

DOOP note (rule 18): the selection position and the row-tree slot are two ints per `GridItem`; the item's
text, colours, fonts, height, visibility and expansion still live in `GridItem` fields. Moving them into lanes of
the row tree is the next DOOP leaf (the data visualizer already holds the cell data).

## Leaves

| Leaf | Scope | Acceptance |
|---|---|---|
| G1 | row extent index; mediated top row, row ranges, row at point, origin, paint skip, next/previous visible | differential identical; paint cost independent of hidden rows |
| G2 | done (see Leaf G2): the item list is a counted tree with row lanes (row, visible rows, pixel extents), O(log n) insert, remove, row of item and every row question; no rebuild | differential; no O(n) on the UI thread after insert/remove |
| G3 | done (see Leaf G3): O(1) row and cell selection membership, one column entry per column, focus by item | differential; probes |
| G4 | tree Grids with `SWT.VIRTUAL` children: sparse child facades through the same lanes, `SetData` once per row | differential vs non-virtual tree; SetData counts |
| G5 | variable-height blit scroll using the index's pixel delta; row-rect damage | pixels vs stock after scroll sequences |
| G6 | column metrics: prefix sums for column x, binary search for column at x, `showColumn` O(log c) | differential on column order/width/visibility |

## Non-claims

- No GTK or Cocoa runs on this host.
- No accessibility change (the accessible children are still all items).
- Scroll metrics are in rows; more than 2^31 rows is not supported by `ScrollBar` and is not claimed.
