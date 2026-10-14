# Project and module records: ganttchart

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U101: `widgets/ganttchart`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/pom.xml)
- Maven artifact: `org.eclipse.nebula:ganttchart:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart` (default), `widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.example` (default), `widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U102: `widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ganttchart`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ganttchart:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ganttchart`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.ganttchart`, `org.eclipse.nebula.widgets.ganttchart.dnd`, `org.eclipse.nebula.widgets.ganttchart.print`, `org.eclipse.nebula.widgets.ganttchart.themes`, `org.eclipse.nebula.widgets.ganttchart.undoredo`, `org.eclipse.nebula.widgets.ganttchart.undoredo.commands`, `org.eclipse.nebula.widgets.ganttchart.utils`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ganttchart`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchart), [`org.eclipse.nebula.widgets.ganttchart.dnd`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartdnd), [`org.eclipse.nebula.widgets.ganttchart.print`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartprint), [`org.eclipse.nebula.widgets.ganttchart.themes`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartthemes), [`org.eclipse.nebula.widgets.ganttchart.undoredo`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartundoredo), [`org.eclipse.nebula.widgets.ganttchart.undoredo.commands`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartundoredocommands), [`org.eclipse.nebula.widgets.ganttchart.utils`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartutils)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U103: `widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ganttchart.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ganttchart.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ganttchart.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.core.runtime;bundle-version="3.4.0"`, `org.eclipse.ui;bundle-version="3.4.0"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.ganttchart;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ganttchart.example`](../packages/ganttchart.md#orgeclipsenebulawidgetsganttchartexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U104: `widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ganttchart/org.eclipse.nebula.widgets.ganttchart.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ganttchart.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ganttchart.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.ganttchart.feature`; packages plug-ins `org.eclipse.nebula.widgets.ganttchart`, `org.eclipse.nebula.widgets.ganttchart.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

