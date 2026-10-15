# Project and module records: opal-tipoftheday

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U221: `widgets/opal/tipoftheday`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/pom.xml)
- Maven artifact: `org.eclipse.nebula:tipoftheday:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday` (default), `widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.feature` (default), `widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U222: `widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.tipoftheday`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.tipoftheday:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.tipoftheday`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.tipoftheday`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.nebula.widgets.opal.header;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.tipoftheday`](../packages/opal-tipoftheday.md#orgeclipsenebulawidgetsopaltipoftheday)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U223: `widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.tipoftheday.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.tipoftheday.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.tipoftheday.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.tipoftheday`, `org.eclipse.nebula.widgets.opal.tipoftheday.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U224: `widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/tipoftheday/org.eclipse.nebula.widgets.opal.tipoftheday.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.tipoftheday.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.tipoftheday.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.tipoftheday.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.tipoftheday;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.tipoftheday.snippets`](../packages/opal-tipoftheday.md#orgeclipsenebulawidgetsopaltipofthedaysnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

