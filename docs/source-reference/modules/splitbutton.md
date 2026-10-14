# Project and module records: splitbutton

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U299: `widgets/splitbutton`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/pom.xml)
- Maven artifact: `org.eclipse.nebula:splitbutton:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton` (default), `widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.feature` (default), `widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U300: `widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton/.project)
- Eclipse project: `org.eclipse.nebula.widgets.splitbutton`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.splitbutton:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.splitbutton`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.splitbutton`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.splitbutton`](../packages/splitbutton.md#orgeclipsenebulawidgetssplitbutton)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U301: `widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.splitbutton.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.splitbutton.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.splitbutton.feature`; packages plug-ins `org.eclipse.nebula.widgets.splitbutton`, `org.eclipse.nebula.widgets.splitbutton.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U302: `widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/splitbutton/org.eclipse.nebula.widgets.splitbutton.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.splitbutton.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.splitbutton.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.splitbutton.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.splitbutton;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.splitbutton`](../packages/splitbutton.md#orgeclipsenebulawidgetssplitbutton)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

