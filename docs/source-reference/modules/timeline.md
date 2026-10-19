# Project and module records: timeline

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U320: `widgets/timeline`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/pom.xml)
- Maven artifact: `org.eclipse.nebula:timeline:0.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/timeline/org.eclipse.nebula.widgets.timeline` (default), `widgets/timeline/org.eclipse.nebula.widgets.timeline.example` (default), `widgets/timeline/org.eclipse.nebula.widgets.timeline.tests` (default), `widgets/timeline/org.eclipse.nebula.widgets.timeline.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U321: `widgets/timeline/org.eclipse.nebula.widgets.timeline`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline/.project)
- Eclipse project: `org.eclipse.nebula.widgets.timeline`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.timeline:0.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.timeline`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.timeline`, `org.eclipse.nebula.widgets.timeline.figures`, `org.eclipse.nebula.widgets.timeline.figures.detail.track`, `org.eclipse.nebula.widgets.timeline.figures.detail.track.lane`, `org.eclipse.nebula.widgets.timeline.impl`, `org.eclipse.nebula.widgets.timeline.jface`, `org.eclipse.nebula.widgets.timeline.util`
- Required bundles: `org.eclipse.swt`, `org.eclipse.draw2d`, `org.eclipse.emf.ecore;visibility:=reexport`, `org.eclipse.core.runtime`, `org.eclipse.jface;bundle-version="3.1.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.timeline`](../packages/timeline.md#orgeclipsenebulawidgetstimeline), [`org.eclipse.nebula.widgets.timeline.borders`](../packages/timeline.md#orgeclipsenebulawidgetstimelineborders), [`org.eclipse.nebula.widgets.timeline.figures`](../packages/timeline.md#orgeclipsenebulawidgetstimelinefigures), [`org.eclipse.nebula.widgets.timeline.figures.detail`](../packages/timeline.md#orgeclipsenebulawidgetstimelinefiguresdetail), [`org.eclipse.nebula.widgets.timeline.figures.detail.cursor`](../packages/timeline.md#orgeclipsenebulawidgetstimelinefiguresdetailcursor), [`org.eclipse.nebula.widgets.timeline.figures.detail.track`](../packages/timeline.md#orgeclipsenebulawidgetstimelinefiguresdetailtrack), [`org.eclipse.nebula.widgets.timeline.figures.detail.track.lane`](../packages/timeline.md#orgeclipsenebulawidgetstimelinefiguresdetailtracklane), [`org.eclipse.nebula.widgets.timeline.figures.overview`](../packages/timeline.md#orgeclipsenebulawidgetstimelinefiguresoverview), [`org.eclipse.nebula.widgets.timeline.impl`](../packages/timeline.md#orgeclipsenebulawidgetstimelineimpl), [`org.eclipse.nebula.widgets.timeline.jface`](../packages/timeline.md#orgeclipsenebulawidgetstimelinejface), [`org.eclipse.nebula.widgets.timeline.layouts`](../packages/timeline.md#orgeclipsenebulawidgetstimelinelayouts), [`org.eclipse.nebula.widgets.timeline.listeners`](../packages/timeline.md#orgeclipsenebulawidgetstimelinelisteners), [`org.eclipse.nebula.widgets.timeline.util`](../packages/timeline.md#orgeclipsenebulawidgetstimelineutil)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U322: `widgets/timeline/org.eclipse.nebula.widgets.timeline.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.timeline.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.timeline.example:0.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.timeline.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.examples`, `org.eclipse.nebula.widgets.timeline`, `org.eclipse.ui`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.timeline.example`](../packages/timeline.md#orgeclipsenebulawidgetstimelineexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U323: `widgets/timeline/org.eclipse.nebula.widgets.timeline.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.timeline.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.timeline.feature:0.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.timeline.feature`; packages plug-ins `org.eclipse.nebula.widgets.timeline`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U324: `widgets/timeline/org.eclipse.nebula.widgets.timeline.snippets`

- Classification: example; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.snippets/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.timeline.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.nebula.widgets.timeline.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.timeline;bundle-version="0.1.0"`, `org.eclipse.swt;bundle-version="3.112.0"`, `org.eclipse.jface;bundle-version="3.17.0"`, `org.eclipse.draw2d;bundle-version="3.10.100"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.timeline.snippets`](../packages/timeline.md#orgeclipsenebulawidgetstimelinesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U325: `widgets/timeline/org.eclipse.nebula.widgets.timeline.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/timeline/org.eclipse.nebula.widgets.timeline.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.timeline.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.timeline.tests:0.1.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.timeline.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.timeline;bundle-version="0.1.0"`
- Exported package clauses: `org.eclipse.nebula.widgets.timeline.tests`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.timeline;visibility:=reexport`, `org.eclipse.emf.ecore.xmi;visibility:=reexport`, `org.junit;visibility:=reexport`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.timeline`](../packages/timeline.md#orgeclipsenebulawidgetstimeline), [`org.eclipse.nebula.widgets.timeline.tests`](../packages/timeline.md#orgeclipsenebulawidgetstimelinetests)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

