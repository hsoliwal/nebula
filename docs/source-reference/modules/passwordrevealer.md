# Project and module records: passwordrevealer

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U251: `widgets/passwordrevealer`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/pom.xml)
- Maven artifact: `org.eclipse.nebula:passwordrevealer:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer` (default), `widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.feature` (default), `widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U252: `widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer/.project)
- Eclipse project: `org.eclipse.nebula.widgets.passwordrevealer`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.passwordrevealer:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.passwordrevealer`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.passwordrevealer`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.passwordrevealer`](../packages/passwordrevealer.md#orgeclipsenebulawidgetspasswordrevealer)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U253: `widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.passwordrevealer.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.passwordrevealer.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.passwordrevealer.feature`; packages plug-ins `org.eclipse.nebula.widgets.passwordrevealer`, `org.eclipse.nebula.widgets.passwordrevealer.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U254: `widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/passwordrevealer/org.eclipse.nebula.widgets.passwordrevealer.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.passwordrevealer.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.passwordrevealer.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.passwordrevealer.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.passwordrevealer;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.passwordrevealer.snippets`](../packages/passwordrevealer.md#orgeclipsenebulawidgetspasswordrevealersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

