# M3 SWT/Nebula UI Distillation Closure — 2026-10-09

## Frozen live refs

- Nebula live master at closure start: `1242068afee7721a26949f08b3c7dfbebf4ff712`
- Synexia live develop at closure start: `e4939c9b1ff0e5ca45bf5a4538f851f7aa3cb3f0`
- Nebula closure branch: `m3/ui-distillation-closure-20261009`
- Synexia closure branch: `m3/ui-distillation-closure-20261009`

This receipt does not rewrite the historical 2026-10-08 checkpoint.  The admitted chain is:

```
M3UiDistillationFinal20261008Recipe
  -> M3NebulaGridGcStateDagRecipe / nebula-grid-gc-state-dag-v3
  -> this 2026-10-09 closure receipt
```

The Oct-8 snapshot remains historical custody.  The v3 recipe is the canonical source-changing
successor for the device-space clipping repair.  This receipt seals the final convergence graph
instead of duplicating already sealed Java postimages.

## Final runtime contract

The distilled SWT/Nebula runtime keeps public SWT/Nebula behavior authoritative while private
mechanics use:

1. logical model extent independent from widget/native residency;
2. sparse materialization with stable escaped facade identity;
3. visible-window plus bounded overscan work;
4. independently invalidated body/fixed/header/footer/editor/scrollbar/feedback z-planes;
5. viewport events independent from repaint;
6. fixed header chrome isolated from vertical body scrolling;
7. the real SWT `GC` as the public/native drawing resource;
8. `GridGCProxy` as a scoped state compiler, never a replacement public GC;
9. `GridTransform` as primitive affine state/composition;
10. `GridGCStateDAG` as equivalent stroke/alpha/affine transition coalescing;
11. device-space clip restoration before original affine state is reinstated;
12. retained paint DAGs only for stable/reusable dependencies;
13. immediate-mode application/renderer callbacks preserved where observability requires it;
14. model/topology/selection/expansion below JFace renderer mechanics;
15. Java semantic authority before any JNI/native promotion.

## Supplied Java2s catalogue closure

The exact user-supplied catalogues remain behavioral/test-shape evidence only.  No Java2s
implementation body is copied.

- SWT:
  https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- SWT 2D Graphics:
  https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- Swing:
  https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- Swing Event:
  https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

The final catalogue review explicitly retains these relevant category counts/shapes:

### SWT

- Canvas: 5
- Table: 18
- TableItem: 11
- Table Event: 11
- Table Editor: 8
- Table Renderer: 4
- Tree: 8
- Tree Editor: 7
- Tree Event: 8
- TreeColumn / TreeTable: 5
- TreeViewer: 4
- ScrolledComposite: 8
- ScrollBar: 3
- ScrollBar Event: 1
- SWT Event: 24
- WIN32 examples: 10

### SWT 2D Graphics

- GC: 2
- SWT Paint: 4
- Line: 6
- Path: 2
- Transform: 4
- Draw String: 8
- Animation: 2

These are mapped to clipping, affine ordering, stroke state, paint invalidation, renderer state
restoration, and screenshot/damage proofs.

### Swing

- JScrollPane: 15
- JScrollBar: 5
- JViewport: 2
- JLayeredPane: 4
- JTable: 59
- JTable Model: 31
- JTable Renderer Editor: 20
- JTableHeader: 11
- JTable Column: 31
- JTable Sort: 9
- JTable Filter: 4
- Table Selection: 21
- JTree: 35
- JTree Node: 15
- TreeModel: 6
- JTree Editor Renderer: 16
- JTree Selection: 8

These remain evidence for model/render separation, reusable renderer/editor shells, fixed header
planes, viewport origin separation, sorting/filtering/selection semantics and layered chrome.

### Swing Event

The closure explicitly includes:

- Event / Event Adapter / Action / InputMap / ActionListener;
- AdjustmentListener;
- ListSelectionListener;
- Mouse / MouseListener / MouseMotionListener / MouseWheelListener;
- TableModelListener;
- TreeExpandedListener;
- TreeModelListener;
- TreeSelectionListener;
- TreeWillExpandListener;
- focus/component/property/window event families.

These are compatibility evidence for ordering, source identity and event granularity; they are not
license authority for source copying.

## Competitive problem-shape lane

LeetCode, HackerRank and GeeksForGeeks remain taxonomy/evaluation evidence only.  Their challenge
statements, editorials and solution bodies are not copied.

Canonical Synexia owners already implement the requested serial review path:

```
problem category
  -> deterministic review queue
  -> contract-frozen work order
  -> exact file + exact atom
  -> Java oracle
  -> admitted permissive GitHub donor or clean-room atom
  -> optional JNI candidate
  -> PARSE / CONTRACT / COMPILE / LINT / UNIT_TEST / INTEGRATION_TEST / BENCHMARK
  -> one serial file publication
  -> re-hash and re-atomize
```

Fast-search priority remains:

- prefix/suffix/trie geometry;
- lower-bound, binary and galloping search;
- KMP / Z / rolling hash;
- Aho-Corasick / automata;
- suffix indexes;
- bitmap/posting intersections;
- interval/range algorithms;
- graph traversal/topological replay;
- packed masks/bitsets;
- mmap/indexed layouts;
- regex candidate pruning;
- JNI only for measured bulk kernels after Java/native parity.

## JNI / Java invariant

No native implementation becomes semantic authority merely because it is faster.

```
Java oracle
  -> pinned permissive native donor/mechanics
  -> explicit license gate
  -> ABI + bounds proof
  -> deterministic replay
  -> Java/native parity
  -> setup-inclusive benchmark
  -> batch-size crossover
  -> review admission
```

The Java path remains the correctness fallback.

## Canonical recipe custody

Current source-changing runtime custody remains in canonical Synexia recipes, including:

- `m3-nebula-grid-true-virtual-storage`
- `m3-nebula-grid-viewport-event`
- `m3-nebula-grid-viewport-zplanes`
- `m3-nebula-grid-gc-state-dag`
- `m3-nebula-grid-gc-affine-parity`
- `m3-nebula-grid-swt-native-screenshot`
- `m3-nebula-java2s-behavior-coverage`
- `m3-fast-search-recipe-first`
- `m3-ui-distillation-final-20261008`

The companion Synexia Oct-9 closure recipe seals this receipt and the custody graph; it does not
create another implementation owner.

## Closure rule

The distillation is considered closed only while all of these remain true:

- public SWT/Nebula contracts stay unchanged;
- exact source recipe custody is retained;
- Java2s/judge platforms remain behavioral taxonomy, not source-copy authority;
- permissive GitHub donors retain pinned revision/license provenance;
- source-changing work remains recipe-first;
- repeated edits improve the recipe rather than hand-editing files;
- full original build/test/screenshot/benchmark gates remain product authority;
- any future regression is repaired additively from this closure, never by rewriting history.
