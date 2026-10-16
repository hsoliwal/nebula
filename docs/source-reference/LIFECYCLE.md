# Widget resource, threading and model contracts

[Reference home](README.md) · [Every module owner](MODULES.md) · [Every declared Java package](PACKAGES.md) · [Coverage ledger](COVERAGE.md)

Source snapshot: `3cf2169433572c96d4b65c83a16c2f907eb37c67`. The following claims come from complete relevant method bodies and the named callers/tests. They apply to this fork revision, not automatically to all upstream versions or every widget. All build, native/UI, screenshot and performance checks in this pass are NOT_RUN.

## Ownership boundaries

A widget bundle owns its control implementation; JFace adapters, CSS adapters, snippets/examples, test bundles and feature packaging are separately identified in [MODULES.md](MODULES.md). `widgets/<family>/pom.xml` typically aggregates those artifacts rather than defining a widget API. The top reactor enters `releng/org.eclipse.nebula.nebula-parent`, which lists both mature and incubation groups as ordinary modules at this revision.

For the reviewed families:

- `org.eclipse.nebula.widgets.grid`: Grid controls/items/columns, visual-data ownership, view/model coordination and CPU-job handoff
- `org.eclipse.nebula.widgets.grid.model`: model-row permutation, sorting/filtering/pinning math. A model helper's CPU-only work does not authorize touching SWT controls from a worker
- `org.eclipse.nebula.widgets.gallery`: Gallery items and item/group renderers, including virtual SetData delivery
- `org.eclipse.nebula.widgets.cdatetime`: date selection, formatting, picker/content lifetime and UI text updates
- `org.eclipse.nebula.widgets.pshelf`: shelf/item selection, animated layout and owned renderer lifetime

The full namespace list also includes tests using the same package as their implementation. Package name alone does not identify a production artifact. Other widget families, RichText JavaScript, and binary/provenance artifacts remain visible in the inventories with their own review gaps.

## NEB-01: UI-thread boundary and Grid creation

[Grid.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/Grid.java) extends the SWT control lifecycle. Its public API documentation repeatedly specifies the creating UI thread and disposed-widget restrictions, and relevant accessors/mutators call `checkWidget`. Do not infer background-thread access is allowed because a particular wrapper omits a direct check: it may delegate to a widget operation and the UI ownership contract still matters.

The two-argument Grid constructor evaluates `Display.getCurrent()` while constructing its default row-addressed visual-data owner, before delegating to the constructor that invokes the SWT superclass. Create it on the owning Display thread; do not promise that every wrong-thread call produces the same SWT error, because the default-constructor path dereferences the current Display first.

The main constructor uses a `GridVirtualItemList` when `SWT.VIRTUAL` is set and an ArrayList otherwise; installs renderers/listeners/accessibility/scroll proxies; sets system colors; and creates a dedicated blended `cellHeaderSelectionBackground` Color. The default visual-data owner is [GridRowAddressedItemDataVisualizer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridRowAddressedItemDataVisualizer.java), a package-private implementation retaining per-Grid, logical-row payload rather than GridItem keys.

`DataVisualizer` is the extension contract for cell text, colors, fonts, images, spans and flags. Its `clearRow`/`clearColumn`/`clearAll` methods are data-store operations. They are not an interface-level promise to dispose every SWT resource. The default row store removes references; inspect an injected implementation's ownership separately. See [DataVisualizer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/DataVisualizer.java).

## NEB-02: resource ownership is API-specific

| Resource / operation | Observed owner and disposal behavior | Integration rule |
| --- | --- | --- |
| Grid cell-header-selection Color | Constructor allocates it. `setCellHeaderSelectionBackground` simply replaces the field. `onDispose` disposes the current field | Treat that setter as a special ownership-sensitive API. Do not assume a shared or system Color is safely borrowed; replacing the field does not dispose the old value in the setter. Track the prior/new owned resources deliberately and verify repeated replacement |
| Grid cell image/font/color through the default DataVisualizer | Stored as references in row payload; clearing/removing rows clears references without native-resource disposal in the reviewed row store | Keep application-created resources alive while referenced and give their application owner a cleanup path. A custom DataVisualizer may have a different policy |
| Grid column renderer | Column disposal calls `setDisplay(null)` on current cell/header renderers, clears those references, then delegates to Item disposal | Review custom renderer `setDisplay` cleanup; do not substitute a nonexistent universal renderer-dispose call. Replacement setters initialize the new renderer and do not clear the previous renderer in these bodies |
| Gallery item/group renderer | `Gallery.onDispose` disposes whichever renderers are currently installed | Renderer replacement setters do not dispose the prior renderer. Retain an explicit replacement/cleanup strategy; do not share one stateful renderer across galleries without an intentional contract |
| Default Gallery drop-shadow colors | `DefaultGalleryItemRenderer.dispose` calls `freeDropShadowsColors`, which disposes live cached colors | This does not mean every supplied Color is owned. `setSelectionBackgroundColor` only assigns its reference; the reviewed dispose body frees the shadow cache |
| PShelf renderer | `setRenderer` disposes the previous renderer, initializes the new one; shelf disposal disposes the current one | Supply a live, non-null, separately owned renderer. Reinstalling the same instance is unsafe as an ownership assumption: it is disposed before reinitialization in this body |
| PShelfItem body composite | Item disposal removes it from the shelf, disposes `bodyParent`, then calls superclass disposal | Child controls belong under the item's body; account for application resources held by those controls separately |

Evidence: [Grid.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/Grid.java), [GridItem.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridItem.java), [GridColumn.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridColumn.java), [GridRowAddressedItemDataVisualizer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridRowAddressedItemDataVisualizer.java), [Gallery.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery/src/org/eclipse/nebula/widgets/gallery/Gallery.java), [DefaultGalleryItemRenderer.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery/src/org/eclipse/nebula/widgets/gallery/DefaultGalleryItemRenderer.java), [PShelf.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/src/org/eclipse/nebula/widgets/pshelf/PShelf.java), [PShelfItem.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/src/org/eclipse/nebula/widgets/pshelf/PShelfItem.java). These are source observations, not a measured leak test or a proposed behavior change.

Grid's disposal listener removes itself, re-notifies other Dispose listeners, sets the event type to `SWT.None`, then marks `disposing`, disposes the header Color, disposes materialized items, clears row data, and disposes column groups/columns. That order is deliberate: other listeners run before its child cleanup. Do not move disposal earlier merely to shorten a method. `GridItem.dispose` has a distinct parent-disposing path and clears custom non-row-addressed visualizer rows before detaching its row identity.

## NEB-03: selection and validation are overload-specific

[Grid.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/Grid.java) distinguishes item selection from cell selection. Enabling cell selection clears selected items; disabling clears selected cells. Unless the widget has SINGLE style, enabling cell selection sets the compatibility multi-selection mode.

`setSelection(int)` leaves selection untouched for an out-of-range index. The range overload clears current selection after its disabled/single-select guards and then selects the in-range portion. The `GridItem[]` overload rejects a null array, rejects disposed items, skips null entries and foreign-parent items, and returns early for a single-select widget given multiple items.

Do not treat the `int[]` overload's prose as a tested normalization contract. Its current body does not explicitly reject a null array, adds row selections without deduplicating them, and breaks on the first index beyond item count rather than scanning the remaining array. Those differences from its Javadoc are retained as review gaps, not silently corrected by this documentation change.

Likewise `GridItem.setImage(int, Image)` calls `checkWidget` and rejects a disposed image, while the one-argument override delegates directly to the visualizer and redraw. Use the correct overload's actual body when specifying validation and event behavior.

For cell drag selection, the documented selection event can have `SWT.DRAG` detail and a later completion event. A redraw or a programmatic selection assignment is not by itself proof that a user-selection event was emitted.

## NEB-04: CPU jobs, snapshots and stale-result rejection

[GridModelJobs.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridModelJobs.java) provides a precise boundary between model math and SWT state:

1. Call `sortByIntKeys` on the UI thread with an installed `RowPermutation`, keys for every model row, sort direction and an Executor
2. Fewer than `1 << 17` model rows use the inline path, mutate the permutation, refresh the Grid view and return a completed true future
3. Larger jobs copy the relevant key array and capture permutation identity/version before dispatch. A worker computes `RowPermutation.sortedOrder` from the snapshot
4. The apply callback runs through the captured Display's `asyncExec`. If the Grid is disposed, the installed permutation object changed, or its version changed, it completes false without applying. Otherwise it installs the order, refreshes and completes true
5. A RuntimeException during worker sorting completes the future exceptionally from the worker. Therefore the Javadoc's completion-on-UI-thread description applies to the normal apply/drop paths, not every exceptional completion

Do not block the UI thread with `join` while waiting for that asynchronous apply; the Display must dispatch the callback. Attach UI updates via the appropriate Display handling and check disposal again. Executor rejection, Display shutdown, callback failure and cancellation behavior are not established as safe/completing by this source review; the method does not catch every scheduling/apply failure.

The separate `sortByIntKeysBusy` API waits with `BusyIndicator.showWhile`, uses a measured per-row estimate against a 100,000,000 ns inline budget after measurements exist, and still checks disposal/identity/version before applying a worker result. That budget is a decision threshold in source, not a measured responsiveness guarantee. The shared measurement field is not a guarantee of performance on another machine.

[RowPermutation.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/model/RowPermutation.java) owns model/view mappings. `setModelCount` rejects negative counts and resets sort/filter/pinning state. `setOrder` requires an exact permutation, rejects duplicate/out-of-range model rows, and copies the supplied order. `setFilter` rejects a null predicate and evaluates each model row. `sortedOrder` reads only its arguments and returns a new array; its keys must not change while computing. Preserve model-row tie order and distinguish model indices from displayed positions.

[GridModelJobs_Test.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridModelJobs_Test.java) was read in full: it checks inline scheduling, worker/UI application with a key snapshot, and dropping a changed-permutation result. Those tests were not run here. [GridRowPermutation_Test.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridRowPermutation_Test.java) is an additional test entry point, not an execution receipt.

## NEB-05: Gallery renderer and virtual-data lifecycle

[Gallery.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery/src/org/eclipse/nebula/widgets/gallery/Gallery.java) sets DOUBLE_BUFFERED, derives virtual/orientation/multi-selection flags, installs event/dispose hooks, installs default renderers and lays out the control. `setItemRenderer` checks the widget, assigns the renderer, binds it to the Gallery when non-null, and redraws. `setGroupRenderer` binds the renderer and updates structural/scrollbar state before redraw. Neither replacement body disposes the previous renderer.

In virtual mode, `updateItem` creates missing items and sends `SWT.SetData` through `setData`, which fills `Event.item` and `Event.index`. Ultra-lazy group placeholders can be initialized with default child count/expanded state without the ordinary SetData delivery until creation is requested. Keep handlers tied to the model row/index and capable of repeated requests. Treat callback execution as part of the UI operation, not permission for blocking I/O inside painting.

`onDispose` removes all items for the nonvirtual case, then disposes installed item and group renderers. It intentionally does not follow that same remove-all path for virtual mode. Do not extrapolate a full virtual payload cleanup guarantee without inspecting item/data owners.

`setSelection(GalleryItem[])` clears selection, selects/shows each supplied item, updates the last-click reference used for keyboard navigation, and redraws. It does not validate/normalize arbitrary inputs in this method. [GalleryTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.tests/src/org/eclipse/nebula/widgets/gallery/tests/GalleryTest.java) and [GalleryVirtualBehaviorTest.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.tests/src/org/eclipse/nebula/widgets/gallery/tests/GalleryVirtualBehaviorTest.java) provide checked-in behavior examples; execution is NOT_RUN. [SnippetGalleryViewerTester.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/gallery/SnippetGalleryViewerTester.java) is a JFace GalleryViewer example entry point.

## NEB-06: CDateTime state, mutable dates and picker teardown

[CDateTime.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime/src/org/eclipse/nebula/widgets/cdatetime/CDateTime.java) combines formatting/selection state with native content. `setSelection(Date)` only changes the stored selection when editable, clears it for null, and accepts non-null values only if valid against min/max dates. It stores the supplied Date reference; `getSelection` returns the stored first Date or null. This is not an immutable snapshot API. Avoid mutating a retained Date behind the control without a deliberate update path.

Opening a dropdown commits the active edit field, records the cancel selection, creates the picker, delegates to the base popup behavior and shows the current selection. A non-dropdown style ignores `setOpen`. `postClose` calls `disposePicker`: the picker and dropdown content are disposed; a remaining popup shell is disposed asynchronously after a non-null/not-disposed check. Do not assume all popup cleanup has completed synchronously when close returns.

`updateText(boolean)` builds a formatted text/selection snapshot, creates a runnable that checks the text control before use, temporarily removes/re-adds its Verify listener while replacing text, and dispatches with `asyncExec` or `syncExec`. The formatting and selection work occurs before dispatch. That implementation does not turn the whole mutable CDateTime into a thread-safe object.

Two important source/prose gaps remain:

- `setPattern` clears allowed-timezone state and may close an open dropdown before constructing/parsing the new formatter. Its Javadoc describes restoration for an invalid pattern, but this body does not implement a complete save-and-restore transaction. Do not promise that invalid input leaves every prior field/popup state unchanged
- `setTimeZone(TimeZone)` assigns the default when the argument is null, but then compares against and assigns the original null argument in its next branch before passing it to calendar/formatter. Consequently the documented null-means-default path needs a focused test/source fix review. For integration examples, pass an explicit valid TimeZone rather than asserting this null fallback is proven

[CDateTimeSnippetBug527399.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.snippets/src/org/eclipse/nebula/widgets/cdatetime/snippets/CDateTimeSnippetBug527399.java) is a concrete checked-in snippet. The CDateTime test project and its analog/discrete clock tests are indexed in the package ledger; none was run. BaseCombo/painter/global-resource lifetime and every locale/DST edge case remain outside the reviewed leaf set.

## NEB-07: PShelf renderer and animation lifecycle

[PShelf.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/src/org/eclipse/nebula/widgets/pshelf/PShelf.java) installs a PaletteShelfRenderer and UI listeners. `setRenderer` checks the widget, rejects null/disposed renderers, disposes the old renderer and initializes the new one, then recalculates layout and redraws. Selection rejects null, ignores foreign items and unchanged selection, and otherwise calls `openItem` with animation enabled.

`animateOpen` is a synchronous loop: it changes item coordinates, resizes clients, redraws/updates and advances by `animationSpeed`. Do not describe this body as a scheduled background animation. The method does not itself establish a latency bound or a valid range for arbitrary animation-speed input. [PShelfItem.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/src/org/eclipse/nebula/widgets/pshelf/PShelfItem.java) disposes its body composite when the item is disposed. [PShelfSnippet1.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/pshelf/PShelfSnippet1.java) is the usage entry point; no dedicated PShelf runtime test was claimed in this pass.

## NEB-08: native evidence and source evidence are separate

[GridSwtScreenshotCapture.java](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridSwtScreenshotCapture.java) keeps three capture-method labels. `captureControl` attempts Control.print and falls back to a control GC copy; that is different evidence from `captureScreenControl`, which checks visible/unclipped bounds and the UI thread, captures using a Display GC's `copyArea`, and has no offscreen rendering fallback. Its GC and Image objects are disposed in finally blocks, and a PNG hash identifies the written artifact.

The fork's native acceptance instructions require visible-screen capture after painting settles, actual SWT/Nebula pixel validators and the SWT resource gate, and rejection of missing/skipped/failed/duplicate evidence and stale/blank/corrupt frames. Finding these helper methods or retained receipts does not prove they ran at this snapshot. Record each real stage separately. For this documentation pass: compilation NOT_RUN; JUnit NOT_RUN; native screen capture NOT_RUN; pixel validation NOT_RUN; SWT resource gate NOT_RUN; CPU/heap/native-memory budgets NOT_RUN.

## Semantic coverage and remaining work

Reviewed behavior is limited to NEB-01 through NEB-08 and the explicitly named source methods. Whole-package completeness is not claimed. Still open: API/inline documentation for the remaining widgets and adapters; model-job cancellation/shutdown paths; repeated renderer/color replacement tests; Gallery virtual disposal; CDateTime invalid-pattern/null-timezone behavior; PShelf animation parameter/latency tests; native-platform qualification; embedded JavaScript and binary-source provenance. No current runtime performance, leak freedom, semantic fixed point, or automatic promotion claim follows from this guide.
