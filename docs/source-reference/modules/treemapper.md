# Project and module records: treemapper

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U326: `widgets/treemapper`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/pom.xml)
- Maven artifact: `org.eclipse.nebula:treemapper:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/treemapper/org.eclipse.nebula.widgets.treemapper` (default), `widgets/treemapper/org.eclipse.nebula.widgets.treemapper.examples` (default), `widgets/treemapper/org.eclipse.nebula.widgets.treemapper.tests` (default), `widgets/treemapper/org.eclipse.nebula.widgets.treemapper.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U327: `widgets/treemapper/org.eclipse.nebula.widgets.treemapper`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper/.project)
- Eclipse project: `org.eclipse.nebula.widgets.treemapper`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`, `org.eclipse.pde.api.tools.apiAnalysisNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.treemapper:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.treemapper`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.treemapper`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.draw2d`, `org.eclipse.jface`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.treemapper`](../packages/treemapper.md#orgeclipsenebulawidgetstreemapper), [`org.eclipse.nebula.widgets.treemapper.internal`](../packages/treemapper.md#orgeclipsenebulawidgetstreemapperinternal)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U328: `widgets/treemapper/org.eclipse.nebula.widgets.treemapper.examples`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.examples/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.examples/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.examples/.project)
- Eclipse project: `org.eclipse.nebula.widgets.treemapper.examples`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.treemapper.examples:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.treemapper.examples`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.treemapper`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`, `org.eclipse.jface`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.treemapper.examples`](../packages/treemapper.md#orgeclipsenebulawidgetstreemapperexamples)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U329: `widgets/treemapper/org.eclipse.nebula.widgets.treemapper.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.treemapper.feature`; natures: 
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.treemapper.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.treemapper.feature`; packages plug-ins `org.eclipse.nebula.widgets.treemapper`, `org.eclipse.nebula.widgets.treemapper.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U330: `widgets/treemapper/org.eclipse.nebula.widgets.treemapper.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/treemapper/org.eclipse.nebula.widgets.treemapper.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.treemapper.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.treemapper.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.treemapper.tests`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.treemapper`, `org.junit;bundle-version="4.0.0"`, `org.eclipse.draw2d`, `org.eclipse.jface`, `org.eclipse.osgi`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.treemapper.tests`](../packages/treemapper.md#orgeclipsenebulawidgetstreemappertests)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

