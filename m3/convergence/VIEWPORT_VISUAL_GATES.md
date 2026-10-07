# Nebula viewport donor and visual regression gates

This document records behavioral evidence used by the M3 Nebula/SWT distillation lane. It does not grant source-copy authority.

## Supplied historical SWT/JFace examples

The supplied Java archives contain earlier viewport/deferred-viewer experiments that are useful as behavioral donors:

- `TreeViewerWithViewPort` synchronizes a logical/external scrollbar with SWT Tree scrolling inside a redraw lock.
- `ViewPort` solves horizontal/vertical scrollbar visibility iteratively because one scrollbar changes the other axis' available extent.
- `TreeViewerLazyTool` / deferred-tree experiments use progressive reveal/frontier growth rather than eagerly materializing the entire logical child set.
- `IndexList` and `PaggedList` separate logical indexing/page access from concrete widget residency.
- filtering examples keep model/filter state separate from presentation refresh.

These are evidence that logical extent, scrollbar geometry, and realized widgets should be independent concerns.

## Supplied Virtual TreeView 8.4.1

The supplied Virtual TreeView source/demos reinforce:

- logical node state and visibility independent of painting;
- first/next-visible traversal instead of walking every node for viewport work;
- header drawing as a separately managed surface/backbuffer;
- explicit expansion, selection and fully-visible state;
- partial-line avoidance and clipping during text painting;
- speed/visibility demos that treat large logical trees as a model problem first.

Useful visual references in the supplied archive include the Advanced demos for:

- Speed;
- Visibility;
- Grid;
- HeaderCustomDraw;
- States;
- Multiline.

## SWT / Java2s / Nebula examples

Regression review should cover the behavioral shapes represented by:

- SWT virtual Table and Tree / `SWT.SetData`;
- owner-draw `MeasureItem`, `EraseItem`, `PaintItem`;
- `ScrolledComposite` resize/origin behavior;
- Canvas clipping and redraw;
- checkbox/grayed/selection/focus state;
- expand/collapse and bulk redraw locks;
- header resize/reorder/sort;
- narrow viewport with both scrollbars;
- large logical models with small visible windows.

Java2s material is used as behavioral/category reference only; example source is not copied.

Nebula-specific donors include Grid visible-range support, external scrollbar proxies, CompositeTable's bounded row-control pool, Grid header painting, and XViewer-style tree/table projection.

## Screenshot invariants

When a transformed candidate changes painting, viewport, scrollbar, header or state code, capture the same scene before and after and compare these invariants:

1. vertical-only scrolling does not move or unnecessarily repaint the column-header plane;
2. horizontal scrolling updates body and scroll-coupled header projection together;
3. scrollbar visibility reaches a stable fixed point after resize;
4. thumb/page extent derives from logical model extent and representative row geometry;
5. selection/focus/check/gray/expanded state remains attached to the same logical item;
6. expand/collapse preserves public facade identity;
7. collapsed cold branches may release private/native residency while retaining an expander sentinel when logically non-empty;
8. owner-draw callbacks preserve ordering and clipping;
9. visible + overscan painting does not draw unrelated logical rows;
10. resize, DPI/font changes and sample-row changes recompute geometry without walking the entire logical model.

SWT's retained `ViewportRewriteStress` screenshot lane is the primary automated visual oracle for the shared mechanics. Nebula examples should be exercised with equivalent states before a source-changing Nebula recipe is promoted.

## External viewport donors

External libraries such as viewport-lib, Dear ImGui, LVGL, MyGUI and NanoGUI may be reviewed for rendering/viewport mechanics. They are architecture or algorithm evidence only unless a compatible license and an explicit source-copy admission are recorded.

The M3 sequence remains:

```
inventory
-> atomize / patternize
-> donor/target shape intersection
-> one bounded source-sealed recipe
-> original build + behavior + screenshot proof
-> serial promotion
```


### Concrete external mechanics retained as donor shapes

- `grimandgreedy/viewport-lib`
  - grouped frame state keeps viewport/render state explicit instead of hidden in widget objects;
  - shared geometry slabs and indexed per-object draw data reduce per-draw rebinding;
  - scissor-rect blit/overlay composition maps naturally to SWT viewport z-planes;
  - GPU resources are reusable residency, not semantic model ownership.
- Dear ImGui
  - `ImGuiListClipper` performs coarse clipping for large evenly spaced lists and advances the
    cursor across skipped items;
  - multi-selection can remain in caller-owned/external storage while only visible items are
    submitted.

These are architecture/algorithm donors only. They reinforce the existing target rules:

```
logical extent / state
  -> visible range + overscan
  -> retained commands / indexed draw data
  -> clipped body pass
  -> independently invalidated chrome/overlay planes
```

## Automated native Grid qualification

The GTK3/X11 Maven lane sets `nebula.grid.viewport.screenshots.screen=true` and requires
`GC(Display).copyArea` for all six Grid viewport scenarios. `Control.print` images remain
separate diagnostics. Capture must reject hidden, clipped, empty, disposed and off-display
targets; the shared SWT coordinate logic is donated through the Synexia recipe packet.
The million-row fixture must fit inside its shell before screen capture.
Capture visible pixels before offscreen rendering: the rejected trial recorded blank native
frames after `Control.print` despite changed offscreen output. Drain the native frame before
capture and require interior content, not only a border or scrollbar. Keep the rejected frames.

Maven runs `GridFixedColumn_Test`, `GridVisibleRangeSupport_Test`,
`GridViewportCoordinates_Test` and `GridGCProxy_Test`. OpenCV checks persisted PNG hashes,
dimensions, nonblank rendering, scrolling freshness, resize and sparse-model observations.
A red/blue geometry oracle proves repaint after moving the shell; six mutated-image controls
must be rejected. All three capture paths undergo 48 captures and three forced write failures,
with Image/GC positive leak controls and no retained tracked graphics. No manual QA is required.
Run `m3/convergence/verify_grid_screenshots.py` with screenshot and JUnit report directories.

These gates complement the separate SWT CPU/heap/process-memory suite. Graphics-object tracking
does not prove all native allocations, and this Grid lane does not qualify all Nebula widgets,
JFace adapters, other platforms or repository-wide M3 convergence. Preserve the original Tycho
reactor as product authority. Recipe, seals and evidence remain canonical in
`hsoliwal/com.synexia`, crate `nebula-screen-qualification-20261007`.

### Hosted ECJ Javadoc visibility gate

The ordinary Tycho build enforces the Grid bundle's existing JDT Javadoc settings.
Package-private helper names in prose use `{@code ...}` when a protected-visibility
link would be rejected. Keep `invalidJavadoc=error` and all visibility checks enabled.
The first hosted run of the native qualification change failed at `GridGCStateDAG`
before tests; retain that diagnostic in the canonical successor recipe rather than
claiming the earlier javac-only compile qualified the entire Tycho reactor.

### Pinned SWT receiving proof

The native Grid lane builds SWT commit `717f952621852850b68c0c301fca7c6d41a68039`
with its original Tycho/native build, publishes its host and GTK fragment using the
standard Tycho P2 publisher, and explicitly includes that repository in Nebula's
platform. `tycho.localArtifacts=ignore` remains enabled. Source seals, built JARs,
GC bytecode and native hashes are recorded; the test JVM must load the exact recorded
GC class. OpenCV also checks the runtime receipt against the independently prepared
expected file and requires the OSGi runtime. No cross-platform promotion follows.

Tycho forwards the screen-capture and receiver-hash properties explicitly. The earlier
hosted run reported one clipping failure and one skipped capture test because it loaded
upstream SWT and lacked this forwarding. Preserve those results. The receiving lane must
run every selected Grid test without a skip and pass the PNG and lifetime oracles.

The prepared SWT repository binding and expected runtime hash live under the separate
P2 publisher's `target/binding` directory. That publisher is outside the ordinary
Nebula reactor. The ordinary `clean verify` must not delete its own input platform or
the expected evidence before the final OpenCV comparison. A real root-clean control
checks removal of a root-target sentinel and preservation of the receiving evidence.

### Full receiving reactor proof, 2026-10-07

The original 314-module Nebula Tycho reactor passed with the explicitly published,
source-built SWT GTK3 receiver: 226 selected JUnit tests, zero failures, errors or
skips. The independent local run used JDK 21.0.12.1+1 and Maven 3.9.11 (`verify`);
the hosted Java CI run 37633371106 at Nebula 5d3e50be used the workflow's ordinary
`clean verify`, built the pinned SWT distribution, and passed OpenCV. Both runtime
receipts bind GC bytecode SHA-256
`7ec17b48aae15bc1930ab66b0980724194a08f48acd6f8f8fd0ded9422f15e2a`.

Canonical successor `nebula-full-receiver-qualification-20261007` retains the full
local log, 66 JUnit XML reports, final PNGs/metadata, exact source-tree readback and
the hosted job result. Eight scene/canary images pass; all six defect controls are
rejected. Forty-eight captures and three write failures retain zero tracked GC/Image.
The existing reactor's test selection is unchanged; this does not claim all legacy
Grid tests or every native platform. Separate M3 convergence, offline closure and
SWT Cocoa/Windows/CPU gates remain independent. Historical failed receipts stay intact.
