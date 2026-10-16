# Project and module records: pgroup

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U255: `widgets/pgroup`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/pom.xml)
- Maven artifact: `org.eclipse.nebula:pgroup:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/pgroup/org.eclipse.nebula.widgets.pgroup` (default), `widgets/pgroup/org.eclipse.nebula.widgets.pgroup.example` (default), `widgets/pgroup/org.eclipse.nebula.widgets.pgroup.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U256: `widgets/pgroup/org.eclipse.nebula.widgets.pgroup`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pgroup`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pgroup:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pgroup`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.pgroup`, `org.eclipse.nebula.widgets.pgroup.internal;x-internal:=true`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pgroup`](../packages/pgroup.md#orgeclipsenebulawidgetspgroup), [`org.eclipse.nebula.widgets.pgroup.internal`](../packages/pgroup.md#orgeclipsenebulawidgetspgroupinternal)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U257: `widgets/pgroup/org.eclipse.nebula.widgets.pgroup.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pgroup.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pgroup.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pgroup.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.pgroup;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pgroup.example`](../packages/pgroup.md#orgeclipsenebulawidgetspgroupexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U258: `widgets/pgroup/org.eclipse.nebula.widgets.pgroup.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pgroup/org.eclipse.nebula.widgets.pgroup.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pgroup.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pgroup.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.pgroup.feature`; packages plug-ins `org.eclipse.nebula.widgets.pgroup`, `org.eclipse.nebula.widgets.pgroup.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

