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
variables initialized directly by `new Label("literal")` or
`new Button("literal")`. The constructor and variable types must resolve
to **exactly** `com.google.gwt.user.client.ui.Label` or `Button` using the
supplied, checksum-pinned binary JARs. Both overload and single-String
parameter are checked from `ExecutableElement`, not source spelling.

Everything else is refused: additional source-written constructors, class fields,
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
budgets, with no duplicates. Before attribution, the intake copies each JAR
without following links into a **private temporary snapshot**, checks the
**copied bytes' SHA-256** against its receipt, and gives `javac` *only* the
verified snapshot paths. Snapshot files are deleted after both successful
and rejected analyses. This closes the earlier digest-to-class-loading race
against mutable caller-owned JAR paths; it does not establish the provenance
or correctness of a caller-supplied binary. Neither arbitrary donor source nor
constructors, annotation processors, JavaScript, or compiler output are run.

The caller's `List<ClasspathJar>` request is itself frozen with
`List.copyOf` **once at method entry**. That single ordered immutable list
drives JAR selection, immutable binary snapshot verification and the final
`Inventory.classpathDigests` receipt. A stateful or concurrently modified
caller-supplied list therefore cannot cause the reported fingerprints to
describe different JARs than `javac` analyzed. A recorded negative test uses
a list whose second read returns a forged all-zero hash; the pre-fix
implementation incorrectly reported the forged hash, whereas this version
reports the verified binary digest.

Each immutable snapshot also passes bounded **ZIP/JAR content admission**
before `javac` opens it. The inventory rejects noncanonical and duplicate
archive entry names, ZIP traversal, empty/corrupt archives, inconsistent
uncompressed lengths and CRC-32, more than 12,000 entries per JAR,
individual entries expanded beyond 16 MiB, aggregate expansion beyond
128 MiB per JAR, or declared compression ratios above 512:1. Every
entry is streamed and checked; trusting only the ZIP central directory
would not prevent forged expanded-length claims. These cold
compiler-phase checks use JDK facilities, add O(actual expanded bytes)
work, and do not create JNI or a second source parser. Bounds are
deliberately strict: a larger or atypical official donor JAR must
remain held until separately evaluated, rather than silently bypassing
a security gate.

Binary class ownership is also checked **across** all admitted JARs:
two independently hashed JARs must not define the same `.class` owner.
Java 21 multi-release entries under `META-INF/versions/9..21/` are normalized
to their source-level owner for conflict checks. A caller cannot make
constructor attribution dependent on classpath order by supplying multiple
JARs with colliding types; such inputs refuse as
`M3_GWT_AST_AMBIGUOUS_BINARY_CLASS_OWNER`. Future-version class entries are
ignored by this Java 21 check, and module descriptors are not treated as
ordinary class owners. This proves deterministic owner selection within the
bounded candidate grammar, **not** authenticity of the donor's signatures.

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
against the previous two exact public inputs, plus eighteen adversarial test
groups through a temporary JUnit-compatible assertion shim. The groups
exercise positive typed GWT constructors, source-written constructors and
static initializers, control flow, callbacks, `var`, Unicode, foreign
symbols, source/hash errors, binary JAR hash, temporary-snapshot cleanup, ZIP traversal and duplicate
entries, compression and total-size bombs, corrupt CRC and entry-count limits,
256-row limits, duplicate class owners including Java 21 multi-release paths,
comment and string decoys, and deterministic parallel inspections. **This is not a
real Maven/JUnit receipt.** The committed Synexia Maven/OpenRewrite recipe
and Nebula Java 21 JUnit workflow are the actual hosted qualification owners.

**Not yet qualified:** complete arbitrary GWT Java programs, RWT/RAP session
semantics, Spring/Wicket/Tapestry/Vaadin flows, original GWT compiler parity,
real browser keyboard/focus/accessibility semantics, SWT native pixels,
Win32/GTK/Cocoa ABI, JNI/HotSpot, or M3JDK21/jtreg. The original SWT/Nebula
widget retains every viewport, item, paint, selection and resource contract.
No general reverse transpiler, automatic merge or native promotion exists.
