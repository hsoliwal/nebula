# Project and module records: ctree

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U062: `widgets/ctree`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctree/pom.xml)
- Maven artifact: `org.eclipse.nebula:ctree:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/ctree/org.eclipse.nebula.widgets.ctree` (default), `widgets/ctree/org.eclipse.nebula.widgets.ctree.tests` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U063: `widgets/ctree/org.eclipse.nebula.widgets.ctree`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctree/org.eclipse.nebula.widgets.ctree/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctree/org.eclipse.nebula.widgets.ctree/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctree/org.eclipse.nebula.widgets.ctree/.project)
- Eclipse project: `org.eclipse.nebula.widgets.ctree`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ctree:0.9.0`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ctree`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.ctree`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ctree`](../packages/ctree.md#orgeclipsenebulawidgetsctree)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U064: `widgets/ctree/org.eclipse.nebula.widgets.ctree.tests`

- Classification: test; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctree/org.eclipse.nebula.widgets.ctree.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/ctree/org.eclipse.nebula.widgets.ctree.tests/pom.xml)
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.ctree.tests:0.9.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.ctree.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.ctree`
- Required bundles: `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.ctree`](../packages/ctree.md#orgeclipsenebulawidgetsctree)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

