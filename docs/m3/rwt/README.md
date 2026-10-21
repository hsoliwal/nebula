<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others. -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# RWT (Eclipse RAP, web) column of the every-platform widget matrix

Directive (user, 2026-10-20): "swingx and all the custom widgets now available across every platform, make them
available". This track owns the RWT column of `docs/m3/custom-widgets/WIDGET_PLATFORM_MATRIX.tsv` (the matrix and
its other columns belong to the custom-widgets track; rows are sent to that track, its files are not edited here).

Status: step 1 (inventory and home) and step 2 (`RWT_COLUMN.tsv`) done 2026-10-20. Later steps: the GC gap
and API drift adapter, widgets one by one on a running RAP application, one example per widget.

## 1. Inventory (2026-10-20, CPU tools only: git, gh, unzip, grep)

### The user's RAP-named repositories

`inventory/RAP_REPOS.tsv` (file trees read with `gh api .../git/trees/main?recursive=1`, counts with grep/wc):

| repo | head | files | .tsx / .ts / .md | .java | pom / MANIFEST / gradle | build |
|---|---|---|---|---|---|---|
| hsoliwal/Rapwt | 24f51ace | 311 | 133 / 79 / 69 | 0 | 0 | `vite build` (package `m3-scale-universal-platform`) |
| hsoliwal/Eclipserapui | 4e56208b | 464 | 203 / 58 / 141 | 0 | 0 | `vite build` (package `@figma/my-make-file`) |
| hsoliwal/Eclipserapui1 | 655d63e8 | 166 | 94 / 22 / 27 | 0 | 0 | `vite build` (package `@figma/my-make-file`) |
| hsoliwal/Eclipserapui3 | 16135329 | 166 | 94 / 22 / 27 | 0 | 0 | `vite build` (same file list as Eclipserapui1: path and size identical for all 166 files) |

None of the four contains Java, an RWT jar, a RAP target platform or a RAP build. They are React/Vite TypeScript
applications (Figma Make exports) that imitate an Eclipse RAP workbench in the browser. The 2026-10-13 architecture
decision (web = Eclipse RAP/RWT on the server plus GWT-compiled Java on the client, no hand-written TypeScript/React)
rules them out as the home; they stay as visual references only.

### Eclipse RAP itself

- No fork of `eclipse-rap/org.eclipse.rap` exists under hsoliwal (checked with `gh repo view`).
- Local clone `E:/ws2/com.synexia/eclipse-rap-clone` (read only for this track, never written): upstream `main` at
  `aef2ff13f` ("Use final release URL of Eclipse RAP + Platform", 2026-03-11, RAP 4.6.0 RC2) plus one local commit
  `cf9604fa0` (Codex, 2026-09-07: line-ending churn in six RAP files and Synexia M3-SCALE docs). It is behind the
  released RAP and is not used for builds.
- Released RWT used by this track: **RAP 4.8.0 from Maven Central** (`inventory/RWT_JARS.tsv`, SHA-256 per jar):
  `org.eclipse.rap.rwt` (bundle version `4.8.0.20260623-0848`, 1,171 classes), `org.eclipse.rap.jface`,
  `org.eclipse.rap.rwt.testfixture`, `org.eclipse.rap.nebula.widgets.grid`, `org.eclipse.rap.nebula.jface.gridviewer`,
  `org.eclipse.rap.nebula.widgets.richtext`, each with its sources jar. Read from the manifest: requires Java 21
  (`osgi.ee ... version=21`), imports `jakarta.servlet` `[5.0.0,7.0.0)`; the jar is signed by Eclipse
  (`META-INF/ECLIPSE_.SF`, `ECLIPSE_.RSA`); every RWT source file read carries the **EPL-1.0** header.
- RAP already ships two Nebula ports as separate bundles: `org.eclipse.rap.nebula.widgets.grid` (+ JFace
  `gridviewer`), drawn by RWT's own client Grid (a DOM row pool, i.e. already a viewport), and
  `org.eclipse.rap.nebula.widgets.richtext` (CKEditor).

### What changed since the 2026-10-13 census (RWT 3.29.0) in `NEBULA_ON_WEB.md`

Read in the RWT 4.8.0 sources:

- `Widget.addTypedListener (EventListener, int...)` and `removeTypedListener (int, EventListener)` now exist in RWT
  (protected, `org/eclipse/swt/widgets/Widget.java:770,809`). The 105 typed-listener errors of the 3.29.0 census are
  therefore gone on 4.8.0; the census is re-run in step 2.
- `GC.isDisposed ()` was never missing: `GC extends Resource`, and `Resource.isDisposed ()` is public
  (`Resource.java:102`). The 2026-10-13 table compared declarations in `GC.java` only. The real GC gap is **11
  methods** (`set/getBackgroundPattern`, `set/getForegroundPattern`, `set/getInterpolation`, `set/getFillRule`,
  `set/getXORMode`, `copyArea`) plus the missing class `Pattern`.
- `Color` still has only device constructors (`Color (Device, RGB)`, `(Device, RGB, int)`, `(Device, RGBA)`,
  `(Device, int, int, int)`, `(Device, int, int, int, int)`); SWT's device-less constructors are absent.
- The browser client replays GC operations with a generic dispatcher: `rwt/widgets/GC.js` handles a few names itself
  (`fillStyle`, `createLinearGradient`, `addColorStop`, `drawImage`, ...) and calls any other name directly on the
  canvas 2D context (`this._context[ op ].apply( ... )`). So `save`, `clip`, `fill ("evenodd")` and gradients with
  arbitrary end points reach the browser without client changes; canvas properties other than the listed ones
  (`globalCompositeOperation`, `imageSmoothingEnabled`) cannot be set, and colour stops lose alpha
  (`rgbToRgbString`).

### Prior art reused (not repeated)

- `hsoliwal/com.synexia` `docs/M3-SCALE/runs/20261013-all-widget-viewport/web/NEBULA_ON_WEB.md` and its TSVs
  (GC census, compile census against RWT 3.29.0, Canvas versus Composite split), plus `rwtgwt/RWT_GWT_RESEARCH_2.md`
  (RWT `ClientListener` carries MouseMove/Wheel/Enter/Exit in the browser; the server protocol does not).
- This repository: `m3/web-widget-reverse/` (family scaffolds; `pagination/rwt/RwtPageWindow.java`, a RemoteObject
  projection marked HOLD) and `widgets/grid/org.eclipse.nebula.widgets.grid.test/gwt-gate/` (Grid model GWT gate).
- SWT fork (`hsoliwal/eclipse.platform.swt` master `839ce8a904c`): the shared viewport core
  `Eclipse SWT/common/org/eclipse/swt/widgets/VirtualViewportPlanner`, `VirtualSelectionModel`, `VirtualRowExtents`
  (package-private, pure Java) and `tests/org.eclipse.swt.tests/gwt-gate/` (GWT 2.12.2 strict compile + JVM versus
  Node digest), which defines the GWT-safe source set.

## 2. Home: hsoliwal/nebula, branch `m3/rwt-column-20261020`

Worktree `S:/m3-scratch/worktrees/nebula-rwt`, branched from `origin/master` `8a89327e`; draft PRs only.

Why here:

1. The RAP-named repositories hold no Java and no RWT; using them would mean TypeScript, which the architecture
   decision excludes.
2. The widgets being made available are Nebula widgets and the new M3 custom widgets (`widgets/m3`), and the matrix
   owner works in this repository. RAP's own precedent is the same: a Nebula widget on RWT is a separate bundle next
   to the widget (`org.eclipse.rap.nebula.widgets.grid`). Keeping the RWT build here lets one widget source compile
   against SWT and RWT unchanged, which is the goal ("same public contract").
3. The SWT fork stays the single home of the shared viewport core. The RWT build compiles those classes from a pinned
   SWT fork commit (the same GWT-safe source set as the SWT GWT gate); it never copies them into a second model.
4. Changes that belong inside RWT (the GC methods) are carried as patched RWT classes in a compatibility bundle here,
   keeping RAP's EPL-1.0 header plus a Synexia contributor line, and are built into a patched RWT jar (the Eclipse
   signature has to be stripped, because patched classes in a signed package fail to load). Offering them to
   `eclipse-rap/org.eclipse.rap` needs a fork of RAP, which is a user decision.

Planned layout on this branch:

- `docs/m3/rwt/` design, `RWT_COLUMN.tsv`, `FOSS_REUSE_DECISION.tsv`, evidence, `EXAMPLES.md`, scripts under `tools/`.
- `widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.compat/` patched RWT classes and the drift adapter.
- `widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.examples/` one RAP entry point per widget and the headless runner.

Builds use plain `javac` against the RAP 4.8.0 jars (JDK 21), run through `C:/m3-superset/tools/build_lock.sh rwt`;
build output goes to `S:/m3-scratch/rwt/` only.

## 3. Step 2: `RWT_COLUMN.tsv` (the RWT route of every matrix concept)

`RWT_COLUMN.tsv` has one row per concept of `WIDGET_MATRIX.tsv` (68 concepts, matrix pinned at
`024f8d3b61` on `m3/nebula-widgets-viewport-20261019`). It is generated by `tools/build_rwt_column.sh` from the
matrix, the hand-made routing table `tools/route_decisions.tsv` (route, RWT class, status, resize/layout behaviour,
note) and the compile census; the error counts and missing symbols are mechanical.

Routes: `RAP_NATIVE` 30 (RAP already has it: a stock RWT widget, RAP's Nebula Grid or RichText port, RAP JFace),
`CANVAS_GC_REPLAY` 19 (a Nebula or M3 `Canvas` painter whose GC calls RWT replays in the browser),
`REBUILD_AS_PAINTER` 17 (Composite-based: on RWT its children are RAP DOM widgets; a single painter is needed for
the drawn look and for viewport residency), `BLOCKED` 2 (`NODE_EDITOR`: draw2d does not exist on RWT;
`PRINT_PREVIEW`: `org.eclipse.swt.printing` does not exist on RWT). Status after step 2: nothing has run yet, so
every row is `NOT_RUN` (with the reason) or `BLOCKED`; 13 concepts have no implementation on any platform yet.

### Differential compile census (RWT 4.8.0, no adapter): `census/rwt-4.8.0-v0/`

All 1,228 main source files of the 81 Nebula main source folders (tests, examples and snippets excluded) are
compiled in one `javac --release 21` run twice, with the same non-toolkit class path: once against the official
SWT 3.135.0 (win32) + JFace 3.40.0, once against RWT 4.8.0 + RAP JFace 4.8.0 (`tools/compile_census.sh`). An error
counts for RWT only when it is absent from the SWT run at the same file, line and symbol (`tools/census_diff.sh`), so
missing third-party dependencies (draw2d, workbench, EMF) cancel out. Result: SWT 3,798 errors (dependencies only),
RWT 4,750, **RWT-only 953 errors in 191 files of 40 families**; 4 families (formattedtext, pagination, radiogroup,
m3) have none. By cause (`tools/census_categories.sh`, `categories.tsv`):

| category | errors | what it is |
|---|---|---|
| COLOR_DRIFT | 172 | device-less `new Color (r, g, b)` / `new Color (rgb)` (RWT has only device constructors) |
| INPUT | 136 | `MouseMoveListener`, `MouseTrackListener`, `MouseWheelListener`, `setCapture`, `traverse`, `TRAVERSE_*` constants, `keyLocation` |
| TEXT_STYLEDTEXT | 122 | `StyledText`, `StyleRange`, BiDi segments, caret API |
| GC_GAP_AND_PATTERN | 93 | the 11 missing GC methods and the `Pattern` class |
| GRAPHICS_CLASSES | 80 | `Region`, `TextLayout`, `Path.addArc`, `Tracker`, `textExtent (String, int)` |
| PRINTING | 71 | `org.eclipse.swt.printing` |
| SWT_CONSTANTS | 31 | `SWT.HIGH`, `SWT.PaintItem`, `SWT.ICON`, ... |
| SCROLLBAR_INCREMENTS | 25 | `ScrollBar.setIncrement/setPageIncrement/getIncrement` |
| DPI_ZOOM | 22 | `getZoom` |
| IMAGE_CTOR | 9 | image constructors RWT lacks |
| OTHER | 192 | everything else (overrides of absent methods, JFace differences, ...) |

### Runtime facts that the census cannot see (read in RWT 4.8.0 sources)

- **Offscreen drawing does not work on RWT.** `GC (Drawable)` picks a delegate only for a `Control` or a `Device`
  (`GC.java` `determineDelegate`); `new GC (image)` compiles (RWT `Image implements Drawable`) but has no delegate,
  so the first drawing call fails. 26 main source files in 15 families draw into an `Image` this way
  (`census/rwt-4.8.0-v0/offscreen_gc_files.tsv`, grep of `new GC (<image or buffer>)`),
  among them GanttComposite, CTree, opal Header, Notifier, LoginDialog, CollapsibleButtons, MonthPick, Carousel.
- **Resize and layout are deltas only for RAP's own widgets** (`resize_layout_delta` column, user rule 24, "resize
  and layout change the port"):
  - `ROWS_ON_DEMAND` (Tree, Table, List, RAP Grid with `SWT.VIRTUAL`): the client asks only for rows that enter the
    visible range; `RWT.PRELOADED_ITEMS` bounds the overscan. Whether rows that leave are released is to be measured.
  - `PROPERTY_DELTA` (other RAP widgets): RWT renders only changed properties per widget, so a resize is a bounds delta.
  - `CHILD_BOUNDS_DELTA` (Composite of RAP widgets): one bounds delta per child, rows as above inside each child.
  - `CANVAS_DAMAGE_RECT` (Canvas painters): `redraw (x, y, w, h)` paints only that rectangle and the client clears
    and clips only that rectangle (`Canvas.java:177-181`, client `rwt/widgets/GC.js:198`), which is a true damage
    delta. A **resize** repaints the whole client area (`Canvas.java:160-165`), and the browser canvas bitmap is reset
    when its width or height changes (`GC.js:186-194`), so the cost of a resize is proportional to the visible window
    (if the painter culls to the paint rectangle), not to the model; sending only the strips that enter needs a
    retained client bitmap, which is client code.
- **Hidden viewports** (`hidden_viewport` column, user rule 24 "Hidden viewports": while hidden, model changes send
  nothing and only mark the window dirty; on becoming visible one window update is sent). RWT 4.8.0 as read:
  - `PAINTS_WHILE_HIDDEN` (Canvas painters): `redraw` repaints at once with no visibility check
    (`Canvas.java:169-181`), so a hidden canvas still sends its operations. Target: a dirty flag while the canvas or
    an ancestor is hidden, one repaint on show.
  - `DELTAS_WHILE_HIDDEN` (other RAP widgets and composites): RWT renders the changed properties of every widget in
    the tree, visible or not. Target: hold the deltas of hidden widgets and render the net change on show.
  - `ROW_DELTAS_WHILE_HIDDEN` (virtual rows): rows the client never asked for cost nothing; rows already on the client
    are updated while hidden.
  These are read from the RWT sources; the step 4 runs measure them per widget.

## 4. Rules 25-29 on RWT

- **25, we own the implementation.** The RWT changes are inside RWT's own classes (patched `Canvas`, `GC`,
  `ScrollBar`, `CanvasLCA`, ...), not wrappers. The hidden-viewport gate of rule 24 lives in RWT's `Canvas.repaint`
  and `CanvasLCA` (`M3CanvasPaint`). `SWT.PartVisible`, `SWT.PartHidden` and `SWT.Viewport` are not yet defined on
  the SWT fork master (checked 2026-10-20); RWT takes the same constant values once the SWT track defines them and
  fires them from the same places (setVisible of a control or an ancestor, CTabFolder selection, StackLayout top
  control, ExpandItem, Shell minimize). The browser already reports Show/Hide to client scripting
  (`rwt/scripting/EventBinding.js`: "appear"/"disappear").
- **26, public model API.** `M3ListModel`/`M3TableModel`/`M3TreeModel`/`M3Patch` are not on the SWT fork master
  yet. RWT's virtual Tree/Table/List and RAP's Grid use `setItemCount` + `SWT.SetData`, the stock path the rule maps
  onto the model, so the RWT examples keep working when the model arrives.
- **28, threads.** RWT runs one UI thread per session and `Display.asyncExec`/`timerExec` are delivered with the
  next request; an animation (M3BusyLabel, ProgressCircle) or a model thread pushing patches needs a
  `ServerPushSession` per session. The Synchronizer rewrite of rule 28 is SWT's; RWT's `Synchronizer` is a separate
  class and is not touched here.
- **29, behaviours: touch paradigms for the tablet case.** `TOUCH_TABLET.md`, `TOUCH_RWT.tsv`: tap, double tap and
  virtual touch scrolling work through RAP's mouse emulation; long press, swipe, pull-to-refresh and pinch zoom are
  BLOCKED until a client-side recogniser exists (user decision: GWT-compiled Java or a RAP client patch).
