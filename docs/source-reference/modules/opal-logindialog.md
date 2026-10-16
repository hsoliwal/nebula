# Project and module records: opal-logindialog

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U173: `widgets/opal/logindialog`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/pom.xml)
- Maven artifact: `org.eclipse.nebula:logindialog:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog` (default), `widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.feature` (default), `widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U174: `widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.logindialog`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.logindialog:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.logindialog`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.loginDialog`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.nebula.widgets.opal.dialog;bundle-version="1.0.0"`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Imported package clauses: `org.eclipse.nebula.widgets.opal.dialog`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.loginDialog`](../packages/opal-logindialog.md#orgeclipsenebulawidgetsopallogindialog)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U175: `widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.logindialog.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.logindialog.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.logindialog.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.logindialog`, `org.eclipse.nebula.widgets.opal.logindialog.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U176: `widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/logindialog/org.eclipse.nebula.widgets.opal.logindialog.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.logindialog.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.logindialog.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.logindialog.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.logindialog;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.loginDialog.snippets`](../packages/opal-logindialog.md#orgeclipsenebulawidgetsopallogindialogsnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

