# Project and module records: tiles

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U316: `widgets/tiles`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/pom.xml)
- Maven artifact: `org.eclipse.nebula:tiles:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/tiles/org.eclipse.nebula.widgets.tiles` (default), `widgets/tiles/org.eclipse.nebula.widgets.tiles.feature` (default), `widgets/tiles/org.eclipse.nebula.widgets.tiles.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U317: `widgets/tiles/org.eclipse.nebula.widgets.tiles`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tiles`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tiles:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tiles`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.tiles`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tiles`](../packages/tiles.md#orgeclipsenebulawidgetstiles)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U318: `widgets/tiles/org.eclipse.nebula.widgets.tiles.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tiles.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tiles.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.tiles.feature`; packages plug-ins `org.eclipse.nebula.widgets.tiles`, `org.eclipse.nebula.widgets.tiles.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U319: `widgets/tiles/org.eclipse.nebula.widgets.tiles.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tiles/org.eclipse.nebula.widgets.tiles.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tiles.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tiles.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tiles.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.tiles;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tiles`](../packages/tiles.md#orgeclipsenebulawidgetstiles)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

