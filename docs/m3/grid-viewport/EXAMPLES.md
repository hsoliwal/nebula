# Nebula Grid examples for the viewport leaves

Three snippets in the Nebula snippets bundle (`examples/org.eclipse.nebula.snippets`, package
`org.eclipse.nebula.snippets.grid`), in the bundle's convention: one class with a `main`, a `Display`
and a `Shell`, no other dependency than SWT and the Grid. Each logs its measurements through
`java.util.logging` (logger `org.eclipse.nebula.snippets.grid`, brief rule 22). With
`-Dnebula.snippet.screenshot=<file.png>` it saves its window to that file and exits.
`-Dswt.widget.mode.Grid=original` runs the stock Grid for comparison; the default is the viewport mode.

| Snippet | Shows | Leaves |
|---|---|---|
| `GridSnippetMillionTreeRows` | 1,000,000 tree rows: 1,000 roots, each with 999 children from `GridItem.setItemCount`; texts from `SWT.SetData` | G2 row tree, G4 steps 2a to 2c |
| `GridSnippetCellSelection200k` | cell selection over 4 columns and 50,000 rows: `selectAllCells` selects 200,000 cells, then single cells are toggled in the middle | G3 selection lists |
| `GridSnippetVariableHeightScroll` | 10,000 rows of 20, 38 and 56 pixels (wrapped text), scrolled one row per 16 ms frame | G1 row index, G5 blit scroll |

## Running

[`run_examples.sh`](run_examples.sh) compiles nothing. It runs the compiled snippets (`$GRID_OUT/examples-classes`) on the
hidden test desktop, under the desktop lock, in both modes. It writes `$GRID_OUT/examples/<snippet>-<mode>.png`
and `.log`; `GRID_OUT` is `S:/m3-scratch/grid`. To compile:

```text
javac -d S:/m3-scratch/grid/examples-classes -cp "<grid classes>;<SWT jar>" \
  examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid/GridSnippet{MillionTreeRows,CellSelection200k,VariableHeightScroll}.java
```

## Results

Run 2026-10-19 on the hidden desktop (win32, JDK 21, SWT 3.135); screenshots in `S:/m3-scratch/grid/examples/`
(`<snippet>-viewport.png`, `<snippet>-original.png`), logs beside them.

| Snippet | viewport mode | original mode (stock) |
|---|---|---|
| `GridSnippetMillionTreeRows` | 1,000,000 rows built in 1.45 s, heap +48 MB; 24 `SetData` requests after the first paint, 47 after jumping to the middle | 152.4 s, heap +277 MB; 23 and 45 |
| `GridSnippetCellSelection200k` | `selectAllCells` of 200,000 cells: 238 ms; one cell toggle inside them: 4.1 ms | 30.6 s; 3.7 ms |
| `GridSnippetVariableHeightScroll` | 12.5 ms per one-row scroll step, scroll and paint | 31.1 ms |

Notes:
- The viewport mode paints with the native look, whose rows fit one more on the page than stock's classic look. That
  is the one and two extra `SetData` requests: one more row is on screen each time.
- A single cell toggle costs milliseconds in both modes. The per-cell counts make the paint's membership test and
  `selectAllCells` fast, but a toggle still updates the stock list. That is the target of the optimization pass's
  cell selection leaf.
- The first variable-height screenshot showed newline characters on one line: the default cell renderer wraps words
  but does not break at `\n`. The snippet now uses texts long enough to wrap onto 2 and 3 lines; its screenshot is
  taken again with the next desktop run.
