# Project and module records: oscilloscope

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U229: `widgets/oscilloscope`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/pom.xml)
- Maven artifact: `org.eclipse.nebula:oscilloscope:1.4.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope` (default), `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example` (default), `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.tests` (default), `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.feature` (default), `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4` (default), `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css` (default), `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U230: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope:1.5.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.oscilloscope`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.oscilloscope;x-internal:=true`, `org.eclipse.nebula.widgets.oscilloscope.multichannel`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.oscilloscope`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscope), [`org.eclipse.nebula.widgets.oscilloscope.multichannel`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopemultichannel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U231: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.css`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.css:1.4.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.oscilloscope.css`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.oscilloscope.css`
- Required bundles: `org.eclipse.swt`, `org.eclipse.nebula.widgets.oscilloscope`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt;bundle-version="0.10.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.oscilloscope.css`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopecss)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U232: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.css.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.css.feature:1.4.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.oscilloscope.css.feature`; packages plug-ins `org.eclipse.nebula.widgets.oscilloscope.css`, `org.eclipse.nebula.widgets.oscilloscope.css.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U233: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.example:1.0.1-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.oscilloscope.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.oscilloscope;bundle-version="1.0.1"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.oscilloscope.example`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopeexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U234: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.example.e4`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.example.e4:1.4.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.oscilloscope.example.e4`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="0.0.0"`, `org.eclipse.e4.ui.model.workbench;bundle-version="0.0.0"`, `org.eclipse.jface;bundle-version="0.0.0"`, `org.eclipse.e4.ui.services;bundle-version="0.0.0"`, `org.eclipse.e4.ui.workbench;bundle-version="0.0.0"`, `org.eclipse.e4.core.di;bundle-version="0.0.0"`, `org.eclipse.e4.ui.di;bundle-version="0.0.0"`, `org.eclipse.e4.core.contexts;bundle-version="0.0.0"`, `org.eclipse.core.runtime;bundle-version="0.0.0"`, `org.eclipse.nebula.widgets.oscilloscope`, `org.eclipse.nebula.widgets.oscilloscope.css`
- Imported package clauses: `javax.annotation;version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.oscilloscope.example.e4`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopeexamplee4), [`org.eclipse.nebula.widgets.oscilloscope.example.e4.handlers`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopeexamplee4handlers), [`org.eclipse.nebula.widgets.oscilloscope.example.e4.parts`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopeexamplee4parts)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U235: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.feature:1.5.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.oscilloscope.feature`; packages plug-ins `org.eclipse.nebula.widgets.oscilloscope`, `org.eclipse.nebula.widgets.oscilloscope.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U236: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.snippets`

- Classification: example; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.snippets:1.4.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.oscilloscope.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="3.5.1"`
- Imported package clauses: `org.eclipse.nebula.widgets.oscilloscope`, `org.eclipse.nebula.widgets.oscilloscope.multichannel`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.oscilloscope.snippets`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U237: `widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.oscilloscope.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.oscilloscope.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.oscilloscope.tests`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.oscilloscope;bundle-version="1.0.1"`, `org.eclipse.ui;bundle-version="3.6.2"`, `org.junit`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.oscilloscope`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscope), [`org.eclipse.nebula.widgets.oscilloscope.multichannel`](../packages/oscilloscope.md#orgeclipsenebulawidgetsoscilloscopemultichannel)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

