# Project and module records: gallery

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U095: `widgets/gallery`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/pom.xml)
- Maven artifact: `org.eclipse.nebula:gallery:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/gallery/org.eclipse.nebula.widgets.gallery` (default), `widgets/gallery/org.eclipse.nebula.widgets.gallery.example` (default), `widgets/gallery/org.eclipse.nebula.widgets.gallery.feature` (default), `widgets/gallery/org.eclipse.nebula.widgets.gallery.tests` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U096: `widgets/gallery/org.eclipse.nebula.widgets.gallery`

- Classification: production/build; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery/.project)
- Eclipse project: `org.eclipse.nebula.widgets.gallery`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.gallery:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.gallery`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.animation`, `org.eclipse.nebula.animation.effects`, `org.eclipse.nebula.animation.movement`, `org.eclipse.nebula.jface.galleryviewer`, `org.eclipse.nebula.widgets.gallery`
- Required bundles: `org.eclipse.swt;bundle-version="[3.126.0,4.0.0)"`, `org.eclipse.jface;resolution:=optional`
- Owned declaration namespaces: [`org.eclipse.nebula.animation`](../packages/gallery.md#orgeclipsenebulaanimation), [`org.eclipse.nebula.animation.effects`](../packages/gallery.md#orgeclipsenebulaanimationeffects), [`org.eclipse.nebula.animation.movement`](../packages/gallery.md#orgeclipsenebulaanimationmovement), [`org.eclipse.nebula.jface.galleryviewer`](../packages/gallery.md#orgeclipsenebulajfacegalleryviewer), [`org.eclipse.nebula.widgets.gallery`](../packages/gallery.md#orgeclipsenebulawidgetsgallery)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U097: `widgets/gallery/org.eclipse.nebula.widgets.gallery.example`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.example/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.example/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.example/.project)
- Eclipse project: `org.eclipse.nebula.widgets.gallery.example`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.gallery.example:1.0.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.gallery.example`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.gallery;bundle-version="0.5.3"`, `org.eclipse.nebula.examples;bundle-version="1.0.4"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.gallery.example`](../packages/gallery.md#orgeclipsenebulawidgetsgalleryexample)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U098: `widgets/gallery/org.eclipse.nebula.widgets.gallery.feature`

- Classification: production/build; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.gallery.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.gallery.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.gallery.feature`; packages plug-ins `org.eclipse.nebula.widgets.gallery`, `org.eclipse.nebula.widgets.gallery.source`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U099: `widgets/gallery/org.eclipse.nebula.widgets.gallery.test.feature`

- Classification: test; outside parsed default reactor; retained, not excluded
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.test.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.test.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.test.feature/.project)
- Eclipse project: `org.eclipse.nebula.widgets.gallery.test.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.gallery.test.feature:1.0.0`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.widgets.gallery.test.feature`; packages plug-ins `org.eclipse.nebula.widgets.gallery`, `org.eclipse.nebula.widgets.gallery.tests`, `org.eclipse.test`, `org.eclipse.ant.optional.junit`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U100: `widgets/gallery/org.eclipse.nebula.widgets.gallery.tests`

- Classification: test; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.tests/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.tests/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/gallery/org.eclipse.nebula.widgets.gallery.tests/.project)
- Eclipse project: `org.eclipse.nebula.widgets.gallery.tests`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.widgets.gallery.tests:1.0.0-SNAPSHOT`; packaging: `eclipse-test-plugin`
- OSGi bundle: `org.eclipse.nebula.widgets.gallery.tests`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.nebula.widgets.gallery`, `org.junit`, `org.eclipse.swt`, `org.eclipse.jface;resolution:=optional`, `org.eclipse.core.runtime;resolution:=optional`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.gallery.tests`](../packages/gallery.md#orgeclipsenebulawidgetsgallerytests)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

