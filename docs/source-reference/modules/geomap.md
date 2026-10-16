# Project and module records: geomap

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U105: `widgets/geomap`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/pom.xml)
- Maven artifact: `org.eclipse.nebula:geomap:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/geomap/org.eclipse.nebula.widgets.geomap` (default), `widgets/geomap/org.eclipse.nebula.widgets.geomap.example` (default), `widgets/geomap/org.eclipse.nebula.widgets.geomap.feature` (default), `widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U106: `widgets/geomap/org.eclipse.nebula.widgets.geomap`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap/.project)
- Eclipse project: `org.eclipse.nebula.widgets.geomap`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.geomap:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.geomap`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.geomap`, `org.eclipse.nebula.widgets.geomap.draw2d`, `org.eclipse.nebula.widgets.geomap.internal;x-internal:=true`, `org.eclipse.nebula.widgets.geomap.internal.geomapbrowser;x-internal:=true`, `org.eclipse.nebula.widgets.geomap.jface`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`, `org.eclipse.jface;bundle-version="3.7.0";resolution:=optional`, `org.eclipse.draw2d;bundle-version="3.9.0";resolution:=optional`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.geomap`](../packages/geomap.md#orgeclipsenebulawidgetsgeomap), [`org.eclipse.nebula.widgets.geomap.draw2d`](../packages/geomap.md#orgeclipsenebulawidgetsgeomapdraw2d), [`org.eclipse.nebula.widgets.geomap.internal`](../packages/geomap.md#orgeclipsenebulawidgetsgeomapinternal), [`org.eclipse.nebula.widgets.geomap.internal.geomapbrowser`](../packages/geomap.md#orgeclipsenebulawidgetsgeomapinternalgeomapbrowser), [`org.eclipse.nebula.widgets.geomap.jface`](../packages/geomap.md#orgeclipsenebulawidgetsgeomapjface)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U107: `widgets/geomap/org.eclipse.nebula.widgets.geomap.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.geomap.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.geomap.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.geomap.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.geomap;bundle-version="0.1.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.geomap.jface.example`](../packages/geomap.md#orgeclipsenebulawidgetsgeomapjfaceexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U108: `widgets/geomap/org.eclipse.nebula.widgets.geomap.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.geomap.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.geomap.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.geomap.feature`; packages plug-ins `org.eclipse.nebula.widgets.geomap`, `org.eclipse.nebula.widgets.geomap.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U109: `widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.geomap.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.geomap.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.geomap.snippets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.geomap.snippets`
- Required bundles: `org.eclipse.jface`, `org.eclipse.ui.forms`, `org.eclipse.nebula.widgets.geomap`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.geomap.snippets`](../packages/geomap.md#orgeclipsenebulawidgetsgeomapsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U110: `widgets/geomap/org.eclipse.nebula.widgets.geomap.tests`

- Classification: test; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/geomap/org.eclipse.nebula.widgets.geomap.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.geomap.tests`; natures: `org.eclipse.m2e.core.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.geomap.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.geomap.tests`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt`, `org.eclipse.swtbot.go`, `org.eclipse.nebula.widgets.geomap;bundle-version="0.1.0"`
- Imported package clauses: `org.slf4j;resolution:=optional`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.geomap.tests`](../packages/geomap.md#orgeclipsenebulawidgetsgeomaptests)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

