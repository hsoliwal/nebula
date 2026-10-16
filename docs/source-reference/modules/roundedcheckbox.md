# Project and module records: roundedcheckbox

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U284: `widgets/roundedcheckbox`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/pom.xml)
- Maven artifact: `org.eclipse.nebula:roundedcheckbox:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox` (default), `widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.feature` (default), `widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U285: `widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedcheckbox`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedcheckbox:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.roundedcheckbox`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.roundedcheckbox`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.roundedcheckbox`](../packages/roundedcheckbox.md#orgeclipsenebulawidgetsroundedcheckbox)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U286: `widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedcheckbox.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedcheckbox.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.roundedcheckbox.feature`; packages plug-ins `org.eclipse.nebula.widgets.roundedcheckbox`, `org.eclipse.nebula.widgets.roundedcheckbox.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U287: `widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedcheckbox.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedcheckbox.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.roundedcheckbox.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.roundedcheckbox;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.roundedcheckbox.snippets`](../packages/roundedcheckbox.md#orgeclipsenebulawidgetsroundedcheckboxsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

