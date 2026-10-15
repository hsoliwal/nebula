# PShelf: the original widget owns primitive viewport geometry

This is a **direct source change to Nebula PShelf**, not a scrollable wrapper
or another control. The public PShelf/PShelfItem API, selection and body
Composite ownership, native SWT event dispatch, renderer and style contracts,
and animation interpolation are retained.

Before: each ordinary Y-coordinate and animation frame stores boxed
Integer instances in an ArrayList; the animation recomputes list.indexOf
for each item, and RedmondShelfRenderer.paint calls public getItems()[0]
for every header. That getter must return a defensive PShelfItem array,
so the renderer creates and immediately discards a complete array
per header (O(items^2) element copying in one full repaint).

After: PShelf itself retains an int[] coordinate plane. It computes its
fixed and animated geometries through an internal pure
PShelfViewportGeometry atom. The current animation indices are looked
up once *per frame* (preserving inter-frame callback mutations); the
new positions are staged and committed before the original native
sizeClients/redraw/update sequence. Redmond uses a package-private
O(1) parent-first-item check; the public getItems defensive-copy
semantics remain unchanged.

## Explicit renderer/viewport admission rule

This change deliberately **does not skip offscreen calls to arbitrary
user-provided AbstractRenderer instances**. Both existing built-in
renderers draw focus in widget-global coordinates and external custom
renderers may have side effects unrelated to their header rectangle.
Blind clip culling changes behavior. Those renderers need separate
source- and pixel-parity proof before suppressing native paints.
The parent PShelf already owns viewport geometry and native Canvas
paint; this refinement reduces its bookkeeping and GC pressure
without creating a second viewport authority.

## Exact-source recipe and verification

Synexia's existing M3GpuToolJoinRecipe / M3GpuToolJoinManifest creates
PShelf.java and RedmondShelfRenderer.java from exact SHA-256-pinned old
sources, plus a pure package-private geometry atom, original-expression
differential test, optional real SWT smoke, host proof runner, and
CI workflow. The recipe SDK checks postimages, fixed point and drift
refusal; no manual production postimage editing is admitted.

Run the native-free Java21 differential (no SWT install necessary):

    bash widgets/pshelf/org.eclipse.nebula.widgets.pshelf/run-geometry.sh

## Executed C17/JNI arithmetic proof (test-only)

The canonical source packet now includes `PShelfGeometryJniParityTest.java`
and `PShelfGeometryJniParityTest.c` plus a strict build runner.
The C implementation uses unsigned 32-bit arithmetic and bit-preserving
conversions for Java signed-int overflow, and explicit saturation/truncation
for Java double-to-int casts including NaN/infinity. It never retains JNI
array addresses, does not export a production PShelf native method,
and bounds proof-only scratch memory. NewIntArray + SetIntArrayRegion
are used only in the test. No JNI change exists in the paint path.

```bash
bash widgets/pshelf/org.eclipse.nebula.widgets.pshelf/run-jni-parity.sh
```

Executed independently with OpenJDK21/GCC C17 strict warnings:
`PSHELF_JNI_PARITY_PASS checks=457053` with `-Xcheck:jni`;
the same `457053` pass with `-Xint -Xcheck:jni`.
Total **914,106 Java-vs-JNI comparisons**, including explicit
rejection of negative and out-of-range logical sibling indices to prevent
undefined arithmetic on non-PShelf inputs, 100,000-row stress,
invalid range refusal and extreme arithmetic. The actual receiver
source must match the canonical Git blobs before reusing this evidence.
This proof establishes algorithm parity, not that native calls improve
PShelf drawing speed. Production remains Java-first.

CI also tries original PShelf/PShelfItem/Redmond/Palette SWT GTK
compilation and Xvfb rendering with pinned SWT 3.126.0 host binaries.
CI is only qualifying evidence if an actual job completes
successfully. Java mock/stub tests cannot establish platform parity.

## Donor catalogue and rights

- PShelf original source, 2006 Chris Gross, EPL-2.0.
- RedmondShelfRenderer original source, EPL-2.0.
- Initiative31 non-virtual custom widgets supply widget-owner patterns,
  but no foreign implementation body is copied.
- Bounded primitive row coordinates and per-frame index hoisting are
  directly proven against *the original PShelf source algorithm*.
- LeetCode/HackerRank/GfG are category evidence only; no contest code
  is imported and a different algorithm is not selected without proof.

**Not complete:** every SWT and Nebula widget conversion, all custom
renderer clipping, large-native-scrollbar range, GWT/Cocoa/Win32,
full Tycho/JNI and binary API equality, arbitrary callback/dispose
reentrancy, and physical heap/JFR profiling. Keep this PR DRAFT/HOLD
until those separate gates are evidenced.
