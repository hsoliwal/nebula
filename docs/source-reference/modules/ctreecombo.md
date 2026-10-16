# Project and module records: ctreecombo

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U065: `widgets/ctreecombo`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/pom.xml)
- Maven artifact: `org.eclipse.nebula:ctreecombo:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo` (default), `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.tests` (default), `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.feature` (default), `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U066: `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ctreecombo`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ctreecombo:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ctreecombo`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.ctreecombo`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.jface`, `org.eclipse.core.runtime;bundle-version="3.13.0"`, `org.eclipse.core.databinding.observable`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ctreecombo`](../packages/ctreecombo.md#orgeclipsenebulawidgetsctreecombo), [`org.eclipse.nebula.widgets.ctreecombo.viewer`](../packages/ctreecombo.md#orgeclipsenebulawidgetsctreecomboviewer)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U067: `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ctreecombo.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ctreecombo.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.ctreecombo.feature`; packages plug-ins `org.eclipse.nebula.widgets.ctreecombo`, `org.eclipse.nebula.widgets.ctreecombo.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U068: `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ctreecombo.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ctreecombo.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ctreecombo.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.ctreecombo;bundle-version="1.0.0"`, `org.eclipse.swt`, `org.eclipse.jface`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ctreecombo.snippets`](../packages/ctreecombo.md#orgeclipsenebulawidgetsctreecombosnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U069: `widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.tests/pom.xml)
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ctreecombo.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ctreecombo.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.ctreecombo`
- Required bundles: `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ctreecombo`](../packages/ctreecombo.md#orgeclipsenebulawidgetsctreecombo)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

