# Project and module records: other-components

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U001: `.`

- Classification: production/build; default-reactor
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/.project)
- Eclipse project: `org.eclipse.nebula`; natures: 
- Maven artifact: `org.eclipse.nebula:nebula-root:1.0.0-SNAPSHOT`; packaging: `pom`
- Owned declaration namespaces: [`org.eclipse.nebula.m3.tt`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3tt)
- Child build inputs: `releng/org.eclipse.nebula.nebula-parent` (default)
- Responsibility boundary: Aggregates the child build units listed above; the aggregator itself is not a widget/runtime API.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U015: `org.eclipse.nebula.nebface`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/org.eclipse.nebula.nebface/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/org.eclipse.nebula.nebface/.project)
- Eclipse project: `org.eclipse.swt.nebula.nebface`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.swt.nebula.nebface`; execution environment: `J2SE-1.4`
- Exported package clauses: `org.eclipse.swt.nebula.nebface.viewers`
- Required bundles: `org.eclipse.jface`, `org.eclipse.swt.nebula;visibility:=reexport`, `org.eclipse.equinox.common;visibility:=reexport`
- Owned declaration namespaces: [`org.eclipse.swt.nebula.nebface.ctabletreeviewer`](../packages/other-components.md#orgeclipseswtnebulanebfacectabletreeviewer), [`org.eclipse.swt.nebula.nebface.ctabletreeviewer.ccontainerviewer`](../packages/other-components.md#orgeclipseswtnebulanebfacectabletreeviewerccontainerviewer), [`org.eclipse.swt.nebula.nebface.viewers`](../packages/other-components.md#orgeclipseswtnebulanebfaceviewers)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U016: `org.eclipse.nebula.presentations.shelf`

- Classification: production/build; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/org.eclipse.nebula.presentations.shelf/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/org.eclipse.nebula.presentations.shelf/.project)
- Eclipse project: `org.eclipse.swt.nebula.presentations.shelf`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- OSGi bundle: `org.eclipse.swt.nebula.presentations.shelf`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.swt.nebula.presentations.shelf.tab`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.swt.nebula.presentations.shelf`](../packages/other-components.md#orgeclipseswtnebulapresentationsshelf), [`org.eclipse.swt.nebula.presentations.shelf.tab`](../packages/other-components.md#orgeclipseswtnebulapresentationsshelftab)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

