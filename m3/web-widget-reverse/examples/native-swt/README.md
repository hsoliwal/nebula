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
```

The public original-repo `org.eclipse.nebula.snippets.grid` also receives two
generated sources: `NativeControlsWebSwt.java` and `NativeWebControlsSnippet.java`.
The latter owns the SWT Display/Shell event loop. This makes the generated
control hierarchy a **runnable plugin snippet candidate** (not a mock-API
example), pending native plugin compile and actual SWT display tests.

The Synexia receipt contains exact SHA-256 original/fixture/generated source
identities and refusal/fixed-point tests. Native construction must run on the
owning Display thread and the parent Shell must be disposed normally. No
background worker, M3 runtime wrapper, control per Grid cell or native JNI
resource allocation is introduced by the generated Java.

Local 98-contract test evidence includes Java 11 strict source compilation
**against stubs**, not real installed original SWT/Nebula classes. Tycho,
OSGi, RAP/RWT, visual/pixel, keyboard/IME, accessibility and benchmarks are
explicitly NOT_RUN. Keep receiver draft and separately qualify the native
plugin, screenshot and resource gates before release.
