<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Proposal: cross-framework web semantics into original SWT/Nebula widgets

**Status: DESIGN_ONLY / NOT_RUN / NO REMOTE MUTATION.**

## Repository and source guards

- Nebula target `hsoliwal/nebula`, branch `master`, inspected commit `1164a567bb3df8e7a9ba8a72c5b02765324152cf` (2026-10-23).
- Original `m3/web-widget-reverse/README.md` Git blob `d5f3bb068c9f4f68c98c595943530bdcf50c9949`; do not rebase or force-push.
- Preserve `AGENTS.md`: Synexia owns Maven/OpenRewrite recipes and transformation memory; Nebula is the receiver, where original SWT/Nebula and Tycho/native tests arbitrate admission.
- Existing `WEB_COMPONENT_SOURCES.tsv`: 369 GWT/RWT *source entries*, not 369 implemented widget conversions. Existing family scaffolds: 45. Existing `docs/m3/rwt/RWT_COLUMN.tsv`: 68 concept routes. These source tables are not replaced by this proposal.
- Reuse `StaticReactJsxFrontend`, TypeScript 5.8.3 AST syntax gate, `UiBinderInventory`, `UiBinderStaticWidgetCompiler`, `M3WebWidgetScreen`, `M3NebulaWebScreen` and `M3NebulaJsxToSwt20261022` OpenRewrite. No second JSX parser, widget tree, model or Canvas owner.

## Actual donor categories (do not conflate forward compilation with reverse translation)

`FRAMEWORK_DONORS_20261025.tsv` distinguishes forward Java→browser transpilers (GWT/J2CL/TeaVM), SWT-compatible web API (RAP/RWT), server-side component renderers (JWt/Vaadin/Wicket/Tapestry/MyFaces), source/template compilers (Royale/Thymeleaf), native UI grammar (Apache Pivot/WindowBuilder) and Spring MVC's HTTP presentation lifecycle.

A forward compiler proves only its *own* forward semantics. Reverse translation is permitted only when original widget identity, properties, layout, model effects, event sequencing and disposal are admitted individually. Generic DOM/HTML is insufficient to recover arbitrary React hooks, CSS layout, browser permissions, renderer op-state or asynchronous effects.

JWt (`emweb/jwt`) has GPL/commercial terms. Use only public shape/API patterns, do not copy its implementation into the Nebula Eclipse/Apache-license distribution. For Apache MyFaces and Tobago, the GitHub repository metadata did not resolve SPDX automatically: perform per-file notice/licence verification before adaptation. Vaadin core Flow and individual components need separate entitlement checks.

## Original-owner mapping rules

1. Source ASTs stay with their appropriate already-admitted parser: TypeScript for TSX, Java/OpenRewrite for generated Java, secure XML parsers for XML/UiBinder/TML/Facelets/BXML, and parsers specific to frameworks when needed. Never parse nested component languages with substring regex.
2. Source identity is `(repository, exact commit, path, Git blob SHA, SPDX route)`. Semantic receiver identity is `(original Java type, original parent/style constructor, public signatures, original event/model/viewport owner)`. The mapping table is *metadata*, not a new runtime intermediate representation or a synthetic widget facade.
3. Admission operates by a **typed semantic footprint**: literal content, explicit role/type, enabled/checked/selected state, container hierarchy, model reference, handler/effect category, sizing, typography, accessibility and disposal. A missing required footprint bit yields HOLD, never a guessed default.
4. Receiver classification is conditional: `STATIC_TEXT`, `STATIC_DISABLED_ONLY`, `STATIC_STATE_ONLY`, `DECLARATIVE_CONDITIONAL`, `VIRTUAL_COLLECTION_CONDITIONAL`, `PAINT_OPERATION_GATED`, `BROWSER_OPT_IN_ONLY`, `HOLD_DYNAMIC`, `BLOCKED`. Every table row is a candidate with `NOT_RUN`; no row grants an implementation claim.
5. The **existing widget owns** Control/Canvas, GC, Display/UI-session access, ScrollBar, focus, hit tests, selection and disposal. CPU-owned primitive arrays / indices / masks / pages own model, sorting, filtering, stable IDs, extents and residency. Reuse `GridViewport`, `M3StackLayout`/`M3PageProvider`, `RwtPageWindow` and other original-source mechanisms after the actual dependencies are qualified. Never allocate one SWT Control for a logical row/cell/character.
6. Present only `visible-viewport ∩ damage ∩ not-occluded`. Empty intersection produces zero paint work. On RAP, use stock virtual rows or RAP's Nebula Grid port when equivalent; otherwise use a qualified `Canvas` GC display list and RemoteObject, never send an unbounded DOM row tree.
7. Do not map an arbitrary browser canvas/WebGL draw to SWT GC until exact operation/state sequencing is supported. RWT offscreen GC, Pattern alpha, XOR, interception of input events, device scaling and client GPU semantics have documented gaps; refuse rather than invent equivalence.
8. Explicitly differentiate SWT native selection/order/Verify/Focus/IME from browser capture/bubble/default actions. A static `button` maps its presentation and disabled state only; **event parity is NOT implied**.
9. For templates and server components, preserve binding scope, validation, request/session state, security context and event lifecycle. Spring MVC and Thymeleaf are not SWT widget transpilers. JWt's component renderer is not a license-compatible source donor.
10. Original API and ABI are locked. New M3 helper types remain internal; recipients use native SWT/Nebula package names and public contracts. No runtime Synexia recipe dependency in published product.

## Serial implementation programme (not executed)

**L0 inventory:** Verify input Git blobs, exact parser/tool versions, upstream license files, original Java methods/events, existing RWT compatibility defects and earlier PR heads. Export provenance TSVs and byte counts. New source paths must be genuinely absent.

**L1 static semantic mapping:** Extend the *existing* Synexia AST + UiBinder admission with literal Wicket/Tapestry/MyFaces/Vaadin/JWt *shape catalogues* as independent grammar profiles, not by passing arbitrary Java component code through the JSX parser. Reuse original Java OpenRewrite AST transformations; qualify static labels, buttons, checkboxes and form structure before any dynamic behavior. Keep original example and golden output unchanged.

**L2 state/viewports:** Drive Nebula Grid, Gallery, XViewer and M3StackLayout with stable row IDs, page providers, damage and original row pool; compare native selection/scroll/identity and RWT protocol update counts. Do not change protected/public methods or model ownership.

**L3 effects and painting:** One recipe per event family or original widget leaf. Qualify parent/child ownership, RWT operation protocol, callbacks and disposal under actual RAP sessions before promotion. Dynamic React, asynchronous component lifecycles, Canvas, WebGL and unsafe HTML remain separate opt-in or HOLD routes.

**L4 qualification and publication:** `inventory → atomization → patternization → docs → fixed-point → patch → lint → compile → tests → runtime` in one deterministic pipeline; capture exact pre/post SHA-256, recipe manifest, logs and failures. Use actual native screen capture and pixel/resource tests on SWT Win32/GTK/Cocoa and real RWT/browser sessions. Review source and license, then raise a PR; do not automatically merge.

## Required proof gates

| Gate | Acceptance invariant | Present status |
|---|---|---|
| G00 | All donor commits and product source Git blobs pinned, licenses classified | PARTIAL_INPUT_EVIDENCE_ONLY |
| G01 | Typed frontend refuses unsupported events, CSS, templates, dynamic expressions | NOT_RUN |
| G02 | Original model/receiver public signatures unchanged, ABI witness passes | NOT_RUN |
| G03 | Original row/selection/focus IDs stable under reorder/filter/reparent | NOT_RUN |
| G04 | Effective visible damage only, zero Control-per-logical-row allocation | NOT_RUN |
| G05 | RWT real-session rendering, dispose/session isolation and message diff | NOT_RUN |
| G06 | Actual native Win32/GTK/Cocoa SWT pixel/resource and JNI checks | NOT_RUN |
| G07 | Two OpenRewrite replays yield identical source SHA-256; foreign drift refused | NOT_RUN |
| G08 | Nebula original Tycho build/tests and source-aware diff review pass | NOT_RUN |

## Blocking examples (deliberate refusal)

- `<button onClick={save}>`: handler closure effects/event lifecycle unproved.
- `<div style={{display:'grid',gridTemplateColumns:'repeat(auto-fit,1fr)'}}>`: responsive CSS constraint solution not proven against `GridLayout`.
- `<canvas>` with arbitrary JS/WebGL methods: no finite, admitted SWT/RWT GC opcode proof.
- `contenteditable` / browser file upload: security/IME/resource permissions differ.
- Form inputs with dynamic validation or asynchronous state: a static control declaration is not a behavioral equivalent.

**No repository code, tests, or GitHub PR was modified by authoring this candidate specification.**
