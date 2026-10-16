# Project and module records: richtext

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U279: `widgets/richtext`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/pom.xml)
- Maven artifact: `org.eclipse.nebula:richtext:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/richtext/org.eclipse.nebula.widgets.richtext` (default), `widgets/richtext/org.eclipse.nebula.widgets.richtext.example` (default), `widgets/richtext/org.eclipse.nebula.widgets.richtext.example.e4` (default), `widgets/richtext/org.eclipse.nebula.widgets.richtext.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U280: `widgets/richtext/org.eclipse.nebula.widgets.richtext`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext/.project)
- Eclipse project: `org.eclipse.nebula.widgets.richtext`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.richtext:1.7.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.richtext`; execution environment: `JavaSE-21`
- Exported package clauses: `org.eclipse.nebula.widgets.richtext;version="1.7.0"`, `org.eclipse.nebula.widgets.richtext.painter;version="1.2.0"`, `org.eclipse.nebula.widgets.richtext.painter.instructions;version="1.3.0"`, `org.eclipse.nebula.widgets.richtext.toolbar;version="1.2.0"`
- Required bundles: `org.eclipse.core.runtime`
- Imported package clauses: `org.eclipse.core.commands`, `org.eclipse.jface.bindings.keys`, `org.eclipse.jface.bindings.keys.formatting`, `org.eclipse.jface.resource`, `org.eclipse.jface.util`, `org.eclipse.jface.viewers`, `org.eclipse.jface.widgets`, `org.eclipse.swt`, `org.eclipse.swt.browser`, `org.eclipse.swt.custom`, `org.eclipse.swt.events`, `org.eclipse.swt.graphics`, `org.eclipse.swt.layout`, `org.eclipse.swt.widgets`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.richtext`](../packages/richtext.md#orgeclipsenebulawidgetsrichtext), [`org.eclipse.nebula.widgets.richtext.painter`](../packages/richtext.md#orgeclipsenebulawidgetsrichtextpainter), [`org.eclipse.nebula.widgets.richtext.painter.instructions`](../packages/richtext.md#orgeclipsenebulawidgetsrichtextpainterinstructions), [`org.eclipse.nebula.widgets.richtext.toolbar`](../packages/richtext.md#orgeclipsenebulawidgetsrichtexttoolbar)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U281: `widgets/richtext/org.eclipse.nebula.widgets.richtext.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.richtext.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.richtext.example:1.7.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.richtext.example`; execution environment: `JavaSE-21`
- Exported package clauses: `org.eclipse.nebula.widgets.richtext.example`
- Required bundles: `org.eclipse.jface;bundle-version="3.10.1"`, `org.eclipse.nebula.widgets.richtext;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.richtext.example`](../packages/richtext.md#orgeclipsenebulawidgetsrichtextexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U282: `widgets/richtext/org.eclipse.nebula.widgets.richtext.example.e4`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.example.e4/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.example.e4/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.example.e4/.project)
- Eclipse project: `org.eclipse.nebula.widgets.richtext.example.e4`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.richtext.example.e4:1.7.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.richtext.example.e4`; execution environment: `JavaSE-21`
- Required bundles: `org.eclipse.core.runtime`, `org.eclipse.swt`, `org.eclipse.jface`, `org.eclipse.e4.ui.model.workbench;bundle-version="1.1.0"`, `org.eclipse.e4.ui.services`, `org.eclipse.e4.ui.workbench;bundle-version="1.2.1"`, `org.eclipse.e4.core.di;bundle-version="1.4.0"`, `org.eclipse.e4.ui.di;bundle-version="1.0.0"`, `org.eclipse.e4.core.contexts;bundle-version="1.3.100"`, `org.eclipse.nebula.widgets.richtext;bundle-version="1.0.0"`, `org.eclipse.nebula.widgets.richtext.example;bundle-version="1.0.0"`
- Imported package clauses: `jakarta.annotation;version="[2.1.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.richtext.example.e4.part`](../packages/richtext.md#orgeclipsenebulawidgetsrichtextexamplee4part)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U283: `widgets/richtext/org.eclipse.nebula.widgets.richtext.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/richtext/org.eclipse.nebula.widgets.richtext.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.richtext.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.richtext.feature:1.7.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.richtext.feature`; packages plug-ins `org.eclipse.nebula.widgets.richtext`, `org.eclipse.nebula.widgets.richtext.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

