# Project and module records: opal-commons

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U146: `widgets/opal/commons`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/pom.xml)
- Maven artifact: `org.eclipse.nebula:commons:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons` (default), `widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U147: `widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.commons`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.commons:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.commons`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.commons`, `org.eclipse.nebula.widgets.opal.commons.resources`
- Required bundles: `org.eclipse.equinox.common`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.commons`](../packages/opal-commons.md#orgeclipsenebulawidgetsopalcommons)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U148: `widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/commons/org.eclipse.nebula.widgets.opal.commons.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.commons.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.commons.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.commons.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.commons`, `org.eclipse.nebula.widgets.opal.commons.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

