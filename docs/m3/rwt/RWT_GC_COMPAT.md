<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others. -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# RWT GC gap and API drift: design (step 3)

Goal: Nebula and M3 painters compile and draw on RWT with the SWT contract. Inputs: `README.md` section 3 (census
`census/rwt-4.8.0-v0/`) and `FOSS_REUSE_DECISION.tsv`. Everything here is Java on the server; the browser client of
RWT 4.8.0 is used as it is (no client JavaScript is added).

## Where the change lives

The missing methods belong to RWT's own classes (`GC`, `Color`, `SWT`), so they are changed there: the bundle
`widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.compat` carries patched copies of six RWT 4.8.0 source files, built
over the RWT jar into `org.eclipse.rap.rwt-4.8.0-m3.jar` (Eclipse signature removed: classes replaced inside a signed
package would be rejected by the class loader). The patched files are first committed byte-identical to the RWT
4.8.0 sources jar, then changed in a separate commit, so `git diff` of that commit is the patch an upstream RAP pull
request would carry. Each patched file keeps RAP's EPL-1.0 header and gains one contributor line. New logic sits in
new classes with the Synexia header; the patched files only call them.

| file (RWT 4.8.0) | change |
|---|---|
| `org/eclipse/swt/SWT.java` | constants `LOW = 1`, `HIGH = 2`, `FILL_EVEN_ODD = 1`, `FILL_WINDING = 2` (SWT values) |
| `org/eclipse/swt/graphics/Color.java` | device-less constructors `Color (int, int, int)`, `(int, int, int, int)`, `(RGB)`, `(RGB, int)`, `(RGBA)`; `dispose ()` of such a colour does nothing, as in SWT |
| `org/eclipse/swt/graphics/GC.java` | the 11 methods; `new GC (Image)` gets a delegate; `drawImage` of a recorded image replays it |
| `org/eclipse/swt/graphics/ControlGC.java` | a package constructor that records into a given `GCAdapter` (used for images) |
| `org/eclipse/swt/internal/graphics/GCAdapter.java` | none (used as the display list) |
| `org/eclipse/swt/internal/widgets/canvaskit/GCOperationWriter.java` | writes the new operations; resynchronises the client state after a group |

New classes (Synexia header): `org.eclipse.swt.graphics.Pattern`, `org.eclipse.swt.graphics.M3DisplayLists`
(image to display list, replay), `org.eclipse.swt.internal.graphics.M3GCOperation` (operations `SetPattern`,
`SetFillRule`, `BeginReplay`, `EndReplay`), `org.eclipse.swt.internal.widgets.canvaskit.M3GCWriterSupport` (pure
arithmetic: gradient stops, alpha bands, transform products).

## Semantics per method

The browser client (`rwt/widgets/GC.js`) handles a few operation names itself and calls every other name directly on
the canvas 2D context. That decides what can be exact:

| SWT API | RWT behaviour after the change | exact? |
|---|---|---|
| `setBackgroundPattern / setForegroundPattern (gradient)` | every following fill (stroke) is drawn with a canvas linear gradient between the pattern's end points; outside the end points the gradient repeats, as SWT does on win32 (GDI+ `WrapModeTile`) and GTK (`CAIRO_EXTEND_REPEAT`): the stops are laid out over the projection of the shape's bounds, a hard edge per period (at most 256 periods, beyond that the ends are clamped) | yes for opaque colours; alpha: the client drops alpha from colour stops (`rgbToRgbString`), so equal alphas become `globalAlpha` (exact) and different alphas become 16 bands of constant alpha per period (approximate, max step 1/16 of the alpha range) |
| `setBackgroundPattern (image pattern)` | fills with the image tiled from (0, 0), clipped to the shape: `save`, shape path, `clip`, one `drawImage` per tile that meets the bounds, `restore` | yes (tiles are bounded by the shape bounds) |
| `get…Pattern` | returns the pattern set | yes |
| `setFillRule / getFillRule` | default `FILL_EVEN_ODD` as in SWT; polygon and path fills emit `fill ("evenodd")` or `fill ()` (non-zero = `FILL_WINDING`) | yes. Before the change RWT filled every polygon non-zero, which contradicts SWT's default even-odd rule (recorded as an upstream defect) |
| `setInterpolation / getInterpolation` | value kept and returned; the browser always smooths scaled images (`imageSmoothingEnabled` is a canvas property the client protocol cannot set) | state only |
| `setXORMode / getXORMode` | value kept and returned; drawing stays in source-over mode (`globalCompositeOperation` cannot be set through the protocol, and canvas `xor` is Porter-Duff, not pixel XOR) | state only |
| `copyArea (srcX, srcY, w, h, destX, destY [, paint])` on a Control | the server keeps no pixels; the destination (and, with `paint`, the source) rectangle is redrawn, so the widget's paint listener draws the moved content from its model | visual result equal when the paint listener draws from the model (the case for scroll blits) |
| `copyArea (Image, x, y)` | Control: no pixels on the server, the image is left unchanged (logged once at FINE); recorded image: the image's display list is copied, translated by (-x, -y) | Control: no; recorded image: yes |
| `new GC (Image)` | records operations into the image's display list (a `GCAdapter`), using the same state logic as a Control GC (patched `ControlGC` constructor) | see next row |
| `drawImage (image, …)` of a recorded image | replays the display list in place: `save`, transform to the destination (with scaling), clip to the destination, the recorded operations, `restore`, then the GC's own state is re-sent so the client's tracked state stays right; nested recorded images are inlined | yes for drawing done through GC. An image with a display list shown by a `Label`, `Button` or item has no pixels (blank); its `ImageData` is the original blank data |
| `isDisposed` | unchanged (inherited from `Resource`) | yes |

## Color drift

`new Color (r, g, b)`, `new Color (r, g, b, a)`, `new Color (rgb)`, `new Color (rgb, a)` and `new Color (rgba)`
exist in SWT since 3.115 and need no device and no dispose. RWT 4.8.0 has only device constructors (172 census
errors). The new constructors create a colour without a device (like RWT's factory colours) that `dispose ()`
ignores, as SWT does. Typed listeners need no adapter on RWT 4.8.0 (`Widget.addTypedListener` exists).

## Not in this step

Input listeners (`MouseMoveListener`, `MouseTrackListener`, `MouseWheelListener`, `setCapture`), `StyledText`,
`Region`, `TextLayout`, printing, `ScrollBar` increments and the other census categories are handled per widget in
step 4 where a ranked widget needs them; each is a separate decision because RWT's server never receives those
events or has no such class.

## Evidence plan

- Compile: re-run the differential census with the patched jar (`census/rwt-4.8.0-m3`), compare per category.
- Run: JUnit tests on RAP's test fixture (`org.eclipse.rap.rwt.testfixture` 4.8.0) render a `Canvas` through the RWT
  life cycle and read the `draw` operations of the protocol message, per method in the table above.
