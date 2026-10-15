<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 React-static Orders → existing SWT/Nebula viewport example

This is a **bounded example and compiler fixture**, not a new native Nebula Control and not a promise that arbitrary React source compiles to SWT. The original SWT/Nebula widget retains ownership of viewport, data, selection, accessibility, event sequencing, resource lifecycle and public API.

Canonical recipe: [Synexia #11601](https://github.com/hsoliwal/com.synexia/pull/11601), stacked on the [typed UiBinder → Java/web compiler #11593](https://github.com/hsoliwal/com.synexia/pull/11593), which relies on UiBinder inventory #11552 and original 45-family scaffold #11538. Nebula master base `9943511a112e25c9fdaeb1e92442674fa0d19443`.

## Supported source
`Orders.jsx` uses a deliberately named M3 JSX declaration subset: one `M3VerticalPanel`, direct self-closing `M3Label` and `M3Button` leaves with stable IDs and static text, and literal disabled flags. It has NO JavaScript state/hook/onClick/style behavior. Unsupported/dynamic React source is **refused**, not guessed. These `M3*` names are compile-time semantic declarations and require separately validated actual React component definitions to run in React.

## Compiler path (on admitted Synexia branch)
```sh
bash synexia-openrewrite-recipes/recipes/web-widgets-react-static-20261015/run-project-proof.sh
```
That runner compiles the **existing** `UiBinderInventory` and `UiBinderStaticWidgetCompiler` (unchanged) with the new source frontend and runs an Orders example:
```
StaticReactJsxFrontend Orders.jsx OrdersScreen <out>
javac ... <out>/OrdersScreen.java
java com.synexia.mindex.viewer.web.generated.OrdersScreen <out>/web
```
Actual emitted `Orders.ui.xml` is included as a byte-exact sample. Generated Java uses the existing `M3WebWidgetScreen` and `M3WebCompiler.write` to emit `index.html`, `widget.css`, `widget.js`. The Java-owned `MIndexViewPort` and original SWT Canvas stay the geometry/renderer authorities; no new Item or web widget lifecycle is created by this metadata example.

## Verification and contracts
Local Java21 standalone source parsing (test-only UiBinder stubs) PASS 16,034 assertions. Separate test-only C17 JNI UTF-16 scalar model PASS 36,007 comparisons. Exact real UiBinder/Java/JS hosted build remains a separate gate. **NOT_PROVEN:** GWT Java→JavaScript compiler, RWT/RAP server lifecycle, actual React runtime/reconciliation, SWT/Nebula native/Skija/GC pixels, APIs/SetData/events/accessibility, full Win32/GTK/Cocoa/GWT or all 45 families. Do not promote this example to complete product behavior.

No React/GWT/RAP donor code copied; original EPL-2.0 SWT/Nebula widget source and all existing license notices are unchanged. These example files are first-party Apache-2.0.
