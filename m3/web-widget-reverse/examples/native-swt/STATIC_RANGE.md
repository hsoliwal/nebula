<!-- SPDX-License-Identifier: Apache-2.0 -->
# Static range controls → original SWT/RWT Scale

This is **generated source**, not a new widget implementation. Canonical source
and the only compiler/execution recipe are in the Synexia Maven crate
`synexia-java-web-workbench/recipes/nebula-uibinder-swt-20261025`.

`StaticRange.xhtml` is a deliberately narrow static example. The generator
requires explicit `min`, `max`, and `value` as canonical nonnegative integers,
`min < max`, `value` within the range, and `step=1` or omitted. It creates one
original `org.eclipse.swt.widgets.Scale` per input with `SWT.HORIZONTAL`,
`setMaximum`, `setMinimum`, `setIncrement(1)`, `setSelection`, and optional
`setEnabled(false)`. It preserves the `web.field` identity; it does not create
per-value Controls, an HTML renderer, JavaScript bridge, or alternate state owner.

Only the initial numeric state and SWT API construction are admitted. HTML's
implicit range defaults, negative/decimal ranges, arbitrary steps, input event
semantics, labels/ARIA, CSS, form submission and accessibility equivalence are
not translated. Do not claim cross-platform interaction parity.

## Deterministic reproduction

With the Synexia Java21 crate compiled and in `$RECIPE_CLASSES`:

```sh
java -cp "$RECIPE_CLASSES" com.synexia.web.recipe.StaticXhtmlUiBinderFrontend   m3/web-widget-reverse/examples/native-swt/StaticRange.xhtml   m3/web-widget-reverse/examples/native-swt/StaticRangeSwt.java   org.eclipse.nebula.examples.web StaticRangeSwt

sha256sum --check --strict m3/web-widget-reverse/WEB_NATIVE_RECEIVER.sha256
```

The companion native Grid test fragment gets `StaticRangeNativeWebUiBinder`
from `NativeTychoReceiverRecipe.generateScaleFixture`, and the existing
`GridWebNativeMapping_Test` gets a seventh JUnit4 test, covering bounds,
selection, parentage, metadata, disabled state, and disposal. The original
Tycho test POM already selects this class by default.

Local evidence: 628 strict Java21 contract checks, Java11 source compilation
against explicit **test-only** SWT/JUnit4 API-shaped stubs, 38/38 checksum
validation. These checks do not establish actual SWT/RWT runtime, Tycho,
GTK/Win32/Cocoa pixels, event ordering, accessibility, or performance.
Keep the receiving PR as DRAFT until independently verified.
