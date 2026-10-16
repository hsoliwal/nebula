<!-- SPDX-License-Identifier: Apache-2.0 -->
# BadgedLabel: original Canvas-owned native GC measurement lease

This is a **one-FILE direct refinement** of Nebula's original
`org.eclipse.nebula.widgets.badgedlabel.BadgedLabel extends Canvas`,
not a wrapper, second Scrollable, new control, data-broker or native ABI.

The old `getTextSizeInPixels()` and `getBadgeTextSizeInPixels()`
allocate `new GC(this)` for cache-miss measurement, select the original
font, perform `stringExtent`, and dispose only on the ordinary success
path. If the font is disposed or measurement throws, the native GC can
escape without its matching `dispose()`.

The canonical Synexia recipe changes ONLY these two private atoms to use
`try { setFont; stringExtent; } finally { dispose(); }`.
The exact same `GC(this)`, font choice, `stringExtent`, cache hit/
miss semantics, caller result, exceptions, and success-path dispose are
retained. If font or extent measurement throws, a subsequently failing `GC.dispose()`
cannot replace the original exception: the disposal failure is recorded as
suppressed on that original exception. On success, disposal errors keep their
original behavior.

The intentional failure-path correction is disposal of the
temporary native GC when native font selection or measurement fails.
The original widget remains the only paint, native resource, sizing,
style, and lifecycle authority. No code is copied from third-party
algorithms. The unrelated `onPaint()`, subclass and `setFont()`
behaviors are **unchanged** and require separate source-local proofs.

## Actual local Java21 test-only model

```bash
bash widgets/badgedlabel/org.eclipse.nebula.widgets.badgedlabel/run-gc-proof.sh
```

Source-exact `BadgedLabelGcOwnershipProof.java` compiled with
`javac --release 21 -Xlint:all -Werror`. It was actually executed
on OpenJDK21 with `java -Xmx128m` and `java -Xint -Xmx128m`:
each run prints `BADGEDLABEL_GC_OWNER_PASS assertions=960000`.
The same 160000 deterministic cases per mode, including exceptional disposal, compare original/candidate
success and exception families, cache-measurement call order, and native
GC open/dispose counts. Baseline leak is an explicit fault witness.
The test uses a Java GC **lifecycle double**; it is not SWT native GTK,
Win32 or Cocoa proof, nor a productivity speedup benchmark. No JNI is
added to the production widget because there is no batch acceleration
benefit for two cold native font measurements.

## Source pins and rights

Original current Nebula base and full source Git blob:
`9943511a112e25c9fdaeb1e92442674fa0d19443`
/ `dcf9be1b3a53354f63afdee2050dbf2150c042f4`.
Original Nebula BadgedLabel keeps its EPL-2.0 license and notices.
Standalone lifecycle test EPL-2.0; first-party canonical recipe Apache-2.0.
Reviewed donor patterns: original SWT/Project31 native GC lease (#182),
Nebula CWT inherited GC-state closure (#191/#197). Those contracts are
not blindly copied; this pass touches only the original private GC
measurement helper. LeetCode/HackerRank/GeeksforGeeks are category
evidence only, no challenge solution code used.

## Proof gates — DRAFT / HOLD

The Maven/OpenRewrite SDK must prove the exact one-file old/postimage,
five proof additions, source path/identity preservation, second-pass
fixed point, source-drift and duplicate-owner denial. The receiver
CI runs the model in both JVM modes. Full BadgedLabel original SWT/
Tycho build and native SWT `Display`, actual `GC` disposal/error
injection on GTK/Cocoa/Win32, screenshot equality, public compiled ABI,
GWT/RAP, DPI/font variations and allocation measurements are **NOT
PROVEN** until their own tests run. Never assert that this test model
qualifies the complete widget.
