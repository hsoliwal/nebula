<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Source-sealed static web-widget declarations to existing UiBinder

This is the **second, additive** compile-time pass on top of `WebWidgetAdmission`.
It generates *candidate XML*, never an SWT Control, JavaScript adapter, native
renderer, new viewport or event model. `WebWidgetAdmission` is unchanged. The
original Synexia `UiBinderInventory` and `UiBinderStaticWidgetCompiler` remain
the authoritative typed XML and Java generation owners; original SWT/Nebula
widgets retain viewport, item identity, selection, painting and disposal.

## Exact contract

`WebWidgetUiBinderProjection.project(receipt, sourceUtf8, declarations)`:

1. Requires a canonical relative source path and a lowercase 64-digit SHA-256
   receipt; verifies the digest against the supplied actual valid UTF-8 bytes.
   Bytes are never evaluated or rewritten, and do **not** prove that a caller's
   declaration inventory completely describes the original AST.
2. Requires each source declaration to match an **exact** `WebWidgetAdmission`
   symbol with status `STATIC_SHAPE_CANDIDATE` and an all-clear effect mask.
   Source parsers must independently prove their symbol resolution and full
   effect inventory before supplying declarations. A zero mask supplied by an
   untrusted caller is NOT proof of absent effects.
3. Validates unique Java-safe `ui:field` IDs, XML-compatible literal Unicode,
   stable visible whitespace, label/button disabled semantics, source and row
   budgets. Unknown types, grids/virtual items, Wicket/Tapestry form behaviors,
   arbitrary callbacks, bindings and unknown effect bits refuse deterministically.
4. Emits only the existing namespace-qualified `ui:UiBinder` with one
   `g:VerticalPanel` containing literal `g:Label`/`g:Button` leaf declarations.
   Strings are XML-escaped exactly once. The output is byte-deterministic with
   source and XML SHA-256 evidence, but remains a **candidate**.

Example Java 21 use (with an independently qualified source inventory):

```java
byte[] bytes = Files.readAllBytes(sourcePath);
String sourceSha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
var receipt = new WebWidgetUiBinderProjection.SourceReceipt("src/Orders.java", sourceSha);
var declaration = new WebWidgetUiBinderProjection.Declaration(
    WebWidgetAdmission.Framework.GWT,
    "com.google.gwt.user.client.ui.Label", "title", "Orders & invoices", true, 0);
var projection = WebWidgetUiBinderProjection.project(receipt, bytes, List.of(declaration));
// Next: canonical UiBinderInventory.inspect(projection.uiBinderXml());
// Then: canonical UiBinderStaticWidgetCompiler.emitJava(...), and an
// independently qualified SWT/Nebula source-sealed OpenRewrite receiver.
```

## Qualification and proof

The new 10-case JUnit suite exercises exact XML bytes, digest identity, Unicode,
XML escaping, required form/grid holds, all known and unknown effect bits,
IDs, invalid UTF-8, malformed source paths, budgets, disabled state, 256 rows,
replay and deterministic concurrent reads. Run `mvn -B -ntp test` in this
existing `framework-admission` module after the parent draft PR is applied.

The canonical *recipe* lives only in `hsoliwal/com.synexia` under
`m3-web-widget-uibinder-20261026`. This public Nebula pass contains three
hash-sealed outputs: this document, production Java and JUnit source. Recipe
replay must reach a fixed point; drift/duplicates must fail closed. The
parent draft is Nebula #278 and its recipe parent is Synexia #13405.

Local proof for this incremental change: JDK 21 compilation with
`--release 21 -Xlint:all -Werror`; ten tests executed through a temporary,
assertion-compatible JUnit API shim, **not** a genuine Maven/JUnit run.
The original UiBinder compiler is a pinned historical Synexia source, and its
full authoritative integration must be repeated in hosted CI before promotion.

**NOT QUALIFIED:** real RAP/RWT sessions, Vaadin/Wicket/Tapestry/Spring
controller and browser semantics, JS effects, JNI/HotSpot, accessibility,
IME, fonts, themes, right-to-left, native Win32/GTK/Cocoa SWT pixels, Nebula
Grid virtualization and full M3JDK21/jtreg. This is not a universal reverse
transpiler, and nothing is auto-merged or promoted to runtime implementation.
