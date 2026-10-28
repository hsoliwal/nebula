# Nebula Gallery on the viewport (custom-widgets leaves)

Matrix row GALLERY (rank 3, P1). Builds on nebula#236 (viewport mode by default, `WidgetMode`/`SwtLog` copies in
`org.eclipse.nebula.widgets.gallery.internal`, the recorded differential `GalleryViewportDifferentialTest` and the scale
oracle `GalleryScaleOracleTest`). The stock reference is the Gallery of commit 33aaf10f on the official SWT 3.135.0 jar
(`build_gallery.sh stock`).

## Leaf G-SEL-1: selection lane with amortized growth (this PR)

Stock keeps the selection twice: bit flags per parent (`selectionFlags`, the model) and a `GalleryItem[] selection`
(the order), which it reallocates and copies on every selected or deselected item (`_addSelection`,
`_removeSelection`), so a shift-click range of n items or `selectAll` costs O(n^2) copies, and `getSelection()` hands
out the internal array.

Viewport mode keeps the bit flags as the only truth of membership and turns the order array into a lane: amortized
doubling growth with a count (`selectionCount`), in-place shift on removal, the array kept and its facades released on
`deselectAll`. `getSelection()` returns a copy of the selected prefix in selection order; `getSelectionCount()` reads
the count. Stock order of the returned items, the Selection events (the item named when one is deselected is still the
last selected one, `lastSelected()`), and every other answer are unchanged: the recorded differential (8 seeds x 100
steps with selection, shift and ctrl clicks, `deselectAll`, final pixels) must stay green, and `GalleryScaleOracleTest`
gains a `selectAll` measurement.

The `original` startup mode (`-Dswt.widget.mode.Gallery=original`) keeps the stock arrays byte for byte.

## Next leaves (in order)

- G-IDX-1: `indexOf` O(n) scan over `items` replaced by an index lane on the facade (`GalleryItem` already knows its
  parent; a slot int keeps it O(1) with the stock `lastIndexOf` heuristic as the fallback for the original mode).
- G-ARR-1: `_arrayAddItem` / `_arrayRemoveItem` copy per item (O(n^2) to build a gallery) replaced by amortized
  growth with a count lane; `items.length` readers in the renderers go through `getItemCount()`.
- G-VIS-1: `getVisibleItems` for custom renderers (the monotone binary search already covers the default renderer).
- Example: `GalleryMillionSnippet` (10^6 virtual items in 10^4 groups) and a screenshot in `../EXAMPLES.md`.

## Proof

- Differential: `GalleryViewportDifferentialTest` on the hidden desktop, recorded on `stock-classes`, compared on
  `gallery-classes` in viewport and original modes (`run_gallery.sh`).
- Oracle: `GalleryScaleOracleTest` at 10^4 and 10^5 items, stock versus viewport, `selectAll` column added.
- Coverage: JaCoCo over the Gallery suite, both modes.
