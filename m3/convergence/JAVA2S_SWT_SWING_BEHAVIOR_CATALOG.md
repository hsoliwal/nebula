# Java2s SWT/Swing behavioral donor catalogue

This catalogue records public example categories used to qualify Nebula Grid's viewport
distillation.  It is behavioral evidence only.  No Java2s tutorial source is copied into
Nebula or Synexia.

## Source catalogues

- SWT: https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- SWT 2D Graphics: https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- Swing: https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- Swing Event: https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

## Distilled behavior families

| Donor family | Observable behavior retained as a Grid/SWT proof |
| --- | --- |
| SWT Tree / lazy Tree | logical child extent may exceed materialized widget residency; explicit indexed access materializes only demanded coordinates |
| SWT Table / TableItem / Table events | selection, check/gray state, owner draw and row identity stay attached to the logical row across scrolling and mutation |
| SWT ScrolledComposite / ScrollBar | logical origin and scrollbar geometry are independent from physical child/control residency; vertical movement must not repaint fixed header chrome |
| SWT Canvas / Paint | clipping limits work to damage/viewport bounds; repaint is an invalidation signal rather than semantic state |
| SWT GC / Transform / Path / line drawing | affine composition is ordered; stroke/alpha state may be deduplicated only when equivalent; GC state is restored after each viewport plane |
| Swing JViewport / JScrollPane | viewport origin, model extent and view residency are separate concepts |
| Swing JLayeredPane | body, fixed content, chrome/editor and transient feedback are separable z-planes |
| Swing JTable / JTableHeader | header is a distinct semantic plane; horizontal projection may couple to body while vertical scrolling does not |
| Swing JTree / TreeModel | model topology, expansion and selection belong outside renderer instances |
| Swing renderer/editor examples | renderers/editors are reusable presentation machinery and must not become semantic item ownership |
| Swing event catalogues | event ordering and logical source identity remain compatibility contracts despite internal virtualization |

## Executable Nebula atoms

The behavior families above are intentionally mapped to existing package-private Grid atoms:

- `GridVirtualItemList`: sparse logical row ownership;
- `GridViewportProjection`: visible rows/columns and fixed/scrolled horizontal projection;
- `GridVisibleRangeSupport`: visible-range deltas independent of repaint;
- `GridViewportDamage`: body/header/footer invalidation;
- `GridPaintDAG`: ordered paint planes;
- `GridGCProxy`: scoped GC restoration;
- `GridGCStateDAG`: deduplicated stroke/alpha state transitions;
- `GridTransform`: allocation-free affine value/composition;
- `GridSwtScreenshotCapture` + OpenCV gate: final native visual evidence.

`GridViewportDistillation_Test` exercises the pure geometry/state part of this catalogue.
Existing million-row, screenshot, owner-draw and GC-lifetime tests remain the runtime proof.

## Admission rule

A donor example is admitted only as a behavior or algorithm shape unless its source license and
provenance are explicitly recorded.  New implementation work must be performed through the
canonical Synexia M3 recipe crate, replayed against the exact source preimage, and qualified by
the original Nebula Tycho reactor plus the retained native screenshot lane.
