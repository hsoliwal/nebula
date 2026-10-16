# Project and module records: badgedlabel

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U025: `widgets/badgedlabel`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/pom.xml)
- Maven artifact: `org.eclipse.nebula:badgedlabel:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel` (default), `widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.feature` (default), `widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U026: `widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel/.project)
- Eclipse project: `org.eclipse.nebula.widgets.badgedlabel`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.badgedlabel:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.badgedlabel`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.badgedlabel`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.badgedlabel`](../packages/badgedlabel.md#orgeclipsenebulawidgetsbadgedlabel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U027: `widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.badgedlabel.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.badgedlabel.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.badgedlabel.feature`; packages plug-ins `org.eclipse.nebula.widgets.badgedlabel`, `org.eclipse.nebula.widgets.badgedlabel.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U028: `widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.badgedlabel.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.badgedlabel.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.badgedlabel.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.badgedlabel;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.badgedlabel`](../packages/badgedlabel.md#orgeclipsenebulawidgetsbadgedlabel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

