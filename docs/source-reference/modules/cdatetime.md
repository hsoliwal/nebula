# Project and module records: cdatetime

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U040: `widgets/cdatetime`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/pom.xml)
- Maven artifact: `org.eclipse.nebula:cdatetime:1.5.1-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime` (default), `widgets/cdatetime/org.eclipse.nebula.jface.cdatetime` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.snippets` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.feature` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4` (default), `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U041: `widgets/cdatetime/org.eclipse.nebula.jface.cdatetime`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.jface.cdatetime/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.jface.cdatetime/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.jface.cdatetime/.project)
- Eclipse project: `org.eclipse.nebula.jface.cdatetime`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.jface.cdatetime:1.3.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.jface.cdatetime`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.jface.cdatetime`
- Required bundles: `org.eclipse.core.runtime;bundle-version="3.4.0"`, `org.eclipse.nebula.widgets.cdatetime;bundle-version="0.14.0"`, `org.eclipse.jface;bundle-version="3.4.0"`, `org.eclipse.core.databinding;bundle-version="1.1.0"`, `org.eclipse.jface.databinding`, `org.eclipse.core.databinding.property;bundle-version="1.6.100"`
- Owned declaration namespaces: [`org.eclipse.nebula.jface.cdatetime`](../packages/cdatetime.md#orgeclipsenebulajfacecdatetime)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U042: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`, `org.eclipse.pde.api.tools.apiAnalysisNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime:1.6.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.cdatetime`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.cdatetime`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.nebula.cwt;bundle-version="0.9.0";visibility:=reexport`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.cdatetime`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetime)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U043: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.css`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.css:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.cdatetime.css`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.cdatetime.css`
- Required bundles: `org.eclipse.swt`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt;bundle-version="0.10.0"`, `org.eclipse.nebula.widgets.cdatetime`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.cdatetime.css`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimecss)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U044: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.css.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.css.feature:1.4.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.cdatetime.css.feature`; packages plug-ins `org.eclipse.nebula.widgets.cdatetime.css`, `org.eclipse.nebula.widgets.cdatetime.css.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U045: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.cdatetime.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.widgets.cdatetime;bundle-version="0.14.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.cdatetime.example`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimeexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U046: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.example.e4`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.example.e4:1.4.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.cdatetime.example.e4`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="0.0.0"`, `org.eclipse.e4.ui.model.workbench;bundle-version="0.0.0"`, `org.eclipse.jface;bundle-version="0.0.0"`, `org.eclipse.e4.ui.services;bundle-version="0.0.0"`, `org.eclipse.e4.ui.workbench;bundle-version="0.0.0"`, `org.eclipse.e4.core.di;bundle-version="0.0.0"`, `org.eclipse.e4.ui.di;bundle-version="0.0.0"`, `org.eclipse.e4.core.contexts;bundle-version="0.0.0"`, `org.eclipse.core.runtime;bundle-version="0.0.0"`, `org.eclipse.nebula.widgets.cdatetime`, `org.eclipse.nebula.widgets.cdatetime.css`
- Imported package clauses: `jakarta.annotation;version="[3.0.0,4.0.0)"`, `org.w3c.dom`, `org.w3c.dom.css`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.cdatetime.example.e4`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimeexamplee4), [`org.eclipse.nebula.widgets.cdatetime.example.e4.handlers`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimeexamplee4handlers), [`org.eclipse.nebula.widgets.cdatetime.example.e4.parts`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimeexamplee4parts)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U047: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.feature:1.6.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.cdatetime.feature`; packages plug-ins `org.eclipse.nebula.widgets.cdatetime`, `org.eclipse.nebula.widgets.cdatetime.source`, `org.eclipse.nebula.jface.cdatetime.source`, `org.eclipse.nebula.jface.cdatetime`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U048: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.cdatetime.snippets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.cdatetime.snippets`
- Required bundles: `org.eclipse.jface`, `org.eclipse.ui.forms`, `org.eclipse.nebula.widgets.cdatetime`, `org.eclipse.nebula.jface.cdatetime`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.cdatetime.snippets`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U049: `widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.cdatetime.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.cdatetime.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.cdatetime.tests`; execution environment: `JavaSE-11`; fragment host: `org.eclipse.nebula.widgets.cdatetime`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.swt`, `org.eclipse.nebula.cwt.tests;bundle-version="0.9.0"`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt`, `org.eclipse.e4.ui.css.swt.theme;bundle-version="0.11.0"`, `org.apache.batik.css;bundle-version="1.8.0"`, `org.apache.batik.util;bundle-version="1.8.0"`, `org.eclipse.ui;bundle-version="3.109.0"`, `org.eclipse.nebula.widgets.cdatetime.css`
- Imported package clauses: `org.eclipse.e4.ui.css.core.engine`, `org.w3c.css.sac`, `org.w3c.dom`, `org.w3c.dom.css`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.cdatetime`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetime), [`org.eclipse.nebula.widgets.cdatetime.css`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimecss), [`org.eclipse.nebula.widgets.cdatetime.screenshots`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimescreenshots), [`org.eclipse.nebula.widgets.cdatetime.tests.css`](../packages/cdatetime.md#orgeclipsenebulawidgetscdatetimetestscss)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

