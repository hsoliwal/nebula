# Project and module records: opal-promptsupport

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U193: `widgets/opal/promptsupport`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/pom.xml)
- Maven artifact: `org.eclipse.nebula:promptsupport:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport` (default), `widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.feature` (default), `widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U194: `widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.promptsupport`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.promptsupport:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.promptsupport`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.promptsupport`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.promptsupport`](../packages/opal-promptsupport.md#orgeclipsenebulawidgetsopalpromptsupport)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U195: `widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.promptsupport.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.promptsupport.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.promptsupport.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.promptsupport`, `org.eclipse.nebula.widgets.opal.promptsupport.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U196: `widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/promptsupport/org.eclipse.nebula.widgets.opal.promptsupport.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.promptsupport.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.promptsupport.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.promptsupport.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.promptsupport;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.promptsupport.snippets`](../packages/opal-promptsupport.md#orgeclipsenebulawidgetsopalpromptsupportsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

