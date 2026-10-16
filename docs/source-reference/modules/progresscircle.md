# Project and module records: progresscircle

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U264: `widgets/progresscircle`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/pom.xml)
- Maven artifact: `org.eclipse.nebula:progresscircle:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle` (default), `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.feature` (default), `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U265: `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle/.project)
- Eclipse project: `org.eclipse.nebula.widgets.progresscircle`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.progresscircle:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.progresscircle`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.progresscircle`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.progresscircle`](../packages/progresscircle.md#orgeclipsenebulawidgetsprogresscircle)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U266: `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.progresscircle.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.progresscircle.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.progresscircle.feature`; packages plug-ins `org.eclipse.nebula.widgets.progresscircle`, `org.eclipse.nebula.widgets.progresscircle.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U267: `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.progresscircle.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.progresscircle.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.progresscircle.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.progresscircle;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.progresscircle.snippets`](../packages/progresscircle.md#orgeclipsenebulawidgetsprogresscirclesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

