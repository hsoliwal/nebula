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

Pending: the runs need the hidden desktop. They have not run yet, because slot locks left over from the
2026-10-19 15:08 reboot block the desktop queue. The screenshots will be in `S:/m3-scratch/grid/examples/`.
