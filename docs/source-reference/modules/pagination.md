# Project and module records: pagination

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U238: `widgets/pagination`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/pom.xml)
- Maven artifact: `org.eclipse.nebula:pagination:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/pagination/org.eclipse.nebula.widgets.pagination` (default), `widgets/pagination/org.eclipse.nebula.widgets.pagination.example` (default), `widgets/pagination/org.eclipse.nebula.widgets.pagination.feature` (default), `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets` (default), `widgets/pagination/org.eclipse.nebula.widgets.pagination.tests` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U239: `widgets/pagination/org.eclipse.nebula.widgets.pagination`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pagination`; natures: `org.eclipse.jdt.core.javanature`, `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pagination:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pagination`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.pagination`, `org.eclipse.nebula.widgets.pagination.collections`, `org.eclipse.nebula.widgets.pagination.renderers`, `org.eclipse.nebula.widgets.pagination.renderers.navigation`, `org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`, `org.eclipse.nebula.widgets.pagination.renderers.pagesize`, `org.eclipse.nebula.widgets.pagination.table`, `org.eclipse.nebula.widgets.pagination.table.forms`, `org.eclipse.nebula.widgets.pagination.tree`, `org.eclipse.nebula.widgets.pagination.tree.forms`
- Required bundles: `org.eclipse.core.runtime`
- Imported package clauses: `org.eclipse.jface.resource`, `org.eclipse.jface.viewers`, `org.eclipse.swt`, `org.eclipse.swt.events`, `org.eclipse.swt.graphics`, `org.eclipse.swt.layout`, `org.eclipse.swt.widgets`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pagination`](../packages/pagination.md#orgeclipsenebulawidgetspagination), [`org.eclipse.nebula.widgets.pagination.collections`](../packages/pagination.md#orgeclipsenebulawidgetspaginationcollections), [`org.eclipse.nebula.widgets.pagination.renderers`](../packages/pagination.md#orgeclipsenebulawidgetspaginationrenderers), [`org.eclipse.nebula.widgets.pagination.renderers.navigation`](../packages/pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigation), [`org.eclipse.nebula.widgets.pagination.renderers.navigation.graphics`](../packages/pagination.md#orgeclipsenebulawidgetspaginationrenderersnavigationgraphics), [`org.eclipse.nebula.widgets.pagination.renderers.pagesize`](../packages/pagination.md#orgeclipsenebulawidgetspaginationrendererspagesize), [`org.eclipse.nebula.widgets.pagination.table`](../packages/pagination.md#orgeclipsenebulawidgetspaginationtable), [`org.eclipse.nebula.widgets.pagination.table.forms`](../packages/pagination.md#orgeclipsenebulawidgetspaginationtableforms), [`org.eclipse.nebula.widgets.pagination.tree`](../packages/pagination.md#orgeclipsenebulawidgetspaginationtree), [`org.eclipse.nebula.widgets.pagination.tree.forms`](../packages/pagination.md#orgeclipsenebulawidgetspaginationtreeforms)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U240: `widgets/pagination/org.eclipse.nebula.widgets.pagination.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pagination.example`; natures: `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pagination.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pagination.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.pagination;bundle-version="1.0.0"`
- Imported package clauses: `org.eclipse.jface.viewers`, `org.eclipse.swt`, `org.eclipse.swt.events`, `org.eclipse.swt.layout`, `org.eclipse.swt.widgets`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pagination.example`](../packages/pagination.md#orgeclipsenebulawidgetspaginationexample), [`org.eclipse.nebula.widgets.pagination.example.model`](../packages/pagination.md#orgeclipsenebulawidgetspaginationexamplemodel), [`org.eclipse.nebula.widgets.pagination.example.services`](../packages/pagination.md#orgeclipsenebulawidgetspaginationexampleservices)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U241: `widgets/pagination/org.eclipse.nebula.widgets.pagination.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pagination.feature`; natures: `org.eclipse.jdt.core.javanature`, `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pagination.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.pagination.feature`; packages plug-ins `org.eclipse.nebula.widgets.pagination`, `org.eclipse.nebula.widgets.pagination.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U242: `widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pagination.snippets`; natures: `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pagination.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pagination.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.pagination;bundle-version="1.0.0"`
- Imported package clauses: `org.eclipse.jface.viewers`, `org.eclipse.swt`, `org.eclipse.swt.custom`, `org.eclipse.swt.graphics`, `org.eclipse.swt.layout`, `org.eclipse.swt.widgets`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pagination.snippets`](../packages/pagination.md#orgeclipsenebulawidgetspaginationsnippets), [`org.eclipse.nebula.widgets.pagination.snippets.model`](../packages/pagination.md#orgeclipsenebulawidgetspaginationsnippetsmodel), [`org.eclipse.nebula.widgets.pagination.snippets.table`](../packages/pagination.md#orgeclipsenebulawidgetspaginationsnippetstable), [`org.eclipse.nebula.widgets.pagination.snippets.table.lazy`](../packages/pagination.md#orgeclipsenebulawidgetspaginationsnippetstablelazy), [`org.eclipse.nebula.widgets.pagination.snippets.table.renderers`](../packages/pagination.md#orgeclipsenebulawidgetspaginationsnippetstablerenderers), [`org.eclipse.nebula.widgets.pagination.snippets.tree`](../packages/pagination.md#orgeclipsenebulawidgetspaginationsnippetstree)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U243: `widgets/pagination/org.eclipse.nebula.widgets.pagination.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pagination/org.eclipse.nebula.widgets.pagination.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pagination.tests`; natures: `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pagination.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pagination.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.pagination`
- Required bundles: `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pagination.tests`](../packages/pagination.md#orgeclipsenebulawidgetspaginationtests)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

