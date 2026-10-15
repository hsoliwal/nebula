<!-- fork-homepage-detail:v1:start -->
# Eclipse Nebula and Opal: richer SWT widgets

Nebula provides reusable SWT-based controls for Java desktop applications,
including grids, charts, galleries, formatted input and the Opal widget family.
Start with the widget catalogue to choose a component, then its snippet and
implementation/test bundles. Nebula builds on SWT's display, native platform and
resource lifecycle; it does not replace those contracts.

[Widget map](#nebula-widget-map) · [Concepts](#nebula-key-concepts) ·
[Quickstart](#nebula-quickstart) · [Example](#nebula-worked-example) ·
[Development guide](docs/DEVELOPMENT_GUIDE.md)

## Nebula widget map

| Goal | Start here |
| --- | --- |
| Choose an existing control | [Widget catalogue](docs/Widgets.md) |
| Explore widget implementations | [widgets/](widgets/) and [Opal](widgets/opal/) |
| Reproduce a Grid interaction | [Grid snippets](examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid/) |
| Change Grid behavior | [Grid bundle](widgets/grid/org.eclipse.nebula.widgets.grid/) and [Grid tests](widgets/grid/org.eclipse.nebula.widgets.grid.test/) |
| Understand build membership/targets | [Parent reactor](releng/org.eclipse.nebula.nebula-parent/pom.xml) |
| Understand fork qualification/tooling | [Development guide](docs/DEVELOPMENT_GUIDE.md) and [M3 guide](m3/README.md) |

## Nebula key concepts

- **Widget / viewer:** a control exposes interaction and state; a JFace-style
  viewer adapts application model elements and labels onto it.
- **Grid item / column:** hierarchical row data and column presentation. Tree
  structure, selection and screen position are distinct concepts.
- **Bundle / feature / p2 site:** implementation unit, installable grouping and
  published update repository.
- **Incubating widget:** an explicitly marked component whose API may change
  between releases; consult the catalogue and release metadata before adoption.
- **Snippet / regression test:** a small demonstration versus an automated
  assertion. A working example does not establish every widget contract.
- **Receiver binding:** an exact SWT source/artifact identity used by the fork's
  qualification lane; a stock or moving SWT build is not equivalent evidence.

## Nebula fork status

This is the downstream [hsoliwal fork](https://github.com/hsoliwal/nebula) of
[Eclipse Nebula](https://github.com/EclipseNebula/nebula).
The checkout contains ordinary widgets, fork-specific Grid/state work,
[recipe tooling](m3/README.md), and a
[bound SWT/Grid screen-validation workflow](.github/workflows/maven.yaml).
Those are inspectable implementations and configured checks, not a blanket claim
of accepted optimization, passing native tests or measured resource budgets.
Keep the normal Tycho build, tooling qualification and native/pixel/resource gates
separate. Original widget licenses and notices remain authoritative.

## Nebula quickstart

The inspected [parent POM](releng/org.eclipse.nebula.nebula-parent/pom.xml) sets
Java 21, and [the Maven workflow](.github/workflows/maven.yaml) uses
**Temurin 21 and Maven 3.9.11**. Prepare network access for Maven/p2 targets and
an SWT/native display environment. Linux CI uses GTK and Xvfb.

```sh
git clone https://github.com/hsoliwal/nebula.git
cd nebula
git rev-parse HEAD
java -version
mvn -version
mvn -V -B clean verify -Dtycho.localArtifacts=ignore
```

This is the ordinary widget build entry point. Reproducing the current Grid
qualification lane additionally requires the workflow's pinned SWT checkout,
native build, receiver p2 preparation, target override and screenshot validator.
The short command does not perform or certify all of those stages.

## Nebula command and diagnosis cheat sheet

```sh
# Normal widget verification on a provisioned Linux/Xvfb host.
xvfb-run -a mvn -V -B clean verify -Dtycho.localArtifacts=ignore
# Find the minimal hierarchical Grid demonstration.
git grep -n 'class GridSnippet1' -- examples
# Inspect reports after the actual selected reactor has run.
find . -path '*/target/surefire-reports/*.xml' -print
```

- **Unresolved bundle:** record the target and repository; do not silently replace
  exact receiver artifacts with an arbitrary local build.
- **UI harness failure:** inspect native GTK/display setup before widget logic.
- **Missing images:** a completed artifact upload is not proof of valid screenshots.
- **Visual change:** use the existing automated capture, pixel and resource gates;
  missing, blank or stale frames are not a pass.

## Nebula worked example

**Build a hierarchical Grid from the existing minimal snippet.** Open
[GridSnippet1.java](examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid/GridSnippet1.java)
in the configured PDE workspace.

1. The snippet creates a display, a shell with `FillLayout` and a scrolling Grid.
2. It creates a visible header and a column with `setTree(true)`, selecting where
   the hierarchy is presented.
3. Follow each `GridItem` constructor's parent to distinguish root and child rows.
4. Run the snippet and exercise expansion, collapse, scroll and close. When
   investigating a bug, record both model/selection state and displayed pixels.
5. Put an automated regression in the Grid test bundle. A reproduction snippet
   alone does not establish native resource or cross-platform coverage.

These are source-derived instructions; this homepage update ran no build or UI
tests. Continue with [the development guide](docs/DEVELOPMENT_GUIDE.md),
[widget catalogue](docs/Widgets.md), [M3 scope](m3/README.md) and
[contributor requirements](CONTRIBUTING.MD).
The original project download links, fork notes and licensing text follow.

> Source tour checked at [`170cb3e9d64c`](https://github.com/hsoliwal/nebula/tree/170cb3e9d64c3cbcfa2e7709a4d422e572bf793b). Commands describe that revision; recheck build configuration when changing revisions. Build/UI/performance verification was not run for this README update.

---

<!-- fork-homepage-detail:v1:end -->

<div align="center">

## M³ × Eclipse Nebula

**Rich widgets expose the cost of repeated layout, search, rendering and retained state.**

[![M3 research target](https://img.shields.io/badge/M%C2%B3-desktop%20research%20target-4b62b7?style=flat-square)](https://github.com/hsoliwal/M3jdk21)
[![Evidence first](https://img.shields.io/badge/acceptance-tests%20%2B%20measurements-287b57?style=flat-square)](https://github.com/hsoliwal/M3jdk21/blob/master/m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md)

**[M3 runtime programme](https://github.com/hsoliwal/M3jdk21)** · **[Technical paper](https://github.com/hsoliwal/M3jdk21/blob/master/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md)** · **[Source invitation](#come-inspect-the-code)**

</div>

> [!NOTE]
> **This is an experimental downstream fork of [the upstream Eclipse Nebula project](https://github.com/EclipseNebula/nebula).** M3 String, regex, precompute and collection integration are **engineering directions**, not verified deployment, performance, compatibility or upstream-endorsement claims. Preserve original licensing, copyright, API behavior and native/UI contracts.

### Engineering questions for this fork

| Surface | What to investigate | Non-negotiable contract |
| --- | --- | --- |
| **Rich widgets** | Baseline large-table, grid, chart and complex-widget interactions | SWT widget API and rendering behavior |
| **Repeated text and search** | Investigate reuse candidates in filtering, labels and model lookups | Ordering, Unicode and model semantics |
| **Lifecycle stress** | Measure allocation and retained UI resources across create/dispose cycles | No widget/resource leaks and stable interaction |

**Approach:** inventory first; copy/derive only with license and provenance; qualify reusable recipes in Synexia; port into the appropriate runtime/product owner; then test with representative desktop projects and publish CPU, allocation, retained-memory, startup and responsiveness numbers, including regressions. No Synexia runtime dependency.

### Come inspect the code

> **Java, JNI, SWT, workbenches, widgets, real code: open the repositories.** Bring a failing case, a performance counterexample, a profiler trace or a better algorithm. Challenge the design on the merits of source, tests and evidence—not slogans or a résumé.

**Explore the family:** [M3JDK21](https://github.com/hsoliwal/M3jdk21) · [SWT](https://github.com/hsoliwal/eclipse.platform.swt) · [Eclipse Platform](https://github.com/hsoliwal/eclipse.platform) · [Platform UI](https://github.com/hsoliwal/eclipse.platform.ui) · [Nebula](https://github.com/hsoliwal/nebula) · [GEF Classic](https://github.com/hsoliwal/gef-classic).

**Contribute here:** [issues](https://github.com/hsoliwal/nebula/issues) · [pull requests](https://github.com/hsoliwal/nebula/pulls) · [M3 compatibility and benchmark contract](https://github.com/hsoliwal/M3jdk21/blob/master/m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md).

The remainder of this README retains the existing M3 fork notes and the **original upstream Eclipse Nebula documentation**. Nothing in this introduction rebrands upstream work as M3-authored code.

---

## M3 direction in this fork

M3 is based on the idea that shared immutable structure and indexed metadata can
provide a basis for reusable computation. The programme starts with M3 String,
regex, and precompute in M3JDK21, then collections and SWT/Eclipse integration.

This Nebula fork is a target for exploring qualified runtime and collection improvements in rich SWT widgets and representative desktop workloads.

**Explore the idea:** [M3 technical design paper](https://github.com/hsoliwal/M3jdk21/blob/master/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md) ·
[Programme goals and roadmap](https://github.com/hsoliwal/M3jdk21#project-goals-and-plan).
The paper explains the proposed architecture, cost tradeoffs, and evaluation plan.
Readers are invited to examine the work and contribute representative workloads.

This section describes the direction of the hsoliwal fork. Upstream documentation
follows below; upstream authorship, licenses, and project identity remain intact.

---

# Nebula and Opal: Rich SWT Widgets
Welcome to the [Eclipse Nebula project](https://projects.eclipse.org/projects/technology.nebula).

This repository contains a large set of UI elements that can be used in applications based on Java and SWT.
See [Nebula Widgets](https://github.com/EclipseNebula/nebula/blob/master/docs/Widgets.md) for information about the available widgets.


## Download
Downloads are available at the following location:
* [https://download.eclipse.org/nebula/updates/](https://download.eclipse.org/nebula/updates/)
* [Link to the archives](https://archive.eclipse.org/nebula/)

These sites provide both mature, stable widgets as well as incubating widgets; the latter are clearly marked as *incubating* and may break API from release to release.

Refer to the [general document](https://eclipse.dev/justj/?page=tools#p2-anatomy) for details about the update site structure.

## Contact
* [Issues](https://github.com/EclipseNebula/nebula/issues)
* [Questions](https://github.com/EclipseNebula/nebula/discussions) 
* [Mailing List](https://dev.eclipse.org/mailman/listinfo/nebula-dev)

## Contribute
We can accept your patches if you have [properly set up an account](https://github.com/eclipse-platform/.github/blob/main/CONTRIBUTING.md#setting-up-your-eclipse-and-github-account).

For every patch we require a [corresponding GitHub issue](https://github.com/EclipseNebula/nebula/issues).
See the [recommended workflow](https://github.com/eclipse-platform/.github/blob/main/CONTRIBUTING.md#setting-up-your-eclipse-and-github-account) for guidelines.

## Setup a Development enviroment
Please use the following automated setup to configure a development environment pull requests:

[![Create Eclipse Development Environment for Eclipse Nebula](https://download.eclipse.org/oomph/www/setups/svg/Nebula.svg)](https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/EclipseNebula/nebula/master/configuration.setup&show=true "Click to open Eclipse-Installer Auto Launch or drag into your running installer")

## Building on our build server
After admin approval, pull requests get built by this job:
 * [Pull Request Builds](https://ci.eclipse.org/nebula/job/nebula-build/view/change-requests/)

## Building locally
    git clone https://github.com/EclipseNebula/nebula.git
    cd nebula
    mvn verify

More info can be found on Nebula's [Build Server](https://ci.eclipse.org/nebula/).

## New Committers
Before you decide to contribute your code to Eclipse, keep in mind that your code must follow the EPL, Eclipse's Public License.  
This means that committers and contributors need to follow some rules.

A good overview of what this entails and how this affects you [can be read here](http://www.eclipse.org/legal/#Committers)

There are a number of ways you can contribute. 

* A new project inside Nebula.
* New widgets in the Nebula project.
* Maintain existing widgets.

If you choose any of these options, just open an issue.

## New Widgets
If you want to contribute a widget, just open an issue and state your intention. We will then guide you through the process.



## M3 / Synexia licensing boundary

Nebula product source remains governed by the Eclipse Public License 2.0 and
its existing file-level notices. Independently authored, separable M3/Synexia
tooling, receipts and documentation are Apache-2.0 only where explicitly
marked. See [M3-SYNEXIA-NOTICE.md](M3-SYNEXIA-NOTICE.md),
[M3-SYNEXIA-AUTHORS.md](M3-SYNEXIA-AUTHORS.md), and
[LICENSE-M3-APACHE-2.0.txt](LICENSE-M3-APACHE-2.0.txt).

These additions do not relicense Eclipse/Nebula product code, OpenJDK,
OpenRewrite or donor material. Their original licenses and attribution remain
authoritative.

## Fork development guide

See the [development guide](docs/DEVELOPMENT_GUIDE.md) for source layout, reproducible build and test lanes, fork-specific boundaries and review handover.
