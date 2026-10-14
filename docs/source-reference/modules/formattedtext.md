# Project and module records: formattedtext

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U091: `widgets/formattedtext`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/pom.xml)
- Maven artifact: `org.eclipse.nebula:formattedtext:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext` (default), `widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.tests` (default), `widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U092: `widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext/.project)
- Eclipse project: `org.eclipse.nebula.widgets.formattedtext`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.formattedtext:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.formattedtext`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.formattedtext`
- Required bundles: `org.eclipse.jface`, `org.eclipse.core.databinding`, `org.eclipse.jface.databinding`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.formattedtext`](../packages/formattedtext.md#orgeclipsenebulawidgetsformattedtext)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U093: `widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.formattedtext.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.formattedtext.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.formattedtext.feature`; packages plug-ins `org.eclipse.nebula.widgets.formattedtext`, `org.eclipse.nebula.widgets.formattedtext.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U094: `widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/formattedtext/org.eclipse.nebula.widgets.formattedtext.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.formattedtext.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.formattedtext.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.formattedtext.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.formattedtext;bundle-version="1.0.0"`
- Exported package clauses: `org.eclipse.nebula.widgets.formattedtext`
- Required bundles: `org.eclipse.swt`, `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.formattedtext`](../packages/formattedtext.md#orgeclipsenebulawidgetsformattedtext)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

