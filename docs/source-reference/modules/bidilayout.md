# Project and module records: bidilayout

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U029: `widgets/bidilayout`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/bidilayout/pom.xml)
- Maven artifact: `org.eclipse.nebula:bidilayout:1.0.0-SNAPSHOT`; packaging: `pom`
- Child build inputs: `widgets/bidilayout/org.eclipse.nebula.widgets.bidilayout` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U030: `widgets/bidilayout/org.eclipse.nebula.widgets.bidilayout`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/bidilayout/org.eclipse.nebula.widgets.bidilayout/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/bidilayout/org.eclipse.nebula.widgets.bidilayout/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/widgets/bidilayout/org.eclipse.nebula.widgets.bidilayout/.project)
- Eclipse project: `org.eclipse.nebula.widgets.bidilayout`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:bidilayout:1.0.0-SNAPSHOT`; packaging: `pom`
- OSGi bundle: `org.eclipse.nebula.widgets.bidilayout`; execution environment: `JavaSE-11`
- Required bundles: `org.eclipse.swt;bundle-version="3.6.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.bidilayout`](../packages/bidilayout.md#orgeclipsenebulawidgetsbidilayout)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

