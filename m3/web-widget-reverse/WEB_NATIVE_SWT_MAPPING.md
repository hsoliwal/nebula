<!-- SPDX-License-Identifier: Apache-2.0 -->
# Web components into intrinsic SWT and Nebula Java — bounded receiver

The canonical source compiler and proof belong **only** in
[`hsoliwal/com.synexia` PR #13396](https://github.com/hsoliwal/com.synexia/pull/13396),
`/synexia-java-web-workbench/recipes/nebula-uibinder-swt-20261025/`.
This repository receives the **generated Java results, static input fixtures,
source fingerprints, runnable snippet, and the explicit semantic mapping**.
No recipe implementation, new general-purpose widget facade, Browser, JNI or
per-cell SWT `Control` is introduced into the Nebula product.

## Two source frontends, one native emitter

```
GWT UiBinder (static Java/XML) ────────┐
                                      ├── GWT UiBinder subset ── UiBinderSwtRecipe
Strict XML XHTML (static only) ────────┘                        │
                                                                └── existing SWT Composite,
                                                                    Label, Button, Text,
                                                                    Combo, GridLayout,
                                                                    Nebula Grid,
                                                                    GridColumn, GridItem
```

**UiBinder:** `VerticalPanel`, `HorizontalPanel`, `Label`, `Button`,
`TextBox`, `TextArea`, `PasswordTextBox`, `CheckBox`, `ListBox` with literal
`Item` labels, plus an explicit Nebula XML namespace for `Grid`, `Column`,
`Row`, `Cell`. **XHTML:** namespace-true, well-formed XML only; static `html`
`body`, `main`, `section`, `div`, `article`, `p`, `span`, `label`, simple headings,
`button type=button`, `input type=text|checkbox|password`, `textarea`,
`select/option`, and a strictly rectangular `table` with `thead/tbody`.
These are intentionally **presentational mappings**, not full GWT or HTML
semantic equivalence. Even accepted headings lose heading rank, label/for
association is refused, and native layout is not CSS box layout.

**Mnemonic correction:** SWT `Label`/`Button` interpret `&` as a keyboard
shortcut prefix. The recipe doubles each literal web `&` for those owners so
`Orders &amp; shipping` displays with its ampersand. Plain `Text`, `Combo`
items and `GridItem` cells do not get mnemonic conversion. A multiline
single-line text field or button is rejected instead of invoking
platform-dependent rendering. SWT `TextArea` maps to `SWT.MULTI`.

**Fail-closed boundary:** DTD/XInclude, arbitrary HTML, JavaScript, CSS, Wicket
`wicket:id`, Thymeleaf `th:text`/`th:field`, forms/submit/reset, browser navigation,
ARIA associations, dynamic binding, event handlers, default password strings,
checkbox form values, option values, select multiple, optgroups, grouped radio
buttons, row/column spans, nested markup in plain text, and unsupported XML
attributes are refused. XHTML HTML-boolean attributes are recognized by
*presence* (`disabled="disabled"`), not wrongly mapped from the string
`"false"`. No JS injection, `innerHTML`, DOM renderer or fallback `Browser`.

The generator caps source at 64 KiB, depth 12, nodes 256, grid columns 32,
static rows 64 and Combo items 64. `ui:field` / admitted XHTML `id` is preserved
as `Control.setData("web.field", ...)`; it is metadata **not** a live binding.
Grid presents one `GridItem` per static data row rather than one heavyweight
SWT Control per cell. The grid gets a standard fill `GridData`; it retains the
original Nebula Grid's own model, painting, selection and scroll owner.

## Native receiving locations

- [`examples/native-swt/`](examples/native-swt/README.md): input fixtures and
  precisely generated `OrdersSwt`, `NativeControlsSwt`, `StaticOrdersSwt`.
- [`examples/org.eclipse.nebula.snippets/.../grid/`](../../examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/grid/):
  generated `NativeControlsWebSwt` factory plus `NativeWebControlsSnippet`
  owning SWT `Display`, `Shell` and event loop. This is a runnable PDE/OSGi
  snippet **candidate**, using original `org.eclipse.nebula.widgets.grid`.
- [`WEB_NATIVE_MAPPING.tsv`](WEB_NATIVE_MAPPING.tsv) and
  [`WEB_FRAMEWORK_CONTRACTS.tsv`](WEB_FRAMEWORK_CONTRACTS.tsv): exact static
  mappings versus future framework-level semantic work.

## Framework boundaries and forward transpilers

- **RAP/RWT:** a Java/SWT-compatible web projection. Existing RAP Nebula Grid
  already exists; reuse it. The actual RAP session, RWT input protocol, layout,
  client painting, loss of certain GC operations and Grid port compatibility
  remain independently gated (see `docs/m3/rwt/`).
- **GWT/J2CL:** Java-to-browser compilation, distinct from this static reverse
  XML-to-Java generation. No client event handler or GWT runtime is ported.
- **JWt/JWT and Vaadin Flow:** Java-owned component trees with server/client
  state and event queues, not binary-compatible SWT widgets. Event order,
  sessions and state synchronization need contract-specific tests.
- **Apache Wicket and MyFaces:** component lifecycles, conversions, validation,
  request phases and model updates must not be silently reduced to `setText`.
- **Apache Tapestry and Royale:** render phases, AS3/MXML/CSS compiler ASTs,
  browser/JS target behavior are **architecture donors**, not admitted reverse
  converters.
- **Apache Pivot:** desktop Java/BXML widgets have their own skin and event
  contracts, requiring an independent frontend before mapping to SWT.
- **Spring MVC/Thymeleaf/Web Flow:** static XHTML presentation without `th:*`
  bindings is admitted; controller, session, flow, validation and security
  remain out of scope.

Only new first-party recipe/generated sources are Apache-2.0. Original
Eclipse/Nebula/RAP code retains upstream EPL notices; no Wicket, JWt, Spring,
Vaadin, Pivot, Royale or Tapestry implementation is copied.

## Qualification, not assumptions

**Locally executed:** Java 21 `javac --release 21 -Xlint:all -Werror`
compiler and test sources, 98 strict contract/fingerprint checks, generated
Java `javac --release 11 -Xlint:all -Werror` using **API-shaped stubs**, and
exact SHA-256 replay of three inputs/five product outputs. The generated snippet
compiles against these test stubs. That is not original SWT or RAP compilation.

**Not executed:** hosted Maven/CI success, exact native Nebula Tycho/PDE reactor,
GTK/Win32/Cocoa SWT widget creation and pixels, RWT session/DOM/event replay,
platform event/IME, visual theme/font/RTL, accessibility/ARIA parity, memory,
performance, resource disposal and M3JDK21 native integration. The screenshot,
GC/resource and CI gate authorities stay with SWT/Nebula/RAP; do not mark
unrun gates PASSED or merge the receiving PR without their evidence.

### XHTML multiline data correction

The source generator retains XHTML `textarea` LF, CR and TAB characters when
creating the intermediate UiBinder XML attributes using XML numeric character
references. It does not replace them with spaces; the generated multiline SWT
`Text.setText` now preserves the source characters. This correction was replayed
through the canonical Synexia generator and tracked with a new SHA-256 receipt.
Other HTML whitespace/CSS layout and semantics remain explicitly unqualified.
