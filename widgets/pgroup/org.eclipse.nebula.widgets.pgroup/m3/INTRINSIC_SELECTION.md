<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original Nebula PGroup as its own viewport — selection snapshot atom

**Both real owners remain the original `PGroup extends Composite` and
`PGroupToolItem extends Item`, under original EPL-2.0 notices.** There is no
wrapper Canvas, new selection broker, user-visible API, parallel item store,
production JNI call, or forced `SWT.VIRTUAL`/`SWT.SetData`.

## Exact source baseline and discovery

Nebula master `4ea8bf0dd5440b64a59b8b529d6ae6d81f7e0bd8`:
- `PGroup.java` Git blob `9f54551fcff410e0880519a9fdb636a297f0c670`.
- `PGroupToolItem.java` Git blob `1fffbb4f6a442a73b2442081e8822df609f3a2a5`.

Original selectable `PGroupToolItem.setSelection(true)` performs the public
`parent.getToolItems()`, which allocates a defensive full-length array.
It then calls each child's original virtual `setSelection(false)` in order,
preserving redraw/event/exception/override behavior. That public getter
**must remain unchanged** for callers and subclass override contracts.

The category corpus selected the [Nebula PShelf #193] pattern of **guarded
exact owner access**, backed by [SWT List #180], [CWT #197], and the
[Synexia all-widget catalogue #11521]. Reusing a third-party challenge
solution for SWT item lifetime is not compatible: no LeetCode/HackerRank/
GeeksforGeeks source code was copied.

## Contract-preserving source atom

- The original `PGroup` retains the same `toolitems` List and native
  event/scroll/layout owner. It adds **one private sticky boolean**, set to
  false on the first derived PGroupToolItem. The flag retains a conservative
  original-path fallback after the subtype is removed.
- The new private/package method `clearToolItemSelectionsWithoutCopy()`
  returns true only for exact `PGroup.class` with all-original item classes.
  It invokes each child's **original public `setSelection(false)`** in
  the same order with the same number of redraw calls. No temporary array
  is retained or allocated on that admitted path.
- `PGroupToolItem.setSelection(true)` selects that private path only if
  `getClass()==PGroupToolItem.class`. Every parent subclass, item subclass
  and unreviewed alternative retains the original public
  `PGroup.getToolItems()` and snapshot iteration untouched.
- No assumptions of changed `SWT.CHECK`/`SWT.RADIO` style constants,
  selection exclusivity, UI-thread callbacks or native items are introduced.

The strict bound is **O(n) ordered original selection calls with O(1)
additional heap**, versus original O(n) time plus an O(n) copied reference
array. This is allocation reduction, **not a claimed CPU speedup**.

## Executed Java21 differential

The locally executed model's **exact Git blob**
`e7b25285bdd3ad79e2ce9aa69f2a8b5a7e73e394`, SHA-256
`903823f2b7f24085b23fe605d00414680321261c887a44f7e84b5c35c8a2633a`
is admitted as a source-sealed test, not product code. It compares
original vs candidate final item selection, per-item redraw/callback order,
derived parent getters, child override dispatch, exceptions, CHECK/RADIO
style categories, removal of a derived item and 180,000 seeded random cases.

```bash
bash widgets/pgroup/org.eclipse.nebula.widgets.pgroup/run-selection-parity.sh
```

**Actually executed** with `javac --release 21 -Xlint:all -Werror` on
OpenJDK21.0.12.1:
- normal `-Xmx192m`: `PGROUP_INTRINSIC_SELECTION_PASS checks=291855 quick=false`
- interpreted `-Xint`: `PGROUP_INTRINSIC_SELECTION_PASS checks=5579 quick=true`

The model is an explicit deterministic native-free test double. It does
not establish a real SWT display, Paint/Canvas callbacks, cross-platform
native lifecycle, or assembled binary/API parity. Production JNI is
**not selected** for this small synchronous UI-thread operation; crossing
the JNI boundary could change callback/order/error behavior and cost more.

## Recipe-first implementation / gates

Canonical `M3PGroupSelection20261015Recipe` **reuses**
`M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`, with two exact original
source preimages and five ABSENT-before proof/docs/CI resources. The
Maven/OpenRewrite SDK JUnit is authored to validate exact postimages,
source-file identities, fixed point, missing/duplicate/drift refusal and
public/protected source declaration equivalence. **An authored test is not
proof of execution**.

Original Nebula PGroup/PGroupToolItem EPL-2.0 rights and attribution
remain untouched; independently authored Synexia recipe, tests and
metadata Apache-2.0 except the EPL-2.0 test model in the target.

**DRAFT / HOLD:** full source-built Nebula/SWT/Tycho, native
GTK/Cocoa/Win32 Display, UI event order and disposal, overload/ABI
javap, GWT/RAP, heap/JFR regression and actual hosted Maven/OpenRewrite
SDK are all not yet qualified. Nothing has been merged to default branches.
