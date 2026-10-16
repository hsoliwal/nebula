# Project and module records: fork-tooling-and-provenance

[Module index](../MODULES.md) · [Reactor edges](../REACTOR.md) · [Packages](../PACKAGES.md) · [Lifecycle guide](../LIFECYCLE.md)

This navigation group is not a new module. The U-identifiers and discovery manifests below identify actual units. All behavior-level review is partial as stated in the coverage ledger.

### U002: `.m3/atom-pattern`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/.m3/atom-pattern/pom.xml)
- Maven artifact: `com.synexia.intake:repository-hierarchy-intake:1`; packaging: `pom`
- Direct POM dependencies: `com.synexia.verification:m3-hierarchy-recipe` (inherited/profile/plugin dependencies are additional)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U003: `.m3/nebula-visible-range`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/.m3/nebula-visible-range/pom.xml)
- Maven artifact: `org.eclipse.nebula.verification:visible-range-diff-contract:1.0.0-SNAPSHOT`; packaging: `jar (Maven default)`
- Owned declaration namespaces: [`org.eclipse.nebula.widgets.grid`](../packages/grid.md#orgeclipsenebulawidgetsgrid)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U004: `.m3/upstream-page-atoms`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/.m3/upstream-page-atoms/pom.xml)
- Maven artifact: `com.synexia.recipes:nebula-upstream-page-atoms:1.0.0`; packaging: `jar (Maven default)`
- Direct POM dependencies: `org.openrewrite:rewrite-core`, `org.eclipse.platform:org.eclipse.swt.gtk.linux.x86_64`, `org.eclipse.platform:org.eclipse.equinox.common` (inherited/profile/plugin dependencies are additional)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U008: `m3/convergence/swt-receiver-p2`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/convergence/swt-receiver-p2/pom.xml)
- Maven artifact: `org.eclipse.nebula.m3:swt-receiver-p2:1.0.0-SNAPSHOT`; packaging: `pom`
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U009: `m3/openrewrite-line-terminators`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/openrewrite-line-terminators/pom.xml)
- Maven artifact: `com.synexia.recipe:openrewrite-line-terminators:8.90.4-m3-javadoc-e898332e8f88`; packaging: `jar (Maven default)`
- Direct POM dependencies: `org.openrewrite:rewrite-java-21`, `org.slf4j:slf4j-api`, `org.junit.jupiter:junit-jupiter`, `org.assertj:assertj-core` (inherited/profile/plugin dependencies are additional)
- Owned declaration namespaces: [`com.synexia.recipe`](../packages/fork-tooling-and-provenance.md#comsynexiarecipe), [`org.eclipse.nebula.snippets.gallery`](../packages/examples.md#orgeclipsenebulasnippetsgallery), [`org.eclipse.nebula.visualization.widgets.figureparts`](../packages/visualization.md#orgeclipsenebulavisualizationwidgetsfigureparts), [`org.openrewrite`](../packages/fork-tooling-and-provenance.md#orgopenrewrite), [`org.openrewrite.internal`](../packages/fork-tooling-and-provenance.md#orgopenrewriteinternal), [`org.openrewrite.java.isolated`](../packages/fork-tooling-and-provenance.md#orgopenrewritejavaisolated)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U010: `m3/recipe-first`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/recipe-first/pom.xml)
- Maven artifact: `org.eclipse.nebula.m3:nebula-m3-recipe-first:1.0.0-SNAPSHOT`; packaging: `jar`
- Direct POM dependencies: `com.synexia.recipe:openrewrite-line-terminators`, `org.openrewrite:rewrite-core`, `org.openrewrite:rewrite-java`, `org.openrewrite:rewrite-java-21`, `org.openrewrite:rewrite-test`, `org.junit.jupiter:junit-jupiter`, `org.eclipse.platform:org.eclipse.swt.win32.win32.x86_64`, `org.eclipse.platform:org.eclipse.jface` (inherited/profile/plugin dependencies are additional)
- Owned declaration namespaces: [`org.eclipse.nebula.m3`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3), [`org.eclipse.nebula.m3.rewrite`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewrite), [`org.eclipse.nebula.m3.rewrite.atom`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewriteatom), [`org.eclipse.nebula.m3.rewrite.convergence`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewriteconvergence), [`org.eclipse.nebula.m3.rewrite.exact`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewriteexact), [`org.eclipse.nebula.m3.rewrite.stream`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewritestream)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U011: `m3/stream-segment-proof`

- Classification: fork tooling/proof; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/stream-segment-proof/pom.xml)
- Maven artifact: `org.eclipse.nebula.m3:nebula-stream-segment-proof:1.0.0-SNAPSHOT`; packaging: `jar (Maven default)`
- Direct POM dependencies: `org.openrewrite:rewrite-core`, `org.openrewrite:rewrite-java`, `org.openrewrite:rewrite-java-21`, `com.synexia.recipe:openrewrite-line-terminators`, `org.junit.jupiter:junit-jupiter`, `org.mockito:mockito-core` (inherited/profile/plugin dependencies are additional)
- Owned declaration namespaces: [`com.synexia.ai.m3scale`](../packages/fork-tooling-and-provenance.md#comsynexiaaim3scale), [`com.synexia.rewrite`](../packages/fork-tooling-and-provenance.md#comsynexiarewrite), [`com.synexia.rewrite.atomdb`](../packages/fork-tooling-and-provenance.md#comsynexiarewriteatomdb), [`java.util`](../packages/fork-tooling-and-provenance.md#javautil), [`java.util.stream`](../packages/fork-tooling-and-provenance.md#javautilstream), [`org.eclipse.nebula.m3.stream`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3stream), [`org.eclipse.nebula.widgets.opal.roundedtoolbar`](../packages/opal-roundedtoolbar.md#orgeclipsenebulawidgetsopalroundedtoolbar), [`org.openrewrite.staticanalysis`](../packages/fork-tooling-and-provenance.md#orgopenrewritestaticanalysis), [`org.openrewrite.staticanalysis.groovy`](../packages/fork-tooling-and-provenance.md#orgopenrewritestaticanalysisgroovy), [`org.openrewrite.staticanalysis.kotlin`](../packages/fork-tooling-and-provenance.md#orgopenrewritestaticanalysiskotlin)
- Responsibility boundary: Produces/identifies the bundle, feature, or project declared above; use its owned package entries for source navigation.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U012: `m3/stream-segment-proof/proof/donor-inventory/nebula-donors/m3/recipe-first`

- Classification: vendor/provenance snapshot; outside parsed default reactor; retained, not excluded
- Discovery: [pom.xml](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/stream-segment-proof/proof/donor-inventory/nebula-donors/m3/recipe-first/pom.xml)
- Maven artifact: `org.eclipse.nebula.m3:nebula-m3-recipe-first:1.0.0-SNAPSHOT`; packaging: `jar`
- Direct POM dependencies: `com.synexia.recipe:openrewrite-line-terminators`, `org.openrewrite:rewrite-core`, `org.openrewrite:rewrite-java`, `org.openrewrite:rewrite-java-21`, `org.openrewrite:rewrite-test`, `org.junit.jupiter:junit-jupiter` (inherited/profile/plugin dependencies are additional)
- Owned declaration namespaces: [`org.eclipse.nebula.m3`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3), [`org.eclipse.nebula.m3.rewrite.atom`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewriteatom), [`org.eclipse.nebula.m3.rewrite.convergence`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewriteconvergence), [`org.eclipse.nebula.m3.rewrite.exact`](../packages/fork-tooling-and-provenance.md#orgeclipsenebulam3rewriteexact)
- Responsibility boundary: Retains dependency/donor metadata as evidence. It is not assumed to be an active product implementation or build input.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U013: `m3/stream-segment-proof/proof/sdk/org.eclipse.jface-3.33.0-metadata`

- Classification: vendor/provenance snapshot; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/stream-segment-proof/proof/sdk/org.eclipse.jface-3.33.0-metadata/META-INF/MANIFEST.MF)
- OSGi bundle: `org.eclipse.jface`; execution environment: `JavaSE-17`
- Exported package clauses: `org.eclipse.jface`, `org.eclipse.jface.action`, `org.eclipse.jface.action.images`, `org.eclipse.jface.bindings`, `org.eclipse.jface.bindings.keys`, `org.eclipse.jface.bindings.keys.formatting`, `org.eclipse.jface.commands`, `org.eclipse.jface.contexts`, `org.eclipse.jface.dialogs`, `org.eclipse.jface.dialogs.images`, `org.eclipse.jface.fieldassist`, `org.eclipse.jface.fieldassist.images`, `org.eclipse.jface.images`, `org.eclipse.jface.internal;x-friends:="org.eclipse.ui.workbench,org.eclipse.e4.ui.workbench.renderers.swt"`, `org.eclipse.jface.internal.provisional.action;x-friends:="org.eclipse.ui.workbench,org.eclipse.ui.ide"`, `org.eclipse.jface.layout`, `org.eclipse.jface.menus`, `org.eclipse.jface.operation`, `org.eclipse.jface.preference`, `org.eclipse.jface.preference.images`, `org.eclipse.jface.resource`, `org.eclipse.jface.util`, `org.eclipse.jface.viewers`, `org.eclipse.jface.viewers.deferred`, `org.eclipse.jface.viewers.internal;x-friends:="org.eclipse.ui.tests,org.eclipse.jface.tests"`, `org.eclipse.jface.widgets`, `org.eclipse.jface.window`, `org.eclipse.jface.wizard`, `org.eclipse.jface.wizard.images`
- Required bundles: `org.eclipse.swt;bundle-version="[3.111.0,4.0.0)";visibility:=reexport`, `org.eclipse.core.commands;bundle-version="[3.4.0,4.0.0)";visibility:=reexport`, `org.eclipse.equinox.common;bundle-version="[3.18.0,4.0.0)"`, `org.eclipse.equinox.bidi;bundle-version="[0.10.0,2.0.0)";resolution:=optional`
- Imported package clauses: `javax.xml.parsers`, `org.osgi.framework;version="[1.8.0,2.0.0)"`, `org.w3c.dom`, `org.xml.sax`
- Responsibility boundary: Retains dependency/donor metadata as evidence. It is not assumed to be an active product implementation or build input.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

### U014: `m3/stream-segment-proof/proof/sdk/org.eclipse.swt.win32.win32.x86_64-3.126.0-metadata`

- Classification: vendor/provenance snapshot; outside parsed default reactor; retained, not excluded
- Discovery: [MANIFEST.MF](https://github.com/hsoliwal/nebula/blob/3cf2169433572c96d4b65c83a16c2f907eb37c67/m3/stream-segment-proof/proof/sdk/org.eclipse.swt.win32.win32.x86_64-3.126.0-metadata/META-INF/MANIFEST.MF)
- OSGi bundle: `org.eclipse.swt.win32.win32.x86_64`; execution environment: `not declared`; fragment host: `org.eclipse.swt;bundle-version="[3.125.100,4.0.0)"`
- Exported package clauses: `org.eclipse.swt`, `org.eclipse.swt.accessibility`, `org.eclipse.swt.awt`, `org.eclipse.swt.browser`, `org.eclipse.swt.custom`, `org.eclipse.swt.dnd`, `org.eclipse.swt.events`, `org.eclipse.swt.graphics`, `org.eclipse.swt.layout`, `org.eclipse.swt.opengl`, `org.eclipse.swt.printing`, `org.eclipse.swt.program`, `org.eclipse.swt.widgets`, `org.eclipse.swt.internal; x-friends:="org.eclipse.ui"`, `org.eclipse.swt.internal.image; x-internal:=true`, `org.eclipse.swt.ole.win32`, `org.eclipse.swt.internal.gdip; x-internal:=true`, `org.eclipse.swt.internal.ole.win32; x-internal:=true`, `org.eclipse.swt.internal.win32; x-internal:=true`, `org.eclipse.swt.internal.opengl.win32; x-internal:=true`
- Responsibility boundary: Retains dependency/donor metadata as evidence. It is not assumed to be an active product implementation or build input.
- Semantic coverage: manifest-accounted, partial; package/API behavior is reviewed only where the lifecycle guide explicitly names it

