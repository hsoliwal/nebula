<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native SWT output: bounded UiBinder source translation

The committed `OrdersSwt.java` is generated from the **existing**
`../react-static/Orders.ui.xml`; `NativeControlsSwt.java` is generated from
`NativeControls.ui.xml`. They are generated outputs, not independently edited
widget code. The generator belongs solely to Synexia, in Maven recipe crate
`synexia-java-web-workbench/recipes/nebula-uibinder-swt-20261025`.

Generation command (JDK 21; `$RECIPE_CLASSES` is the compiled Synexia recipe):

```sh
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.UiBinderSwtRecipe \
  ../react-static/Orders.ui.xml OrdersSwt.java org.eclipse.nebula.examples.web OrdersSwt
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.UiBinderSwtRecipe \
  NativeControls.ui.xml NativeControlsSwt.java org.eclipse.nebula.examples.web NativeControlsSwt
```

`create(Composite parent)` must run on the owning Display thread. It returns a
normal SWT Composite. The caller retains the owner and disposes it on the
usual SWT lifecycle; these generated classes manage no background thread,
HTTP session, native allocation or secondary collection of controls.

`OrdersSwt` renders two SWT Labels and two SWT Buttons; the disabled Cancel
state and decoded `&amp;` text are retained. `NativeControlsSwt` shows native
SWT Text and CheckBox plus a **real Nebula Grid**, two GridColumns and two
GridItems. The static Grid cells do **not** each allocate a Control.

This source is deliberately outside the active Tycho bundle. To execute in an
Eclipse SWT/Nebula application, compile it against SWT and the Nebula Grid
bundle and create it on a Display thread. Local proof used Java 11 API-shaped
stubs, not actual SWT jars or real RAP/Grid running sessions. No implicit
claim of native, RAP, callback or visual equivalence is made.

See [`../../WEB_NATIVE_MAPPING.tsv`](../../WEB_NATIVE_MAPPING.tsv) for native
receiver names and explicitly unimplemented Wicket/JWt/Flow/HTML mappings.
