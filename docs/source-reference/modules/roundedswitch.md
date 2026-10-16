# Project and module records: roundedswitch

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U288: `widgets/roundedswitch`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/pom.xml)
- Maven artifact: `org.eclipse.nebula:roundedswitch:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch` (default), `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.feature` (default), `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.snippets` (default), `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4` (default), `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css` (default), `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css.feature` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U289: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedswitch`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedswitch:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.roundedswitch`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.roundedswitch`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.roundedswitch`](../packages/roundedswitch.md#orgeclipsenebulawidgetsroundedswitch)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U290: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedswitch.css`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedswitch.css:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.roundedswitch.css`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.roundedswitch.css`
- Required bundles: `org.eclipse.swt`, `org.eclipse.nebula.widgets.roundedswitch`, `org.eclipse.e4.ui.services;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"`, `org.eclipse.e4.ui.css.swt;bundle-version="0.10.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.roundedswitch.css`](../packages/roundedswitch.md#orgeclipsenebulawidgetsroundedswitchcss)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U291: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedswitch.css.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedswitch.css.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.roundedswitch.css.feature`; packages plug-ins `org.eclipse.nebula.widgets.roundedswitch.css`, `org.eclipse.nebula.widgets.roundedswitch.css.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U292: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedswitch.example.e4`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedswitch.example.e4:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.roundedswitch.example.e4`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="0.0.0"`, `org.eclipse.e4.ui.model.workbench;bundle-version="0.0.0"`, `org.eclipse.jface;bundle-version="0.0.0"`, `org.eclipse.e4.ui.services;bundle-version="0.0.0"`, `org.eclipse.e4.ui.workbench;bundle-version="0.0.0"`, `org.eclipse.e4.core.di;bundle-version="0.0.0"`, `org.eclipse.e4.ui.di;bundle-version="0.0.0"`, `org.eclipse.e4.core.contexts;bundle-version="0.0.0"`, `org.eclipse.core.runtime;bundle-version="0.0.0"`, `org.eclipse.nebula.widgets.roundedswitch`, `org.eclipse.nebula.widgets.roundedswitch.css`
- Imported package clauses: `javax.annotation;version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.roundedswitch.example.e4`](../packages/roundedswitch.md#orgeclipsenebulawidgetsroundedswitchexamplee4), [`org.eclipse.nebula.widgets.roundedswitch.example.e4.handlers`](../packages/roundedswitch.md#orgeclipsenebulawidgetsroundedswitchexamplee4handlers), [`org.eclipse.nebula.widgets.roundedswitch.example.e4.parts`](../packages/roundedswitch.md#orgeclipsenebulawidgetsroundedswitchexamplee4parts)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U293: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedswitch.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedswitch.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.roundedswitch.feature`; packages plug-ins `org.eclipse.nebula.widgets.roundedswitch`, `org.eclipse.nebula.widgets.roundedswitch.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U294: `widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.roundedswitch.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.roundedswitch.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.roundedswitch.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.roundedswitch;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.roundedswitch`](../packages/roundedswitch.md#orgeclipsenebulawidgetsroundedswitch)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

