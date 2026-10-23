<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original Nebula LED Canvas: nested native Paint GC lifetime

**Existing owner only**: `org.eclipse.nebula.widgets.led.BaseLED` extends
the original SWT `Canvas`. Its native paint handler records the currently
borrowed `Event.gc` in the **existing protected `gc` field**. All existing
`LED`, `DotsLed`, `LEDSeparator` and subclass objects keep the same native
control, public/protected surface, events, colors, own item shapes and GC API.

## Observed reentrant defect and contract

Original `BaseLED.onPaint` overwrites `gc = e.gc` on each paint and does
not restore it. When a subclass `paintInternal()` callback synchronously
re-enters `SWT.Paint`, the outer callback may resume using the nested GC.
If that GC was disposed or belongs to a different drawing surface, drawing
may fail or affect the wrong surface. **Ordinary, nonnested original
behavior retains e.gc after paint**, and a subclass can read this protected
field. Clearing the field unconditionally would violate that observable
contract.

Source-sealed one-FILE refinement:
- Add only `private int paintDepth`, owned by the ORIGINAL BaseLED widget.
- Snapshot prior protected `gc` and entry depth *before* entering paint;
  maintain the same background, fill, advanced mode, antialias and
  `paintInternal()` ordering.
- Restore parent GC **only for nested paint**, in `finally`, even if a
  provider raises RuntimeException or Error. Maintain the same primary
  exception and no additional GC disposal or event dispatch.
- The outermost callback deliberately retains `e.gc` even on failure, as
  the original implementation did. No wrapper Canvas, automatic SWT.VIRTUAL
  behavior, new JNI native symbol, or extra GC is created.

## Local actual Java21 proof and negative controls

The exact source-blob `LEDNestedGcScopeProof.java` Git
`7a975fcd843cba511b030674b47b627b7a5e17e4` compiled locally on
OpenJDK21.0.12.1 using `--release 21 -Xlint:all -Werror`. Executions:
- `java -Xmx128m`: `LED_ORIGINAL_GC_SCOPE_PASS checks=933332 quick=false`
- `java -Xint -Xmx128m`: `LED_ORIGINAL_GC_SCOPE_PASS checks=18666 quick=true`

The model checks 100,000 ordinary and reentrant callback arrangements,
nested RuntimeException/Error identity, setup/draw event ordering, original
and candidate postpaint GC behavior and balanced nesting. It includes
negative evidence: the previous code retains the wrong nested GC.
An initial test source attempt failed javac due an untyped conditional throw;
the **test source** was fixed and re-run successfully, not counted as a
passing initial attempt. These are **headless reference-model results**,
not original native SWT/GTK/Win32/Cocoa widget runs or compiled toolkit
source equivalence.

From the receiver root:
```bash
bash widgets/led/org.eclipse.nebula.widgets.led/run-gc-scope-proof.sh
```

## Mandatory promotion gates

Canonical `com.synexia.m3.NebulaLedGcScope20261019` Maven/OpenRewrite
source recipe (unchanged `M3GpuToolJoinRecipe` /
`M3GpuToolJoinManifest`) owns one exact original Java source postimage
and five new test, runner, decision and CI files. The original source
EPL-2.0 donor header (Akuiteo, Laurent Caron, Frank Delporte) is
untouched. Independent Synexia recipe Apache-2.0, proof EPL-2.0.

**DRAFT / HOLD:** actual Maven SDK generation tests, full Nebula LED
bundle/Tycho, native GTK/Win32/Cocoa paint with a real Display/GC,
nested native callback disposal, exact `javap` binary ABI, screenshot/pixel
equivalence, GWT/RAP and memory/latency benchmarks are **NOT_PROVEN**.
No production JNI accelerator is introduced because the defect is
borrowed SWT GC callback ownership, not a bulk arithmetic operation.
LeetCode/HackerRank/GeeksforGeeks provide taxonomy only; no donor
challenge solution source is copied. Synexia `develop` must never
be rebased. See [existing owner catalogue PR #11521].
