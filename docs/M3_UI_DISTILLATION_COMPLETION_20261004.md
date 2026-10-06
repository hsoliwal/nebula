# M3 UI distillation completion receipt — 2026-10-04

This receipt closes the viewport/paint/event distillation discussed across SWT, Nebula and
Synexia. It records the canonical owners, applied adapters, recipes, donor boundaries and
remaining non-blocking research branches.

## Behavioral donor catalogues

Reviewed as behavioral/problem catalogues only; source is not copied:

- Java2s SWT:
  https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- Java2s SWT 2D Graphics:
  https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- Java2s Swing:
  https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- Java2s Swing Event:
  https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

Independent toolkit donors include SWT, Nebula Grid, Swing JTable/JTree/JViewport/JLayeredPane
and the repository's existing viewport examples.

Challenge sites remain algorithm-family catalogues:

- interval merge/intersection -> viewport damage/range composition;
- topological sort / DAG -> deterministic paint dependency ordering;
- sliding window / two pointers -> visible + overscan windows;
- binary search -> pixel/range lookup;
- indexed-tree / prefix / Fenwick / segment-tree families -> future variable-height logical
  coordinate mapping when the simpler index is insufficient.

LeetCode, HackerRank and GeeksForGeeks solution source is not copied into Nebula/Synexia.

## Distilled architecture

The accepted model is:

1. logical model state is authoritative;
2. viewport projection maps logical coordinates to visible coordinates;
3. native/widget/render residency is replaceable and bounded;
4. exposed public facades remain identity-pinned;
5. paint work is organized as deterministic z/dependency planes;
6. native GC/Transform resources live only inside actual paint callbacks;
7. retained logical paint state uses immutable affine values, primitive geometry and command IDs;
8. only clip-intersecting work plus required dependencies is replayed;
9. header/footer/editor/feedback planes are independent from scrolling body damage;
10. every repeatable LLM transformation is owned by an OpenRewrite/Maven recipe crate and must
    refuse unknown source drift.

## Nebula applied adapter

Merged Nebula PR #40:

**M3: distill Java2s graphics/event donors into Grid paint DAG**

It introduced/applied:

- `GridAffineTransform`
- `GridGcProxy`
- `GridPaintDag`
- Grid paint-plane admission
- fixed/frozen overlay clipping
- scoped GC state restoration
- visible-range / screenshot regression proof

Saved branch:

`m3/java2s-paint-dag-distillation-20261004`

The adapter intentionally keeps SWT `GC` and `Transform` native-resource ownership local to
Nebula. Renderers continue receiving the real SWT GC; the proxy scopes clip/transform mutation.

## Source-sealed Nebula recipe

Merged Synexia PR #8861:

**M3: source-seal Nebula Grid Java2s paint DAG**

Canonical recipe:

`M3NebulaGridPaintDagRecipe`

Crate:

`nebula-grid-paint-dag-v1`

The crate owns exact pre/post images for the Nebula Grid production/test mutation, reaches a
fixed point, and refuses source drift.

## Generic Synexia owner

Merged Synexia PR #8877:

**M3: distill retained affine paint DAG from SWT/Nebula/Swing donors**

Canonical generic owner:

`synexia-visual-pixel-runtime`

Added toolkit-neutral atoms:

- `PaintPlane`
- `PaintCommandKind`
- `PaintCommand`
- `PaintCommandDag`
- `PaintViewportIndex`

Existing Synexia owners reused rather than duplicated:

- `AffineTransform2D`
- `GeometryBounds`
- `VisualContext`
- `IProgressMonitor`

Saved branch:

`m3/visual-paint-dag-distillation-20261004`

Recipe:

`M3VisualPaintDagDistillationRecipe`

Crate:

`visual-paint-dag-distillation-v1`

Maven profile:

`m3-visual-paint-dag-distillation`

The recipe is additive-only for the six generic Java targets, stores exact SHA-256 postimages,
replays to a fixed point and rejects unexpected pre-existing target source.

## Deterministic benchmark receipt

The first viewport index deliberately stays simpler than an interval/Fenwick/segment tree.

For a retained plan containing:

- 1 background command;
- 10,000 row commands;
- viewport Y=[250,270];

a linear scan examines 10,001 command bounds.

The minimum-Y index examines 4 candidate bounds before exact intersection and dependency closure:

`10,001 / 4 = 2,500.25x`

fewer candidate bounds checks for that deterministic scenario.

The full receipt lives in:

`synexia-visual-pixel-runtime/PAINT_DAG_BENCHMARK_RECEIPT.md`

A more complex index should be admitted only after representative SWT/Nebula measurements prove
the simpler index insufficient.

## Recipe-first execution on Nebula

Merged Nebula PR #43:

**M3: run guarded OpenRewrite catalogue from current Nebula master**

The canonical executor:

- runs real Maven/OpenRewrite recipes serially;
- validates source/index/plan custody;
- refuses scope escape/deletion/mode drift;
- runs validate/compile/test/verify gates between steps;
- requires the first application to change admitted source;
- replays every recipe and requires fixed point.

This is the operational form of the invariant:

**improve the recipe, then replay the recipe; do not repeatedly hand-edit targets.**

## SWT convergence status

The corresponding SWT distillation is already promoted through the viewport/render work,
including:

- Cocoa / GTK / Win32 virtual Table convergence;
- GTK / Win32 virtual Tree logical topology and viewport work;
- collapsed native-residency compaction;
- retained affine paint DAG and stable z-order;
- retained primitive `PathData`;
- edge-stroke culling regression proof;
- shared viewport runtime;
- ScrolledComposite / StyledText / List viewport integration;
- editor/feedback plane synchronization;
- DND facade-identity proofs;
- screenshot/example regression lanes;
- style/bit-admission atomization.

SWT PR #65 closes the enumerated style/bit-admission family through its canonical Synexia
recipe/proof set.

## Deliberately separate open research

The following Nebula drafts are not blockers for this UI distillation and remain separate:

- #36 — upstream pagination atom experiment;
- #37 — statement/block/loop materialization experiment; its own description explicitly forbids
  production promotion without broader gates;
- #44 — offline p2/CDateTime custody and dependency closure.

They should not be merged merely to make this receipt appear complete.

## Completion boundary

This distillation pass is complete when judged against its stated scope:

- behavioral donors reviewed;
- viewport/paint/event mechanics distilled;
- Nebula adapter applied;
- SWT integration promoted;
- generic Synexia owner created;
- OpenRewrite/Maven recipes saved;
- exact source custody recorded;
- deterministic benchmark/proof retained;
- saved branches preserved;
- unrelated drafts kept isolated.

Future work should extend these owners rather than create parallel viewport, paint-DAG, affine,
selection or recipe engines.


## Final graphics closure — 2026-10-06

The later graphics passes are now part of the same completed distillation rather than separate
experimental owners.

### Proxy GC + affine + retained DAG

The final rendering split is deliberately three-layered:

1. **Immediate dynamic paint remains the real SWT `GC`.**
2. **A scoped GC proxy guards mutable native GC state** so one viewport plane/renderer cannot leak
   clipping, transform, line, alpha, antialias, interpolation, fill-rule, XOR, color, pattern or
   font state into the next plane.
3. **Stable paint is retained in the existing primitive paint DAG** using resource-free affine
   values and primitive geometry/PathData. No second SWT/Nebula scene graph is introduced.

A separate `StrokeDag` is therefore not duplicated inside SWT or Nebula. Synexia retains the
canonical iterative primitive stroke-DAG/affine algorithm shapes; SWT `ViewportPaintGraph` and
Nebula `GridPaintDAG` are the toolkit-local retained owners.

This separation keeps native SWT resources callback-local while still allowing transform
composition, z/dependency ordering, culling and retained replay.

### Final SWT graphics parity

Merged SWT PR #83:

**Distill viewport GC scopes and affine v2 semantics**

- adds `ViewportGcProxy` as a scoped mutable-GC state guard;
- evolves `ViewportPaintGraph.Affine` with identity/no-op factories, translate/scale/rotate/shear,
  determinant/inverse and value fast paths;
- keeps application/widget callbacks on the real SWT GC;
- keeps stable/frozen decoration in the existing retained viewport DAG.

Merged SWT PR #87:

**Finish SWT viewport affine quadrant and GC restoration proof**

- canonical PI/2 rotations use exact integer matrices;
- adjacent representable angles stay on ordinary trigonometry;
- exact non-rectangular Region clipping is restored;
- foreground/background Pattern and Font state are restored;
- scope close remains safe after caller-disposed GC.

The source-sealed Synexia recipe custody is:

- #9396 — `swt-viewport-gc-affine-v2`;
- #9410 — final `swt-viewport-gc-affine-v2-final` bounded-batch repair.

### Final Nebula graphics parity

Merged Nebula PR #68 promoted the Grid graphics atoms to v2.

Merged Nebula PR #70 normalized the public/internal factory vocabulary to SWT-native
`translate(...)` / `rotate(...)` naming.

Merged Nebula PR #74:

**Finish Grid GC scope and SWT-native affine parity**

Final owners:

- `GridTransform` — finite, resource-free affine value;
- `GridGCProxy` — scoped mutable SWT-GC state guard;
- `GridPaintDAG` — retained primitive paint-plane planner;
- `GridVisibleRangeSupport` — visible-range/viewport delta owner.

The final source-sealed Synexia recipe is merged in PR #9414:

- crate: `nebula-grid-gc-affine-parity-v1`;
- Maven profile: `m3-nebula-grid-gc-affine-parity`;
- exact four-target pre/post SHA-256 custody;
- deterministic replay;
- fixed point;
- source-drift refusal;
- explicit lifecycle/affine semantic proof.

Nebula renderers still receive the real SWT GC. The proxy is a scope, not a renderer replacement.

## Expanded Java2s behavior matrix

The supplied Java2s catalogues remain behavioral/test donors only.

### SWT catalogue

The reviewed SWT catalogue concentrates useful behavior cases around:

- Canvas;
- Table / TableItem / TableColumn / Table renderer / Table editor / Table events;
- Tree / TreeItem / Tree editor / Tree events / TreeTable / TreeViewer;
- ScrolledComposite;
- ScrollBar and scroll events;
- StyledText;
- drag/drop, focus, mouse/key and generic SWT events;
- SWT/AWT/Swing interop.

These examples are valuable as small compatibility witnesses for event ordering, public item
identity, scroll semantics, editor placement, header behavior and redraw boundaries.

### SWT 2D Graphics catalogue

The reviewed graphics catalogue isolates:

- GC state;
- paint callbacks;
- line/stroke behavior;
- rectangles/arcs/polygons;
- Path;
- text/font/string drawing;
- Transform;
- animation;
- images.

Those categories directly drive the GC-proxy restoration matrix, affine composition/inversion,
stroke-edge culling, retained PathData, clipping and repaint tests.

### Swing / Swing Event catalogues

Swing is used as an independent architecture/behavior comparator for:

- JTable + JTableHeader + TableModel + renderer/editor separation;
- JTree + TreeModel + TreePath + renderer/editor + selection/expansion events;
- JScrollPane/JViewport scrolling;
- layered/overlay component ordering;
- model/listener/event separation.

Swing source is not transplanted into SWT/Nebula.

## Challenge-site algorithm donor boundary

LeetCode, HackerRank and GeeksForGeeks remain problem-family catalogues for mechanical algorithm
review rather than source-copy sources.

For this UI distillation the relevant families are:

- interval merge/intersection -> dirty/visible range composition;
- sliding window/two pointers -> visible + overscan window maintenance;
- binary search -> pixel/logical-index lookup;
- topological sort / DAG traversal -> paint dependency replay;
- prefix/Fenwick/segment-tree families -> variable-height coordinate indexing only when measured
  evidence justifies replacing the simpler index;
- iterative DFS/BFS -> stack-safe topology traversal.

Any accepted reusable algorithm belongs in Synexia's canonical owner and must be applied through
a Maven/OpenRewrite recipe, not hand-copied into a widget.

## Final saved checkpoint

Qualified Nebula product baseline before this receipt update:

`hsoliwal/nebula@0df4a4aa21fb29205ec0978dc43fb85f8f3c9ec9`

Permanent checkpoint branch:

`m3/ui-distillation-final-20261006`

The final graphics recipe is preserved in Synexia via PR #9414 and its
`nebula-grid-gc-affine-parity-v1` crate. Future work should branch from the current qualified
owners and extend those recipes rather than create another viewport, affine, GC proxy, stroke or
paint-DAG stack.
