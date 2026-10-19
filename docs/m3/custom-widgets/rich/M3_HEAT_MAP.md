# M3HeatMap (matrix row HEATMAP, rank 8, P1)

Missing from stock SWT and stock Swing; 6 libraries implement it (XChart `HeatMapChart`, prasser/jhc `JHCHeatmap`,
GC4S `JHeatMap`, WebLaF `HeatMap`, fireplace `FlamegraphView`, chartx). Licence route: XChart (Apache-2.0) and jhc
(EPL-1.0) are ADAPT donors; patterns only were taken (cell window painting, colour lookup table). Swing twin:
`javax.swing.M3HeatMap` (row CW-HEATMAP in `../ENHANCED_API_MAP.tsv`).

## Contract

`org.eclipse.nebula.widgets.m3.M3HeatMap extends Canvas` (styles `SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER`).

| Member | Meaning |
|---|---|
| `setSize(int rows, int columns)` | the grid; values are zero until set |
| `setValues(float[] rowMajor)` / `setValue(int row, int column, float v)` / `getValue` | the data, one float lane |
| `setRange(float min, float max)` / `setAutoRange(boolean)` | the value range mapped onto the palette |
| `setPalette(RGB[] stops)` | palette stops; a 256-entry lookup table is derived once |
| `setCellSize(int width, int height)` | cell geometry; the scroll bars cover `rows x height` and `columns x width` |
| `getCell(Point)` / `getCellBounds(int row, int column)` | hit test and geometry (viewport-relative) |
| `setSelection(int row, int column)` / `getSelection()` / `Selection` events | one selected cell |
| `setRowLabels(String[])` / `setColumnLabels(String[])` | optional label gutters |
| `update(int fromRow, int toRow)` | delta: repaint only the rows that changed (`M3-DELTA`) |

## DOOP data

One `float[] values` lane (row-major), one `int[] lut` (256 ARGB ints), ints for rows, columns, cell size, origin.
No per-cell objects. Paint walks only the cells intersecting the clip and draws runs of equal colour with one
`fillRectangle` each (run-length over the row). Hit test is integer division. A value change outside the viewport
only marks nothing (no repaint); inside, it repaints the cell.

## Proof

- `M3HeatMapTest`: API, errors, range and palette mapping, geometry, selection events, delta repaint regions
  (Paint events recorded), both modes.
- Differential: no stock reference exists; the oracle is a straight port of the formula (value -> LUT index) checked
  on 10^6 random cells, and pixels are compared against a painted reference image of a small grid.
- Coverage 99% line/branch (JaCoCo). Example: `M3HeatMapSnippet` (1000 x 1000 = 10^6 cells) with a screenshot.
