# Project and module records: radiogroup

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U274: `widgets/radiogroup`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/pom.xml)
- Maven artifact: `org.eclipse.nebula:radiogroup:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup` (default), `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.example` (default), `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.tests` (default), `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U275: `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup/.project)
- Eclipse project: `org.eclipse.nebula.widgets.radiogroup`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.radiogroup:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.radiogroup`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.jface.viewer.radiogroup`, `org.eclipse.nebula.widgets.radiogroup`, `org.eclipse.nebula.widgets.radiogroup.forms`
- Required bundles: `org.eclipse.equinox.common`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.jface`, `org.eclipse.core.databinding;bundle-version="1.2.0"`, `org.eclipse.core.databinding.property;bundle-version="1.2.0"`, `org.eclipse.jface.databinding;bundle-version="1.3.0"`, `org.eclipse.ui.forms`
- Owned declaration namespaces: [`org.eclipse.nebula.jface.viewer.radiogroup`](../packages/radiogroup.md#orgeclipsenebulajfaceviewerradiogroup), [`org.eclipse.nebula.widgets.radiogroup`](../packages/radiogroup.md#orgeclipsenebulawidgetsradiogroup), [`org.eclipse.nebula.widgets.radiogroup.forms`](../packages/radiogroup.md#orgeclipsenebulawidgetsradiogroupforms)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U276: `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.radiogroup.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.radiogroup.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.radiogroup.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.radiogroup;bundle-version="1.0.0"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.radiogroup.example`](../packages/radiogroup.md#orgeclipsenebulawidgetsradiogroupexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U277: `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.radiogroup.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.radiogroup.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.radiogroup.feature`; packages plug-ins `org.eclipse.nebula.widgets.radiogroup`, `org.eclipse.nebula.widgets.radiogroup.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U278: `widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/radiogroup/org.eclipse.nebula.widgets.radiogroup.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.radiogroup.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.radiogroup.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.radiogroup.tests`; execution environment: `JavaSE-11`
- Required bundles: `org.junit`, `org.eclipse.swt`, `org.eclipse.nebula.widgets.radiogroup`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.radiogroup`](../packages/radiogroup.md#orgeclipsenebulawidgetsradiogroup)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

