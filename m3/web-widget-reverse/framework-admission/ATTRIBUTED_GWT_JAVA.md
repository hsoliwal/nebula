<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Source-bound Java 21 GWT AST inventory to original UiBinder

This **third additive pass** is a bounded, read-only Java source frontend for the
existing `WebWidgetAdmission` and `WebWidgetUiBinderProjection` in Nebula. It
uses the **JDK 21 `JavacTask`, `Trees`, and `javax.lang.model` type attribution**
to derive declarations from an actual source AST rather than trusting an
unexamined list of widget names and effect flags. It does not execute donor
code or generate new SWT, JNI or HTML runtimes.

## Admitted source profile

A single `public final` class, with package/path agreement and exactly one
`public static void build()` method. Its complete body consists of 1–256 local
variables initialized directly by `new GWT Label("literal")` or
`new GWT Button("literal")`. The constructor and variable types must resolve
to **exactly** `com.google.gwt.user.client.ui.Label` or `Button` using the
supplied, checksum-pinned binary JARs. Both overload and single-String
parameter are checked from `ExecutableElement`, not source spelling.

Everything else is refused: explicit/implicit constructors, class fields,
static initializers, nested classes, imports other than exact GWT types,
annotations, callbacks, form handlers, branches, loops, assignments,
`setText`/`setEnabled` mutations, `var`, expressions, other imports or type
symbols, anonymous classes, dynamic text and extra compilation units.
`Button(String)` can carry HTML semantics, so only inert printable ASCII
text is accepted as a **candidate**, with full runtime behavioral equivalence
still held. GWT `Label` Unicode is retained as literal text and XML-escaped
in the existing `WebWidgetUiBinderProjection` owner.

The compiler is invoked with `--release 21 -proc:none -implicit:none`, an
**empty source path** and an explicitly provided binary-only, SHA-256 checked
classpath. Each JAR must be a regular, non-symlink file, within the size/count
budgets, with no duplicates. Neither arbitrary caller source nor donor
constructors, annotation processors, JavaScript, or compiler output are run.

`javac` rewrites its own AST during `analyze()`: the inventory snapshots the
source-written class members and `var` declarations **before attribution**.
This explicitly blocks two dangerous false admissions: a synthetic implicit
constructor hiding a user constructor, and a `var` declaration becoming a
regular type in the attributed tree.

## Example (candidate, never runtime promotion)

```java
// GwtSignatures.jar must be independently licence/revision/source-qualified.
var jar = new GwtAttributedJavaInventory.ClasspathJar(gwtJar, pinnedSha256);
var receipt = new WebWidgetUiBinderProjection.SourceReceipt("demo/Orders.java", sourceSha256);
var evidence = GwtAttributedJavaInventory.inspect(receipt, javaBytes, List.of(jar));
String candidateXml = evidence.candidateXml().uiBinderXml();
// Then the ORIGINAL UiBinderInventory / UiBinderStaticWidgetCompiler;
// not a new SWT adapter or a new widget ownership model.
```

A SHA-256 JAR receipt authenticates only the **supplied bytes**, not their
legal origin or equivalence to an official GWT release. Production promotion
must separately validate the donor's official binary/revision and the
source inventory's complete syntax contract; local JUnit creates minimal
**test-only** signature JARs and does not execute or copy upstream GWT code.

## Proof boundaries

Executed locally: strict Java 21 `javac --release 21 -Xlint:all -Werror`
against the previous two exact public inputs, plus nine adversarial test
groups through a temporary JUnit-compatible assertion shim. The groups
exercise positive typed GWT constructors, source-written constructors and
static initializers, control flow, callbacks, `var`, Unicode, foreign
symbols, source/hash errors, binary JAR hash checks, 256-row limits, comment
and string decoys, and deterministic parallel inspections. **This is not a
real Maven/JUnit receipt.** The committed Synexia Maven/OpenRewrite recipe
and Nebula Java 21 JUnit workflow are the actual hosted qualification owners.

**Not yet qualified:** complete arbitrary GWT Java programs, RWT/RAP session
semantics, Spring/Wicket/Tapestry/Vaadin flows, original GWT compiler parity,
real browser keyboard/focus/accessibility semantics, SWT native pixels,
Win32/GTK/Cocoa ABI, JNI/HotSpot, or M3JDK21/jtreg. The original SWT/Nebula
widget retains every viewport, item, paint, selection and resource contract.
No general reverse transpiler, automatic merge or native promotion exists.
