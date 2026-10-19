# Nebula CompositeTable on the viewport (custom-widgets leaves)

Matrix row DATA_GRID lists CompositeTable (a table of row controls, virtual by construction: it creates only the row
controls that fit and binds them on `RowConstructionListener` / `RowContentProvider`). Stock reference: commit
33aaf10f on the official SWT 3.135.0 jar.

## What nebula#150 already did (merged)

- No row population while the table is hidden: an update or refresh requested while hidden sets a dirty flag and is
  caught up once, on the first paint or the first row read (`flushRows`).
- Helper planes are `NO_FOCUS` (internal composites, sliders); a slider that gets focus hands it back to the rows.
- The focused editor's caret is reproduced after a row scrolls out and back (16 packed int slots).
- Row refreshes are coalesced during fast thumb drags.

No startup mode switch exists, and the differential of #150 was an API probe (628 rows), not a recorded script.

## Leaf CT-MODE-1: WidgetMode switch and recorded differential (this PR)

- `org.eclipse.nebula.widgets.compositetable.internal.WidgetMode` and `SwtLog` copies;
  `static final boolean VIEWPORT = WidgetMode.of("CompositeTable", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT`.
- `original` mode: rows are populated while hidden, as stock did (the deferral is gated by `VIEWPORT`); the caret
  reproduction and the NO_FOCUS planes stay (they do not change any answer and fix stock focus defects; rule 12).
- `CompositeTableViewportDifferentialTest`: seeded scripts (set the row count, scroll with `setTopRow`, select a row,
  type into the current row's editor, refresh rows, hide and show the table, resize, change the row count while
  hidden, set `setMaxRowsVisible`) on two CompositeTables, recorded on the stock classes and compared on this build
  in both modes. Every step records the top row, the current row, the number of row controls, the first and last
  bound rows, the text of the visible editors, the slider, the events (RowConstruction, RowContent, ScrollListener,
  RowFocus) and the pixels.
- Example: `CompositeTableMillionSnippet` (1,000,000 logical rows, three Text editors per row).

## Next leaves

- CT-LANE-1: `shownPosition` (an IdentityHashMap from row control to position) and the caret slots become one int
  lane indexed by row slot (one model, no map); the row-control list becomes a slot array.
- CT-DELTA-1: `refreshAllRows` after a model change takes a delta (insert/remove/update runs over positions) and
  touches only the rows in the window; rows outside only move the slider extents.
