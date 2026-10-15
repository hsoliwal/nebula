# Project and module records: opal-heapmanager

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U161: `widgets/opal/heapmanager`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/pom.xml)
- Maven artifact: `org.eclipse.nebula:heapmanager:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager` (default), `widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.feature` (default), `widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U162: `widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.heapmanager`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.heapmanager:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.heapmanager`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.heapmanager`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.heapmanager`](../packages/opal-heapmanager.md#orgeclipsenebulawidgetsopalheapmanager)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U163: `widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.heapmanager.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.heapmanager.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.heapmanager.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.heapmanager`, `org.eclipse.nebula.widgets.opal.heapmanager.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U164: `widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/heapmanager/org.eclipse.nebula.widgets.opal.heapmanager.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.heapmanager.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.heapmanager.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.heapmanager.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.heapmanager;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.heapmanager.snippets`](../packages/opal-heapmanager.md#orgeclipsenebulawidgetsopalheapmanagersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

