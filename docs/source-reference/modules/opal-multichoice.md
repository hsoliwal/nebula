# Project and module records: opal-multichoice

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U177: `widgets/opal/multichoice`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/pom.xml)
- Maven artifact: `org.eclipse.nebula:multichoice:1.1.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice` (default), `widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.feature` (default), `widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U178: `widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.multichoice`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.multichoice:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.multichoice`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.opal.multichoice`
- Required bundles: `org.eclipse.nebula.widgets.opal.commons;bundle-version="1.0.0";visibility:=reexport`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.multichoice`](../packages/opal-multichoice.md#orgeclipsenebulawidgetsopalmultichoice)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U179: `widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.multichoice.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.multichoice.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.opal.multichoice.feature`; packages plug-ins `org.eclipse.nebula.widgets.opal.multichoice`, `org.eclipse.nebula.widgets.opal.multichoice.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U180: `widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/opal/multichoice/org.eclipse.nebula.widgets.opal.multichoice.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.opal.multichoice.snippets`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.opal.multichoice.snippets:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.opal.multichoice.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.opal.multichoice;bundle-version="1.0.0"`, `org.eclipse.swt`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.opal.multichoice.snippets`](../packages/opal-multichoice.md#orgeclipsenebulawidgetsopalmultichoicesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

