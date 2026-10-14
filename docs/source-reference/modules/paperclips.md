# Project and module records: paperclips

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U244: `widgets/paperclips`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/pom.xml)
- Maven artifact: `org.eclipse.nebula:paperclips:2.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/paperclips/org.eclipse.nebula.paperclips.core` (default), `widgets/paperclips/org.eclipse.nebula.paperclips.widgets` (default), `widgets/paperclips/org.eclipse.nebula.paperclips.tests` (default), `widgets/paperclips/org.eclipse.nebula.paperclips.snippets` (default), `widgets/paperclips/org.eclipse.nebula.widgets.paperclips.feature` (default), `widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U245: `widgets/paperclips/org.eclipse.nebula.paperclips.core`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.core/.project)
- Eclipse project: `org.eclipse.nebula.paperclips.core`; natures: `org.eclipse.jdt.core.javanature`, `org.eclipse.pde.PluginNature`, `org.eclipse.pde.api.tools.apiAnalysisNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.paperclips.core:2.2.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.paperclips.core`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.paperclips.core`, `org.eclipse.nebula.paperclips.core.border`, `org.eclipse.nebula.paperclips.core.border.internal`, `org.eclipse.nebula.paperclips.core.grid`, `org.eclipse.nebula.paperclips.core.grid.internal;x-internal:=true`, `org.eclipse.nebula.paperclips.core.internal;x-internal:=true`, `org.eclipse.nebula.paperclips.core.internal.piece`, `org.eclipse.nebula.paperclips.core.internal.util;x-internal:=true`, `org.eclipse.nebula.paperclips.core.page`, `org.eclipse.nebula.paperclips.core.text`, `org.eclipse.nebula.paperclips.core.text.internal`
- Required bundles: `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.paperclips.core`](../packages/paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.border`](../packages/paperclips.md#orgeclipsenebulapaperclipscoreborder), [`org.eclipse.nebula.paperclips.core.border.internal`](../packages/paperclips.md#orgeclipsenebulapaperclipscoreborderinternal), [`org.eclipse.nebula.paperclips.core.grid`](../packages/paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.grid.internal`](../packages/paperclips.md#orgeclipsenebulapaperclipscoregridinternal), [`org.eclipse.nebula.paperclips.core.internal`](../packages/paperclips.md#orgeclipsenebulapaperclipscoreinternal), [`org.eclipse.nebula.paperclips.core.internal.piece`](../packages/paperclips.md#orgeclipsenebulapaperclipscoreinternalpiece), [`org.eclipse.nebula.paperclips.core.internal.util`](../packages/paperclips.md#orgeclipsenebulapaperclipscoreinternalutil), [`org.eclipse.nebula.paperclips.core.page`](../packages/paperclips.md#orgeclipsenebulapaperclipscorepage), [`org.eclipse.nebula.paperclips.core.text`](../packages/paperclips.md#orgeclipsenebulapaperclipscoretext), [`org.eclipse.nebula.paperclips.core.text.internal`](../packages/paperclips.md#orgeclipsenebulapaperclipscoretextinternal)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U246: `widgets/paperclips/org.eclipse.nebula.paperclips.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.snippets/.project)
- Eclipse project: `org.eclipse.nebula.paperclips.snippets`; natures: `org.eclipse.jdt.core.javanature`, `org.eclipse.pde.PluginNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.paperclips.snippets:2.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.paperclips.snippets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.paperclips.snippets`
- Required bundles: `org.eclipse.nebula.paperclips.core;bundle-version="[2.1.0,3.0.0)"`, `org.eclipse.nebula.paperclips.widgets;bundle-version="[2.1.0,3.0.0)"`, `org.eclipse.swt;bundle-version="[3.2.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.paperclips.snippets`](../packages/paperclips.md#orgeclipsenebulapaperclipssnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U247: `widgets/paperclips/org.eclipse.nebula.paperclips.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.tests/.project)
- Eclipse project: `org.eclipse.nebula.paperclips.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.paperclips.tests:2.1.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.paperclips.tests`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.paperclips.tests.benchmark`
- Required bundles: `org.eclipse.nebula.paperclips.core;bundle-version="[2.0.0,3.0.0)"`, `org.eclipse.nebula.paperclips.snippets;bundle-version="[2.0.0,3.0.0)"`, `org.eclipse.nebula.paperclips.widgets;bundle-version="[2.0.0,3.0.0)"`, `org.eclipse.swt;bundle-version="[3.2.0,4.0.0)"`, `org.junit;bundle-version="[3.8.2,5.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.paperclips.core`](../packages/paperclips.md#orgeclipsenebulapaperclipscore), [`org.eclipse.nebula.paperclips.core.grid`](../packages/paperclips.md#orgeclipsenebulapaperclipscoregrid), [`org.eclipse.nebula.paperclips.core.internal`](../packages/paperclips.md#orgeclipsenebulapaperclipscoreinternal), [`org.eclipse.nebula.paperclips.tests.benchmark`](../packages/paperclips.md#orgeclipsenebulapaperclipstestsbenchmark), [`org.eclipse.nebula.paperclips.tests.main`](../packages/paperclips.md#orgeclipsenebulapaperclipstestsmain)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U248: `widgets/paperclips/org.eclipse.nebula.paperclips.widgets`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.widgets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.widgets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.paperclips.widgets/.project)
- Eclipse project: `org.eclipse.nebula.paperclips.widgets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.paperclips.widgets:2.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.paperclips.widgets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.paperclips.widgets`
- Required bundles: `org.eclipse.nebula.paperclips.core;bundle-version="[2.0.0,3.0.0)"`, `org.eclipse.swt;bundle-version="[3.2.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.paperclips.widgets`](../packages/paperclips.md#orgeclipsenebulapaperclipswidgets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U249: `widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.paperclips.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.paperclips.example:2.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.paperclips.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.nebula.paperclips.core;bundle-version="[2.0.0,3.0.0)"`, `org.eclipse.nebula.paperclips.widgets;bundle-version="[2.0.0,3.0.0)"`, `org.eclipse.swt;bundle-version="[3.2.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.paperclips.example`](../packages/paperclips.md#orgeclipsenebulawidgetspaperclipsexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U250: `widgets/paperclips/org.eclipse.nebula.widgets.paperclips.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/paperclips/org.eclipse.nebula.widgets.paperclips.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.paperclips.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.paperclips.feature:2.2.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.paperclips.feature`; packages plug-ins `org.eclipse.nebula.paperclips.core`, `org.eclipse.nebula.paperclips.widgets`, `org.eclipse.nebula.paperclips.core.source`, `org.eclipse.nebula.paperclips.widgets.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

