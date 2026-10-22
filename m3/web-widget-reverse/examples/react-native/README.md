<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula example: static React JSX translated by Synexia OpenRewrite

This example is the **output**, not an independently maintained Java widget
implementation. Its canonical input is the existing
`../react-static/Orders.jsx`, originally admitted through the bounded
`StaticReactJsxFrontend` and `UiBinderStaticWidgetCompiler` in Synexia.

The source in `OrdersScreen.java` is the precise result of
`com.synexia.rewrite.M3NebulaJsxToSwt20261022` (Synexia
`recipes/m3-nebula-jsx-to-swt-20261022`). Its Git blob is
`3d48af56dd5244039e98e8246f0acc9083cf971f`.
Input JSX, UiBinder and the unchanged compiler owner are source-pinned in
Synexia's `SOURCE_DONORS.tsv`; original Java before/after SHA-256 is sealed
in the OpenRewrite receipt.

Translated API:

```java
public static org.eclipse.swt.widgets.Control createSwt(
        org.eclipse.swt.widgets.Composite parent, int style) {
    return new com.synexia.mindex.viewer.swt.M3NebulaWebScreen(parent, style, build());
}
```

This calls the **existing** `M3NebulaWebScreen` (a single original SWT
`Canvas`), with the same immutable `M3WebWidgetScreen` rows originally
generated for web. It does not create one SWT Control per React child, own
selection/scroll state separately, or rewrite any public Nebula widget API.
The source lives under an *example* folder, not the active Nebula Tycho
plugin source; compiling it as a native example requires the original
Synexia MIndex viewer and SWT dependencies.

This is a deliberately bounded **static** JSX translation, not a
transpiler for arbitrary React hooks, effects, CSS, JSX expressions or
event handlers. Those inputs are refused by the original frontend and
remain on the separately opt-in `../react-live` Browser presentation route.

The original frontend/compiler and OpenRewrite exact-rewrite/fixed-point
jobs run in the linked Synexia PR. Native Win32/GTK/Cocoa pixels,
RAP/RWT execution, GPU/theme/locale/RTL and full widget contract
verification remain **NOT_RUN**, not completed by source generation.
