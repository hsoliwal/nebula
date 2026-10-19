# Final SWT/Nebula viewport distillation closure — 2026-10-14

This is the current-head successor to the historical 2026-10-12 closure. It records the
distilled runtime that is already present in the repositories and the exact proof boundary.
It is documentation/provenance only: it does not rename public SWT/Nebula APIs, introduce a
second renderer or GC, copy Java2s/competitive-programming source, or grant native promotion.

## Exact current receivers

- Nebula: `hsoliwal/nebula` master `ccbe02eba4346994f76c352e93409082bb29de71`
- SWT: `hsoliwal/eclipse.platform.swt` master `c30f44c91457a96d33d90134dbc37eb3c8dccb3a`
- Synexia recipe authority at packet preparation: `hsoliwal/com.synexia` develop `7b30f5673bbf4412bdf69880631739ebf7f0ce38`
- Historical predecessor: `m3/convergence/FINAL_UI_DISTILLATION_CLOSURE_20261012.md`

## Distilled architecture

1. **The widget is a viewport over logical state.**
   Logical extent is independent of Java facade/native/render residency. Tables, trees, lists,
   StyledText and Nebula Grid retain sparse/paged/packed state and materialize only demanded
   coordinates. Once a public Item facade escapes, its identity is pinned.
2. **Painting is damage- and viewport-bounded.**
   Work is driven by visible rows/columns plus bounded overscan and explicit damage. Hidden,
   empty or unrelated planes do not earn paint/layout work.
3. **Viewport chrome is a separate z-domain.**
   Body, fixed/frozen content, headers/footers/editors/scrollbars and transient feedback have
   explicit ordering. Vertical body scrolling does not semantically require a header repaint;
   horizontal projection couples only the scroll-coupled chrome.
4. **Tree topology is model authority, not native-widget ownership.**
   SWT `VirtualTreeTopology`, visible projection and viewport owners retain logical child counts,
   expansion/selection/state and visible-row coordinates independently of native row residency.
   Bulk expansion/collapse lives below JFace; JFace is model/adaptation logic.
5. **The real SWT GC remains the observable graphics boundary.**
   Nebula `GridGCProxy` is a scoped state/compiler guard around the real final `GC`, not a
   replacement graphics API. `GridGCStateDAG` deduplicates stroke/alpha state and composes ordered
   affine deltas; `GridPaintDAG` orders paint planes.
6. **SWT owns retained backend command replay.**
   SWT's packed `OpStream` now retains absolute affine state for the existing retained-plane/Skija
   backend. Nebula does not duplicate that command stream or depend on SWT internal GPU classes.
   The layers are complementary: Nebula scopes Grid renderer state; SWT may retain/replay the
   resulting ordinary GC operations underneath.
7. **Java remains semantic authority before JNI/native promotion.**
   Native/JNI candidates require exact differential behavior, explicit ownership/lifetime/thread
   rules and a repeatable target-specific crossover. No native implementation is admitted merely
   because an operation can be expressed in JNI.
8. **Recipes are the repeatable mutation unit.**
   LLM-assisted changes are retained as Maven/OpenRewrite recipes with exact pre/post custody,
   drift refusal, fixed-point replay and original compiler/test/runtime authority.

## Current Nebula owner evidence

- `Grid.java`: `f7dd8beccd3f501d05aeb429d08ccc0c30429d7c`
- `GridGCProxy.java`: `6d9e2f715e4058c70a4561cde3795ebaa0753156`
- `GridGCStateDAG.java`: `d44ac1ce3a6ead23ce2592c1a76bf2f433f81027`
- `GridPaintDAG.java`: `7713c58b1dfc007d47f5d22b364d4f1bbb3623d3`
- `GridVirtualItemList.java`: `2b2d5397012c7afd34b75f9b6f9c453d3de76064`
- `HeapGrowth.java`: `3c5a9446a230d03ddf9b6509235cfb9edc15c4f1`
- `RowBitLanes.java`: `82abc3c3ca1d2c2c440d8d0b8f5f3c90c9ae8373`
- `GridViewportDistillation_Test.java`: `7dd8e7a3dd358e705f9fd61ac6030c8673ae99ea`
- Java2s/SWT/Swing behavior catalogue: `cfae0ab629e1a4744aab2e5034ed082c2bacf04b`
- corrected current-master reproof test: `aa56447c9fb9e2970af8dae860fbdea3a6926289`

The current line includes the merged tree/table-family viewport work (Nebula #150), packed Grid
sort/filter/grouping/pinning and residency model (#151), Xmx-aware automatic growth and sparse
billion-row masks (#161), superset consolidation (#162), and exact recipe-first reproof header
repair/application (#163/#164).

## Current SWT owner evidence

- retained affine `OpStream.java`: `1ec477dc675be55115af1607eceac0a38810d8cd`
- `VirtualTreeTopology.java`: `fb2ddcdc152ff0f55203de3d226701ce767e4af8`
- `VirtualTreeViewport.java`: `69f53285960e17175359bae6684086f3cbf9e94c`
- `ViewportLayerState.java`: `33394d85b5f8bd3d499ba1ce503ec3b161d10a62`
- retained-plane JUnit proof: `be252291c8b3e52e759080bc9077199d8ee8054c`

SWT #143 closes the affine retained-command seam without creating a second scene graph:
ordinary transformed retained paint can be recorded and replayed by the existing packed stream,
while transform-local clipping remains fail-closed until its exact semantics have independent proof.

## Behavioral donor closure

The requested Java2s catalogues remain behavior taxonomies only:

- SWT: Canvas, Table/Tree, virtual SetData, editors/renderers/events, ScrolledComposite,
  ScrollBar, StyledText, drag/drop and capture behavior;
- SWT 2D Graphics: GC, Paint, strokes, paths, clipping, transforms, animation and images;
- Swing: JViewport/JScrollPane, JTable/JTableHeader, JTree/TreeModel, JLayeredPane,
  reusable renderers/editors and accessibility;
- Swing Event: selection, tree expansion/model, focus/key/mouse and listener ordering.

No Java2s example body is copied.

LeetCode, HackerRank and GeeksForGeeks remain category/problem-shape donors only. The retained
queue covers binary/lower-bound search, interval work, prefix/Fenwick/segment rank mapping,
tries, top-k/lazy priority frontiers, iterative tree/graph traversal, bitset packing, bounded
caches and event/state-machine shapes. Challenge/editorial solution bodies are not copied.
A heavier structure is admitted only when its query/update shape and measured crossover justify it.

## Proof and compatibility boundary

The retained proof corpus includes sparse million/billion-row logical models, distant viewport
movement and return, facade identity/state retention, visible-range deltas, owner-draw state
isolation, nested/non-commuting affine transforms, z-plane ordering, damage-only repaint, native
screen/control capture checks, resource-lifetime checks and recipe replay/fixed-point/drift refusal.

The source-level endpoint now includes the Oct-14 reproof-header repair. The newly introduced
qualification workflow for that proof did not execute on the fork before merge; therefore this
receipt does **not** invent a fresh all-platform hosted Tycho/native green result.

`SOURCE_IMPLEMENTATION=COMPLETE_FOR_RECORDED_DISTILLATION`

`RECIPE_CUSTODY=COMPLETE_FOR_RECORDED_PACKETS`

`JAVA2S_DONOR_DISTILLATION=COMPLETE_FOR_RECORDED_CATALOGUES`

`COMPETITIVE_DONOR_ADMISSION=PROBLEM_SHAPES_ONLY`

`NATIVE_CURRENT_HEAD_EXECUTION=NOT_RUN_OR_EXTERNAL_CI_REQUIRED`

No manual screenshot sign-off substitutes for automated evidence. No rebase, squash, force-push,
history rewrite, API renaming, Swing facade, second GC/scene graph, donor-source copying or
relicensing is introduced by this closure.
