# Project and module records: opal-rangeslider

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U201: `widgets/opal/rangeslider`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/pom.xml)
- Maven artifact: `org.eclipse.nebula:rangeslider:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider` (default), `widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.feature` (default), `widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U202: `widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.rangeslider`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.rangeslider:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.rangeslider`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.rangeslider`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.rangeslider`](../packages/opal-rangeslider.md#orgeclipsenebulawidgetsopalrangeslider)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U203: `widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.rangeslider.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.rangeslider.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.rangeslider.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.rangeslider`, `org.eclipse.nebula.widgets.opal.rangeslider.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U204: `widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/rangeslider/org.eclipse.nebula.widgets.opal.rangeslider.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.rangeslider.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.rangeslider.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.rangeslider.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.rangeslider;bundle-version="1.0.0"`, `org.eclipse.nebula.widgets.opal.titledseparator;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.rangeslider.snippets`](../packages/opal-rangeslider.md#orgeclipsenebulawidgetsopalrangeslidersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

