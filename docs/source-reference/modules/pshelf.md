# Project and module records: pshelf

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U268: `widgets/pshelf`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/pom.xml)
- Maven artifact: `org.eclipse.nebula:pshelf:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/pshelf/org.eclipse.nebula.widgets.pshelf` (default), `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.example` (default), `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css` (default), `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.feature` (default), `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U269: `widgets/pshelf/org.eclipse.nebula.widgets.pshelf`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pshelf`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pshelf:1.2.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pshelf`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.jface.pshelfviewer`, `org.eclipse.nebula.widgets.pshelf`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.jface`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.opal.commons`
- Owned declaration namespaces: [`org.eclipse.nebula.jface.pshelfviewer`](../packages/pshelf.md#orgeclipsenebulajfacepshelfviewer), [`org.eclipse.nebula.widgets.pshelf`](../packages/pshelf.md#orgeclipsenebulawidgetspshelf)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U270: `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pshelf.css`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pshelf.css:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pshelf.css`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.pshelf.css`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`, `org.eclipse.nebula.widgets.pshelf;bundle-version="1.0.0"`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt;bundle-version="0.10.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pshelf.css`](../packages/pshelf.md#orgeclipsenebulawidgetspshelfcss), [`org.eclipse.nebula.widgets.pshelf.css.internal`](../packages/pshelf.md#orgeclipsenebulawidgetspshelfcssinternal)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U271: `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pshelf.css.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pshelf.css.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.pshelf.css.feature`; packages plug-ins `org.eclipse.nebula.widgets.pshelf.css`, `org.eclipse.nebula.widgets.pshelf.css.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U272: `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pshelf.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pshelf.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.pshelf.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.pshelf;bundle-version="1.0.0"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.pshelf.example`](../packages/pshelf.md#orgeclipsenebulawidgetspshelfexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U273: `widgets/pshelf/org.eclipse.nebula.widgets.pshelf.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/pshelf/org.eclipse.nebula.widgets.pshelf.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.pshelf.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.pshelf.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.pshelf.feature`; packages plug-ins `org.eclipse.nebula.widgets.pshelf`, `org.eclipse.nebula.widgets.pshelf.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

