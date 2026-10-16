# Project and module records: carousel

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U036: `widgets/carousel`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/pom.xml)
- Maven artifact: `org.eclipse.nebula:carousel:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/carousel/org.eclipse.nebula.widgets.carousel` (default), `widgets/carousel/org.eclipse.nebula.widgets.carousel.feature` (default), `widgets/carousel/org.eclipse.nebula.widgets.carousel.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U037: `widgets/carousel/org.eclipse.nebula.widgets.carousel`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel/.project)
- Eclipse project: `org.eclipse.nebula.widgets.carousel`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.carousel:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.carousel`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.carousel`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.carousel`](../packages/carousel.md#orgeclipsenebulawidgetscarousel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U038: `widgets/carousel/org.eclipse.nebula.widgets.carousel.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.carousel.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.carousel.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.carousel.feature`; packages plug-ins `org.eclipse.nebula.widgets.carousel`, `org.eclipse.nebula.widgets.carousel.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U039: `widgets/carousel/org.eclipse.nebula.widgets.carousel.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/carousel/org.eclipse.nebula.widgets.carousel.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.carousel.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.carousel.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.carousel.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.carousel;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.chips`](../packages/carousel.md#orgeclipsenebulawidgetschips)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

