# Project and module records: xviewer

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U339: `widgets/xviewer`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/pom.xml)
- Maven artifact: `org.eclipse.nebula:xviewer:1.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core` (default), `widgets/xviewer/org.eclipse.nebula.widgets.xviewer` (default), `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example` (default), `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests` (default), `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U340: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.xviewer`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.xviewer`, `org.eclipse.nebula.widgets.xviewer.action`, `org.eclipse.nebula.widgets.xviewer.column`, `org.eclipse.nebula.widgets.xviewer.customize`, `org.eclipse.nebula.widgets.xviewer.customize.dialog`, `org.eclipse.nebula.widgets.xviewer.edit`, `org.eclipse.nebula.widgets.xviewer.util`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.ui.forms`, `org.eclipse.swt`
- Imported package clauses: `org.eclipse.nebula.widgets.xviewer.core.model`, `org.eclipse.nebula.widgets.xviewer.core.util`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.xviewer`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewer), [`org.eclipse.nebula.widgets.xviewer.action`](../packages/xviewer.md#orgeclipsenebulawidgetsxvieweraction), [`org.eclipse.nebula.widgets.xviewer.column`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewercolumn), [`org.eclipse.nebula.widgets.xviewer.customize`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewercustomize), [`org.eclipse.nebula.widgets.xviewer.customize.dialog`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewercustomizedialog), [`org.eclipse.nebula.widgets.xviewer.edit`](../packages/xviewer.md#orgeclipsenebulawidgetsxvieweredit), [`org.eclipse.nebula.widgets.xviewer.util`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerutil), [`org.eclipse.nebula.widgets.xviewer.util.internal`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerutilinternal), [`org.eclipse.nebula.widgets.xviewer.util.internal.dialog`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerutilinternaldialog), [`org.eclipse.nebula.widgets.xviewer.util.internal.images`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerutilinternalimages)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U341: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.core/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer.core`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer.core:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.xviewer.core`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.xviewer.core.model`, `org.eclipse.nebula.widgets.xviewer.core.util`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.xviewer.core.model`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewercoremodel), [`org.eclipse.nebula.widgets.xviewer.core.util`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewercoreutil)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U342: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.xviewer.example`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.xviewer.example`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.xviewer`
- Imported package clauses: `org.eclipse.nebula.widgets.xviewer.core.model`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.xviewer.example`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerexample), [`org.eclipse.nebula.widgets.xviewer.example.images`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerexampleimages), [`org.eclipse.nebula.widgets.xviewer.example.model`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerexamplemodel), [`org.eclipse.nebula.widgets.xviewer.example.styledExample`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerexamplestyledexample), [`org.eclipse.nebula.widgets.xviewer.example.util`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerexampleutil)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U343: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.xviewer.feature`; packages plug-ins `org.eclipse.nebula.widgets.xviewer`, `org.eclipse.nebula.widgets.xviewer.source`, `org.eclipse.nebula.widgets.xviewer.core`, `org.eclipse.nebula.widgets.xviewer.core.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U344: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature.test`

- Classification: test; outside parsed default reactor; retained, not excluded
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature.test/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature.test/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.feature.test/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer.feature.test`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer.feature.test:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.xviewer.feature.test`; packages plug-ins `org.eclipse.test`, `org.eclipse.ant.optional.junit`, `org.eclipse.nebula.widgets.xviewer.tests`, `org.eclipse.nebula.widgets.xviewer.integration`; included features `org.eclipse.nebula.widgets.xviewer.feature.test.source`
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U345: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.integration/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer.integration`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer.integration:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.xviewer.integration`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.xviewer`, `org.eclipse.nebula.examples`, `org.eclipse.nebula.widgets.xviewer.example`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.xviewer.integration`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewerintegration)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U346: `widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.xviewer.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.xviewer.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.xviewer.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.xviewer`
- Exported package clauses: `org.eclipse.nebula.widgets.xviewer.tests`
- Required bundles: `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.xviewer.tests`](../packages/xviewer.md#orgeclipsenebulawidgetsxviewertests)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

