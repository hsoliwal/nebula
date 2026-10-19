# Grid binding for the M3 column sort and filter (leaf 1)

Branch `m3/grid-sort-filter-20261019` (from `m3/grid-viewport-20261016` @ `e6fd7137`). Owner: sort-filter agent.
The sort and filter themselves live in SWT (`org.eclipse.swt.m3`: `M3ColumnSort`, `M3ColumnFilter`, `M3RowView`,
`M3TextMatch`; hsoliwal/eclipse.platform.swt PR #232, merged into `m3/viewport-integration-20261016`); this leaf adds
the Grid side only, in package `org.eclipse.nebula.widgets.grid.m3`:

| class | what |
|---|---|
| `M3GridRowBinding.bind(Grid, M3RowView, RowFill)` | drives a virtual Grid from any view chain (filter -> sort -> window): item count = view size, `SWT.SetData` fills from `view.map(index)`, a view change reaches the grid as a patch (`setItemCount` for the extent, `clear(first, last, false)` for the damaged rows only), selection kept by source row (`getSelectedRows`, `setSelectedRows`) |
| `M3GridSortHeader.install(Grid, M3ColumnSort)` | column header click sorts (ascending, descending, unsorted), Shift+click adds the next key; every sorted column shows its direction through `GridColumn.setSort` (the Grid's own header renderer draws the arrow, so no generated image is needed) |

One model (user 2026-10-19): the binding keeps no copy of the rows; the Grid's lanes (`GridItemLanes`, the
viewport) and the view's lanes (masks, permutation) are the only state. Deltas: a source edit reported to the view
re-filters only the changed rows and merges them into the permutation (SWT side); the Grid receives the damaged
view range, so a change on screen repaints its rows and a change off screen only moves the extent (G7 row deltas
on this branch do the same inside the Grid).

Filter UI: the SWT `M3FilterRow` and `M3FilterPopup` are Table/Tree specific (column geometry through
`M3Columns`); a Grid filter row is the next leaf (Grid column geometry: `GridColumn.getWidth`,
`Grid.getColumnOrder`, header height, horizontal origin).

Build: the grid bundle now imports `org.eclipse.swt.m3`, which exists only in the fork's SWT jar
(`org.eclipse.swt.win32.win32.x86_64-m3-*.jar` from `m3/viewport-integration-20261016` or later); the stock SWT
3.135 jar does not carry it, so the bundle's Maven build needs the fork jar on its path (the programme's
`build_and_tests.sh` output). Compile check of this leaf: javac against `S:/m3-scratch/grid/dev/grid` (the Grid
agent's classes) + the fork jar + the `org.eclipse.swt.m3` classes.

Tests: `M3GridRowBindingTest` (hidden desktop): virtual Grid of 20,000 rows bound to filter + sort; header clicks
with Shift; filter change leaves the selection on the same source rows; only damaged rows are re-asked.
