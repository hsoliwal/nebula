# Project and module records: visualization

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U331: `widgets/visualization`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/pom.xml)
- Maven artifact: `org.eclipse.nebula:visualization:2.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/visualization/org.eclipse.nebula.visualization.widgets` (default), `widgets/visualization/org.eclipse.nebula.visualization.xygraph` (default), `widgets/visualization/org.eclipse.nebula.visualization.feature` (default), `widgets/visualization/org.eclipse.nebula.visualization.example` (default), `widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U332: `widgets/visualization/org.eclipse.nebula.visualization.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.example/.project)
- Eclipse project: `org.eclipse.nebula.visualization.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.visualization.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.visualization.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.visualization.xygraph.exampleview`, `org.eclipse.nebula.examples`
- Owned declaration namespaces: [`org.eclipse.nebula.visualization.example`](../packages/visualization.md#orgeclipsenebulavisualizationexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U333: `widgets/visualization/org.eclipse.nebula.visualization.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.feature/.project)
- Eclipse project: `org.eclipse.nebula.visualization.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.visualization.feature:2.2.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.visualization.feature`; packages plug-ins `org.eclipse.nebula.visualization.widgets`, `org.eclipse.nebula.visualization.widgets.source`, `org.eclipse.nebula.visualization.xygraph`, `org.eclipse.nebula.visualization.xygraph.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U334: `widgets/visualization/org.eclipse.nebula.visualization.widgets`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets/.project)
- Eclipse project: `org.eclipse.nebula.visualization.widgets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.visualization.widgets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.visualization.widgets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.visualization.internal.widgets.introspection`, `org.eclipse.nebula.visualization.internal.widgets.raster;x-internal:=true`, `org.eclipse.nebula.visualization.widgets.datadefinition`, `org.eclipse.nebula.visualization.widgets.figureparts`, `org.eclipse.nebula.visualization.widgets.figures`, `org.eclipse.nebula.visualization.widgets.util`
- Required bundles: `org.eclipse.swt`, `org.eclipse.draw2d`, `org.eclipse.nebula.visualization.xygraph;bundle-version="[3.0.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.visualization.internal.widgets.introspection`](../packages/visualization.md#orgeclipsenebulavisualizationinternalwidgetsintrospection), [`org.eclipse.nebula.visualization.internal.widgets.raster`](../packages/visualization.md#orgeclipsenebulavisualizationinternalwidgetsraster), [`org.eclipse.nebula.visualization.widgets.datadefinition`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsdatadefinition), [`org.eclipse.nebula.visualization.widgets.examples`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsexamples), [`org.eclipse.nebula.visualization.widgets.figureparts`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsfigureparts), [`org.eclipse.nebula.visualization.widgets.figures`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsfigures), [`org.eclipse.nebula.visualization.widgets.util`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsutil)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U335: `widgets/visualization/org.eclipse.nebula.visualization.widgets.tests`

- Classification: test; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/.project)
- Eclipse project: `org.eclipse.nebula.visualization.widgets.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.visualization.widgets.test:2.1.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.visualization.widgets.tests`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt`, `org.eclipse.nebula.visualization.widgets;bundle-version="1.0.0"`, `org.eclipse.nebula.visualization.xygraph;bundle-version="1.0.0"`, `org.eclipse.draw2d`, `org.junit;bundle-version="4.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.visualization.widgets.figures`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsfigures)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U336: `widgets/visualization/org.eclipse.nebula.visualization.xygraph`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph/.project)
- Eclipse project: `org.eclipse.nebula.visualization.xygraph`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.visualization.xygraph:3.2.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.visualization.xygraph`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.visualization.internal.xygraph.toolbar`, `org.eclipse.nebula.visualization.internal.xygraph.decimation;x-internal:=true`, `org.eclipse.nebula.visualization.internal.xygraph.undo`, `org.eclipse.nebula.visualization.xygraph.dataprovider`, `org.eclipse.nebula.visualization.xygraph.figures`, `org.eclipse.nebula.visualization.xygraph.linearscale`, `org.eclipse.nebula.visualization.xygraph.styleprovider`, `org.eclipse.nebula.visualization.xygraph.util`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`, `org.eclipse.draw2d`, `org.eclipse.jface`, `org.eclipse.core.runtime`
- Owned declaration namespaces: [`org.eclipse.nebula.visualization.internal.xygraph.decimation`](../packages/visualization.md#orgeclipsenebulavisualizationinternalxygraphdecimation), [`org.eclipse.nebula.visualization.internal.xygraph.toolbar`](../packages/visualization.md#orgeclipsenebulavisualizationinternalxygraphtoolbar), [`org.eclipse.nebula.visualization.internal.xygraph.undo`](../packages/visualization.md#orgeclipsenebulavisualizationinternalxygraphundo), [`org.eclipse.nebula.visualization.internal.xygraph.utils`](../packages/visualization.md#orgeclipsenebulavisualizationinternalxygraphutils), [`org.eclipse.nebula.visualization.xygraph`](../packages/visualization.md#orgeclipsenebulavisualizationxygraph), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.examples`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphexamples), [`org.eclipse.nebula.visualization.xygraph.figures`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphlinearscale), [`org.eclipse.nebula.visualization.xygraph.styleprovider`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphstyleprovider), [`org.eclipse.nebula.visualization.xygraph.util`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphutil)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U337: `widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.exampleview/.project)
- Eclipse project: `org.eclipse.nebula.visualization.xygraph.exampleview`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.visualization.xygraph.exampleview:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.visualization.xygraph.exampleview`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.draw2d`, `org.eclipse.nebula.visualization.widgets;bundle-version="1.0.0"`, `org.eclipse.nebula.visualization.xygraph;bundle-version="3.0.0"`, `org.eclipse.ui`
- Owned declaration namespaces: [`org.eclipse.nebula.visualization.xygraph.exampleview`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphexampleview)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U338: `widgets/visualization/org.eclipse.nebula.visualization.xygraph.test`

- Classification: test; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/visualization/org.eclipse.nebula.visualization.xygraph.test/.project)
- Eclipse project: `org.eclipse.nebula.visualization.xygraph.test`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.nebula.visualization.xygraph.test`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.visualization.xygraph`
- Required bundles: `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.visualization.internal.xygraph.decimation`](../packages/visualization.md#orgeclipsenebulavisualizationinternalxygraphdecimation), [`org.eclipse.nebula.visualization.xygraph.dataprovider`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphdataprovider), [`org.eclipse.nebula.visualization.xygraph.figures`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphfigures), [`org.eclipse.nebula.visualization.xygraph.linearscale`](../packages/visualization.md#orgeclipsenebulavisualizationxygraphlinearscale)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

