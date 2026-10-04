# M3 UI distillation: viewport graphics, events, and donor catalogue

This note records the donor evidence used for the Grid graphics distillation. It is
not a copied implementation catalogue. The invariant is to preserve Nebula/SWT
public behavior while moving repeated mechanics into replaceable atoms and recipes.

## Primary example catalogues reviewed

- Java2s SWT catalog:
  https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- Java2s SWT 2D Graphics catalog:
  https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- Java2s Swing catalog:
  https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- Java2s Swing Event catalog:
  https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

The SWT catalogue supplies small behavioral examples around Table, Tree, TreeViewer,
editors, renderers, scrolling, lazy tree population and SWT event delivery. The SWT
2D catalogue explicitly catalogues GC, Paint, Transform, Path and related drawing
examples. Swing supplies the independent model/view/renderer/editor/header split in
JTable/JTree, while Swing Event separates selection/model/expand/scroll events from
rendering.

The examples are used as behavioral donors only. Their source is not copied.

## Distilled patterns

### 1. Graphics state is a scope, not widget state

SWT GC is final, so Grid must not depend on subclassing it. GridGcProxy wraps the real
GC and scopes temporary clipping/affine state. Renderers continue to receive the real
GC, preserving their API and native semantics.

GridAffineTransform is a resource-free matrix value. It can be retained in logical
paint plans without retaining SWT Transform native resources.

### 2. Painting is a small dependency DAG

GridPaintDag represents six planes with primitive mask bits:

1. background
2. scrolling body
3. fixed/frozen body overlay
4. header
5. footer
6. transient overlay

This is deliberately not an object graph allocated per paint event. The DAG answers
which planes intersect the native paint clip; existing renderer calls execute only
for admitted planes.

### 3. Header/footer are chrome, not body rows

Vertical scroll damage is confined to the body plane. Header/footer CPU painting is
skipped when the native clipping rectangle does not intersect those planes. Horizontal
scroll may still invalidate chrome because header/footer column projection changes.

This continues the prior GridViewportDamage work instead of creating a second
invalidation model.

### 4. Model/event state remains outside paint

Swing JTable/JTree and their event catalogues reinforce the same separation already
being moved into SWT/Nebula:

- logical model state owns selection/expansion/data;
- viewport projection maps logical state to visible coordinates;
- renderers paint a projected facade;
- editors and event dispatch are independent responsibilities.

The paint DAG therefore owns no item model, listeners, selection, native widgets or
scroll bars.

### 5. Transform and clip are orthogonal

A paint node may carry an affine transform and a clip. This supports future logical
origin rendering, frozen overlays, zoom/HiDPI projection and ScrolledComposite-style
viewport illusions without requiring every renderer to perform scroll subtraction
itself.

Native Transform use is optional because SWT documents advanced graphics as
platform-dependent. The pure affine value remains usable even when native transform
application is not admitted.


## CodeSplitJava atomization donor

Additional donor:
- https://github.com/tushartushar/CodeSplitJava

CodeSplitJava is Apache-2.0 and uses Eclipse JDT AST visitors rather than textual
splitting. Its useful M3 contribution is discovery, not mutation:

- MethodVisitor enumerates method declarations.
- MethodInvVisitor records invocation edges.
- LocalVarVisitor records local-variable declarations.
- MethodControlFlowVisitor inventories if/switch/loop/try control-flow nodes.
- Resolver resolves type/method relationships, with conservative fallbacks when
  JDT bindings are unavailable.
- Graph computes connected components and directed strongly connected components.

For M3 atomization, the distilled rule is:

1. build a method/type dependency graph;
2. form strongly connected components as indivisible behavior groups;
3. score statement/control-flow regions inside an SCC as candidate extraction
   boundaries;
4. reject extraction that changes public signatures, captured-variable semantics,
   exception boundaries, synchronization boundaries, event ordering, or resource
   lifetime;
5. execute accepted extraction through OpenRewrite/Eclipse refactoring recipes;
6. compile/test and compare behavior/hashes before promoting the recipe.

The CodeSplitJava emitter itself is not used as the production transformation engine.
It writes classes/methods as standalone .code artifacts, whereas M3 must preserve a
compilable project and its public contract after every promoted recipe.

## Algorithm donor catalogue

For viewport/range algorithms, review donor solutions by problem family rather than
copying submissions:

- interval intersection / merge intervals -> damage and visible-range composition;
- sliding window / two pointers -> contiguous visible rows/columns;
- binary search on monotonic prefix widths -> pixel-to-column lookup;
- prefix sums / Fenwick-tree families -> variable-height row to logical-pixel mapping;
- sparse set / occurrence counting -> visible-range enter/leave deltas;
- DAG topological ordering -> deterministic paint/composition ordering.

LeetCode, HackerRank and GeeksForGeeks are candidate catalogues for these algorithm
families. Any admitted algorithm must be re-expressed behind Nebula/Synexia contracts,
benchmarked against the existing serial implementation, and recipe-sealed before
production replacement.

## M3 acceptance rules

- no public Grid/renderer API change;
- no event-order change;
- no selection/identity change;
- no retained SWT Transform/GC outside the paint call;
- clip/transform state must be restored in finally/AutoCloseable scope;
- only clip-intersecting paint planes execute;
- fixed overlay remains above scrolling body;
- recipe replay must be exact and reach a fixed point;
- source drift must be refused rather than guessed through.

## Current application

The first applied slice introduces:

- GridAffineTransform
- GridGcProxy
- GridPaintDag
- Grid.onPaint plane admission
- scoped clipping for fixed-column and insertion-mark passes
- runtime/pure regression tests for plane admission, affine composition and clip restore

Further slices can move logical-origin scrolling and renderer-bound projection through
the same atoms without changing renderer signatures.
