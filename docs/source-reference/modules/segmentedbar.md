# Project and module records: segmentedbar

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U295: `widgets/segmentedbar`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/pom.xml)
- Maven artifact: `org.eclipse.nebula:segmentedbar:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar` (default), `widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.feature` (default), `widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U296: `widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar/.project)
- Eclipse project: `org.eclipse.nebula.widgets.segmentedbar`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.segmentedbar:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.segmentedbar`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.segmentedbar`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.jface`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.segmentedbar`](../packages/segmentedbar.md#orgeclipsenebulawidgetssegmentedbar)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U297: `widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.segmentedbar.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.segmentedbar.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.segmentedbar.feature`; packages plug-ins `org.eclipse.nebula.widgets.segmentedbar`, `org.eclipse.nebula.widgets.segmentedbar.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U298: `widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/segmentedbar/org.eclipse.nebula.widgets.segmentedbar.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.segmentedbar.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.segmentedbar.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.segmentedbar.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.segmentedbar;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.segmentedbar`](../packages/segmentedbar.md#orgeclipsenebulawidgetssegmentedbar)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

