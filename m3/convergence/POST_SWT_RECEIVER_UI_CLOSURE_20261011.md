# Nebula post-SWT-receiver UI distillation closure — 2026-10-11

## Status

- `CURRENT_NEBULA_RUNTIME_CLOSURE=GREEN_BY_RECIPE_BOUND_SUCCESSOR`
- `CURRENT_NEBULA_MASTER=6f501f086ee2206514ad9a42237e5a54a2d7012f`
- `CURRENT_SWT_RECEIVER=695efd3c5bf984f5ff38c941c341d60ab309cfd7`
- `FINAL_ZPLANE_SUCCESSOR=NEBULA_PR_128`
- `FINAL_ZPLANE_RECIPE=SYNEXIA_PR_10315`
- `FINAL_RUNTIME_DELTA=GRID_GRIDPAINTDAG_VIEWPORT_TEST_ONLY`
- `FRESH_HOSTED_RERUN_FOR_FINAL_ZPLANE=NOT_EMITTED`
- `RUNTIME_LLM_DEPENDENCY=NONE`

This receipt closes the Nebula side after the current SWT receiver and the final executable
z-plane successor. It does not rewrite historical proof receipts and does not invent a hosted/native
result that was not emitted.

## Exact final runtime successor

Nebula PR #128 changes exactly three files from its sealed preimage
`b866cbff4e26b7aaea31d379448142aa8c7fd68a`:

- `Grid.java`;
- `GridPaintDAG.java`;
- `GridViewportDistillation_Test.java`.

The exact preimages and postimages are byte-bound by Synexia PR #10315,
`M3NebulaGridViewportZPlanesRecipe`, crate `nebula-grid-viewport-zplanes-v2`.

The final execution order is:

`background -> scrolling body -> fixed/frozen body -> header/footer chrome -> transient feedback`

Header and footer share one chrome z-rank because their rectangles do not overlap. Transient
insertion/drag feedback is painted last and body-local feedback is clipped away from chrome.
The real SWT `GC` remains the native resource.

## Distilled Grid owner set

Nebula Grid now retains:

- sparse logical row ownership independent of million-row logical extent;
- visible rows plus bounded overscan as the normal paint/materialization frontier;
- stable escaped `GridItem` identity;
- semantic state independent from transient paint/native residency;
- executable body/fixed/header/footer/feedback z-plane ordering;
- vertical body scroll independent from fixed-header repaint;
- horizontal origin shared only by planes that move horizontally;
- visible-range events decoupled from repaint;
- redraw-bounded mutation and native screenshot/OpenCV regression gates.

## Graphics owner set

SWT `GC` remains the real observable/native graphics object.

Nebula retains:

- `GridGCProxy` as the scoped state compiler/guard around the real GC;
- `GridTransform` as the primitive affine value/composition owner;
- `GridGCStateDAG` as the retained/deduplicated stroke, alpha and affine transition planner;
- `GridPaintDAG` as the ordered viewport-plane dependency owner;
- `GridViewportDamage` as the invalidation owner.

The proxy does not subclass or replace SWT GC. Affine deltas are coalesced before native handoff;
equivalent stroke/alpha transitions are collapsed; clipping and mutable GC state are restored at
renderer/plane boundaries. No JNI graphics path is admitted without differential correctness and
a measured crossover.

## Java2s / Swing donor closure

The retained behavior catalogues are:

- https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

They are behavior/test taxonomies only. No Java2s tutorial implementation body is copied. The
admitted families remain Canvas/Paint/GC/Transform, Table/Tree/TreeViewer,
ScrolledComposite/ScrollBar, owner-draw/editor behavior, JTable/JTree model-render separation,
JViewport/JScrollPane logical-origin separation, JTableHeader/JLayeredPane plane separation and
event-order/identity contracts.

## Competitive donor policy

LeetCode, HackerRank and GeeksForGeeks remain problem-shape donors only. The serial work-order
layer binds reviewed file atoms to category/problem evidence and existing owners without copying
challenge/editorial solution bodies. Direct arithmetic and compact primitive arrays remain preferred
where fixed geometry or a bounded viewport frontier makes heavier structures unnecessary.

## Recipe and branch custody

The runtime transformations and closure receipts remain owned by `hsoliwal/com.synexia`.
The final z-plane transformation is the v2 evolution of the existing Grid z-plane recipe owner,
not a parallel implementation family.

Future work must branch from this receiver and improve the recipe rather than repeat per-file LLM
edits.

## Verification boundary

Source custody is exact: the three v2 postimages in Synexia are byte-identical to the Nebula #128
runtime/test files, and their preimages are byte-identical to the sealed Nebula base. Focused GitHub
workflows created zero jobs for the final heads, so this receipt does not claim a fresh hosted
Tycho/native execution for #128. Earlier screenshot, resource, native and recipe proofs remain
retained; the final z-plane JUnit source is part of the sealed successor.

## Final invariant

The SWT/Nebula viewport distillation is closed at the exact heads recorded above. Subsequent source
changes require additive recipe successors, fixed-point/drift proof, original reactor qualification,
and preservation of public API/event/rendering behavior.

No LLM is required in the Nebula runtime/core.
