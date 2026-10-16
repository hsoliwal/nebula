# Project and module records: opal-breadcrumb

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U130: `widgets/opal/breadcrumb`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/pom.xml)
- Maven artifact: `org.eclipse.nebula:breadcrumb:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb` (default), `widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.feature` (default), `widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U131: `widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.breadcrumb`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.breadcrumb:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.breadcrumb`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.breadcrumb`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.breadcrumb`](../packages/opal-breadcrumb.md#orgeclipsenebulawidgetsopalbreadcrumb)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U132: `widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.breadcrumb.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.breadcrumb.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.breadcrumb.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.breadcrumb`, `org.eclipse.nebula.widgets.opal.breadcrumb.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U133: `widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/breadcrumb/org.eclipse.nebula.widgets.opal.breadcrumb.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.breadcrumb.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.breadcrumb.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.breadcrumb.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.breadcrumb;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.breadcrumb.snippets`](../packages/opal-breadcrumb.md#orgeclipsenebulawidgetsopalbreadcrumbsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

