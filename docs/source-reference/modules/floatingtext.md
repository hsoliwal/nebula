# Project and module records: floatingtext

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U083: `widgets/floatingtext`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/pom.xml)
- Maven artifact: `org.eclipse.nebula:floatingtext:1.0.1-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext` (default), `widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.snippets` (default), `widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U084: `widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext/.project)
- Eclipse project: `org.eclipse.nebula.widgets.floatingtext`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.floatingtext:1.0.1-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.floatingtext`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.floatingtext;version="1.0.1"`
- Required bundles: `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.floatingtext`](../packages/floatingtext.md#orgeclipsenebulawidgetsfloatingtext)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U085: `widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.floatingtext.feature`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.floatingtext.feature:1.0.1-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.floatingtext.feature`; packages plug-ins `org.eclipse.nebula.widgets.floatingtext`, `org.eclipse.nebula.widgets.floatingtext.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U086: `widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/floatingtext/org.eclipse.nebula.widgets.floatingtext.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.floatingtext.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.floatingtext.snippets:1.0.1-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.floatingtext.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt`
- Imported package clauses: `org.eclipse.nebula.widgets.floatingtext`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.floatingtext.snippets`](../packages/floatingtext.md#orgeclipsenebulawidgetsfloatingtextsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

