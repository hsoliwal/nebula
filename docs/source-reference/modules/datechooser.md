# Project and module records: datechooser

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U075: `widgets/datechooser`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/pom.xml)
- Maven artifact: `org.eclipse.nebula:datechooser:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/datechooser/org.eclipse.nebula.widgets.datechooser` (default), `widgets/datechooser/org.eclipse.nebula.widgets.datechooser.example` (default), `widgets/datechooser/org.eclipse.nebula.widgets.datechooser.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U076: `widgets/datechooser/org.eclipse.nebula.widgets.datechooser`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser/.project)
- Eclipse project: `org.eclipse.nebula.widgets.datechooser`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.datechooser:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.datechooser`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.datechooser`
- Required bundles: `org.eclipse.jface;resolution:=optional`, `org.eclipse.core.databinding;resolution:=optional`, `org.eclipse.jface.databinding;resolution:=optional`, `org.eclipse.nebula.widgets.formattedtext;bundle-version="1.0.0"`
- Imported package clauses: `org.eclipse.jface.layout`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.swt.events`, `org.eclipse.swt.graphics`, `org.eclipse.swt.internal`, `org.eclipse.swt.layout`, `org.eclipse.swt.widgets`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.datechooser`](../packages/datechooser.md#orgeclipsenebulawidgetsdatechooser)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U077: `widgets/datechooser/org.eclipse.nebula.widgets.datechooser.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.datechooser.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.datechooser.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.datechooser.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.datechooser;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.datechooser.example`](../packages/datechooser.md#orgeclipsenebulawidgetsdatechooserexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U078: `widgets/datechooser/org.eclipse.nebula.widgets.datechooser.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/datechooser/org.eclipse.nebula.widgets.datechooser.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.datechooser.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.datechooser.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.datechooser.feature`; packages plug-ins `org.eclipse.nebula.widgets.datechooser`, `org.eclipse.nebula.widgets.datechooser.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

