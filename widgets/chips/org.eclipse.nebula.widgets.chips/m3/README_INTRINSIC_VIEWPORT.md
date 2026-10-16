<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula Chips — the original Canvas owns its close-button viewport

**Source scope:** one private `drawClose(GC,int)` behavioral atom inside
`org.eclipse.nebula.widgets.chips.Chips`, not a new widget, scrollbar,
`SWT.VIRTUAL` mode, data broker, JNI paint method or representation of Items.

Original Nebula Chips at `hsoliwal/nebula master@4ea8bf0dd5440b64a59b8b529d6ae6d81f7e0bd8`:
`widgets/chips/org.eclipse.nebula.widgets.chips/src/org/eclipse/nebula/widgets/chips/Chips.java`
original Git blob `14eb88d5ac94798f77b002b02fd9d24b2dffa760`.
The reviewed exact after template Git blob is
`714bd6b9221964c8a1f108447b6c104a1ba2cb89`.

## One contract-sealed source atom

Original `drawClose()` creates a new SWT `Point` at every paint, then
uses the original `closeCenter.x`/`.y` for the circle/cross geometry and
mouse-up distance. The original private field remains **exactly**
`Point closeCenter`; initial null is retained. After the first
successful coordinate calculation, the existing Point's two primitive
members are reused instead of making another Point object every paint.
The exact signed-int arithmetic, GC order, coordinate order, null-before-paint,
image/text/check drawing, cursor, hover, selection/close listeners,
and their event identities are unchanged. If native painting fails
before/during circle drawing, the same center has already been staged.

The same SWT Canvas remains the *only* viewport and owns GC, selection,
hit-testing and native widget lifecycle. No new source file is needed in
the product package and no JNI ABI/public or protected member is changed.

## Java21 model proof: source-exact arithmetic and events

The source-sealed Java model `ChipsCloseCenterParityTest.java` checks
the original new-Point-per-paint atom against the parent-owned reuse atom.
It exercises extreme signed int geometry, original circle/line paint
coordinates, mouse-up float-sqrt and overflow behavior, cold NPE,
provider failures before/after center assignment and repeated paints.

Run in the Nebula receiver:

    bash widgets/chips/org.eclipse.nebula.widgets.chips/proof/run-chips-java.sh

Actually executed on OpenJDK21.0.12.1:
`javac --release 21 -Xlint:all -Werror`, `java -Xmx64m`:
`CHIPS_CLOSE_CENTER_JAVA_PASS assertions=830384 oldPointAlloc=118934 newPointAlloc=1 extraPaints=59 exceptions=5240 prePaintNpe=2`.

The point allocation figures are deterministic oracle **counts**, not JFR/native
allocation measurements in the real widget. The model's event outcomes
are equal for every tested input. Native SWT display/resource proof remains
separate.

## C17/JNI test-only arithmetic twin (not production acceleration)

`ChipsCloseCenterJniProof.java` and `.c` preserve Java modulo-2^32
integer operations with unsigned C arithmetic and `memcpy` bit-casts;
signed C overflow must never be used for Java source parity.
Original `(height-14)/2` uses Java division toward zero, retained in
the C17 helper. The JNI twin constructs only a two-int test result
through `NewIntArray` and `SetIntArrayRegion`, with no native pointers
retained by the Chips product.

Run:

    bash widgets/chips/org.eclipse.nebula.widgets.chips/proof/run-chips-jni.sh

Strict C17 GCC `-Wall -Wextra -Wconversion -Wpedantic -Werror`:
- ordinary `-Xcheck:jni`: 150,324 comparisons PASS;
- bounded `-Xint -Xcheck:jni`: 1,324 comparisons PASS;
- UBSan `-fsanitize=undefined -fno-sanitize-recover=undefined` plus
  `-Xcheck:jni`: 150,324 comparisons PASS.
**301,972 total JNI geometry comparisons** on the identical canonical
Java/C test blobs. No JNI paint optimization is inferred.

## Evaluated and rejected candidate atoms

`Chips.drawImage()` requests `Image.getBounds()` twice per draw.
The local immutable-bound geometry model observes equality when a
single result is reused, but real SWT `Image` is final yet may use
native zoom/derived-image providers, disposed-state checks and
dynamic underlying handles. Eliding the second call could change
observable provider/error behavior without live SWT proof.
**Therefore no image bounds caching is in the production patch.**

Likewise, replacing original float-`sqrt` circle hit with squared
`long` distance would change the original Java 32-bit intermediate
overflow and NaN outcomes for extreme points; no such change is made.
Challenge shapes at [LeetCode 1828](https://leetcode.com/problems/queries-on-number-of-points-inside-a-circle/),
[HackerRank A Circle and a Square](https://www.hackerrank.com/challenges/a-circle-and-a-square/problem)
and [GeeksforGeeks circle queries](https://www.geeksforgeeks.org/dsa/queries-count-point-lie-inside-circle/)
are categorized separately; **no editorial or submission code copied**.

Donor source copyright Laurent CARON 2020, EPL-2.0, retained in
the complete original Java file. First-party Synexia Maven/OpenRewrite
recipe/test proof Apache-2.0; target test-only Java/C references EPL-2.0.

## Admission and remaining HOLD gates

The existing `M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`
receive exact before/after source SHA-256, every required ABSENT-before
test/doc/workflow target, and Maven/OpenRewrite SDK fixed-point/refusal
tests. Preserve the failed controls and rerun recipes after any change.

**Not yet qualified:** actual Maven/OpenRewrite SDK on the source branch,
complete Nebula/SWT/Tycho build against real platform classes, public
binary descriptors, GTK/Cocoa/Win32 native Canvas rendering, GC resource
lifetime, DPI/font/RTL/IME/accessibility, GUI pixel/callback snapshots and
heap/frame benchmarks. The existing 360-entry source catalogue
[Synexia #11521](https://github.com/hsoliwal/com.synexia/pull/11521)
continues to HOLD all these product gates; do not merge on arithmetic
models alone. Never rebase `develop`.
