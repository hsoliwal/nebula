# Project and module records: effects

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U079: `widgets/effects`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/pom.xml)
- Maven artifact: `org.eclipse.nebula:effects:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/effects/org.eclipse.nebula.effects.stw` (default), `widgets/effects/org.eclipse.nebula.effects.stw.example` (default), `widgets/effects/org.eclipse.nebula.effects.stw.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U080: `widgets/effects/org.eclipse.nebula.effects.stw`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw/.project)
- Eclipse project: `org.eclipse.nebula.effects.stw`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.effects.stw:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.effects.stw`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.effects.stw; uses:="org.eclipse.swt.events,  org.eclipse.swt.custom,  org.eclipse.swt.graphics,  org.eclipse.swt.widgets"`, `org.eclipse.nebula.effects.stw.transitions;uses:="org.eclipse.swt.graphics,org.eclipse.nebula.effects.stw"`
- Required bundles: `org.eclipse.swt;bundle-version="3.5.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.effects.stw`](../packages/effects.md#orgeclipsenebulaeffectsstw), [`org.eclipse.nebula.effects.stw.transitions`](../packages/effects.md#orgeclipsenebulaeffectsstwtransitions)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U081: `widgets/effects/org.eclipse.nebula.effects.stw.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.example/.project)
- Eclipse project: `org.eclipse.nebula.effects.stw.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.effects.stw.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.effects.stw.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.effects.stw;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="3.6.0"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`
- Owned declaration namespaces: [`org.eclipse.nebula.effects.stw.example`](../packages/effects.md#orgeclipsenebulaeffectsstwexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U082: `widgets/effects/org.eclipse.nebula.effects.stw.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/effects/org.eclipse.nebula.effects.stw.feature/.project)
- Eclipse project: `org.eclipse.nebula.effects.stw.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.effects.stw.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.effects.stw.feature`; packages plug-ins `org.eclipse.nebula.effects.stw`, `org.eclipse.nebula.effects.stw.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

