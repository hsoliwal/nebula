<!-- SPDX-License-Identifier: Apache-2.0 -->
# Web ↔ SWT/Nebula and RAP/RWT: evidence-led receiving routes

**Product owner:** `hsoliwal/nebula`. **Recipe/convergence owner:**
`hsoliwal/com.synexia`. This work adds NO source-transformation recipes to
Nebula and does not change existing widget public APIs. The native toolkit
remains SWT/Nebula, not a second generic UI object hierarchy.

## Choose the least lossy route

1. **Direct native receiver (preferred).** A *bounded, admitted* HTML/JSX,
   UiBinder, Wicket/Facelets/Tapestry template or Java component tree is
   analyzed in the Synexia convergence workspace. Emit Java constructors,
   typed properties and listeners for the original SWT/Nebula owner directly:
   `Button`, `Text`, `CTabFolder`, `Grid`, `M3StackLayout`, etc.
   Use `SWT.VIRTUAL`, SetData and existing Grid/viewport controllers for
   large collections; never turn every row/cell into an SWT Control.
2. **RAP/RWT single-source route.** Where a component only uses the common
   SWT/RWT subset, compile/use that Java widget against RWT. Prefer the
   **existing RAP Nebula Grid bundle** for web data grids instead of copying
   desktop Grid's GC and DND internals. The RWT column already records
   unavailable classes/methods and offscreen-GC limitations.
3. **RWT Remote API native browser widget.** If a control needs high-frequency
   browser-specific gestures or drawing, write/port the client-side handler
   using RAP's public RemoteObject TypeHandler API; keep the native SWT
   widget implementation separately. Do not depend on private qooxdoo or
   internal `rwt` client classes.
4. **Opt-in SWT/RWT Browser fallback (implemented here).** For untranslatable
   JavaScript/custom elements, `M3BrowserWidget` wraps ONE `Browser`,
   delegates `click/input/change` from `data-nebula-id` to Java and
   applies up to 1024 text or form-value deltas in one evaluated script,
   capped by a 256-KiB generated-script transport budget.
   This is explicitly *not* a native SWT widget transpilation and not a
   substitute for the scalable original Grid/Canvas/virtual widget owners.

`WIDGET_ROUTES.tsv` enumerates native widget destinations, RWT availability
and acceptance gaps. `FRAMEWORK_ROUTES.tsv` distinguishes Java→JS
transpilers (GWT/J2CL/TeaVM), server-side component frameworks (JWt,
Wicket, MyFaces, Tapestry, Vaadin), templating/server layers (Spring
MVC/Thymeleaf) and AS3/MXML→JS (Apache Royale). **None of them by itself
transpiles arbitrary web widgets into SWT/Nebula.** A transformation must
separately prove semantics and cannot guess event or lifecycle behavior.

## Example: direct SWT receiver (preferred)

```java
// A compiler-generated native body for a static, admitted HTML form:
Composite content = new Composite(parent, SWT.NONE);
content.setLayout(new GridLayout(1, false));
Label heading = new Label(content, SWT.NONE);
heading.setText("Orders & shipping");
Button refresh = new Button(content, SWT.PUSH);
refresh.setText("Refresh orders");
refresh.addListener(SWT.Selection, event -> refreshOrders());

// For dynamic tab panels use M3StackLayout / M3PageProvider.
// For large tables use original Nebula Grid + SWT.VIRTUAL + SetData;
// never allocate one Composite per row or one Widget per cell.
```

This is an illustrative *receiver construction*, not proof of a complete
HTML/CSS DOM translator. See `m3/web-widget-reverse/examples/react-native`
for the source-sealed static React→existing SWT Canvas example and its
explicit gaps; see `docs/m3/rwt/RWT_COLUMN.tsv` for the real RWT census.

## Executable static JSX → original SWT controls (Synexia #13402)

The canonical first-party compiler now has a **second receiving route**
for its *bounded static* `M3VerticalPanel/M3Label/M3Button` grammar:

```text
Original Orders.jsx (source SHA-256 pinned)
  -> Microsoft TypeScript 5.8.3 real AST, existing inspectJsx
  -> Synexia native-static-to-swt.mjs (compile time only)
  -> generated OrdersNativeSwt.java
  -> original Composite/GridLayout + Label + Button (Nebula plugin)
  -> ordinary SWT and RWT API-subset compilation, independently qualified
```

Native product source:
`widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/generated/OrdersNativeSwt.java`.
It constructs **two Labels and two Buttons**, with `setData("m3.web.id",id)`
for stable source IDs; only the enabled button has a `Selection`
listener. All web `&` literals are translated to SWT `&&` mnemonic
escapes, including `&amp;` decoded by the canonical JSX entity rule.

```java
Composite form = OrdersNativeSwt.create(parent, SWT.NONE, id -> {
    if ("refresh".equals(id)) {
        refreshOrders();
    }
});
```

The *runtime* depends only on SWT, not on TypeScript, UiBinder, a
Browser, React, a Synexia runtime JAR or a new M3 UI object hierarchy.
For million-row data widgets, retain original Grid/Gallery/Tree
virtualization and viewport mediation; this static four-control form
does **not** imply that per-row controls are appropriate for data grids.

**Source recipe and negative tests:** [Synexia #13402](https://github.com/hsoliwal/com.synexia/pull/13402).
**Target review:** [Nebula #274](https://github.com/hsoliwal/nebula/pull/274).
Generated source SHA-256:
`e89d464a0c5590d08d7b12fad35d22f4a5c0423e742fac6e7ad107d1136fe7f0`.
Original JSX SHA-256:
`df940377e71bc653171be91778d2b21ae8c5f93623e755010826f59a28abb28f`.

The generated Java and six pre-existing Browser-helper regression methods
are in Nebula's M3 product/test plugin source trees. An additional
`OrdersNativeSwtTest` asserts real SWT control types, order, IDs,
enabled state and selection routing; it requires a real SWT display.
Headless synthetic Java11-signature compilation **passed locally**
and 5 generator test groups (including 512 static permutations)
**passed locally** against an equivalent reconstructed admission
gate. **Actual target Tycho/JUnit, real SWT and RWT are still NOT_RUN**.

The existing static JSX → immutable semantic rows → *one Canvas*
route is preserved untouched as an alternative. It is not an
arbitrary React/CSS/Shadow DOM compiler. Avoid claiming framework
equivalence until each framework's state, lifecycle, event, model,
security and accessibility contract passes its own gates.

For concrete framework-source symbols rather than generic categories,
see `SOURCE_AST_RECEIVERS.tsv`: GWT UiBinder, JWt, Apache Wicket,
Tapestry, MyFaces, Royale, Spring MVC/Thymeleaf/WebFlux and Vaadin
Flow, with explicit HOLD statuses wherever no implementation exists.


## Typed static DOM form → original SWT Text and CHECK controls

The additive second source-compiled receiver is
`widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/generated/ContactFormNativeSwt.java`.
Canonical build-time sources and tests remain in
[Synexia #13402](https://github.com/hsoliwal/com.synexia/pull/13402)
(`static-dom-form-to-swt.mjs`). The generated Java is compiled into
the **existing Nebula M3 SWT bundle** and has no runtime React, DOM,
TypeScript, or Synexia dependency.

For the **bounded admitted static JSX DOM grammar** (bare root
`<div>`, literal direct `span`, `button`, `input type=text` and
`input type=checkbox`), the actual Java code uses:

| Source | Native SWT result | Outgoing Java event |
| --- | --- | --- |
| Static `span` | `Label` | None |
| Static `button` | `Button(SWT.PUSH)` | `Selection` to `Consumer<String>` |
| `input type="text"` | `Text(SWT.SINGLE)` | `Modify` to `BiConsumer<String,String>` |
| `input type="checkbox"` | `Button(SWT.CHECK)` | `Selection` to checked-state callback |

The checkbox `label` attribute in this admitted JSX profile is a **build-time annotation** and does not replace HTML `<label for>` semantics; accessible labelling remains a qualification gate.

Initial text and selection are written **before** listeners
attach. Disabled controls do not get action listeners. Java
`Text` values keep literal ampersands; `Button` and `Label`
escape SWT mnemonic ampersands as `&&`. Stable original element IDs
are kept in `Control.setData("m3.web.id", id)`.

```java
Composite form = ContactFormNativeSwt.create(parent, SWT.NONE,
    actionId -> invokeAction(actionId),
    (inputId, value) -> onFormValueChanged(inputId, value));
```

The generated static fixture contains **five actual Controls**:
`Label`, `Text`, `Button(SWT.CHECK)`, and two
`Button(SWT.PUSH)`; there are no per-element wrappers beyond
these original controls and the one parent composite. This is not
an endorsement of one SWT control per logical row in a million-row
data table; use `SWT.VIRTUAL` and existing Grid/Tree viewports for
large collections.

**Source SHA-256:**
`c1b4118fa3656d28e307710bd97dbf009a388964d9d87db89e379a484f63893e`.
**Generated Java SHA-256:**
`2b0b6f1340d828fdc300d759406eae9e23ae6093e3cce8bf4089de5c9cfd7126`.

Local AST tests and Java11-signature compilation passed. A real
`ContactFormNativeSwtTest` is committed under the existing Nebula
tests bundle, but **full Tycho/native display, RAP 4.8.0 browser
execution, focus/IME/ARIA/RTL and pixel comparisons remain NOT_RUN**.
No claim is made for dynamic React hooks/CSS, HTML forms submitting
to a server, JSF/Wicket/Spring request behavior or arbitrary DOM.

### Framework-specific conversion is a separate semantic proof

Official Apache Wicket documentation separates cached markup from
per-request Java models and component resolution. An SWT receiver
cannot be generated correctly from Wicket HTML alone; both
Java component ownership and `wicket:id` resolution must be
accounted for. Wicket Ajax also requires the original render/visibility
and request lifecycle to be mapped, not merely button click handlers.

Likewise, Vaadin Flow/Grid and Spring services may feed a native
virtual Grid through stable IDs and pageable back-end queries, but
server push, UI-thread confinement, sorting/filtering,
`DataProvider` lifecycle, authentication and cancellation require
distinct qualified recipes. Apache Tapestry, MyFaces, JWt and Royale
remain inventoried **HOLD** sources, not implemented transpilers.

Sources: https://wicket.apache.org/docs/auto-components.html ,
https://wicket.apache.org/docs/how-ajax-works.html ,
https://vaadin.com/docs/latest/flow/binding-data/data-provider .

## Example: opt-in Browser fallback for dynamic web components

```java
M3BrowserWidget view = new M3BrowserWidget(parent, SWT.NONE);
view.addWebEventListener((type, id, value) -> {
    if ("click".equals(type) && "refresh".equals(id)) {
        refreshOrders();
    }
});
view.setTrustedHtml("<html><body>"
        + "<button data-nebula-id='refresh'>Refresh</button>"
        + "<span data-nebula-id='status'>Ready</span>"
        + "</body></html>");

// After isBridgeReady() is true:
view.setTexts(java.util.Map.of("status", "Loaded")); // one browser request
```

`setTrustedHtml` deliberately requires application-trusted documents.
It does **not** sanitize HTML, isolate hostile scripts, guarantee CSP,
authenticate RPCs or safely embed arbitrary third-party pages. Avoid
untrusted remote scripts and navigation. Post-load navigation is
denied in the fallback, including URL-changing SPA routers; choose a
separate vetted route when navigation is part of the contract.
`BrowserFunction` and
`Browser.evaluate` may each involve a network request in RAP.
JavaScript output is constructed with escaped string literals, an
exact-id attribute match and `textContent`/`value` (never
`innerHTML` or caller-provided executable expressions). The event
protocol is limited to three string fields and bounded lengths.
For a high-frequency input/graphics device choose RWT Remote API.

## Compatibility/proof gates

- **Source/rights:** retain origin, exact source owner, license, version,
  imported-vs-API-shape route, and fixed-point Synexia recipe evidence.
  JWt is GPL/commercial: API patterns only, not source copied.
- **Contract:** constructors/style flags, focus, modality, keyboard,
  IME, text and selection, listener add/remove/ordering, nested events,
  disposal, sessions, Unicode/RTL, validation, disabled state, ARIA,
  and client/server transport must be independently qualified.
- **Scale:** million-row/grid virtual SetData, stable identities, bounded
  visible DOM or Canvas damage, row overscan, thread ownership,
  CPU/heap/native memory and round-trip counts under scroll/resize.
- **Native acceptance:** SWT Win32/GTK/Cocoa screenshots with `GC(display).copyArea`,
  independent SWT/Nebula pixel gates, DPI/zoom, platform resources,
  and RAP 4.8.0 real-browser tests. A headless Java test does not
  establish UI equivalence.
- **Security:** HTML trust boundary, same-origin requirements, CSRF in
  server frameworks, XSS/CSP, upload permissions and browser navigation.
  Do not treat a BrowserFunction callback as an authentication scheme.

## Current proof state (2026-10-25)

The change implements the narrow Browser helper **and** the generated
original-SWT static Orders control factory; Java/JUnit test sources live
in Nebula's M3 Tycho product/test plugins. The local native-code
compilation used test-only SWT signatures, not a real UI session.
**Full Tycho/native SWT/RWT, platform pixels and third-party framework
conformance gates remain NOT_RUN.** No RWT custom-widget port or
arbitrary web-to-native compiler is claimed here.

### Existing receiver evidence

- Nebula #211 and #272: admitted static React JSX → semantic rows →
  original single SWT Canvas receiver, NOT arbitrary React hooks/CSS.
- Nebula #261: 66 cross-platform widget concepts and priority/ranking.
- Nebula #263: RWT 4.8.0 compatibility column, GC constraints and
  native RAP Grid route.
- Nebula #273: viewport consolidation, separate from this bridge.
- `m3/web-widget-reverse`: 45 family scaffolds and source provenance.

### Primary references

- RAP/RWT differences: https://eclipse.dev/rap/developers-guide/rwt.html
- RAP custom widgets and Browser: https://help.eclipse.org/latest/topic/org.eclipse.rap.doc/guide/articles/custom-widget.html
- RAP Browser embedding: https://help.eclipse.org/latest/topic/org.eclipse.rap.doc/guide/articles/web-embedding.html
- RAP incubator Nebula Grid: https://eclipse.dev/rap/incubator/
- JWt Java widget component model/license: https://www.webtoolkit.eu/jwt/latest/doc/userguide/userguide.html and https://www.webtoolkit.eu/jwt/download
- Wicket: https://wicket.apache.org/docs/why-wicket.html and https://wicket.apache.org/docs/how-ajax-works.html
- Apache Royale compiler: https://apache.github.io/royale-docs/compiler
- Spring MVC/Thymeleaf: https://docs.spring.io/spring-framework/reference/web/webmvc-view/mvc-thymeleaf.html
- Vaadin with Spring: https://vaadin.com/docs/latest/flow/integrations/spring
- TeaVM restrictions: https://teavm.org/docs/intro/overview.html
- Native Web Components: https://developer.mozilla.org/en-US/docs/Web/API/Web_components

## Select/option → one native SWT Combo (third receiver)

Canonical source transformation: [Synexia #13402](https://github.com/hsoliwal/com.synexia/pull/13402).
Original Nebula runtime receiver:
`org.eclipse.nebula.widgets.m3.generated.PreferencesNativeSwt`.
The static HTML-like JSX `<select>` and direct `<option>`
children produce **one** `Combo(SWT.READ_ONLY)`, never a new
`Control` for each option. Labels and values are kept separate
so Java callbacks retain the original `option.value`.

```java
Composite screen = PreferencesNativeSwt.create(parent, SWT.NONE,
    actionId -> handleAction(actionId),
    (widgetId, originalOptionValue) ->
        handleChangedValue(widgetId, originalOptionValue));
```

`SWT.Selection` looks up `Combo.getSelectionIndex()` in one
indexed `String[]` of source option values; it checks bounds
before dispatching. `setItems` and `select(initialIndex)`
run before listener registration. A static disabled `select`
has no listener. Explicit multiple selected options, `multiple`,
`optgroup`, per-option disabled state, arbitrary DOM attributes,
dynamic JSX and HTML form submission are refused—not approximated.

**Proof:** static source SHA-256
`c4901e803686d6a66df7cf1d5e41729474d2911ca74e022b5ed49c2dfc43fad1`,
generated original Java SHA-256
`fe54d1b0387b4c09526f51efb021f0c3711cabe339424cbf9e5cf0483655250c`.
The canonical generator passed Node/TypeScript source tests
(17 total across the selected suites, including 1024 selection
permutations) and Java11-target synthetic SWT API compilation
with action/change smoke. Old ContactForm receiver SHA remained
unchanged. Native `PreferencesNativeSwtTest` is committed
as an SWT display-backed qualification test, **NOT_RUN**.

**New hosted gate:** generated code compiled with `javac` against
real, SHA-256-verified RWT 4.8.0 JAR in the Synexia workflow.
That cross-compilation has not yet completed and cannot substitute
for RAP's live browser transport and actual SWT/Skija native pixels.
This API surface is appropriate for small, static choice menus.
Large/lazy collections should use original virtual Table/Grid,
not allocate a giant Combo or Controls for logical rows.

## Virtual web data tables: original SWT/RWT Table rather than per-row Controls

The fourth receiver in this PR is **`M3VirtualTableBinding`**, using
the existing `org.eclipse.swt.widgets.Table` with `SWT.VIRTUAL`.
It accepts a bounded, already-indexed Java row provider and populates
`TableItem` cells exclusively through `SWT.SetData`. Native Table
owns the viewport and scrollbar. There are no generated `Control`
objects per logical row or cell and no competing Nebula Grid state.

A typical web `DataTable`/Vaadin `Grid`/GWT `CellTable`/Spring
`Pageable` producer **requires its own typed source/model recipe**
before its data can supply this receiver. This change implements the
**product-side receiving contract only**, not those source compilers or
their session/transaction semantics. For rich spans/frozen columns,
retain the existing Nebula Grid (or RAP's separately ported Grid).

`M3VirtualTableBinding.Page` keeps row-major flat cell lanes,
unique keys per page, defensive copies and a strict memory budget.
A fixed four-slot LRU caches 64-row pages, even with 1,000,000
logical rows. Stable selection keys are retained across sort/filter
snapshots; `replaceRows` invalidates stale cache and re-resolves
visible selection, while `invalidate(first,last)` clears only
damaged rows. No per-row entry objects are used.

**Mandatory nonblocking rule:** `SWT.SetData` runs on the SWT/RWT
UI thread. Its provider must read an already-resident indexed
snapshot, not synchronously fetch from Wicket, Vaadin, Spring,
HTTP, SQL or remote services. Any background fetch must be
completed and safely published before the UI callback.

Detailed API example, limits and original framework contracts:
[`VIRTUAL_TABLE_BINDING.md`](VIRTUAL_TABLE_BINDING.md).
The native `M3VirtualTableBindingTest` and pure Java
`M3VirtualTablePageTest` have been added to Nebula's existing
JUnit test bundle. The original RWT 4.8 API compilation workflow
includes the binding source. **Full Tycho, Win32/GTK/Cocoa pixel
tests, RAP browser sessions and latency/resource acceptance are
still NOT_RUN; no successful real-RWT result is claimed.**
