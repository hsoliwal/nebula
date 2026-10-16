# Project and module records: opal-roundedtoolbar

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U205: `widgets/opal/roundedtoolbar`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/pom.xml)
- Maven artifact: `org.eclipse.nebula:roundedtoolbar:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar` (default), `widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.feature` (default), `widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U206: `widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.roundedtoolbar`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.roundedtoolbar:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.roundedtoolbar`; execution environment: `JavaSE-17`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.roundedtoolbar`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt`
- Imported package clauses: `org.eclipse.jface.action`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.roundedtoolbar`](../packages/opal-roundedtoolbar.md#orgeclipsenebulawidgetsopalroundedtoolbar)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U207: `widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.roundedtoolbar.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.roundedtoolbar.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.roundedtoolbar.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.roundedtoolbar`, `org.eclipse.nebula.widgets.opal.roundedtoolbar.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U208: `widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets`; execution environment: `JavaSE-17`
- Required bundles: `org.eclipse.nebula.widgets.opal.roundedtoolbar;bundle-version="1.0.0"`, `org.eclipse.swt`, `org.eclipse.jface`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.roundedtoolbar.snippets`](../packages/opal-roundedtoolbar.md#orgeclipsenebulawidgetsopalroundedtoolbarsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

