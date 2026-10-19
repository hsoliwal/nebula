<!-- SPDX-License-Identifier: Apache-2.0 -->
# Chips + PGroup: original Canvas paint/measurement GC failure scope

Every original widget owns its own viewport, native Canvas, paint event, native
SWT GC borrowing and public API. This pass modifies exactly two original
Nebula widget sources, `Chips.java` and `PGroup.java`, using a source-sealed
Maven/OpenRewrite recipe. No wrapper, additional GC, Scrollable, data model,
SWT.VIRTUAL conversion or product JNI ABI is introduced.

## Original behavioral atoms

**Chips.computeSize** had `new GC(this)` followed by `stringExtent` and a
normal-path `gc.dispose()`. If native measurement or callback failed, its
owned GC was never disposed. A Java `try/finally` now encloses exactly the
original text-extent and size arithmetic and disposes it on both outcomes.

**Chips SWT.Paint** and **PGroup.onPaint** borrow the event GC. In the normal
path their old operation order is maintained: original paint, renderers,
selected colors, selection/callbacks, and final foreground/background state
are unchanged. On an exceptional exit, catch `RuntimeException | Error`,
attempt both background and foreground restoration independently, attach any
cleanup errors as suppressed on the original failure, and rethrow the same
failure. Do not dispose the paint-event GC, which belongs to SWT.

These are bounded private behavior refinements. They do not imply clipping,
item recycling, item interning, JNI paint acceleration or full virtual
conversion. In particular, normal paint state **is not forcibly reset after
PGroup's item renderer**; caller-observable success remains as in the donor.

## Java21 executed differential

Original-vs-candidate JDK21 model lives at
`tests/org/eclipse/nebula/widgets/proof/CanvasGcOwnershipProof.java`.
The proof preserves the original normal call/GC color order and differentiates
the old exceptional-path leak and failed borrowed-state restoration.
Its deterministic corpus includes 80,000 seeded original/candidate phases,
double-cleanup failures with suppression, nested paint state and
owned-measurement-GC exceptional disposal.

Actual locally executed, strict Java 21 compilation:
```
javac --release 21 -Xlint:all -Werror -d classes CanvasGcOwnershipProof.java
java -Xmx128m -cp classes org.eclipse.nebula.widgets.proof.CanvasGcOwnershipProof
java -Xint -Xmx128m -cp classes org.eclipse.nebula.widgets.proof.CanvasGcOwnershipProof
# Both: M3_ORIGINAL_CANVAS_GC_PROOF_PASS assertions=224006
```
Source Git blob `be0436229e67a86366f4e9bf209c56c8ee4362b8`,
SHA-256 `550fffe4dc2c630ec9cec75f727384e9b8fa332ee54819296421d74cb68dbd4b`.
**448,012 executed assertions across normal and interpreter modes**, with
original failures deliberately recorded as defect counterexamples.
This is a source-pattern MODEL, not a compiled SWT control or native display.

## Reused admitted donors

- Original Nebula Chips and PGroup, original EPL-2.0 copyrights intact.
- [Nebula LED nested GC PR #255](https://github.com/hsoliwal/nebula/pull/255),
  [BadgedLabel measurement GC PR #215](https://github.com/hsoliwal/nebula/pull/215),
  [CWT clip ownership #191/#197](https://github.com/hsoliwal/nebula/pull/197).
- Synexia existing `M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`,
  original before-state and SHA-256 target receipts.
- LeetCode/HackerRank/GeeksforGeeks consulted as problem categories, **no
  challenge submissions used** because SWT GC exceptions/native lifetime and
  subclass callbacks have different contracts.

## Required HOLD gates

The authored isolated Maven/OpenRewrite SDK JUnit must actually execute and
verify 2 source preimages + 5 generated proof files, fixed point and refusal.
Complete Chips/PGroup Nebula/Tycho build, native SWT GC/Display on Win32,
GTK/Cocoa, source+binary ABI, exception callbacks with real GC, nested native
Paint/GC, DPI/IME/accessibility, screenshot pixels, and performance/heap/JFR
measurements are not qualified by this model. Keep the receiver and recipe
DRAFT/HOLD until these are observed. No automatic merge/rebase of develop.
