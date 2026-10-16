# Project and module records: opal-columnbrowser

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U142: `widgets/opal/columnbrowser`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/pom.xml)
- Maven artifact: `org.eclipse.nebula:columnbrowser:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser` (default), `widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.feature` (default), `widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U143: `widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.columnbrowser`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.columnbrowser:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.columnbrowser`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.columnbrowser`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.columnbrowser`](../packages/opal-columnbrowser.md#orgeclipsenebulawidgetsopalcolumnbrowser)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U144: `widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.columnbrowser.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.columnbrowser.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.columnbrowser.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.columnbrowser`, `org.eclipse.nebula.widgets.opal.columnbrowser.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U145: `widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/columnbrowser/org.eclipse.nebula.widgets.opal.columnbrowser.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.columnbrowser.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.columnbrowser.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.columnbrowser.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.columnbrowser;bundle-version="1.0.0"`, `org.eclipse.nebula.widgets.opal.dialog;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.opal.columnbrowser.snippets`](../packages/opal-columnbrowser.md#orgeclipsenebulaopalcolumnbrowsersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

