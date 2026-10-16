# Project and module records: stepbar

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U303: `widgets/stepbar`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/pom.xml)
- Maven artifact: `org.eclipse.nebula:stepbar:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/stepbar/org.eclipse.nebula.widgets.stepbar` (default), `widgets/stepbar/org.eclipse.nebula.widgets.stepbar.feature` (default), `widgets/stepbar/org.eclipse.nebula.widgets.stepbar.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U304: `widgets/stepbar/org.eclipse.nebula.widgets.stepbar`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar/.project)
- Eclipse project: `org.eclipse.nebula.widgets.stepbar`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.stepbar:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.stepbar`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.stepbar`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.stepbar`](../packages/stepbar.md#orgeclipsenebulawidgetsstepbar)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U305: `widgets/stepbar/org.eclipse.nebula.widgets.stepbar.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.stepbar.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.stepbar.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.stepbar.feature`; packages plug-ins `org.eclipse.nebula.widgets.stepbar`, `org.eclipse.nebula.widgets.stepbar.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U306: `widgets/stepbar/org.eclipse.nebula.widgets.stepbar.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/stepbar/org.eclipse.nebula.widgets.stepbar.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.stepbar.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.stepbar.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.stepbar.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.stepbar;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.stepbar`](../packages/stepbar.md#orgeclipsenebulawidgetsstepbar)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

