# Project and module records: opal-titledseparator

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U225: `widgets/opal/titledseparator`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/pom.xml)
- Maven artifact: `org.eclipse.nebula:titledseparator:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator` (default), `widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.feature` (default), `widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U226: `widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.titledseparator`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.titledseparator:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.titledseparator`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.titledseparator`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.titledseparator`](../packages/opal-titledseparator.md#orgeclipsenebulawidgetsopaltitledseparator)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U227: `widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.titledseparator.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.titledseparator.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.titledseparator.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.titledseparator`, `org.eclipse.nebula.widgets.opal.titledseparator.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U228: `widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/titledseparator/org.eclipse.nebula.widgets.opal.titledseparator.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.titledseparator.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.titledseparator.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.titledseparator.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.titledseparator;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.titledseparator.snippets`](../packages/opal-titledseparator.md#orgeclipsenebulawidgetsopaltitledseparatorsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

