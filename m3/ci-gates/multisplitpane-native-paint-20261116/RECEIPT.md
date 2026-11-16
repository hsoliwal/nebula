# M3 Nebula native MultiSplitPane paint-event qualification — 2026-11-16

## Observed failure; no product fix inferred
- GitHub Tycho GTK3 run [#43826450753](https://github.com/hsoliwal/nebula/actions/runs/43826450753), job `133161328262`, reached `org.eclipse.nebula.widgets.multisplitpane.MultiSplitPaneTest` and reported **6 tests / 1 failure / 0 errors / 0 skips**.
- Failing native condition: `hiddenPaneFoldsDeltasAndPaintsOnceOnShow` after a visible restore returned `one paint pass after show: [] expected:<1> but was:<0>` from `TestSupport.assertHiddenGate`.
- The existing test helper invokes `setVisible(true); flush(display); control.update(); assertEquals(1, paints.size())`. A native GTK3 `SWT.Paint` can arrive asynchronously after `Control.update()`, so the assertion's observation is premature. **Do not assume a production painting bug from this evidence.**

## Immutable source and donor
- Repository `hsoliwal/nebula`; branch base `master@5f6c4572ed728924bd9bc9a89a58bab08908d433`.
- Original target: `widgets/multisplitpane/org.eclipse.nebula.widgets.multisplitpane.tests/src/org/eclipse/nebula/widgets/multisplitpane/TestSupport.java`, Git blob SHA `1d05048951c6de92f64315d33f206845eb787243`.
- Established in-repo donor: `widgets/hyperlink/org.eclipse.nebula.widgets.hyperlink.tests/src/org/eclipse/nebula/widgets/hyperlink/TestSupport.java` Git blob `06aff05fc17372cb948cb6d0c4f46b811dbf1bc4`, merged via [PR #496](https://github.com/hsoliwal/nebula/pull/496).
- Both files were inspected in full. The new MultiSplitPane test-helper change must transplant the donor's **actual native event wait** unchanged apart from widget-specific diagnostic text.
- No production widget source or public API/ABI changes. No thread-unsafe background dispatch. The `Display` owner remains the native SWT UI thread.

## Required semantic invariants
1. Never call `notifyListeners(SWT.Paint, ...)` or `redraw()` to manufacture proof.
2. Pump `Display.readAndDispatch()` and `Control.update()` until at least one real existing SWT.Paint recorder event is observed, and the native count is stable 200 ms. Abort after 5 seconds with count/error context; preserve interrupt status if interrupted.
3. Preserve `paints.size() == 0` and zero repaint/probe/measure/move/create assertions during 10,000 hidden changes.
4. Preserve `paints.size() == 1` and `RepaintCounters.reveals == 1` after showing; do **not** change expected values, skip tests, change widget geometry or relax predicates.
5. One file/one purpose. Diff must be helper insertion plus replacing immediate reveal wait; all other lines unchanged.

## Verification sequence
Inventory/source hash -> this document -> patch -> inspect diff -> compile/lint -> run **real original + viewport Tycho GTK3 JUnit** -> full reactor -> native cross-platform qualification -> only then request merge.

Passing gates are restricted to executed evidence. This receipt is **not proof that native CI passes**. Keep the product PR draft pending exact head tests. If `master` changes in this owner, recheck SHA before patching, never force-push or overwrite.
