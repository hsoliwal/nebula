# Developing the Nebula fork

Nebula and Opal provide SWT-based widgets packaged as Eclipse bundles, features and update sites. This guide covers `hsoliwal/nebula` and complements the [widget catalogue](Widgets.md), [root README](../README.md) and [contribution guide](../CONTRIBUTING.MD).

## Start with the normal widget build

```sh
git clone https://github.com/hsoliwal/nebula.git
cd nebula
git rev-parse HEAD
java -version
mvn -version
mvn -V -B clean verify -Dtycho.localArtifacts=ignore
```

The checked-in [parent POM](../releng/org.eclipse.nebula.nebula-parent/pom.xml) sets Java source/target 21 and the JavaSE-21 execution environment. The [Maven workflow](../.github/workflows/maven.yaml) currently selects Temurin 21 and Maven 3.9.11. Those are the workflow's tool versions; its pinned SWT preparation and screen-validation steps, described below, are additional to the ordinary widget build.

Tests use Tycho's UI harness. On Linux, provide GTK and a display; CI runs the Maven command through Xvfb. A local equivalent, with Xvfb installed, is:

```sh
xvfb-run -a mvn -V -B clean verify -Dtycho.localArtifacts=ignore
```

The target platform resolves Eclipse, GEF and SWTBot repositories from the parent POM. Resolution requires network access and those update sites can move. Record the resolved environment when reproducing a failure. Do not remove tests or silently substitute locally installed bundles to hide a dependency failure.

The plain command above is the normal Tycho entry point, not a reproduction of every current CI acceptance step. The current workflow first builds a pinned SWT receiver, prepares its p2 repository and binds that repository and GC hash into the widget build. It then runs an OpenCV screen-pixel validator. Follow those ordered workflow steps if reproducing that qualification lane; do not substitute a stock SWT distribution and label the result equivalent.

All commands in this guide are source-checked examples, not execution results from this documentation change. They require the dependencies and display environment described here.

## Repository map

- `widgets/<widget>/`: widget families, usually grouping implementation, feature, examples/snippets and tests.
- `widgets/opal/`: Opal widget families.
- `examples/`: aggregated example bundles and features.
- `releng/`: parent configuration, release/incubation features and update-site packaging.
- `docs/Widgets.md`: user-facing catalogue and navigation.
- `m3/` and `.m3/`: additional fork-specific recipe, inventory and evidence work.

Find the implementation bundle and its corresponding test bundle before editing. Grid, for example, has `widgets/grid/org.eclipse.nebula.widgets.grid.test/`; other widget families use `.test` or `.tests` names, so do not guess an artifact selector from the widget name alone.

The root POM enters the parent reactor. The current [parent module list](../releng/org.eclipse.nebula.nebula-parent/pom.xml) includes both the released widget group and the incubation group (including CalendarCombo, DateChooser and Timeline), their example features, and the update site. It does not define an `incubation` profile. The commented-out incubation command in the workflow is not an active build lane.

Use the actual reactor and test bundle names when selecting a narrower build. A selected reactor does not establish coverage for omitted widgets. The update-site output collected by [Jenkins](../Jenkinsfile) is `releng/org.eclipse.nebula.site/target/repository/`; test reports are under `**/target/surefire-reports/*.xml`.

## Working on a widget

1. Use the README's Oomph setup or import the relevant bundles into an Eclipse/PDE workspace with the matching target platform.
2. Reproduce the problem in the closest example or snippet and identify its public behavior.
3. Add a regression in the widget's test owner, including edge cases relevant to selection, scrolling, disposal and display-thread access.
4. Run the existing tests and the normal Tycho verification lane.
5. For visual changes, include a before/after reproduction with OS, SWT version, scale and theme.

CI retains Grid visible-range screenshots from `widgets/grid/org.eclipse.nebula.widgets.grid.test/target/m3-visible-range-screenshots/` when produced. An absent screenshot is not a visual pass. Check test reports and actual images in addition to the Maven exit status.

## Reproducing the fork's Grid CI lane

The ordinary `mvn verify` command above does not reproduce every step of
[.github/workflows/maven.yaml](../.github/workflows/maven.yaml).
That workflow currently runs on Ubuntu 24.04 with GTK3 and X11
(`SWT_GTK4=0`, `GDK_BACKEND=x11`) and:

1. Checks out a specific commit of `hsoliwal/eclipse.platform.swt` into
   `.m3-swt`, rather than following that repository's moving default branch
2. Builds its GTK native fragment and uses
   [prepare_swt_receiver.py](../m3/convergence/prepare_swt_receiver.py) plus the
   [receiver p2 project](../m3/convergence/swt-receiver-p2/pom.xml) to prepare the
   bound target platform and expected identity
3. Runs the Nebula reactor under Xvfb with the generated target-platform
   override and Grid screen-capture properties
4. Runs [verify_grid_screenshots.py](../m3/convergence/verify_grid_screenshots.py)
   against the captured images, Surefire reports and expected receiver identity

Copy the exact pinned revision and commands from the workflow when reproducing
this lane; substituting the latest SWT checkout or the default target platform
changes what is being tested. The native fragment build needs the GTK
development packages listed in that workflow.

The uploaded `grid-visible-range-screenshots` artifact includes the expected
identity, receiver repository metadata, screenshots and Surefire reports.
A Maven build, screenshot generation and successful pixel validation are separate
results. Report failures or missing evidence at the stage where they occurred;
a normal widget build alone does not qualify this screen-validation lane.
The workflow excludes Markdown/docs-only changes, so an absent run for a
documentation PR is not evidence of passing UI tests.

## Reader cheat sheet

| Question | Start here | What to look for |
| --- | --- | --- |
| Which widget fits an application? | [Widget catalogue](Widgets.md) | API purpose and examples, before changing internals |
| How does a widget behave? | [Grid snippets](../examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid) | A small public-API reproduction |
| Where is Grid implemented? | [Grid bundle](../widgets/grid/org.eclipse.nebula.widgets.grid) | Controls, items, columns and rendering owners |
| Where should a regression live? | [Grid test bundle](../widgets/grid/org.eclipse.nebula.widgets.grid.test) | Existing fixtures and test ownership |
| Which widgets does Maven build? | [Parent POM](../releng/org.eclipse.nebula.nebula-parent/pom.xml) | Actual modules, target platform and UI harness |
| Why is CI different from a plain build? | [Maven workflow](../.github/workflows/maven.yaml) | Pinned receiver preparation, screenshot flags and validator |

A bundle is an OSGi implementation/dependency unit. A feature groups bundles for installation; a p2 repository publishes those features and bundles. A snippet demonstrates an interaction and is not automatically a regression test. SWT provides the display, shell and native platform beneath Nebula controls; Nebula's widget-specific behavior remains owned by the corresponding widget bundle.

## Worked exploration: a hierarchical Grid

Start with [GridSnippet1.java](../examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid/GridSnippet1.java), rather than a large application:

1. The snippet creates a `Display`, a `Shell` with `FillLayout`, and a `Grid` with horizontal and vertical scrolling.
2. It makes the header visible, creates a `GridColumn`, and calls `setTree(true)`. The column determines where the hierarchy is presented.
3. It constructs one root `GridItem` and two nested children. Follow the parent passed to each constructor to distinguish hierarchy from row ordering.
4. The shell opens and the event loop dispatches events until disposal. The application-created display is disposed afterward.
5. In a configured PDE workspace, run the snippet and inspect expansion/collapse, header width, scrolling and closing. This is a suggested exploration, not a recorded successful run.

To turn an observation into a fix, capture the smallest failing interaction, trace the matching Grid implementation method, and add assertions in the Grid test bundle. Check selection and disposal as well as pixels. A screenshot or a working snippet alone does not establish test coverage, native-resource bounds or cross-platform compatibility.

## Grid API semantics: rows, cells and viewer models

The public [Grid implementation](../widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/Grid.java) is the place to check overload-specific behavior:

| Operation | Meaning in the current source | Debugging consequence |
| --- | --- | --- |
| `select(index)` | Adds a valid item in multi-selection mode; single-selection mode clears the previous item | Check selection mode before assuming accumulation |
| `setSelection(index)` | For a valid index, replaces selected rows, or selects the row's cells in cell mode | This overload ignores an invalid index; do not assume every overload clears first |
| `setCellSelectionEnabled(true)` | Clears selected rows and enables cell selection | Switching modes is a state transition, not a cosmetic repaint |
| `setCellSelectionEnabled(false)` | Clears selected cells | Save any application selection state explicitly if you need it later |
| `getCellSelection()` | Returns cell coordinates: `Point.x` is column index and `Point.y` is item index | These are logical indices, not display pixels |
| Selection listener | Cell drag events can carry `SWT.DRAG`, followed by a completion event | Avoid treating every intermediate drag notification as a final operation |

Check the exact overload, enabled state and row/cell mode when reproducing a selection bug. The programmatic selection methods above update selection/redraw; they do not themselves emit a user selection notification. Test programmatic state and mouse/keyboard notification behavior separately.

### Follow an edit through the viewer

[GridViewerSnippet1](../examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid/viewer/GridViewerSnippet1.java) demonstrates a different abstraction from constructing `GridItem` objects directly:

1. `GridTableViewer` wraps the Grid. A content provider supplies model elements; the label provider converts those elements into column text and presentation.
2. `setColumnProperties` associates property names with the editor columns. The snippet installs text editors and a cell modifier.
3. The activation strategy accepts traversal, double-click and Enter. Editor activation alone is not a committed model change.
4. Crucially, this snippet's `ICellModifier.modify(...)` body is empty. It demonstrates editor/provider wiring, **not persistence**. If edited text disappears, inspect the modifier's model update and viewer refresh path before diagnosing a paint failure.
5. Its colors come from the display and its fonts from a registry. Those borrowed resources must not be treated as per-cell application-owned allocations.

### Turn the reproduction into a targeted test

Use the existing [Grid test sources](../widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid) and fixture conventions. Separate these assertions:

- Starting from multiple valid rows, compare `select` with `setSelection`, then repeat in single-selection and cell modes.
- Assert cell coordinates and selected model elements independently of their painted locations; test scrolling and reordered columns separately.
- For editing, assert model mutation first, then label refresh and keyboard traversal. An open editor is not evidence of a saved value.
- Exercise closing/disposal before delayed UI work executes. UI access belongs on the display thread, and the callback must recheck whether its Grid still exists.
- Release application-created graphics resources and preserve fixture teardown. A successful Java assertion does not substitute for the existing native pixel/resource gates.

These are source-derived test cases to implement or run in a configured environment, not claims of newly executed tests.

## Troubleshooting without weakening the checks

- **Target-platform resolution fails:** record the unresolved bundle/repository and selected target. The default repositories use moving `latest` URLs; compare them with CI's explicitly prepared receiver.
- **UI harness cannot start:** check GTK, the display server and Xvfb availability. A headless JVM property does not supply a native display.
- **Build passes but visual artifacts are absent:** check whether the screen-capture lane and validator actually ran. Documentation-only paths are excluded by the normal Maven workflow.
- **Only one widget was selected:** record the reactor selection; do not extrapolate that result to every widget or platform.

## Separate recipe tooling from widget acceptance

The public [m3/README.md](../m3/README.md) explains the fork's recipe-first invariants and opt-in profiles. The source-first [m3/reactor.xml](../m3/reactor.xml) builds the qualified parser module before the recipe module, outside the normal Tycho reactor:

```sh
mvn -f m3/reactor.xml verify
mvn -f m3/reactor.xml -Pinventory \
  -Dm3.nebula.root="$(pwd)" \
  -Dm3.nebula.out="$(pwd)/m3/recipe-first/target/nebula-inventory" verify
```

These commands validate/inventory the tooling's scope. They do not establish native UI equivalence, a widget release or promotion of generated candidates. Broader root profiles are opt-in and some require separately installed exact-bound artifacts. Read each profile and its binding before invoking it; an arbitrary SNAPSHOT with the same coordinate is not equivalent evidence.

Keep source-changing work within the existing tested recipe rules. Inventory and orchestration can nominate or order work; the compiler, tests, fixed point, ordinary Tycho reactor and review remain distinct acceptance gates. Do not describe proposed orchestration or candidate transformations as already deployed widget behavior.

## Contributing and handing over

Fork-local documentation and experiments belong in a draft PR targeting `hsoliwal/nebula:master`. Upstream contributions follow [CONTRIBUTING.MD](../CONTRIBUTING.MD), the Eclipse contributor setup and the issue requirement in the README. Keep existing license headers and public widget contracts intact.

A reviewable handover records:

- base/head SHA, changed widget and bundle owners;
- JDK/Maven, OS, display backend and resolved target platform;
- exact normal-build and optional-tooling commands, kept separate;
- test count, failures, reports and any screenshots;
- candidate versus accepted behavior, remaining profiles/platforms and next action.

For a documentation-only PR, inspect every changed link and ensure existing text/licenses are preserved. Report build/test execution as not run when no suitable environment was available, rather than inferring success from source inspection or an older CI receipt.
