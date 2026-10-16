# Project and module records: examples

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U005: `examples/org.eclipse.nebula.examples`

- Classification: example; default-reactor
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.examples/META-INF/MANIFEST.MF), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.examples/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.examples/.project)
- Eclipse project: `org.eclipse.nebula.examples`; natures: `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.examples:1.1.0-SNAPSHOT`; packaging: `eclipse-plugin`
- OSGi bundle: `org.eclipse.nebula.examples`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.examples`
- Required bundles: `org.eclipse.ui`, `org.eclipse.core.runtime`, `org.eclipse.swt;bundle-version="[3.115.0,4.0.0)"`
- Owned declaration namespaces: [`org.eclipse.nebula.examples`](../packages/examples.md#orgeclipsenebulaexamples), [`org.eclipse.nebula.examples.internal`](../packages/examples.md#orgeclipsenebulaexamplesinternal)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U006: `examples/org.eclipse.nebula.examples.feature`

- Classification: example; default-reactor
- Discovery: [feature.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.examples.feature/feature.xml), [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.examples.feature/pom.xml), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.examples.feature/.project)
- Eclipse project: `org.eclipse.nebula.examples.feature`; natures: `org.eclipse.pde.FeatureNature`
- Maven artifact: `org.eclipse.nebula:org.eclipse.nebula.examples.feature:1.1.0-SNAPSHOT`; packaging: `eclipse-feature`
- Feature identity: `org.eclipse.nebula.examples.feature`; packages plug-ins `org.eclipse.nebula.examples`; included features none declared
- Responsibility boundary: Assembles the declared plug-in and feature membership for installation/distribution; it does not define a Java namespace by itself.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U007: `examples/org.eclipse.nebula.snippets`

- Classification: example; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.snippets/META-INF/MANIFEST.MF), [.project](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/examples/org.eclipse.nebula.snippets/.project)
- Eclipse project: `org.eclipse.nebula.snippets`; natures: `org.eclipse.jem.workbench.JavaEMFNature`, `org.eclipse.pde.PluginNature`, `org.eclipse.jdt.core.javanature`, `org.eclipse.jem.beaninfo.BeanInfoNature`
- OSGi bundle: `org.eclipse.nebula.snippets`; execution environment: `JavaSE-11`
- Exported package clauses: `org.eclipse.nebula.snippets.grid`
- Required bundles: `org.eclipse.swt`, `org.eclipse.jface`, `org.eclipse.core.runtime`, `org.eclipse.nebula.widgets.cdatetime;resolution:=optional`, `org.eclipse.nebula.widgets.ctree;resolution:=optional`, `org.eclipse.nebula.widgets.datechooser;resolution:=optional`, `org.eclipse.nebula.widgets.formattedtext;resolution:=optional`, `org.eclipse.nebula.widgets.gallery;resolution:=optional`, `org.eclipse.nebula.widgets.grid;resolution:=optional`, `org.eclipse.nebula.widgets.pgroup;resolution:=optional`, `org.eclipse.nebula.widgets.pshelf;resolution:=optional`, `org.eclipse.nebula.widgets.compositetable;resolution:=optional`, `org.eclipse.nebula.widgets.ganttchart;bundle-version="1.0.0";resolution:=optional`, `org.eclipse.nebula.widgets.radiogroup;bundle-version="1.0.0";resolution:=optional`, `org.eclipse.jface.databinding;bundle-version="1.3.0";resolution:=optional`, `org.eclipse.core.databinding;bundle-version="1.2.0";resolution:=optional`, `org.eclipse.core.databinding.beans;bundle-version="1.2.0";resolution:=optional`, `org.eclipse.core.databinding.property;bundle-version="1.2.0";resolution:=optional`, `org.eclipse.nebula.widgets.collapsiblebuttons;bundle-version="1.0.0";resolution:=optional`, `org.eclipse.nebula.widgets.radiogroup;resolution:=optional`, `org.eclipse.nebula.widgets.tablecombo;resolution:=optional`, `org.eclipse.nebula.visualization.widgets;bundle-version="1.0.0";resolution:=optional`, `org.eclipse.nebula.visualization.xygraph;bundle-version="1.0.0";resolution:=optional`, `org.eclipse.draw2d;bundle-version="3.6.0"`
- Owned declaration namespaces: [`org.eclipse.nebula.snippets.cdatetime`](../packages/examples.md#orgeclipsenebulasnippetscdatetime), [`org.eclipse.nebula.snippets.collapsiblebuttons`](../packages/examples.md#orgeclipsenebulasnippetscollapsiblebuttons), [`org.eclipse.nebula.snippets.compositetable`](../packages/examples.md#orgeclipsenebulasnippetscompositetable), [`org.eclipse.nebula.snippets.compositetable.ve`](../packages/examples.md#orgeclipsenebulasnippetscompositetableve), [`org.eclipse.nebula.snippets.datechooser`](../packages/examples.md#orgeclipsenebulasnippetsdatechooser), [`org.eclipse.nebula.snippets.formattedtext`](../packages/examples.md#orgeclipsenebulasnippetsformattedtext), [`org.eclipse.nebula.snippets.gallery`](../packages/examples.md#orgeclipsenebulasnippetsgallery), [`org.eclipse.nebula.snippets.ganttchart`](../packages/examples.md#orgeclipsenebulasnippetsganttchart), [`org.eclipse.nebula.snippets.grid`](../packages/examples.md#orgeclipsenebulasnippetsgrid), [`org.eclipse.nebula.snippets.grid.viewer`](../packages/examples.md#orgeclipsenebulasnippetsgridviewer), [`org.eclipse.nebula.snippets.pgroup`](../packages/examples.md#orgeclipsenebulasnippetspgroup), [`org.eclipse.nebula.snippets.pshelf`](../packages/examples.md#orgeclipsenebulasnippetspshelf), [`org.eclipse.nebula.snippets.radiogroup`](../packages/examples.md#orgeclipsenebulasnippetsradiogroup), [`org.eclipse.nebula.snippets.tablecombo`](../packages/examples.md#orgeclipsenebulasnippetstablecombo), [`org.eclipse.nebula.snippets.tablecombo.viewer`](../packages/examples.md#orgeclipsenebulasnippetstablecomboviewer), [`org.eclipse.nebula.snippets.visualization`](../packages/examples.md#orgeclipsenebulasnippetsvisualization)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

