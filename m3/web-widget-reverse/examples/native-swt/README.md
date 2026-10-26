<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native SWT and Nebula receivers from static web markup

The recipe is in Synexia only (`hsoliwal/com.synexia` PR #13396), not this
Nebula fork. These are source-generation artifacts; they do **not** reimplement
SWT, ship a browser runtime, or replace the original Grid model.

| Source | Generated Java | Qualified semantic subset |
| --- | --- | --- |
| Existing `../react-static/Orders.ui.xml` | `OrdersSwt.java` | labels and push buttons; decoded literal `&` escaped as SWT `&&` |
| `NativeControls.ui.xml` | `NativeControlsSwt.java` | nested panels, text, checkbox and original Nebula Grid |
| `StaticOrders.xhtml` | `StaticOrdersSwt.java` | plain XHTML, text/checkbox/password, Combo select, multiline Text, static table to Grid |
| `TreeHierarchy.ui.xml` | `TreeHierarchySwt.java` | static SWT Tree/TreeItem hierarchy, literal text, explicit expansion and parent identity |
| `TreeHierarchy.xhtml` | `TreeHierarchyHtmlSwt.java` | XHTML `ul`/`li` hierarchy and expanded nested lists; no JS/CSS/ARIA |

The XHTML frontend lowers its strict markup into the **same UiBinder source
recipe**; there is no second backend or Java widget hierarchy. Existing HTML
forms, bindings, client events, script, CSS and ARIA are not reinterpreted.
HTML heading rank, label association, layout and accessibility parity are
explicitly unproved.

With the Synexia Maven crate's compiled classes on `$RECIPE_CLASSES`:

```sh
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.UiBinderSwtRecipe \
  ../react-static/Orders.ui.xml OrdersSwt.java org.eclipse.nebula.examples.web OrdersSwt
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.UiBinderSwtRecipe \
  NativeControls.ui.xml NativeControlsSwt.java org.eclipse.nebula.examples.web NativeControlsSwt
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.StaticXhtmlUiBinderFrontend \
  StaticOrders.xhtml StaticOrdersSwt.java org.eclipse.nebula.examples.web StaticOrdersSwt
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.UiBinderSwtRecipe \
  TreeHierarchy.ui.xml TreeHierarchySwt.java org.eclipse.nebula.examples.web TreeHierarchySwt
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.StaticXhtmlUiBinderFrontend \
  TreeHierarchy.xhtml TreeHierarchyHtmlSwt.java org.eclipse.nebula.examples.web TreeHierarchyHtmlSwt
```

The public original-repo `org.eclipse.nebula.snippets.grid` also receives two
generated sources: `NativeControlsWebSwt.java` and `NativeWebControlsSnippet.java`, as well as
`NativeTreeWebSwt.java` and `NativeTreeWebSnippet.java`.
The latter owns the SWT Display/Shell event loop. This makes the generated
control hierarchy a **runnable plugin snippet candidate** (not a mock-API
example), pending native plugin compile and actual SWT display tests.

The Synexia receipt contains exact SHA-256 original/fixture/generated source
identities and refusal/fixed-point tests. Native construction must run on the
owning Display thread and the parent Shell must be disposed normally. No
background worker, M3 runtime wrapper, control per Grid cell or native JNI
resource allocation is introduced by the generated Java.

Local 141-check contract evidence includes Java 11 strict source compilation
**against stubs**, not real installed original SWT/Nebula classes. Tycho,
OSGi, RAP/RWT, visual/pixel, keyboard/IME, accessibility and benchmarks are
explicitly NOT_RUN. Keep receiver draft and separately qualify the native
plugin, screenshot and resource gates before release.

The Tree receiver is static and source-only. The emitted SWT `TreeItem` graph
reuses original native SWT names and constructors. It is not a GWT DOM runtime,
a selection model, or an RWT server-session admission. TreeItem is an SWT
Item/Widget, not a child Control. 14 source seals PASS locally; original native
plugin, RAP/RWT and native screen tests remain unexecuted.


For real native acceptance, the original Grid Tycho test fragment, rather than
this example directory or the separate snippets folder, now owns generated
`GridNativeWebUiBinder`, `TreeNativeWebUiBinder` and
`GridWebNativeMapping_Test` under
`widgets/grid/org.eclipse.nebula.widgets.grid.test/src`. The original Grid test
POM selects the JUnit4 test by default. Its original SWT/GTK native execution
still requires a passing hosted/native Tycho run.

New generated native factories dispose their partially constructed root on
child initialization failure while preserving the original throwable (and
suppressing any cleanup failure). The recipe-first source and injectable
Java-only tests remain in Synexia; do not manually edit these emitted files.

### ProgressBar source-only receiver

`StaticProgress.xhtml` is the source of `StaticProgressSwt.java`, emitted by
Synexia's same strict XHTML -> bounded UiBinder -> SWT Java pipeline. The
product result reuses SWT `ProgressBar`; it does not introduce a browser
renderer, duplicate selection model or Java/SWT adapter framework. Original
Nebula Grid test and snippets bundles also receive the generated native
progress factories. An added JUnit4 original-owner test checks `getMaximum`,
`getSelection`, owner, `web.field`, and shell disposal when native CI executes.
The currently unavailable hosted runners leave that native test unproven.
