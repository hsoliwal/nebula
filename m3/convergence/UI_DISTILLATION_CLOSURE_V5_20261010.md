# SWT / Nebula UI distillation closure v5 - 2026-10-10

This receipt is the additive successor to the 2026-10-09 v4 closure. It does not
rewrite any historical checkpoint or source-changing recipe. It freezes the final
current-master distillation after the Java2s/Swing donor catalogue refresh requested
on 2026-10-10.

## Final repository heads and recipe chain

- Nebula final master: `ce9f7c376d1b051d9e2dbbee9b7f941362fb0504`
- Nebula final donor-catalogue PR: `hsoliwal/nebula#120`
- Nebula donor-catalogue merge: `ce9f7c376d1b051d9e2dbbee9b7f941362fb0504`
- Synexia donor-catalogue recipe PR: `hsoliwal/com.synexia#10095`
- Synexia donor-catalogue recipe merge: `d8e1d3d8ce289f77aeec2d9365ed80b3e5b3218d`
- Donor-catalogue recipe wrapper:
  `com.synexia.rewrite.M3NebulaJava2sDonorRefresh20261010Recipe`
- Donor-catalogue crate: `nebula-java2s-donor-refresh-20261010-v1`
- Final donor-catalogue SHA-256:
  `33560fca466ae192882ccb27ef503d0e16445905a2b8b1e6f1e3e19d90b39d59`

The runtime/UI implementation remains the v4 owner set. Comparing the v4 runtime
freeze commit `33e7c443bb52ebeb5b797dc9bfa153c8adc089c6` with this final master changes only
convergence documentation: the v4 closure receipt is added and the Java2s/Swing donor
catalogue is refreshed. No Grid runtime Java, JNI/native owner, public API, renderer
contract or screenshot oracle changes after the v4 runtime freeze.

## Final graphics architecture

SWT `GC` remains the real public/native graphics resource.

Nebula Grid keeps:

- `GridGCProxy` as the scoped graphics-state compiler/proxy;
- `GridTransform` as the primitive affine value/composition owner;
- `GridGCStateDAG` as the retained and deduplicated stroke/alpha/affine transition DAG;
- `GridPaintDAG` as the ordered viewport-plane dependency owner;
- `GridViewportDamage` as the damage-region owner;
- `GridVisibleRangeSupport` as the visible-range delta owner;
- `GridVirtualItemList` as the sparse flat virtual-row owner.

The proxy restores complete mutable SWT GC state at the viewport-plane boundary.
Affine deltas are coalesced before crossing into the real GC. Equivalent stroke and
alpha requests collapse to retained DAG state rather than causing redundant native
mutations. Application renderer callbacks remain immediate-mode wherever SWT
observability requires the real GC identity.

No JNI implementation is admitted for the GC state DAG. Java already batches native
transitions at the renderer boundary. Native promotion remains conditional on a
repeatable benchmark crossover, exact Java/native differential behavior, explicit
ownership/lifetime/thread rules, bounded ABI scope and admitted permissive donors.

## Final viewport architecture

The distilled Grid/SWT model is:

1. logical extent is independent from Java/native/render residency;
2. visible rows plus bounded overscan form the normal work frontier;
3. sparse indexed/columnar ownership represents cold logical coordinates without
   manufacturing facades;
4. escaped item facades retain identity and are never rebound to another coordinate;
5. semantic item state remains separate from transient paint/native residency;
6. vertical scrolling damages the body plane without repainting fixed header/footer
   chrome;
7. horizontal scrolling may couple body and horizontally projected header/fixed
   planes;
8. body, fixed content, header/editor chrome and transient feedback are ordered
   z-planes;
9. bulk mutations are redraw-bounded transactions;
10. JFace/Workbench remain model/workbench adapters while SWT owns low-level tree and
    viewport mechanics.

The retained native proof evidence includes the million-row virtual Grid case with
bounded materialized residency and screenshot qualification. The v4 GC clipping
receipt remains authoritative for the current SWT clipping coordinate contract.

## Java2s / Swing donor catalogue closure

The four requested catalogues were reviewed as behavior taxonomies, not source donors:

- `https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html`
- `https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html`
- `https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html`
- `https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html`

The reviewed SWT catalogue explicitly contains Canvas, Table/TableItem/TableColumn,
Table Event/Cursor/Editor/Renderer, Tree/TreeItem/Tree Event/Tree Editor/TreeTable,
TreeViewer, ScrolledComposite, ScrollBar/ScrollBar Event, generic SWT events, key and
mouse events, drag/drop, StyledText/TextLayout and screen capture categories.

The SWT 2D catalogue explicitly contains GC, Paint, line/rectangle/polygon/path,
focus, font/string, Transform, animation and image categories.

The Swing catalogue provides independent behavior shapes for JList, JTable and its
model/renderer/editor/header/column/sort/filter families, JTree/TreeModel, JScrollPane,
JScrollBar, JViewport, JLayeredPane, DebugGraphics, SwingWorker, accessibility and
SwingUtilities.

The Swing Event catalogue provides event-order/source-identity test shapes for
adjustment, focus, key, mouse, list-data/list-selection, table-model, tree expansion,
tree-model, tree-selection and tree-will-expand events.

No Java2s tutorial implementation is copied into Nebula or Synexia.

## Competitive algorithm donor closure

LeetCode, HackerRank and GeeksForGeeks remain problem/category donors only. Solution
and editorial bodies are not copied.

The admitted serial review queue is:

1. binary search / lower-bound for compact sorted sparse owners;
2. interval merge/split/overlap for visible and damaged ranges;
3. prefix sums for static rank/offset mapping;
4. Fenwick/Binary Indexed Trees for variable-height point updates plus prefix/rank
   queries;
5. segment trees/lazy propagation only when measured online range updates and queries
   justify the heavier structure;
6. iterative DFS/BFS for expansion, accessibility and release traversal;
7. bit manipulation/bitsets for packed state;
8. bounded caches/eviction for transient render/native residency;
9. finite-state/automata shapes for event and graphics-state convergence.

Direct arithmetic and compact sorted primitive arrays remain preferred for fixed row
height and bounded viewport frontiers.

## JNI / Java closure

Java is the semantic oracle.

JNI/native promotion is not an optimization default. It is admitted only after:

- a fixed Java implementation;
- compiler/JUnit/native screenshot or equivalent behavioral proof;
- differential Java/native tests;
- a repeatable benchmark crossover;
- explicit ownership, lifetime and thread rules;
- bounded ABI scope;
- admitted permissive donor provenance.

The current Nebula viewport/graphics distillation does not require a new JNI path.

## M3 closure

This final state keeps the M3 invariants:

- recipe-first source mutation;
- exact hash-pinned pre/post custody;
- fixed-point replay and source-drift refusal;
- file-local work before package/module/API escalation;
- additive superset preservation;
- serial mutation with parallelism only for proven independent target sets;
- public API and observable behavior as the compatibility boundary;
- deterministic runtime/core with no LLM dependency;
- no rebase, squash, force-push or historical receipt rewriting.

The Java2s/Swing refresh is already source-sealed by Synexia PR #10095 and applied by
Nebula PR #120. This receipt introduces no new runtime owner. It closes the requested
SWT/Nebula distillation on the live 2026-10-10 master.

## Freeze rule

The distillation task is closed at Nebula master
`ce9f7c376d1b051d9e2dbbee9b7f941362fb0504`.

Future source changes must be additive successor recipes/PRs from this baseline.
Historical receipts, source-sealed postimages and merge ancestry remain immutable.
