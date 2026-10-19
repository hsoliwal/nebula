# Project and module records: picture

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U259: `widgets/picture`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/pom.xml)
- Maven artifact: `org.eclipse.nebula:picture:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/picture/org.eclipse.nebula.widgets.picture` (default), `widgets/picture/org.eclipse.nebula.widgets.picture.example` (default), `widgets/picture/org.eclipse.nebula.widgets.picture.feature` (default), `widgets/picture/org.eclipse.nebula.widgets.picture.snippets` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U260: `widgets/picture/org.eclipse.nebula.widgets.picture`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture/.project)
- Eclipse project: `org.eclipse.nebula.widgets.picture`; natures: `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.picture:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.picture`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.widgets.picture`, `org.eclipse.nebula.widgets.picture.forms`
- Imported package clauses: `org.eclipse.swt`, `org.eclipse.swt.graphics`, `org.eclipse.swt.layout`, `org.eclipse.swt.widgets`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.picture`](../packages/picture.md#orgeclipsenebulawidgetspicture), [`org.eclipse.nebula.widgets.picture.forms`](../packages/picture.md#orgeclipsenebulawidgetspictureforms), [`org.eclipse.nebula.widgets.picture.internal`](../packages/picture.md#orgeclipsenebulawidgetspictureinternal), [`org.eclipse.nebula.widgets.picture.internal.input`](../packages/picture.md#orgeclipsenebulawidgetspictureinternalinput), [`org.eclipse.nebula.widgets.picture.internal.output`](../packages/picture.md#orgeclipsenebulawidgetspictureinternaloutput)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U261: `widgets/picture/org.eclipse.nebula.widgets.picture.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.picture.example`; natures: `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.picture.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.picture.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.examples;bundle-version="1.0.4"`, `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.picture;bundle-version="1.0.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.picture.example`](../packages/picture.md#orgeclipsenebulawidgetspictureexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U262: `widgets/picture/org.eclipse.nebula.widgets.picture.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.picture.feature`; natures: `org.eclipse.jdt.core.javanature`, `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.picture.feature:1.0.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.picture.feature`; packages plug-ins `org.eclipse.nebula.widgets.picture`, `org.eclipse.nebula.widgets.picture.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U263: `widgets/picture/org.eclipse.nebula.widgets.picture.snippets`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.snippets/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.snippets/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/picture/org.eclipse.nebula.widgets.picture.snippets/.project)
- Eclipse project: `org.eclipse.nebula.widgets.picture.snippets`; natures: `org.maven.ide.eclipse.maven2Nature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.picture.snippets:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.picture.snippets`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt`, `org.eclipse.nebula.widgets.picture`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.picture.snippets`](../packages/picture.md#orgeclipsenebulawidgetspicturesnippets)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

