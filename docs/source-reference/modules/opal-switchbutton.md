# Project and module records: opal-switchbutton

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U213: `widgets/opal/switchbutton`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/pom.xml)
- Maven artifact: `org.eclipse.nebula:switchbutton:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton` (default), `widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.feature` (default), `widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U214: `widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.switchbutton`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.switchbutton:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.switchbutton`; execution environment: `JavaSE-17`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.switchbutton`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.switchbutton`](../packages/opal-switchbutton.md#orgeclipsenebulawidgetsopalswitchbutton)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U215: `widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.switchbutton.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.switchbutton.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.switchbutton.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.switchbutton`, `org.eclipse.nebula.widgets.opal.switchbutton.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U216: `widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/switchbutton/org.eclipse.nebula.widgets.opal.switchbutton.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.switchbutton.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.switchbutton.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.switchbutton.snippets`; execution environment: `JavaSE-21`
- Required bundles: `org.eclipse.nebula.widgets.opal.switchbutton;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.switchbutton.snippets`](../packages/opal-switchbutton.md#orgeclipsenebulawidgetsopalswitchbuttonsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

