# Project and module records: chips

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U050: `widgets/chips`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/pom.xml)
- Maven artifact: `org.eclipse.nebula:chips:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/chips/org.eclipse.nebula.widgets.chips` (default), `widgets/chips/org.eclipse.nebula.widgets.chips.feature` (default), `widgets/chips/org.eclipse.nebula.widgets.chips.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U051: `widgets/chips/org.eclipse.nebula.widgets.chips`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips/.project)
- Eclipse project: `org.eclipse.nebula.widgets.chips`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.nebula.widgets.chips`; execution environment: `JavaSE-17`
- Exported package clauses: `org.eclipse.nebula.widgets.chips;version="2.0.0"`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.chips`](../packages/carousel.md#orgeclipsenebulawidgetschips)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U052: `widgets/chips/org.eclipse.nebula.widgets.chips.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.chips.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.chips.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.chips.feature`; packages plug-ins `org.eclipse.nebula.widgets.chips`, `org.eclipse.nebula.widgets.chips.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U053: `widgets/chips/org.eclipse.nebula.widgets.chips.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips.snippets/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/chips/org.eclipse.nebula.widgets.chips.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.chips.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.nebula.widgets.chips.snippets`; execution environment: `JavaSE-17`
- Required bundles: `org.eclipse.nebula.widgets.chips;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.chips`](../packages/carousel.md#orgeclipsenebulawidgetschips)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

