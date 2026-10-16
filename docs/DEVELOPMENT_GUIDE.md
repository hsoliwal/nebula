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
