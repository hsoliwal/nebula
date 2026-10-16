# Project and module records: nebulaslider

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U123: `widgets/nebulaslider`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/pom.xml)
- Maven artifact: `org.eclipse.nebula:nebulaslider:1.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider` (default), `widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.feature` (default), `widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U124: `widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider/.project)
- Eclipse project: `org.eclipse.nebula.widgets.nebulaslider`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.nebulaslider:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.nebulaslider`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.nebulaslider`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.nebulaslider`](../packages/nebulaslider.md#orgeclipsenebulawidgetsopalnebulaslider)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U125: `widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.nebulaslider.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.nebulaslider.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.nebulaslider.feature`; packages plug-ins `org.eclipse.nebula.widgets.nebulaslider`, `org.eclipse.nebula.widgets.nebulaslider.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U126: `widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/nebulaslider/org.eclipse.nebula.widgets.nebulaslider.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.nebulaslider.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.nebulaslider.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.nebulaslider.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.nebulaslider`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.nebulaslider.snippets`](../packages/nebulaslider.md#orgeclipsenebulawidgetsopalnebulaslidersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

