# Java2s SWT/Swing behavioral donor catalogue

This catalogue records public example categories used to qualify Nebula Grid's viewport
distillation. It is behavioral evidence only. No Java2s tutorial source is copied into
Nebula or Synexia.

## Source catalogues

- SWT: https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- SWT 2D Graphics: https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- Swing: https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- Swing Event: https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

The catalogue pages are used as taxonomies, not source donors. Current indexed examples that
directly exercise the viewport rewrite include:

- SWT: Canvas, Table, TableItem, TableColumn, Table Event, Table Cursor, Table Editor,
  Table Renderer, Tree, TreeItem, Tree Editor, Tree Event, TreeColumn/TreeTable,
  TreeViewer, ScrolledComposite, ScrollBar, ScrollBar Event, SWT Event, KeyEvent,
  MouseEvent, drag/drop, TextLayout, StyledText and screen capture.
- SWT 2D Graphics: GC, Color, SWT Paint, point/line/arc/oval/rectangle/polygon/path,
  Draw Focus, Font, Draw String, Transform, Animation and image/PNG/GIF.
- Swing: JComponent, JList, JTable, JTable model/renderer/editor/header/column/sort/filter,
  JTree, TreeModel, tree renderer/editor/selection, JScrollPane/JScrollBar/JViewport,
  JLayeredPane, DebugGraphics, SwingWorker, accessibility and SwingUtilities.
- Swing Event: general events/adapters/actions plus focus, key, mouse, list-data,
  list-selection, menu, tree-expansion/model/selection and other listener families.

## Distilled behavior families

| Donor family | Observable behavior retained as a Grid/SWT proof |
| --- | --- |
| SWT Tree / lazy Tree | logical child extent may exceed materialized widget residency; explicit indexed access materializes only demanded coordinates |
| SWT Table / TableItem / Table events | selection, check/gray state, owner draw and row identity stay attached to the logical row across scrolling and mutation |
| SWT ScrolledComposite / ScrollBar | logical origin and scrollbar geometry are independent from physical child/control residency; vertical movement must not repaint fixed header chrome |
| SWT Canvas / Paint | clipping limits work to damage/viewport bounds; repaint is an invalidation signal rather than semantic state |
| SWT GC / Transform / Path / line drawing | affine composition is ordered; equivalent stroke/alpha state may be collapsed; clipping and complete mutable GC state are restored after each viewport plane |
| Swing JViewport / JScrollPane | viewport origin, model extent and view residency are separate concepts |
| Swing JLayeredPane | body, fixed content, chrome/editor and transient feedback are separable z-planes |
| Swing JTable / JTableHeader | header is a distinct semantic plane; horizontal projection may couple to body while vertical scrolling does not |
| Swing JTree / TreeModel | model topology, expansion and selection belong outside renderer instances |
| Swing renderer/editor examples | renderers/editors are reusable presentation machinery and must not become semantic item ownership |
| Swing event catalogues | event ordering and logical source identity remain compatibility contracts despite internal virtualization |

## Canonical Nebula execution atoms

The behavior families above map to the existing package-private Grid atoms:

- `GridVirtualItemList`: sparse logical row ownership;
- `GridViewportProjection`: visible rows/columns and fixed/scrolled horizontal projection;
- `GridVisibleRangeSupport`: visible-range deltas independent of repaint;
- `GridViewportDamage`: body/header/footer invalidation;
- `GridPaintDAG`: ordered paint planes;
- `GridGCProxy`: scoped state compiler around SWT's final `GC`;
- `GridGCStateDAG`: retained/deduplicated stroke, alpha and affine state transitions;
- `GridTransform`: allocation-free affine value/composition;
- `GridSwtScreenshotCapture` + OpenCV gate: final native visual evidence.

The proxy-GC decision is therefore closed: do not subclass or replace SWT `GC`. Keep the real GC
as the observable/native resource, accumulate safe affine/stroke/alpha state internally, flush
at renderer boundaries, and restore the captured SWT graphics state exactly when the plane scope
closes.

`GridViewportDistillation_Test` exercises pure geometry/state. Million-row, screenshot,
owner-draw and graphics-lifetime tests remain runtime authority.

## Competitive problem-category donor queue

LeetCode, HackerRank and GeeksForGeeks are category/problem-shape donors only. Their solution and
editorial bodies are not copied. Review is serial by category and complexity before a mechanic is
admitted:

1. binary search / lower-bound / ordered sparse lookup;
2. interval merge, split and overlap search;
3. prefix sums, Fenwick trees and segment trees for variable-height rank/offset mapping;
4. trie/prefix/fuzzy lookup;
5. top-k / bounded priority queues;
6. graph/iterative DFS/BFS for tree expansion, release and accessibility traversal;
7. bit manipulation/bitset state packing;
8. bounded cache / eviction;
9. automata/state-machine shapes for event and paint-state convergence.

Prefer direct arithmetic or compact sorted arrays whenever the viewport has fixed row height or a
small bounded frontier; do not introduce asymptotically heavier structures without a measured
query/update crossover.

## Java / JNI admission

Java remains semantic authority. JNI/native promotion is allowed only when all of these are true:

- the Java implementation and behavior tests are fixed;
- the native candidate preserves exact SWT/Nebula public behavior and event ordering;
- a benchmark demonstrates a repeatable crossover large enough to justify the ABI/native risk;
- ownership, lifetime and thread rules are explicit;
- a permissive native donor/provenance record exists when donor mechanics are reused;
- differential Java/native tests remain in the gate.

No JNI path is justified merely because a state transition can be expressed natively. In
particular, the current Grid GC state/affine DAG remains Java because it already coalesces native
transitions at the renderer boundary and preserves SWT's GC contract.

## Admission rule

A donor example is admitted only as a behavior or algorithm shape unless its source license and
provenance are explicitly recorded. New implementation work must be performed through the
canonical Synexia M3 recipe crate, replayed against the exact source preimage, and qualified by
the original Nebula Tycho reactor plus retained native screenshot/behavior gates.

The sequence is:

inventory -> atomize/patternize -> donor/target shape intersection -> bounded recipe ->
compiler/JUnit/native proof -> fixed point -> serial promotion.

No LLM is required in the runtime/core.
