# Project and module records: opal-preferencewindow

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U189: `widgets/opal/preferencewindow`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/pom.xml)
- Maven artifact: `org.eclipse.nebula:preferencewindow:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow` (default), `widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.feature` (default), `widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U190: `widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.preferencewindow`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.preferencewindow:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.preferencewindow`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.preferencewindow`, `org.eclipse.nebula.widgets.opal.preferencewindow.enabler`, `org.eclipse.nebula.widgets.opal.preferencewindow.widgets`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.nebula.widgets.opal.titledseparator;bundle-version="1.0.0"`, `org.eclipse.nebula.widgets.opal.dialog;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.preferencewindow`](../packages/opal-preferencewindow.md#orgeclipsenebulawidgetsopalpreferencewindow), [`org.eclipse.nebula.widgets.opal.preferencewindow.enabler`](../packages/opal-preferencewindow.md#orgeclipsenebulawidgetsopalpreferencewindowenabler), [`org.eclipse.nebula.widgets.opal.preferencewindow.widgets`](../packages/opal-preferencewindow.md#orgeclipsenebulawidgetsopalpreferencewindowwidgets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U191: `widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.preferencewindow.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.preferencewindow.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.preferencewindow.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.preferencewindow`, `org.eclipse.nebula.widgets.opal.preferencewindow.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U192: `widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/preferencewindow/org.eclipse.nebula.widgets.opal.preferencewindow.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.preferencewindow.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.preferencewindow.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.preferencewindow.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.preferencewindow;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.preferencewindow.snippets`](../packages/opal-preferencewindow.md#orgeclipsenebulawidgetsopalpreferencewindowsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

