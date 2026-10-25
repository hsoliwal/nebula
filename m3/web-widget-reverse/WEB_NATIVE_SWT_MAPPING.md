<!-- SPDX-License-Identifier: Apache-2.0 -->
# Web semantics into existing SWT / Nebula Java owners

This is a **compile-time, source-only receiving pass**, not a universal web-to-SWT
transpiler, runtime facade, SWT fork or new viewer hierarchy. The recipe **and
all source transformations** live exclusively in
`hsoliwal/com.synexia/synexia-java-web-workbench/recipes/nebula-uibinder-swt-20261025`.
Nebula receives the exact emitted, inspectable Java and static UiBinder fixtures
under [`examples/native-swt`](examples/native-swt/README.md).

The map in [`WEB_NATIVE_MAPPING.tsv`](WEB_NATIVE_MAPPING.tsv) distinguishes
**executed static transformations** from mere donor *semantic candidates*.
The implemented subset consumes GWT UiBinder panel, Label, Button, TextBox and
CheckBox declarations plus explicit, namespace-scoped Nebula static Grid,
Column, Row and Cell declarations. Output is normal `Composite`, `GridLayout`,
`Label`, `Button`, `Text`, `Grid`, `GridColumn` and `GridItem` Java, with no
`Browser`, DOM engine, per-cell `Control`, JNI allocation or Synexia runtime API.
Original Eclipse/Nebula owner names, methods, typed layouts and SWT lifecycle
are retained. Generated examples are **outside the compiled plugin source**;
no existing Nebula public Java API or Tycho bundle is modified by this pass.

The source recipe refuses DTD/entities, executable markup, unrecognized
attributes/elements and namespaces, HTML/JS/JSX, event handlers, CSS, model
bindings, arbitrary expressions, non-boolean flags, duplicate field keys,
unpaired surrogate literals, excessive depth/rows/columns and destination
drift. Layout and Grid data are static, limited to 256 nodes, 12 nested levels,
32 columns, 64 rows, input at most 64 KiB. No implicit fallback to `Browser`.
`ui:field` becomes `Widget.setData("web.field", ...)` metadata, not a live GWT
binding. The SWT `Display` thread owns construction and disposal; there is no
background UI access. Logical Grid cells use `GridItem.setText`, not SWT Controls.

## Existing web rendering/forward paths retained

- **RAP/RWT:** reuse SWT-compatible `Composite`, native `Button`, `Text` and the
  already-shipped RAP Nebula Grid port. The earlier RWT 4.8.0 differential
  compile census and missing GC/event semantics remain in `docs/m3/rwt/`.
- **GWT/J2CL:** Java-to-browser compilation, not the reverse mapping. A
  statically admitted UiBinder subset has Java-native construction here.
- **JWt:** widget-tree/event concepts are donors; its `WWidget` object model is
  not binary-compatible with SWT and no JWt frontend is implemented.
- **Wicket, Vaadin Flow, Apache Pivot, Spring/Thymeleaf:** reusable semantic
  concepts only; request/model/validation/session contracts are NOT converted.
- **Existing JSX:** the earlier Synexia source-sealed `OrdersScreen.java`
  creates a single SWT Canvas from an immutable screen. The native SWT pass is
  an **alternative explicit output** for a static UiBinder, not a change to
  that verified receiver or a second Canvas engine.

## Evidence and boundaries

Local Java 21 `javac --release 21 -Xlint:all -Werror` compiled the recipe
and its standalone test source. **37 assertion/checks passed** including fixture
replay/fixed-point, bad-input refusal, Unicode, strict `--release 11 -Werror`
compilation against *API-shaped test stubs*, and a headless stub-instance
behavior smoke. Running the generator on the original Orders UiBinder and
new Grid fixture produced the committed examples byte-for-byte.

This is **not** an execution of the full Synexia OpenRewrite SDK, Maven,
Nebula Tycho reactor, real SWT jars, GTK/Win32/Cocoa native widget scenes,
RAP browser/RWT sessions, screen pixels, accessibility or performance gates.
The method-shape stubs are not a substitute for the original bundle tests.
In particular, JSP/Thymeleaf bindings, Wicket actions, JWt signals, Flow
client-server state, JS input dispatch and GWT click handlers are not portable
without an explicit semantic lowering and tests.

Design and provenance donors: Eclipse RAP/RWT developers guide and Nebula Grid,
GWT UiBinder patterns, JWt, Wicket, Vaadin Flow, Spring MVC and WindowBuilder
construction descriptors. **No third-party Java source was copied**. Preserve
upstream EPL notices and licenses on original widget implementations; new
recipes, fixtures, source outputs and mapping metadata are Apache-2.0.
