<!-- SPDX-License-Identifier: Apache-2.0 -->
# Immutable Java source receipt — Java 21 GWT static inventory

This is a source-sealed, behavior-preserving refinement of the **existing**
`org.eclipse.nebula.m3.web.GwtAttributedJavaInventory` private implementation.
It does not introduce a new compiler, an adapter, a widget, a JNI symbol, a
new public method, or a distinct source model.

The `inspect(receipt, byte[], jars)` API receives a caller-owned mutable
byte array. Previously it checked that array's SHA-256, decoded it into
Java source, and eventually passed the caller's original array into
`WebWidgetUiBinderProjection.project`. If the caller replaced bytes between
attribution and projection, the projection could reject the already-attributed
source with `SOURCE_DIGEST_DRIFT`. In a concurrent caller this could also
make source identity and attributed facts diverge across passes.

The corrected implementation validates the input-size budget, then takes **one
private byte-array clone**. The SHA-256, strict UTF-8 decoder and original
UiBinder projection all operate exclusively on that same clone. The classpath
list and JAR snapshots retain their separate existing integrity checks.

## Exact qualification boundaries

The first-party regression `GwtSourceReceiptSnapshotTest` forces a change from
literal `"Before"` to `"Mutate"` when the admitted classpath list is copied,
after source hashing and decoding but before projection. It is deterministic:
the merged preimage code fails with `SOURCE_DIGEST_DRIFT`, while the revised
postimage retains `"Before"`, and its receipt and XML refer to the same
original source SHA-256. The test uses throwaway, self-compiled GWT-like
constructor signatures, not an official GWT JAR or copied donor code.

- **Preimage production Java SHA-256:**
  `7760d95a44bccf97c107be34937688de24b8f77a0784045273d463570cd2775b`.
- **Postimage production Java SHA-256:**
  `0bd8b0f4629af63357f64a88f362f881b2c226b69260dae2bfe45c4eb1979930`.
- **New regression Java SHA-256:** recorded in the canonical recipe manifest.
- **Receiver:** same original SWT/Nebula owners; no viewport, paint,
  selection, focus, events, IME or resource lifetime is modified.

Locally executed under strict `javac --release 21 -proc:none -Xlint:all
-Werror`: deterministic preimage **FAIL** and postimage **PASS**; the JUnit
regression was also invoked under an assertion-compatible local shim (not
real Maven/JUnit). The existing 25-case actual Chromium Java-to-browser proof
was replayed using the corrected Java output; all 25 browser checks passed
and the stdout SHA-256 remained
`aedae250b759807906187f09a634eebc82f7ae98e1985412bae534c2d0f3283f`.

**OpenRewrite Maven/JUnit, official GWT, network resource loading, native SWT
and complete M3JDK21/JNI/HotSpot remain independently unqualified.** A
source-sealed reviewable draft PR does not constitute runtime promotion.
