# Nebula Grid as a mediated viewport

Status: design (this commit). Leaf G1 follows in the same branch, with its measurements.

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

## Leaves

| Leaf | Scope | Acceptance |
|---|---|---|
| G1 | row extent index; mediated top row, row ranges, row at point, origin, paint skip, next/previous visible | differential identical; paint cost independent of hidden rows |
| G2 | visible, expanded, height and has-children state as parent lanes; `GridItem` getters read the lanes; index rebuild on a worker over a generation-stamped snapshot | differential; no O(n) on the UI thread after insert/remove |
| G3 | selection as ranges + bitset by density (row and cell), focus by position | selection count exact over 1,000,000 rows; differential |
| G4 | tree Grids with `SWT.VIRTUAL` children: sparse child facades through the same lanes, `SetData` once per row | differential vs non-virtual tree; SetData counts |
| G5 | variable-height blit scroll using the index's pixel delta; row-rect damage | pixels vs stock after scroll sequences |
| G6 | column metrics: prefix sums for column x, binary search for column at x, `showColumn` O(log c) | differential on column order/width/visibility |

## Non-claims

- No GTK or Cocoa runs on this host.
- No accessibility change (the accessible children are still all items).
- Scroll metrics are in rows; more than 2^31 rows is not supported by `ScrollBar` and is not claimed.
