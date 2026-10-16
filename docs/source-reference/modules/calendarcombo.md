# Project and module records: calendarcombo

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U031: `widgets/calendarcombo`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/pom.xml)
- Maven artifact: `org.eclipse.nebula:calendarcombo:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo` (default), `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.tests` (default), `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.example` (default), `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U032: `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo/.project)
- Eclipse project: `org.eclipse.nebula.widgets.calendarcombo`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.calendarcombo:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.calendarcombo`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.calendarcombo`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.calendarcombo`](../packages/calendarcombo.md#orgeclipsenebulawidgetscalendarcombo)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U033: `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.calendarcombo.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.calendarcombo.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.calendarcombo.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.calendarcombo;bundle-version="0.14.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.calendarcombo.example`](../packages/calendarcombo.md#orgeclipsenebulawidgetscalendarcomboexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U034: `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.calendarcombo.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.calendarcombo.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.calendarcombo.feature`; packages plug-ins `org.eclipse.nebula.widgets.calendarcombo`, `org.eclipse.nebula.widgets.calendarcombo.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U035: `widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/calendarcombo/org.eclipse.nebula.widgets.calendarcombo.tests/pom.xml)
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.calendarcombo.tests:1.1.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.calendarcombo.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.calendarcombo`
- Required bundles: `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.calendarcombo`](../packages/calendarcombo.md#orgeclipsenebulawidgetscalendarcombo)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

