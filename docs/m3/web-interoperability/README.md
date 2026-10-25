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
untrusted remote scripts and navigation. `BrowserFunction` and
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

The change implements the narrow Browser helper and headless script
contracts; its Java and JUnit test sources are in the active Nebula M3
Tycho plugin/test plugin. **Native SWT, RWT, platform, pixel and
third-party framework conformance gates remain NOT_RUN.** No RWT
widget-port or arbitrary web-to-native compiler is claimed here.

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
