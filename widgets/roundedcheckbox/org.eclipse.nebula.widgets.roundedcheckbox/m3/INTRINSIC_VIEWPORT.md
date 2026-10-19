<!-- SPDX-License-Identifier: Apache-2.0 -->
# RoundedCheckbox: original Canvas owns nested paint GC scope

**Source owner:** original
`org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox extends Canvas`,
EPL-2.0 (2018 Laurent Caron). The SWT Canvas itself owns its event, paint
viewport, selection, native GC and lifecycle; no wrapper Scrollable/Canvas,
external data broker, SWT.VIRTUAL, extra native GC, or production JNI entry.

## Pass-by-pass original-source atom

Baseline original Java source Git blob:
`ff2e2c488003bf1eef86e78cc8715efc9b998b87`.
The original SWT.Paint handler simply assigns private `gc = e.gc` and invokes
private `drawElement()`. The drawing methods use that field repeatedly,
including after public/inherited SWT accessors such as `getClientArea()` that
may be overridden by callers. A nested paint changes the same field to a
different borrowed Event GC while the outer paint continues; the nested GC may
already be disposed. This is observable wrong-surface/native resource use.

The generated owner adds **one private int paintDepth** and changes **only
the original SWT.Paint lambda** to save its incoming GC, increment depth,
execute the unchanged `drawElement()`, and restore the caller's GC in
`finally` for nested paints. Top-level Paint retains its Event GC afterward,
even if rendering throws, exactly as the baseline original. No callbacks are
suppressed, no exceptions caught/replaced, and no borrowed GC is disposed.
The complete original drawing, selection and mouse hit-test code is untouched.

Donor precedent: merged [Nebula #255](https://github.com/hsoliwal/nebula/pull/255)
and original [Synexia #12438](https://github.com/hsoliwal/com.synexia/pull/12438)
already admit the **nested-only finally GC ownership pattern** inside BaseLED.
Only this bounded pattern is reused; no LED widget source is copied. Parent
SWT GC ownership and Eclipse/Nebula EPL-2.0 notices remain original.

The old code also adjusts antialiasing and line width, and `onClick` contains
its own signed-int/XOR formula. Those are independently observable contracts:
this pass DOES NOT silently change them or assert that all RoundedCheckbox
bugs/performance surfaces have been fixed.

## Executed reproducible Java21 and JNI test-only oracle

```bash
bash widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox/run-viewport-proof.sh
```

Actual Debian OpenJDK 21.0.12.1 strict `javac --release 21 -Xlint:all
-Werror`: `ROUNDED_CHECKBOX_PAINT_SCOPE_PASS checks=400000` in normal and
`-Xint` JVMs, **800,000 total checks**. The oracle includes 20,000 seeded
outer/inner/three-level scopes, nested callback failures, primary exception
preservation, top-level Event GC retention, restart after error and a control
case demonstrating the original stale nested GC field.

Test-only C17/JNI oracle for the original Oval/Ticker Canvas integer
coordinates, not a new native product method: under GCC
`-std=c17 -Wall -Wextra -Wconversion -Wpedantic -Werror`,
`ROUNDED_CHECKBOX_JNI_COORDINATES_PASS checks=120100` with
`-Xcheck:jni`; another 120,100 in `-Xint`; another 120,100 with
`-fsanitize=undefined -fno-sanitize-recover=undefined` and
`-Xcheck:jni` — **360,300 Java/JNI geometry comparisons**. Native C uses
unsigned 32-bit bit-preserving conversions to implement Java overflow and
explicitly retains Java signed truncation for center coordinates.
No JDK bootstrap, production native symbols, or per-frame JNI crossing.

These are source-identical locally executed tests, **not** native SWT GUI
proof, JMH benchmarks or complete binary API parity. The separate hosted
workflow tries to compile the **real** RoundedCheckbox/Opal dependency
classes against pinned platform SWT GTK 3.126.0 and launch the test
`RoundedCheckboxNativePaintSmoke` with `xvfb-run`. Only an actual completed
hosted job can qualify that limited native GTK outcome; Win32/Cocoa, UI
screenshot/pixel, exception/reentrancy across SWT native listeners, complete
Nebula/Tycho/GWT/RAP build, memory/JFR/ABI remain HOLD.

## Recipe-first generation, provenance and acceptance

- Named Maven/OpenRewrite recipe:
  `com.synexia.m3.NebulaRoundedCheckboxGcScope20261019` reuses unchanged
  `M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`.
- A hash-pinned original `RoundedCheckbox.java` preimage, one generated
  postimage, and eight ABSENT-before proof, build, catalogue and workflow
  files. All target Git blobs must equal canonical template Git blobs.
- Isolated JUnit verifies full recipe replay, source IDs, fixed point, original
  source refusal and duplicate/mutated source denial. SDK test is authored,
  not assumed passing merely because a proof source compiles.
- New test files retain EPL-2.0; first-party Synexia transformation crate
  Apache-2.0. No LeetCode/HackerRank/GeeksforGeeks solution code is copied:
  native GUI paint ownership is not a comparable contest algorithm.
- Nebula's original `pom.xml` has a historical EPL-1.0 notice while its
  current Java source explicitly declares EPL-2.0. Neither is rewritten
  or used to claim relicensing. Any reuse remains subject to review.
- This is one SOURCE FILE owner, one existing widget viewport; the separate
  all-widget [Synexia catalogue #11521](https://github.com/hsoliwal/com.synexia/pull/11521)
  remains unqualified per entry, not completed by this proof.
