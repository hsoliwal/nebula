# Project and module records: led

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U119: `widgets/led`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/pom.xml)
- Maven artifact: `org.eclipse.nebula:led:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/led/org.eclipse.nebula.widgets.led` (default), `widgets/led/org.eclipse.nebula.widgets.led.feature` (default), `widgets/led/org.eclipse.nebula.widgets.led.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U120: `widgets/led/org.eclipse.nebula.widgets.led`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led/.project)
- Eclipse project: `org.eclipse.nebula.widgets.led`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.led:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.led`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.led`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.led`](../packages/led.md#orgeclipsenebulawidgetsled)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U121: `widgets/led/org.eclipse.nebula.widgets.led.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.led.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.led.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.led.feature`; packages plug-ins `org.eclipse.nebula.widgets.led`, `org.eclipse.nebula.widgets.led.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U122: `widgets/led/org.eclipse.nebula.widgets.led.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/led/org.eclipse.nebula.widgets.led.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.led.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.led.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.led.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.led;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.led`](../packages/led.md#orgeclipsenebulawidgetsled)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

