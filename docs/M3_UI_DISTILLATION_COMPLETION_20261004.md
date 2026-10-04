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
