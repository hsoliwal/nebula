# Project and module records: cwt

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U070: `widgets/cwt`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/pom.xml)
- Maven artifact: `org.eclipse.nebula:cwt:1.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/cwt/org.eclipse.nebula.cwt` (default), `widgets/cwt/org.eclipse.nebula.cwt.snippets` (default), `widgets/cwt/org.eclipse.nebula.cwt.tests` (default), `widgets/cwt/org.eclipse.nebula.cwt.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U071: `widgets/cwt/org.eclipse.nebula.cwt`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt/.project)
- Eclipse project: `org.eclipse.nebula.cwt`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.cwt:1.2.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.cwt`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.cwt.animation`, `org.eclipse.nebula.cwt.animation.effects`, `org.eclipse.nebula.cwt.animation.movement`, `org.eclipse.nebula.cwt.base`, `org.eclipse.nebula.cwt.svg`, `org.eclipse.nebula.cwt.v`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.cwt.animation`](../packages/cwt.md#orgeclipsenebulacwtanimation), [`org.eclipse.nebula.cwt.animation.effects`](../packages/cwt.md#orgeclipsenebulacwtanimationeffects), [`org.eclipse.nebula.cwt.animation.movement`](../packages/cwt.md#orgeclipsenebulacwtanimationmovement), [`org.eclipse.nebula.cwt.base`](../packages/cwt.md#orgeclipsenebulacwtbase), [`org.eclipse.nebula.cwt.svg`](../packages/cwt.md#orgeclipsenebulacwtsvg), [`org.eclipse.nebula.cwt.v`](../packages/cwt.md#orgeclipsenebulacwtv)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U072: `widgets/cwt/org.eclipse.nebula.cwt.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.feature/.project)
- Eclipse project: `org.eclipse.nebula.cwt.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.cwt.feature:1.2.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.cwt.feature`; packages plug-ins `org.eclipse.nebula.cwt`, `org.eclipse.nebula.cwt.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U073: `widgets/cwt/org.eclipse.nebula.cwt.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.snippets/.project)
- Eclipse project: `org.eclipse.nebula.cwt.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.cwt.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.cwt.snippets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.cwt.snippets`
- Required bundles: `org.eclipse.swt;bundle-version="3.4.0"`, `org.eclipse.nebula.cwt;bundle-version="0.9.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.cwt.snippets`](../packages/cwt.md#orgeclipsenebulacwtsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U074: `widgets/cwt/org.eclipse.nebula.cwt.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cwt/org.eclipse.nebula.cwt.tests/.project)
- Eclipse project: `org.eclipse.nebula.cwt.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.cwt.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.cwt.tests`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.cwt.test`
- Required bundles: `org.eclipse.swt`, `org.eclipse.nebula.cwt;bundle-version="0.9.0";visibility:=reexport`, `org.junit;bundle-version="3.8.2";visibility:=reexport`
- Owned declaration namespaces: [`org.eclipse.nebula.cwt.base`](../packages/cwt.md#orgeclipsenebulacwtbase), [`org.eclipse.nebula.cwt.svg`](../packages/cwt.md#orgeclipsenebulacwtsvg), [`org.eclipse.nebula.cwt.test`](../packages/cwt.md#orgeclipsenebulacwttest), [`org.eclipse.nebula.cwt.v`](../packages/cwt.md#orgeclipsenebulacwtv)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

