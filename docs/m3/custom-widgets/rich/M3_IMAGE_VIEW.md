# M3ImageView (matrix row IMAGE_VIEW, rank 5, P1)

Missing from stock SWT and stock Swing; 12 libraries implement it: SwingX `JXImageView` / `JXImagePanel` (LGPL: shape
only), ControlsFX `SnapshotView`, GemsFX `PhotoView`, greip `Picture` (EPL-1.0, ADAPT), SWTImageJ `ImageCanvas`
(EPL-2.0, ADAPT), pumpernickel `BufferedImageView` (MIT, ADAPT), GestureFX `GesturePane` (Apache-2.0, ADAPT), webfx.
Swing twin: `javax.swing.M3ImageView` (row CW-IMAGE_VIEW in `../ENHANCED_API_MAP.tsv`).

## API shape taken from the donors (shape only, no code)

SwingX: `setImage`, `getImage`, `setScale` / `getScale`, zoom in / out and rotate actions, drag enabled, editable,
image location / URL, error listeners. GesturePane: pinch-to-zoom and pan with min/max scale and fit modes.
ImageCanvas: zoom around a point, source-to-view and view-to-source coordinate mapping.

## Contract

`org.eclipse.nebula.widgets.m3.M3ImageView extends Canvas` (styles `SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER`).

| Member | Meaning |
|---|---|
| `setImage(Image)` / `setImage(ImageData)` / `getImage()` | the image; `ImageData` is kept as the source and tiles are created from it |
| `setScale(double)` / `getScale()` / `zoomIn()` / `zoomOut()` / `zoomTo(double scale, Point viewAnchor)` | scale with an anchor that stays put |
| `setFit(Fit)` (NONE, FIT, FILL, WIDTH, HEIGHT) | automatic scale on resize |
| `setRotation(int quarterTurns)` | 0..3 |
| `toImage(Point view)` / `toView(Point image)` | coordinate mapping |
| `setPanEnabled(boolean)` / mouse wheel zoom (Ctrl) / drag to pan | interaction |
| `getViewport()` | the image rectangle on screen, in image coordinates |
| `Selection` events | sent when scale, rotation or the origin changes |

## DOOP data

The image is a tile pyramid: level 0 is the source, each level halves; tiles are 256 x 256 `Image`s created on
demand and kept in a bounded cache keyed by `(level << 40 | ty << 20 | tx)` longs (exact LRU on a tick lane, as in
`M3ResidentPages`). Paint draws only the tiles intersecting the clip at the level nearest the scale, with one
`drawImage` per tile (scaled by the residual factor). Scrolling and zooming change ints (origin, scale as a
fixed-point int at 1/65536) and never re-decode the source. A 20,000 x 20,000 image costs the visible tiles only.

## Proof

- `M3ImageViewTest`: API, errors, fit modes, zoom anchors, rotation mapping (`toImage`/`toView` round trips on a
  10^6-point random set), tile residency under the budget, events, both modes.
- Differential: pixels against a reference drawn with one `GC.drawImage` of the whole image at the same scale
  (equal for scale 1 and for integer down-scales; documented tolerance otherwise).
- Coverage 99% (JaCoCo). Example: `M3ImageViewSnippet` (a generated 16,384 x 16,384 image).
