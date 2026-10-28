# Nebula TableCombo on the viewport (custom-widgets leaves)

Matrix row TABLE_COMBO (rank 14, P2; the drop-down is a virtualizable table). Stock reference: TableCombo of commit
33aaf10f on the official SWT 3.135.0 jar.

## What nebula#150 already did (merged)

- The drop-down shell and its `Table` are built on first use (`table()` / `createPopup`), not in the constructor.
- With `SWT.VIRTUAL` the popup table is virtual and the rows the table asked for are kept as a packed bit set
  (`materializedRows`); counts never build item arrays; measuring never builds the drop-down.
- CheckTableCombo the same.

No startup mode switch exists yet, so the stock (eager) path cannot be chosen, and there is no recorded differential
against stock.

## Leaf TC-MODE-1: WidgetMode switch and recorded differential (this PR)

- `org.eclipse.nebula.widgets.tablecombo.internal.WidgetMode` and `SwtLog`: Nebula-local copies of the SWT programme
  files (package line changed), as in Grid and Gallery. `static final boolean VIEWPORT = WidgetMode.of("TableCombo",
  WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT`.
- `original` mode (`-Dswt.widget.mode.TableCombo=original`): the constructor builds the drop-down at once, as stock
  did (`createPopup(-1)` at the end of the constructor), so every later answer comes from a live table exactly as in
  stock. The lazy build and the virtual bit set stay the viewport-mode path.
- `TableComboViewportDifferentialTest`: seeded scripts (add items with text, image and colours, set columns and widths,
  select by index and by text, type into the text, set visible item count, drop down and close with the arrow, keys
  and the popup's Deactivate, set fonts and colours, remove, removeAll, setItemCount for virtual) on two TableCombos,
  recorded on the stock classes and compared on this build in both modes. Every step records the text, selection
  index, item count, the drop-down's visibility and bounds, the events (Selection, DefaultSelection, Modify) and the
  pixels of the combo and of the open drop-down.
- Example: `TableComboMillionSnippet` (SWT.VIRTUAL, 1,000,000 rows, 3 columns) in `examples/org.eclipse.nebula.snippets`.

## Next leaves

- TC-LANE-1: `columnWidths`, line colours and the materialized bit set as one lane set shared with CheckTableCombo
  (one model, deltas: a `setItemCount` change updates only the extents).
- TC-POP-1: the popup table rides the SWT Table viewport (SetData once per row, resident window) once the SWT fork
  jar replaces the official jar in the Nebula builds.
