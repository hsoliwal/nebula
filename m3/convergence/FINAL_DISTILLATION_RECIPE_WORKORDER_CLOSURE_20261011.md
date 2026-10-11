# Final SWT / Nebula distillation work-order closure — 2026-10-11

This additive receipt closes the requested SWT/Nebula viewport distillation on the live Nebula master after the canonical per-file competitive problem work-order layer was admitted to Synexia.

It does not rewrite the historical closure receipts and it does not claim a fresh hosted Nebula CI run that was not emitted.

## Status

- `FINAL_DISTILLATION=GREEN_BY_RUNTIME_EQUIVALENCE`
- `NEBULA_RUNTIME_DRIFT_FROM_QUALIFIED_V5=ZERO`
- `COMPETITIVE_WORK_ORDERS=MERGED`
- `RECIPE_FIRST_INVARIANT=BOUND`
- `FRESH_HOSTED_NEBULA_RERUN=NOT_CLAIMED`

## Exact repository heads

- Nebula repository: `hsoliwal/nebula`
- live Nebula master: `b866cbff4e26b7aaea31d379448142aa8c7fd68a`
- qualified runtime/UI freeze: `ce9f7c376d1b051d9e2dbbee9b7f941362fb0504`
- current-master runtime-equivalence closure: Nebula PR #124
- licensing application: Nebula PR #126
- sealed recipe-first workflow recovery: Nebula PR #127

- canonical recipe repository: `hsoliwal/com.synexia`
- Synexia develop after work-order admission: `5c6a99fe8c0603a8e9be93f4936743089d09a3de`
- exact workload/category review repair: Synexia PR #10306, merge `bf2bc72f7d8ebdc787d8b61c0f7ec02f4ab7618d`
- recipe-bound competitive work orders: Synexia PR #10305, merge `5c6a99fe8c0603a8e9be93f4936743089d09a3de`

Comparing the qualified v5 runtime freeze to the live Nebula master changes only M3 convergence/proof/workflow/licensing files. No Grid runtime Java, JNI/native source, public widget API, renderer owner, screenshot oracle, or SWT receiver source changed after the qualified runtime freeze.

## Final viewport architecture

The admitted runtime remains:

1. logical extent independent from Java/native/render residency;
2. visible rows plus bounded overscan as the normal work frontier;
3. sparse indexed/columnar ownership for cold logical coordinates;
4. stable escaped item-facade identity;
5. semantic item state independent from transient paint/native residency;
6. body/fixed/header/editor/feedback z-plane separation;
7. vertical body scrolling independent from fixed header repaint;
8. horizontal projection coupled only to planes that actually move;
9. redraw-bounded bulk mutation;
10. low-level viewport/tree mechanics below JFace/Workbench model adapters.

## Proxy GC / affine / stroke DAG closure

SWT `GC` remains the real observable/native graphics resource. It is not subclassed or replaced.

Nebula Grid retains:

- `GridGCProxy` as the scoped state compiler around the real GC;
- `GridTransform` as the primitive affine value/composition owner;
- `GridGCStateDAG` as the retained/deduplicated affine, stroke and alpha state DAG;
- `GridPaintDAG` as the ordered viewport-plane dependency owner;
- `GridViewportDamage` as the damage-region owner.

Affine deltas remain ordered and are coalesced before the renderer boundary. Equivalent stroke and alpha states collapse in the retained state graph. Identity affine batches do not cross into the native GC. Clipping and mutable SWT GC state are restored at scope close.

No new JNI implementation is admitted for this graphics layer. Java remains semantic authority. A native/JNI candidate still requires differential correctness, explicit ownership/lifetime/thread rules, a bounded ABI surface and a repeatable benchmark crossover.

## Java2s / Swing behavior donor closure

The four requested public catalogues remain behavior/test taxonomies only:

- SWT — `https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html`
- SWT 2D Graphics — `https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html`
- Swing — `https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html`
- Swing Event — `https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html`

The retained behavior families cover Canvas/Paint/GC/Transform, Table/Tree/TreeViewer, ScrolledComposite/ScrollBar, owner-draw/editor behavior, JTable/JTree model-render separation, JViewport/JScrollPane origin separation, JTableHeader/JLayeredPane, and selection/focus/mouse/key/tree expansion/model/selection events.

No Java2s tutorial implementation body is copied into Nebula or Synexia.

## Competitive donor work-order closure

LeetCode, HackerRank and GeeksForGeeks are taxonomy/problem-shape evidence only. Their editorials, submissions and solution bodies are not source-copy authority.

`M3CompetitiveProblemWorkOrderRecipe` now binds every reviewed file atom to exactly three serial work orders:

`LeetCode -> HackerRank -> GeeksForGeeks`

Each work order binds:

- the file atom and contract root;
- canonical `AlgorithmShape`;
- category/problem evidence and complexity profile;
- existing Java owner when one exists;
- optional JNI/native shadow only when already admitted;
- parity/correctness tests;
- reusable recipe id;
- explicit no-copy, no-substitution and no-JNI-promotion authority.

An unbound shape emits `GAP_REQUIRES_RECIPE`; it does not fabricate an implementation owner.

The retained UI-relevant problem-shape queue remains:

1. lower-bound/binary/galloping search for sparse sorted coordinates;
2. interval merge/split/overlap for visible and damage ranges;
3. prefix sums and measured Fenwick/segment structures for variable-height rank/offset mapping;
4. iterative DFS/BFS for expansion, release, accessibility and projection traversal;
5. packed bitsets/masks for item state;
6. bounded cache/eviction for native/render residency;
7. finite-state/automata shapes for event and graphics-state convergence.

Direct arithmetic and compact primitive arrays remain preferred when fixed geometry or a bounded frontier makes heavier structures unnecessary.

## M3 invariant

For every LLM-assisted source task:

- create or reuse the Maven/OpenRewrite recipe first;
- improve the recipe and its tests rather than hand-editing each target;
- replay only from exact admitted preimages;
- require fixed point and source-drift refusal;
- preserve public API, event ordering and observable behavior;
- keep file-local mutation before package/module/reactor/API escalation;
- keep Java as correctness oracle before any JNI/native promotion;
- preserve additive merge ancestry; no rebase, squash or force-push is required for this closure.

The runtime/core has no LLM dependency.

## Final application rule

This receipt is the successor closure for the live Nebula master named above. Its exact postimage is owned by the canonical Synexia recipe crate and is applied to Nebula through a dedicated branch and pull request.

Future Nebula runtime changes must be additive successor recipes from this baseline. Existing source-sealed postimages, donor catalogues, proof receipts and merge ancestry remain immutable.
