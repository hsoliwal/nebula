# Project and module records: collapsiblebuttons

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U054: `widgets/collapsiblebuttons`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/pom.xml)
- Maven artifact: `org.eclipse.nebula:collapsiblebuttons:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons` (default), `widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.example` (default), `widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U055: `widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons/.project)
- Eclipse project: `org.eclipse.nebula.widgets.collapsiblebuttons`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.collapsiblebuttons:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.collapsiblebuttons`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.collapsiblebuttons`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.collapsiblebuttons`](../packages/collapsiblebuttons.md#orgeclipsenebulawidgetscollapsiblebuttons)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U056: `widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.collapsiblebuttons.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.collapsiblebuttons.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.collapsiblebuttons.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.collapsiblebuttons;bundle-version="1.0.0"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.collapsiblebuttons.example`](../packages/collapsiblebuttons.md#orgeclipsenebulawidgetscollapsiblebuttonsexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U057: `widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/collapsiblebuttons/org.eclipse.nebula.widgets.collapsiblebuttons.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.collapsiblebuttons.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.collapsiblebuttons.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.collapsiblebuttons.feature`; packages plug-ins `org.eclipse.nebula.widgets.collapsiblebuttons`, `org.eclipse.nebula.widgets.collapsiblebuttons.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

