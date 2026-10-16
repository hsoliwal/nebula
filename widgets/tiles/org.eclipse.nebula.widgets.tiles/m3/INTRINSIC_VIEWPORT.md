<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula Tiles becomes its own intrinsic CPU hit viewport (2026-10-15)

**Target:** the ORIGINAL `Tiles<T> extends Canvas` and its original private
`FrameStatic<T>` source. No new window, Control, Scrollable, widget,
selection broker, public method, production JNI call or SWT.VIRTUAL style.

## Current source and contract

The original `Tiles.getTileAt` performs a first-match ordered scan across
`current.getTiles()` for every mouse movement/down. It tests **inclusive**
right/bottom edges using original Java `int` addition and returns the exact
first matching `Tile<T>`. `FrameDynamic` transitions may overlap and cannot
be addressed as uniform rows. On the original static frame, however,
`FrameStatic.update` generates uniformly spaced x/y coordinates from private
tile width, height and margins.

**Candidate:** as the original static frame completes its existing update,
perform one O(n) private check of the *actual generated tile coordinates*,
positive gaps, non-overlap, integer-overflow safety and capacity. Commit four
parent-frame integers only when every tile matches. For an exact
`FrameStatic.class` the original Tiles event owner uses a direct O(1)
coordinate to return the SAME Tile object, with the SAME inclusive edge and
first-hit semantics. All other cases, including animation, legacy unusual
size/margin, unqualified lists and partial nested render updates, execute
the **verbatim original ordered loop**. No additional persistent array.

The `FrameStatic` private fields are invalidated before original
`rendered.clear()`. They are republished **last**, after all callbacks and
validation. Native GC drawing, filter, comparator, decorator order, item
identity, image/Color lifetime, mouse event delivery, tooltip, hover/selection,
sorting and frame animation are unchanged. The current loop still owns paint.

## Actual Java21 algorithm proof

This source-sealed leaf includes the identical local executed model source
`TilesHitGridProof.java` at SHA-256
`9bc2e4ad702e3823cf664cb8b66a47929f9bc16d6e08728ba85241a828a732aa`
(Git blob `65dd776623478fa0f51937228c90a99ed5f5ac76`).
The pure-JDK21 reference reconstructs exact original
`FrameStatic.update` signed-int layout and original `Tiles.getTileAt`
first-hit behavior, and compares all selected or fallback candidate results.

```bash
bash widgets/tiles/org.eclipse.nebula.widgets.tiles/run-hit-grid.sh
```

Actual locally executed with `javac --release 21 -Xlint:all -Werror`,
`java -Xmx256m` and `-Xmx64m`:
`TILES_INTRINSIC_HIT_GRID_PASS checks=170784 indexedFrames=1079
fallbackFrames=3537 resident=10000`.
The full oracle includes thousands of signed-overflow/zero/malformed frames,
tile-edge and gap endpoints, 60,000 warm large-grid mouse coordinates, and
mutated-position rejection. A larger `-Xint` reference replay **timed out**
locally and is not evidence of failure or passing JVM interpreter results.

## Independent C17/JNI proof only (zero product JNI calls)

Test-only `TilesHitGridJniProof.java` + `TilesHitGridJniProof.c` implement
the admitted cell index in strict C17 signed-64 arithmetic. The native method
is not in the Tiles widget or frame; Java stays the runtime authority.
The JNI oracle must be given exact admitted static-frame metadata, rejects
invalid columns/steps/tile extents, and compares the ORIGINAL first-hit scan,
private Java grid lookup, and native result for every tested coordinate.

```bash
bash widgets/tiles/org.eclipse.nebula.widgets.tiles/run-hit-grid-jni.sh
```

Actual executed source (same Git blobs as canonical templates) compiled under
Java 21 `-Xlint:all -Werror` and GCC C17
`-Wall -Wextra -Wconversion -Wpedantic -Werror`.
The test-only native oracle passed:
- regular JVM `-Xcheck:jni`: **37,749** comparisons;
- interpreter `-Xint -Xcheck:jni`, bounded reference workload: **1,005**;
- GCC `-fsanitize=undefined -fno-sanitize-recover=undefined`,
  `UBSAN_OPTIONS=halt_on_error=1`, JVM `-Xcheck:jni`: **37,749**.

Total: **76,503 Java/native comparisons with no mismatch**, including
invalid-input refusals. This is source-arithmetic parity, NOT a native SWT
Display/GC run, end-user performance benchmark, or a reason to put JNI into
per-pointer hot paths. Exact locally compiled/oracle source Git blobs:
`TilesHitGridJniProof.java = d23156aaf8485f431671c0f41c1ea3f580bd190b`;
`TilesHitGridJniProof.c = c273c5b32599848c6449725d68e2349371015219`;
`run-hit-grid-jni.sh = 620dbcf1cf5586f73929d2d208026f5aac7d8d2f`.
The workflow also attempts actual original package compilation against
pinned SWT GTK 3.126.0 Maven artifacts. Only a completed hosted job
provides that platform compilation proof.

## Source/rights and gates

Original source and headers EPL-2.0, with Fabian Prasser and Laurent Caron
contributors retained. Canonical first-party Synexia Maven/OpenRewrite recipe
Apache-2.0. Candidate donor patterns include [Nebula Carousel #195],
[Nebula GeoMap #196], [CWT #191] and [SWT #177/#179]; these are
comparison evidence, not blindly copied source. Competitive-platform
LeetCode/HackerRank/GeeksforGeeks categories are reviewed as algorithm
shapes only; no unattributed challenge solutions imported.

No claim of full SWT/Tycho, native Win32/GTK/Cocoa rendering, GWT/RAP,
DPI/IME/accessibility, pixel-screenshot, `javap` descriptor, JNI production
ABI, heap/JFR performance or **Maven/OpenRewrite SDK execution** until each
completes on the actual head. Keep this a DRAFT/HOLD candidate. Entire
360-source widget programme remains separately tracked by [Synexia #11521].
