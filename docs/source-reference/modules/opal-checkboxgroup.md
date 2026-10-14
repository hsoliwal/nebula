# Project and module records: opal-checkboxgroup

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U138: `widgets/opal/checkboxgroup`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/pom.xml)
- Maven artifact: `org.eclipse.nebula:checkboxgroup:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup` (default), `widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.feature` (default), `widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U139: `widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.checkboxgroup`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.checkboxgroup:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.checkboxgroup`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.checkboxgroup`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.checkboxgroup`](../packages/opal-checkboxgroup.md#orgeclipsenebulawidgetsopalcheckboxgroup)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U140: `widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.checkboxgroup.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.checkboxgroup.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.checkboxgroup.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.checkboxgroup`, `org.eclipse.nebula.widgets.opal.checkboxgroup.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U141: `widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/checkboxgroup/org.eclipse.nebula.widgets.opal.checkboxgroup.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.checkboxgroup.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.checkboxgroup.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.checkboxgroup.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.checkboxgroup;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.checkboxgroup.snippets`](../packages/opal-checkboxgroup.md#orgeclipsenebulawidgetsopalcheckboxgroupsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

