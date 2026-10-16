# Project and module records: tablecombo

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U307: `widgets/tablecombo`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/pom.xml)
- Maven artifact: `org.eclipse.nebula:tablecombo:1.3.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.feature` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.snippets` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.test` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css` (default), `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U308: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo`; natures: `org.eclipse.jdt.core.javanature`, `org.eclipse.pde.PluginNature`, `org.eclipse.pde.api.tools.apiAnalysisNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo:1.3.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tablecombo`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.jface.checktablecomboviewer`, `org.eclipse.nebula.jface.tablecomboviewer`, `org.eclipse.nebula.widgets.checktablecombo`, `org.eclipse.nebula.widgets.tablecombo`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.jface;resolution:=optional`
- Owned declaration namespaces: [`org.eclipse.nebula.jface.checktablecomboviewer`](../packages/tablecombo.md#orgeclipsenebulajfacechecktablecomboviewer), [`org.eclipse.nebula.jface.tablecomboviewer`](../packages/tablecombo.md#orgeclipsenebulajfacetablecomboviewer), [`org.eclipse.nebula.widgets.checktablecombo`](../packages/tablecombo.md#orgeclipsenebulawidgetschecktablecombo), [`org.eclipse.nebula.widgets.tablecombo`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecombo)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U309: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.css`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.css:1.3.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tablecombo.css`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.tablecombo.css`
- Required bundles: `org.eclipse.swt`, `org.eclipse.nebula.widgets.tablecombo`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt;bundle-version="0.10.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tablecombo.css`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecombocss)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U310: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.css.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.css.feature:1.3.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.tablecombo.css.feature`; packages plug-ins `org.eclipse.nebula.widgets.tablecombo.css`, `org.eclipse.nebula.widgets.tablecombo.css.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U311: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.example:1.3.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tablecombo.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.tablecombo;bundle-version="1.0.0"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tablecombo.example`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecomboexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U312: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.example.e4`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.example.e4:1.3.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tablecombo.example.e4`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="0.0.0"`, `org.eclipse.e4.ui.model.workbench;bundle-version="0.0.0"`, `org.eclipse.jface;bundle-version="0.0.0"`, `org.eclipse.e4.ui.services;bundle-version="0.0.0"`, `org.eclipse.e4.ui.workbench;bundle-version="0.0.0"`, `org.eclipse.e4.core.di;bundle-version="0.0.0"`, `org.eclipse.e4.ui.di;bundle-version="0.0.0"`, `org.eclipse.e4.core.contexts;bundle-version="0.0.0"`, `org.eclipse.core.runtime;bundle-version="0.0.0"`, `org.eclipse.nebula.widgets.tablecombo`, `org.eclipse.nebula.widgets.tablecombo.css`
- Imported package clauses: `javax.annotation;version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tablecombo.example.e4`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecomboexamplee4), [`org.eclipse.nebula.widgets.tablecombo.example.e4.handlers`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecomboexamplee4handlers), [`org.eclipse.nebula.widgets.tablecombo.example.e4.parts`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecomboexamplee4parts)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U313: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.feature:1.3.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.tablecombo.feature`; packages plug-ins `org.eclipse.nebula.widgets.tablecombo`, `org.eclipse.nebula.widgets.tablecombo.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U314: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.snippets`; natures: `org.eclipse.jdt.core.javanature`, `org.eclipse.pde.PluginNature`, `org.eclipse.pde.api.tools.apiAnalysisNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.snippets:1.3.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tablecombo.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt`, `org.eclipse.jface;resolution:=optional`, `org.eclipse.nebula.widgets.tablecombo`, `org.eclipse.core.runtime`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tablecombo`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecombo)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U315: `widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.test`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.test/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.test/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.test/.project)
- Eclipse project: `org.eclipse.nebula.widgets.tablecombo.test`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.tablecombo.test:1.3.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.tablecombo.test`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.tablecombo;bundle-version="1.3.0"`
- Required bundles: `org.junit`, `org.eclipse.swt`, `org.eclipse.swtbot.go`, `org.eclipse.core.runtime;bundle-version="3.12.0"`
- Direct POM dependencies: `org.easymock:easymock` (inherited/profile/plugin dependencies are additional)
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.tablecombo`](../packages/tablecombo.md#orgeclipsenebulawidgetstablecombo)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

