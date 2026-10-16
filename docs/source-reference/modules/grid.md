# Project and module records: grid

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U111: `widgets/grid`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/pom.xml)
- Maven artifact: `org.eclipse.nebula:grid:1.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/grid/org.eclipse.nebula.widgets.grid` (default), `widgets/grid/org.eclipse.nebula.widgets.grid.example` (default), `widgets/grid/org.eclipse.nebula.widgets.grid.feature` (default), `widgets/grid/org.eclipse.nebula.widgets.grid.css` (default), `widgets/grid/org.eclipse.nebula.widgets.grid.css.feature` (default), `widgets/grid/org.eclipse.nebula.widgets.grid.example.e4` (default), `widgets/grid/org.eclipse.nebula.widgets.grid.test` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U112: `widgets/grid/org.eclipse.nebula.widgets.grid`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.nebula.widgets.grid`; execution environment: `JavaSE-17`
- Exported package clauses: `org.eclipse.nebula.jface.gridviewer`, `org.eclipse.nebula.jface.gridviewer.internal;x-internal:=true`, `org.eclipse.nebula.widgets.grid`, `org.eclipse.nebula.widgets.grid.internal;x-internal:=true`, `org.eclipse.nebula.widgets.grid.model`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.jface;resolution:=optional`
- Owned declaration namespaces: [`org.eclipse.nebula.jface.gridviewer`](../packages/grid.md#orgeclipsenebulajfacegridviewer), [`org.eclipse.nebula.jface.gridviewer.internal`](../packages/grid.md#orgeclipsenebulajfacegridviewerinternal), [`org.eclipse.nebula.widgets.grid`](../packages/grid.md#orgeclipsenebulawidgetsgrid), [`org.eclipse.nebula.widgets.grid.internal`](../packages/grid.md#orgeclipsenebulawidgetsgridinternal), [`org.eclipse.nebula.widgets.grid.internal.win7`](../packages/grid.md#orgeclipsenebulawidgetsgridinternalwin7), [`org.eclipse.nebula.widgets.grid.model`](../packages/grid.md#orgeclipsenebulawidgetsgridmodel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U113: `widgets/grid/org.eclipse.nebula.widgets.grid.css`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid.css`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.grid.css:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.grid.css`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.grid.css`
- Required bundles: `org.eclipse.swt`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt;bundle-version="0.10.0"`, `org.eclipse.nebula.widgets.grid`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.grid.css`](../packages/grid.md#orgeclipsenebulawidgetsgridcss)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U114: `widgets/grid/org.eclipse.nebula.widgets.grid.css.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.css.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid.css.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.grid.css.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.grid.css.feature`; packages plug-ins `org.eclipse.nebula.widgets.grid.css`, `org.eclipse.nebula.widgets.grid.css.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U115: `widgets/grid/org.eclipse.nebula.widgets.grid.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.grid.example:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.grid.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.grid;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.grid.example`](../packages/grid.md#orgeclipsenebulawidgetsgridexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U116: `widgets/grid/org.eclipse.nebula.widgets.grid.example.e4`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid.example.e4`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.grid.example.e4:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.grid.example.e4`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="0.0.0"`, `org.eclipse.e4.ui.model.workbench;bundle-version="0.0.0"`, `org.eclipse.jface;bundle-version="0.0.0"`, `org.eclipse.e4.ui.services;bundle-version="0.0.0"`, `org.eclipse.e4.ui.workbench;bundle-version="0.0.0"`, `org.eclipse.e4.core.di;bundle-version="0.0.0"`, `org.eclipse.e4.ui.di;bundle-version="0.0.0"`, `org.eclipse.e4.core.contexts;bundle-version="0.0.0"`, `org.eclipse.core.runtime;bundle-version="0.0.0"`, `org.eclipse.nebula.widgets.grid`, `org.eclipse.nebula.widgets.grid.css`
- Imported package clauses: `javax.annotation;version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.grid.example.e4`](../packages/grid.md#orgeclipsenebulawidgetsgridexamplee4), [`org.eclipse.nebula.widgets.grid.example.e4.handlers`](../packages/grid.md#orgeclipsenebulawidgetsgridexamplee4handlers), [`org.eclipse.nebula.widgets.grid.example.e4.parts`](../packages/grid.md#orgeclipsenebulawidgetsgridexamplee4parts)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U117: `widgets/grid/org.eclipse.nebula.widgets.grid.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.grid.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.grid.feature`; packages plug-ins `org.eclipse.nebula.widgets.grid`, `org.eclipse.nebula.widgets.grid.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U118: `widgets/grid/org.eclipse.nebula.widgets.grid.test`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/grid/org.eclipse.nebula.widgets.grid.test/.project)
- Eclipse project: `org.eclipse.nebula.widgets.grid.test`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.grid.test:0.4.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.grid.test`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.grid;bundle-version="1.0.0.qualifier"`
- Exported package clauses: `org.eclipse.nebula.widgets.grid`
- Required bundles: `org.junit;bundle-version="4.8.2"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.grid`](../packages/grid.md#orgeclipsenebulawidgetsgrid), [`org.eclipse.nebula.widgets.grid.bench`](../packages/grid.md#orgeclipsenebulawidgetsgridbench), [`org.eclipse.nebula.widgets.grid.model`](../packages/grid.md#orgeclipsenebulawidgetsgridmodel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

