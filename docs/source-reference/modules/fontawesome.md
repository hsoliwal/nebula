# Project and module records: fontawesome

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U087: `widgets/fontawesome`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/pom.xml)
- Maven artifact: `org.eclipse.nebula:fontawesome:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome` (default), `widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.feature` (default), `widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U088: `widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome/.project)
- Eclipse project: `org.eclipse.nebula.widgets.fontawesome`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.fontawesome:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.fontawesome`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.fontawesome`
- Required bundles: `org.eclipse.swt;visibility:=reexport`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.fontawesome`](../packages/fontawesome.md#orgeclipsenebulawidgetsfontawesome)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U089: `widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.fontawesome.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.fontawesome.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.fontawesome.feature`; packages plug-ins `org.eclipse.nebula.widgets.fontawesome`, `org.eclipse.nebula.widgets.fontawesome.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U090: `widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/fontawesome/org.eclipse.nebula.widgets.fontawesome.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.fontawesome.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.fontawesome.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.fontawesome.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.fontawesome;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.fontawesome`](../packages/fontawesome.md#orgeclipsenebulawidgetsfontawesome)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

