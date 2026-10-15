# Project and module records: opal-duallist

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U153: `widgets/opal/duallist`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/pom.xml)
- Maven artifact: `org.eclipse.nebula:duallist:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist` (default), `widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.feature` (default), `widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U154: `widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.duallist`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.duallist:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.duallist`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.duallist`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.duallist`](../packages/opal-duallist.md#orgeclipsenebulawidgetsopalduallist)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U155: `widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.duallist.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.duallist.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.duallist.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.duallist`, `org.eclipse.nebula.widgets.opal.duallist.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U156: `widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/duallist/org.eclipse.nebula.widgets.opal.duallist.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.duallist.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.duallist.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.duallist.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.duallist;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.duallist.snippets`](../packages/opal-duallist.md#orgeclipsenebulawidgetsopalduallistsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

