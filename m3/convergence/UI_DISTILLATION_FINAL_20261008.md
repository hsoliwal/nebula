# M3 SWT/Nebula UI Distillation Final Checkpoint — 2026-10-08

## Frozen product refs

- SWT checkpoint branch: `m3/ui-distillation-final-20261008`
- SWT source head at checkpoint: `17c8d0bc104018cd93eeeee447632afc34730861`
- Nebula checkpoint branch: `m3/ui-distillation-final-20261008`
- Nebula source head before this receipt: `49773fe935e4da36f527bf3b3b2a510b1ddf486e`
- Synexia recipe branch: `m3/ui-distillation-final-20261008`
- Synexia develop baseline before this receipt: `68c13bc81c2a7d6f42926f62fb3babe1c3810e39`

No rebase, squash, force-push, or public API rewrite is part of this checkpoint.

## Distilled runtime contract

The completed receiver keeps SWT/Nebula public contracts authoritative while changing private mechanics:

1. logical item/model extent is independent from native/widget residency;
2. virtual rows use sparse materialization and preserve escaped facade identity;
3. viewport work is visible-window plus bounded overscan, not whole-model traversal;
4. body, fixed/frozen content, header/footer chrome, editor, scrollbar and transient feedback are independent damage/z-planes;
5. viewport events are published independently from repaint;
6. vertical scrolling does not repaint fixed column chrome; horizontal scrolling invalidates x-coupled body/chrome projection;
7. the real SWT `GC` remains the public/native drawing resource — it is not subclassed or replaced;
8. `GridGCProxy` is a scoped state compiler around the raw GC and restores clipping/render state;
9. affine state is retained as primitive value lanes and coalesced at renderer handoff;
10. `GridGCStateDAG` deduplicates equivalent stroke/alpha requests and batches ordered affine deltas;
11. retained paint DAGs are restricted to stable/reusable paint dependencies; dynamic application/renderer callbacks remain immediate-mode;
12. JFace/Workbench remain model/command/persistence layers; low-level viewport, bulk tree expansion/collapse and residency mechanics belong below them in SWT;
13. Java remains semantic authority for JNI candidates; native promotion requires ABI, bounds, parity, deterministic replay and setup-inclusive benchmark proof.

## Final Nebula owner set

The source-sealed final-state verifier covers these live owners/tests:

- `Grid.java`
- `GridVirtualItemList.java`
- `GridViewportDamage.java`
- `GridPaintDAG.java`
- `GridGCProxy.java`
- `GridGCStateDAG.java`
- `GridTransform.java`
- `GridVisibleRangeSupport.java`
- `GridViewportDistillation_Test.java`
- `GridVisibleRangeSupport_Test.java`
- `GridGCProxy_Test.java`
- `GridSwtScreenshotCapture.java`

These are verified as exact read-only postimages by the companion Synexia Java snapshot crate.

## Behavioral donor catalogues

The following public catalogues are behavior/test-shape evidence only; their implementation source is not copied:

- SWT: https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html
- SWT 2D Graphics: https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html
- Swing: https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html
- Swing Event: https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html

The admitted behavior families include SWT Table/Tree/TreeViewer, Canvas/Paint, ScrolledComposite/ScrollBar, owner draw, GC/Transform/Path/stroke, Swing JTable/JTree model-renderer-editor separation, viewport/scroll separation, layered chrome/overlay composition, selection/expansion/focus and event-order compatibility.

The canonical Java2s behavior-coverage lane retains the complete reviewed taxonomy and maps it to existing SWT/Nebula/Synexia owners rather than creating another UI runtime.

## Algorithm/problem-shape donors

LeetCode, HackerRank and GeeksForGeeks remain category/problem-shape donors only.

Admitted reusable families include:

- lower-bound/binary/galloping search for sparse coordinates and visible-window lookup;
- interval merge/split/intersection for dirty, selected and viewport ranges;
- prefix/Fenwick/segment structures only for variable-height/rank workloads where direct arithmetic is not O(1);
- BFS/DFS/topological traversal for bulk tree operations, release and paint-DAG replay;
- sorting/search, graph, greedy/dynamic-programming, recursion and bit-manipulation categories as review evidence;
- packed masks/bitsets as the default per-item state representation.

Challenge solution bodies are not copied. Every source-changing promotion still requires an exact target atom, contract hash, deterministic recipe packet, differential proof and PARSE → CONTRACT → COMPILE → LINT → UNIT_TEST → INTEGRATION_TEST → BENCHMARK gates.

## Canonical Synexia recipe entry points

The final receiver reuses the existing canonical profiles rather than creating competing recipe owners:

- `m3-nebula-grid-true-virtual-storage`
- `m3-nebula-grid-viewport-event`
- `m3-nebula-grid-gc-state-dag`
- `m3-nebula-grid-graphics-v2`
- `m3-nebula-grid-gc-affine-parity`
- `m3-nebula-grid-swt-native-screenshot`
- `m3-nebula-java2s-behavior-coverage`
- `m3-fast-search-recipe-first`
- `m3-ui-distillation-closure`

This checkpoint adds only one new profile: `m3-ui-distillation-final-20261008`, whose Java child is a read-only final-state verifier and whose text child installs this receipt.

## Executed evidence retained

The prior full receiving proof retained in `VIEWPORT_VISUAL_GATES.md` records:

- original Nebula 314-module Tycho reactor;
- 226 selected JUnit tests;
- zero failures, errors or skips;
- source-built SWT GTK3 receiver;
- SWT-native display-pixel captures;
- OpenCV scene/resize/million-row checks;
- six rejected negative image controls;
- GC/Image lifetime controls;
- exact runtime receiver receipt.

For the 2026-10-08 current-SWT requalification (Nebula PR #111, head `03ae26cb759a1228e00add1589cc9bf0c444ac88`):

- Java CI with Maven run `37714520749`: completed success;
- CodeQL run `37714520790`: completed success.

The exact-bound final recipe-transfer repair (Nebula PR #112, head `772eab249334957d1a496ef01847c211e08b9bb2`) had its four hosted runs still queued at this checkpoint. This receipt does not claim those queued runs are green.

## Completion rule

The UI distillation is complete for the admitted SWT/Nebula viewport scope at these frozen refs.

Future changes are ordinary compatibility/performance follow-ups and must:

1. preserve public SWT/Nebula behavior;
2. reuse the canonical owner/recipe first;
3. source-seal any changed pre/post image;
4. add or strengthen the relevant regression;
5. qualify through the original product build/runtime gates;
6. preserve `swt-classic` as the compatibility refuge rather than weakening the optimized default.
