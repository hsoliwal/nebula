# Project and module records: opal-launcher

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U169: `widgets/opal/launcher`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/pom.xml)
- Maven artifact: `org.eclipse.nebula:launcher:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher` (default), `widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.feature` (default), `widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U170: `widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.launcher`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.launcher:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.launcher`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.launcher`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.launcher`](../packages/opal-launcher.md#orgeclipsenebulawidgetsopallauncher)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U171: `widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.launcher.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.launcher.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.launcher.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.launcher`, `org.eclipse.nebula.widgets.opal.launcher.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U172: `widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/launcher/org.eclipse.nebula.widgets.opal.launcher.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.launcher.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.launcher.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.launcher.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.launcher;bundle-version="1.0.0"`, `org.eclipse.nebula.widgets.opal.dialog;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.launcher.snippets`](../packages/opal-launcher.md#orgeclipsenebulawidgetsopallaunchersnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

