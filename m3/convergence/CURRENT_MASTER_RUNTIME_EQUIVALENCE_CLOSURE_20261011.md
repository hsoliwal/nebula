# Current-master runtime-equivalence closure — 2026-10-11

This receipt closes the SWT/Nebula UI distillation on the live Nebula master without rewriting the historical hosted-proof request.

## Status

- `CURRENT_MASTER_RUNTIME_EQUIVALENCE=GREEN`
- `FRESH_HOSTED_RERUN=NOT_EMITTED`
- `RUNTIME_SOURCE_DRIFT_FROM_QUALIFIED_V5=ZERO`

This is not a claim that PR #123 emitted a fresh hosted CI run. The GitHub run list for its head was empty when this closure was prepared. The earlier fail-closed request therefore remains historical evidence and is not edited.

## Exact receiver and qualified ancestor

- repository: `hsoliwal/nebula`
- live master: `bb4fc921c100432ddf7fc78c590588e28193fe96`
- final proof-request PR: `hsoliwal/nebula#123`
- proof-request merge: `bb4fc921c100432ddf7fc78c590588e28193fe96`
- previously qualified v5 runtime freeze: `ce9f7c376d1b051d9e2dbbee9b7f941362fb0504`
- v5 donor-catalogue PR: `hsoliwal/nebula#120`

The Git comparison from the v5 freeze to the live master changes only convergence/proof infrastructure:

- `.github/workflows/m3-nebula-recipe-first.yml`
- `m3/catalogue/current-master-reproof.tsv`
- `m3/catalogue/pure-int-public-receiver.tsv`
- `m3/convergence/CURRENT_MASTER_PROOF_20261011.md`
- `m3/convergence/UI_DISTILLATION_CLOSURE_V5_20261010.md`
- `m3/convergence/install_bound_synexia_recipe.sh`

No Grid runtime Java, JNI/native source, public widget API, renderer owner, screenshot oracle, or SWT receiving implementation changes in that range.

## Final distilled runtime

The admitted runtime remains the v5 owner set:

- sparse logical Grid residency independent of million-row logical extent;
- visible rows plus bounded overscan as the normal paint/materialization frontier;
- stable escaped item-facade identity;
- semantic item state independent from transient native/paint residency;
- body/fixed/header/editor/feedback z-plane separation;
- vertical body scrolling independent from fixed header repaint;
- horizontal projection shared only by the planes that actually move;
- redraw-bounded bulk mutation;
- JFace/Workbench kept as model/workbench adapters while low-level viewport mechanics live below them.

## Graphics closure

SWT `GC` remains the real public/native graphics resource.

Nebula keeps:

- `GridGCProxy` as a scoped proxy/state compiler around the real `GC`;
- `GridTransform` as the primitive affine value/composition owner;
- `GridGCStateDAG` as the retained stroke/alpha/affine transition DAG;
- `GridPaintDAG` as the ordered viewport-plane dependency owner;
- `GridViewportDamage` as the damage owner.

Ordered affine deltas are coalesced before native handoff. Repeated equivalent stroke/alpha states are eliminated. Clipping and the full mutable SWT GC state are restored at the viewport-plane boundary. Application renderer callbacks retain SWT GC identity wherever that identity is observable.

No new JNI implementation is admitted for this graphics layer. Java remains semantic authority; native promotion still requires differential correctness plus a measured crossover that justifies ABI/lifetime/thread risk.

## Java2s / Swing behavior closure

The four requested catalogues are retained as behavior/test taxonomies only:

- SWT — `https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html`
- SWT 2D Graphics — `https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html`
- Swing — `https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html`
- Swing Event — `https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html`

Their admitted behavior families include Canvas/Paint/GC/Transform, Table/Tree/TreeViewer, ScrolledComposite/ScrollBar, owner-draw/editor/event ordering, JTable/JTree model-render separation, JViewport/JScrollPane origin separation, JTableHeader, JLayeredPane, selection, focus, mouse/key and tree expansion/model/selection events.

No Java2s tutorial implementation body is copied.

## Competitive algorithm donor closure

LeetCode, HackerRank and GeeksForGeeks remain category/problem-shape donors only. Challenge editorials and solution bodies are not copied.

The retained serial queue is:

1. lower-bound/binary/galloping search for sparse sorted coordinates;
2. interval merge/split/overlap for visible and damage ranges;
3. prefix sums and, only where measured, Fenwick/segment structures for variable-height rank/offset mapping;
4. iterative DFS/BFS for expansion, release, accessibility and projection traversal;
5. packed masks/bitsets for item state;
6. bounded cache/eviction for native/render residency;
7. finite-state/automata shapes for event and graphics-state convergence.

Direct arithmetic and compact primitive arrays remain preferred when row geometry is fixed or the viewport frontier is bounded.

## Canonical Synexia recipe custody

Runtime mutation and proof custody remain singular in `hsoliwal/com.synexia`, including:

- `M3NebulaGridTrueVirtualStorageRecipe`
- `M3NebulaGridViewportDistillationRecipe`
- `M3NebulaGridViewportEventRecipe`
- `M3NebulaGridViewportZPlanesRecipe`
- `M3NebulaGridGcStateDagRecipe`
- `M3NebulaGridGcAffineParityRecipe`
- `M3NebulaGridSwtNativeScreenshotRecipe`
- `M3NebulaJava2sSwingViewportDistillationRecipe`
- `M3NebulaJava2sDonorRefresh20261010Recipe`
- `M3NebulaUiDistillationFinalRecipe`
- `M3NebulaCurrentMasterProofRequest20261011Recipe`

This closure is itself source-sealed by a successor hash-pinned text recipe. It introduces no duplicate runtime owner.

## Final invariant

The distillation is closed at Nebula master `bb4fc921c100432ddf7fc78c590588e28193fe96` by exact runtime equivalence to the already-qualified v5 freeze.

Future source changes must be additive successor recipes from this baseline. Existing source-sealed postimages, proof requests, closure receipts, and merge ancestry remain immutable. No LLM is required in the runtime/core.
