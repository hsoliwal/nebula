<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Web widget admission: Java declarations toward intrinsic SWT/Nebula

This optional Java 21 module is an **exact-symbol, effect-aware review gate** for
the existing `m3/web-widget-reverse` programme. It does not install a second
widget runtime, parse arbitrary HTML/JSX/Java, or allocate a Control for every
logical row. Original SWT and Nebula widgets retain their viewport, renderer,
item identity, selection, event ordering, resources and disposal.

The existing canonical Synexia `UiBinderInventory` / `UiBinderStaticWidgetCompiler`
and `M3NebulaJsxToSwt20261022` OpenRewrite source receiver remain the compilation
owners. `WebWidgetAdmission` supplies an earlier, read-only classification of
an attributed declaration reported by a framework-specific source frontend.
Its `g:Label`/`g:Button` tags identify an eligible **static UiBinder shape**,
not executable Java or an equivalence guarantee.

## Current supported review surface

| Donor family | Structural candidates | Explicit holds |
| --- | --- | --- |
| RAP/RWT | `Label`, `Button` | `Table`; RWT session and event protocol |
| GWT | `Label`, `Button` | `CellTable`, deferred binding, JS effects |
| JWt (`JWT`) | `WText`, `WPushButton` | `WTable`, markup, browser/session effects; GPL/commercial donor is reference-only |
| Apache Wicket | `Label` literal shape | `Button` submission, `ListView`, model binding and callbacks |
| Apache Tapestry | Symbol references only | `Label` field association, `Submit` form semantics, `Loop` iteration/state |
| Spring MVC / Thymeleaf | static `html:label`, `html:button` | `th:*` template expressions, controllers and model bindings |
| Vaadin Flow | `Span`, `Button` | `Grid`, server-side synchronization and client events |

`STATIC_SHAPE_CANDIDATE` means only that the exact source symbol has a
plausible literal visual projection. The source frontend must still prove the
**absence of every relevant dynamic effect** before the existing typed compiler
may consume it. In particular, framework defaults such as Wicket submission,
Tapestry field association, JWt markup and Vaadin sessions need separate
source- and runtime-bound proofs; this table never claims their equivalence.

`HOLD_REQUIRED_SEMANTICS` also blocks Wicket's form `Button` and Tapestry's
field-associated `Label`/form `Submit` even with a zero effect mask: their
source-class contracts themselves imply behavior that a static UiBinder
label/button cannot represent.

`HOLD_INTRINSIC_VIEWPORT` identifies a collection whose selection, lazy
data, item identity, geometry, scroll and paint behavior must be implemented
inside the original `Table`/Nebula `Grid` owner. It cannot become a list of
independent SWT child Controls. `HOLD_RUNTIME_EFFECTS` retains a supplied
bit-mask signal for bindings, handlers, browser APIs, sessions, layout,
virtual items and accessibility. Unknown flags are also held. Unknown
source symbols fail closed as `HOLD_UNSUPPORTED_SOURCE`.

## Example API (metadata only)

```java
var decision = WebWidgetAdmission.classify(
    WebWidgetAdmission.Framework.GWT,
    "com.google.gwt.user.client.ui.Label",
    0);
assert decision.status() == WebWidgetAdmission.Status.STATIC_SHAPE_CANDIDATE;
assert decision.needsQualification();

// Unqualified dynamic behavior is retained as a hold, never discarded:
var held = WebWidgetAdmission.classify(
    WebWidgetAdmission.Framework.VAADIN_FLOW,
    "com.vaadin.flow.component.button.Button",
    WebWidgetAdmission.EVENT_CALLBACK | WebWidgetAdmission.SESSION_STATE);
assert held.status() == WebWidgetAdmission.Status.HOLD_RUNTIME_EFFECTS;
```

The classifier accepts already-inventoried exact names and a full signal
mask; it **does not infer** effects from the short class name. A real importer
must use the donor's AST/component model and refuse incomplete effect coverage.
No JNI acceleration is appropriate for this small cold metadata lookup: Java's
fixed, binary-searched table avoids native transition and ownership costs.

## Proof and remaining gates

Run `mvn -B -ntp test` with Java 21 in this directory. Focused JUnit covers
every listed source shape and intrinsic-viewport hold, each effect bit plus
unknown bits, malformed/foreign sources, and deterministic concurrent reads.
This module is not included in the original Nebula Tycho reactor.

- SOURCE/COMPILER: focused Java 21 JUnit is a classifier check only.
- OPENREWRITE: canonical versioned Synexia recipe execution, drift refusal,
  exact receiving SHA-256 and fixed-point replay are separately mandatory.
- RUNTIME: actual RAP/RWT sessions, Wicket/Tapestry/Spring/Vaadin controllers,
  GWT/TeaVM/J2CL compilation, real SWT pixels, accessibility, IME,
  Win32/GTK/Cocoa and original Nebula widget contracts remain **NOT_RUN**.
- COVERAGE: only the enumerated symbols, not the existing 45 Nebula families,
  are classified by this first pass. No general reverse transpiler exists.

See `FRAMEWORK_GATES.tsv` for capability classifications and rights decisions.
