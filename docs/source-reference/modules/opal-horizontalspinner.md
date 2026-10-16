# Project and module records: opal-horizontalspinner

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U165: `widgets/opal/horizontalspinner`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/pom.xml)
- Maven artifact: `org.eclipse.nebula:horizontalspinner:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner` (default), `widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.feature` (default), `widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U166: `widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.horizontalspinner`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.horizontalspinner:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.horizontalspinner`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.horizontalspinner`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.horizontalspinner`](../packages/opal-horizontalspinner.md#orgeclipsenebulawidgetsopalhorizontalspinner)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U167: `widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.horizontalspinner.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.horizontalspinner.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.horizontalspinner.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.horizontalspinner`, `org.eclipse.nebula.widgets.opal.horizontalspinner.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U168: `widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/horizontalspinner/org.eclipse.nebula.widgets.opal.horizontalspinner.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.horizontalspinner.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.horizontalspinner.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.horizontalspinner.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.horizontalspinner;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.horizontalspinner.snippets`](../packages/opal-horizontalspinner.md#orgeclipsenebulawidgetsopalhorizontalspinnersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

