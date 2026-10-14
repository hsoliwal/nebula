# Project and module records: compositetable

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U058: `widgets/compositetable`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/pom.xml)
- Maven artifact: `org.eclipse.nebula:compositetable:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/compositetable/org.eclipse.nebula.widgets.compositetable` (default), `widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests` (default), `widgets/compositetable/org.eclipse.nebula.widgets.compositetable.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U059: `widgets/compositetable/org.eclipse.nebula.widgets.compositetable`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable/.project)
- Eclipse project: `org.eclipse.nebula.widgets.compositetable`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.compositetable:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.compositetable`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.compositetable`, `org.eclipse.nebula.widgets.compositetable.day`, `org.eclipse.nebula.widgets.compositetable.month`, `org.eclipse.nebula.widgets.compositetable.timeeditor`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`, `org.eclipse.jface`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.compositetable`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetable), [`org.eclipse.nebula.widgets.compositetable.day`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetableday), [`org.eclipse.nebula.widgets.compositetable.day.internal`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetabledayinternal), [`org.eclipse.nebula.widgets.compositetable.internal`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetableinternal), [`org.eclipse.nebula.widgets.compositetable.month`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetablemonth), [`org.eclipse.nebula.widgets.compositetable.month.internal`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetablemonthinternal), [`org.eclipse.nebula.widgets.compositetable.timeeditor`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetabletimeeditor), [`org.eclipse.nebula.widgets.compositetable.viewers`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetableviewers)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U060: `widgets/compositetable/org.eclipse.nebula.widgets.compositetable.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.compositetable.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.compositetable.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.compositetable.feature`; packages plug-ins `org.eclipse.nebula.widgets.compositetable`, `org.eclipse.nebula.widgets.compositetable.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U061: `widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.compositetable.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.compositetable.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.compositetable.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.compositetable;bundle-version="1.0.0"`
- Exported package clauses: `org.eclipse.nebula.widgets.compositetable`, `org.eclipse.nebula.widgets.compositetable.day`
- Imported package clauses: `junit.framework;version="4.12.0"`, `junit.runner;version="4.12.0"`, `org.junit;version="4.12.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.compositetable`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetable), [`org.eclipse.nebula.widgets.compositetable.day`](../packages/compositetable.md#orgeclipsenebulawidgetscompositetableday)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

