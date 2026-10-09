# SWT / Nebula UI distillation closure v4 - 2026-10-09

This receipt is an additive successor to the historical 2026-10-08 final checkpoint and
the first 2026-10-09 closure. It does not rewrite those receipts. It records the final
GC clipping repair required by the current SWT coordinate contract and freezes the
post-v4 Nebula Grid owner set.

## Final repository heads and recipe chain

- Nebula live master after v4: `33e7c443bb52ebeb5b797dc9bfa153c8adc089c6`
- Nebula product PR: `hsoliwal/nebula#117`
- Synexia source recipe PR: `hsoliwal/com.synexia#9953`
- Synexia recipe merge: `b9a957d2cba422f809b3b15a97c3522f3c49bd5d`
- Canonical recipe wrapper: `com.synexia.rewrite.M3NebulaGridGcStateDagRecipe`
- Final GC crate: `nebula-grid-gc-state-dag-v4`
- Ordered GC recipe history: v1 -> v2 affine batching -> v3 historical device-space
  restore -> v4 current-user-space restore.

The real SWT `GC` remains the public/native resource. `GridGCProxy` remains a scoped
state compiler/proxy. `GridTransform` remains the primitive affine owner.
`GridGCStateDAG` remains the retained state-transition DAG. Renderer/application
callbacks remain immediate-mode where SWT observability requires it.

## Why v4 supersedes the v3 clipping interpretation

The pinned SWT receiver used by Nebula CI is
`25e637e40c5243f5210586007a74206d8dce7953`.

On that receiver, `GC.getClipping(Region)` returns clipping in the current user
coordinate space. The native GTK3 SWT artifact `11501527287` contains a passing
`Test_org_eclipse_swt_graphics_GC#test_getClippingRegion_respectsTransformAndPreservesHoles`
case. That test proves that after a transform-relative clip is queried into a Region,
reapplying that Region under the same current transform round-trips the same clipping
bounds and preserves holes.

The prior Nebula v3 native proof run `37807007188`, artifact `11571652792`, had one
real Grid failure:
`GridGCProxy_Test#restoresCompleteMutableGcStateAfterPlanePaint`.

Expected clipping: `Rectangle {-1, -1, 40, 41}`
Observed v3 clipping: `Rectangle {-3, -4, 39, 40}`

v4 therefore restores `originalTransform` before reapplying the Region snapshot,
because the snapshot was captured in that same user space.

GitHub Actions did not create a new workflow run for Nebula PR #117 after open,
synchronize, and reopen events. This receipt does not claim such a run. Promotion is
based on the exact source-sealed recipe plus the native SWT contract proof above and
the existing Nebula regression oracle that exposed the v3 ordering error.

## Current canonical Nebula Grid owners

The following SHA-256 values are for live Nebula master
`33e7c443bb52ebeb5b797dc9bfa153c8adc089c6`.

| Owner | SHA-256 |
| --- | --- |
| GridGCProxy_Test.java | 74bea0c4a20ec2adedaaef407c44a63190e141d1af5dfbfb12b14f2439746454 |
| GridSwtScreenshotCapture.java | c7660e9d59fef67610315507443664d7efd4fcb4d41f4807ebd7922f6d6f54e4 |
| GridViewportDistillation_Test.java | a353e5168e39a070663ad6ed5b830ceea1589b688bec4e830861df04caa58c09 |
| GridVisibleRangeSupport_Test.java | 1abaf6fbf698f87c382529383ba3ba504f19bbcec949f9bb08440854e14c2a1f |
| Grid.java | f7d70e32ea65755679a6ff26af0aee6e088538aba5ea7a99e94de7cfcae82906 |
| GridGCProxy.java | 604802f3de598f8d255654ac2e1cb3be84d1ebb6a9949ec35b15a9c114e133a8 |
| GridGCStateDAG.java | 5fa62f13f1a06e19a3bf64d15a6a25073e6fb88baa22d1f759e29c30b75945ce |
| GridPaintDAG.java | 1e026cafb1ac1768849dcebaa6a72bbe220750dee5fad409b1626120a85b4e33 |
| GridTransform.java | 7da35eccedc281b81b2d3c729db350c4564d7017577c4ab621add80ae32b19bc |
| GridViewportDamage.java | 302f91a4524975c37534fc1a20c1a7265e5cddb55c763d06a99024ec6b576f05 |
| GridVirtualItemList.java | 6fd4eb72dfb4d74d7418821c5144be2cde054bd9b17148a05fe701db9a26bb3f |
| GridVisibleRangeSupport.java | b1dd077a1802ef026eb604fcd9d49d41c12b6d189cfbd5049be7b2db2928a26f |

The eleven non-proxy owners remain byte-identical to the 2026-10-08 final verifier.
`GridGCProxy.java` is byte-identical to the v4 source-sealed postimage.

## Viewport evidence retained from the native proof

The preserved v3 Java CI artifact still proves the viewport substrate independently
of the clip-order regression:

- logical virtual rows: 1,000,000
- top-scene materialized items: 21
- middle-scene materialized items: 41
- visible rows: 20
- middle top index: 500,000
- middle selection count: 3
- retained graphics delta: 0

Thus the remaining v3 defect was GC clipping state restoration, not sparse viewport
residency or million-row projection.

## Behavioral donor boundary

Java2s catalogues remain behavior/test-shape evidence only; source bodies are not
copied:

- https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

The retained behavior catalogue covers SWT Canvas/Table/Tree/TreeViewer/
ScrolledComposite/ScrollBar/Event shapes, SWT 2D GC/paint/line/path/transform/text/
animation shapes, and Swing viewport/table/tree/header/selection/event shapes.

LeetCode, HackerRank and GeeksForGeeks remain problem/category references only.
They define serial problem-shape review queues for binary search, adaptive order,
trie/prefix/fuzzy search, top-k, primitive lookup, bounded cache, automata and related
algorithm families. Solution/editorial bodies are not copied. Permissive GitHub donors
such as fastutil, Lucene and Caffeine may supply mechanics only after provenance and
license admission.

## M3 invariants retained

1. API and observable behavior are the compatibility boundary.
2. Logical model extent is independent of native/widget/render residency.
3. Visible plus bounded overscan is the normal paint/native frontier.
4. Escaped SWT/Nebula item facades retain identity.
5. Packed state and columnar/indexed topology remain semantic authority.
6. Header/editor/scrollbar/feedback chrome remains separable from scrolling body paint.
7. Bulk changes remain redraw-bounded transactions.
8. JFace remains a model/workbench adapter; SWT owns low-level tree mechanics.
9. Repeated transformations are Maven/OpenRewrite recipe crates, not hand-edited loops.
10. Exact source hashes, fixed point and drift refusal are required for source-changing recipes.
11. Java is the semantic oracle before JNI. JNI promotion requires parity, benchmark crossover,
    bounded ABI scope and admitted permissive native donors.
12. Core/runtime operation remains deterministic; LLM assistance prepares recipes/tests/signals
    rather than becoming runtime authority.

## Freeze rule

This receipt closes the post-v4 UI distillation baseline. Future source changes must be
additive successor recipes/PRs from this state. Historical receipts and recipe packets
must not be rebased, squashed, force-pushed or rewritten.
