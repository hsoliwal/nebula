# Project and module records: opal-propertytable

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U197: `widgets/opal/propertytable`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/pom.xml)
- Maven artifact: `org.eclipse.nebula:propertytable:1.0.2-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable` (default), `widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.feature` (default), `widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U198: `widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.propertytable`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.propertytable:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.propertytable`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.propertytable`, `org.eclipse.nebula.widgets.opal.propertytable.editor`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.nebula.widgets.opal.dialog;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.propertytable`](../packages/opal-propertytable.md#orgeclipsenebulawidgetsopalpropertytable), [`org.eclipse.nebula.widgets.opal.propertytable.editor`](../packages/opal-propertytable.md#orgeclipsenebulawidgetsopalpropertytableeditor)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U199: `widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.propertytable.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.propertytable.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.propertytable.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.propertytable`, `org.eclipse.nebula.widgets.opal.propertytable.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U200: `widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/propertytable/org.eclipse.nebula.widgets.opal.propertytable.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.propertytable.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.propertytable.snippets:1.0.2-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.propertytable.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.propertytable`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.propertytable.snippets`](../packages/opal-propertytable.md#orgeclipsenebulawidgetsopalpropertytablesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

