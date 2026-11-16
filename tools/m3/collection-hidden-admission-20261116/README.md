# Hidden collection item paint admission

The existing Collection model now tests the item's HIDDEN bit after its positive-width check. Hidden items skip their complete cell paint, including text, surface, swipe action and drag outline. Item identity, row slots, counts, callbacks, shared headers and the pull indicator retain their existing contracts. No public API or storage structure is added.

`receive-hidden-collection.yml` applies the exact qualified 28-character source change. `create-hidden-collection-test.yml` creates the new test without overwriting an occupied path. Both are stock Maven/OpenRewrite recipes with exact/replay/drift/path/foreign controls.

`CollectionHiddenCellAdmissionTest` adds six JUnit laws parameterized across all eight existing Collection modes (48 cases). Focused qualification passes 84 cases; broader qualification passes 148 selected cases (62 model, 38 native lifetime, 48 new), with all 185 source units compiled. The broader run does not execute every other module test. APIs, serialized UID identities and unchanged class bodies were compared; only CollectionModel bytecode changes.

The matching canonical template and full evidence are in [com.synexia PR #17842](https://github.com/hsoliwal/com.synexia/pull/17842), pinned at commit 349786122a5929cab0c8fe570f849f98e6aa3ff3 under docs/M3-SCALE/runs/20261116-collection-hidden-admission. All 630 generated Java sources compile, with only the two Collection model class bodies changing and zero replay writes.

Native SCREEN inspection confirms visible/hidden/restored behavior for P1 in one actual InfiniteScroll widget with stable row rectangles and zero child widgets; OpenCV preserves the original counterexample and confirms repaired row suppression/unchanged siblings/exact restoration. Captures explicitly request public redraw/update, so automatic invalidation is unproved. The different-data S3 run refused before capture; V4 remains blocked. The corrected reporter has CPU-only qualification and no GUI rerun.

This is a draft receiver. Hosted Tycho discovery of the new class, complete Maven/OSGi qualification, O(count) scans, JNI/paging/residency integration and toolkit/platform parity remain open. No merge or production promotion is included. Local qualification used no disk output, downloads or cache copies; existing storage/process controls remain active.
