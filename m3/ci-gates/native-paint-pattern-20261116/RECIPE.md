# M3 pattern recipe — native SWT.Paint observation, 14 test owners

## Canonical purpose
Fix premature **test observation** of native GTK/SWT asynchronous painting, without changing widget production implementation, public API, ABI, native peer or viewport behavior.

## Dependency and code boundaries
- Repository: `hsoliwal/nebula`; product default baseline: `master@5f6c4572ed728924bd9bc9a89a58bab08908d433`.
- This branch is **stacked upon** the independent MultiSplitPane draft [#502](https://github.com/hsoliwal/nebula/pull/502), commit `1f333053cbdf7f4f939db723163f06bdbc7ea124`.
- Exact known-good donor: Hyperlink `TestSupport.java` blob `06aff05fc17372cb948cb6d0c4f46b811dbf1bc4` from [PR #496](https://github.com/hsoliwal/nebula/pull/496); full native wait helper extracted literally, not rewritten or generalized to production.
- The MultiSplitPane file is **excluded** (belongs to PR #502); the already patched Hyperlink file is **excluded**. The special `M3TestSupport.java` file is excluded pending separate inspection.
- The 14 files below are source-pattern candidates, **not 14 reproduced runtime failures**. The full, untruncated `widgets/` Git tree contained 8033 entries and 17 conventional `TestSupport.java` blobs; 15 used the immediate assertion (one belongs to #502), one Hyperlink was already patched.

## Exact input Git blob ledger (read-only preinventory)
| Atom | File | Expected pre-blob SHA-1 |
|---|---|---|
| datechooser | `widgets/datechooser/org.eclipse.nebula.widgets.datechooser.tests/src/org/eclipse/nebula/widgets/datechooser/TestSupport.java` | `73378e10a0dbabf3c400b2936677f016dbbed25a` |
| grid-internal | `widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/internal/TestSupport.java` | `89b0c4e4371295817374b5b9cc1ff2f930bac96a` |
| opal-colorbutton | `widgets/opal/colorbutton/org.eclipse.nebula.widgets.opal.colorbutton.tests/src/org/eclipse/nebula/widgets/opal/colorbutton/TestSupport.java` | `2451e8b170267ff57ae9d05c2e3ca3c96db10a94` |
| opal-dialog | `widgets/opal/dialog/org.eclipse.nebula.widgets.opal.dialog.tests/src/org/eclipse/nebula/widgets/opal/dialog/TestSupport.java` | `101a96a2c0b9c97eb87752802f356d6e7039ce65` |
| opal-findbar | `widgets/opal/findbar/org.eclipse.nebula.widgets.opal.findbar.tests/src/org/eclipse/nebula/widgets/opal/findbar/TestSupport.java` | `ecc83287546c4670dc24bf0a3cf4b270c22d8470` |
| opal-gradientchooser | `widgets/opal/gradientchooser/org.eclipse.nebula.widgets.opal.gradientchooser.tests/src/org/eclipse/nebula/widgets/opal/gradientchooser/TestSupport.java` | `fa18b0b6c0da185736e0956424bcd69d936fb69e` |
| opal-notifier | `widgets/opal/notifier/org.eclipse.nebula.widgets.opal.notifier.tests/src/org/eclipse/nebula/widgets/opal/notifier/TestSupport.java` | `f009361932dafd5f6ae2425d74424838573cbf7e` |
| opal-statusbar | `widgets/opal/statusbar/org.eclipse.nebula.widgets.opal.statusbar.tests/src/org/eclipse/nebula/widgets/opal/statusbar/TestSupport.java` | `b307c074688a52b3a78cde451be2bebef439b7a2` |
| opal-textassist | `widgets/opal/textassist/org.eclipse.nebula.widgets.opal.textassist.tests/src/org/eclipse/nebula/widgets/opal/textassist/TestSupport.java` | `24df2e027369061d14cb851e66a52e57b1344526` |
| pgroup | `widgets/pgroup/org.eclipse.nebula.widgets.pgroup.tests/src/org/eclipse/nebula/widgets/pgroup/TestSupport.java` | `f3cfde6edb10c349786762ab8866174ebcde0591` |
| picture | `widgets/picture/org.eclipse.nebula.widgets.picture.tests/src/org/eclipse/nebula/widgets/picture/TestSupport.java` | `87816929c31a33bbf4e7a71ece67c2e2b4c422bd` |
| progresscircle | `widgets/progresscircle/org.eclipse.nebula.widgets.progresscircle.tests/src/org/eclipse/nebula/widgets/progresscircle/TestSupport.java` | `3fc92c222bdae22970c64503bf6e990c4698f983` |
| statuspane | `widgets/statuspane/org.eclipse.nebula.jface.statuspane.tests/src/org/eclipse/nebula/jface/statuspane/TestSupport.java` | `b6da5223d7633b5068bb06d3f569d3685373c340` |
| visualization | `widgets/visualization/org.eclipse.nebula.visualization.widgets.tests/src/org/eclipse/nebula/visualization/widgets/TestSupport.java` | `28f4f2844120e038b24651547a69cf497d74e81d` |

## Mechanical recipe and fail-closed guards
For each one file in the allowlist:
1. Fully read original target and donor. Fail if expected pre-blob SHA differs; fail on any existing `awaitNativePaint(` or nonunique anchor.
2. Extract the byte-identical donor `awaitNativePaint(Control, List<Rectangle>, int, String)` method using the exact adjacent Javadoc boundaries. Insert exactly once before the `The hidden-viewport gate` Javadoc.
3. Replace exactly one occurrence of `setVisible(true); flush(display); control.update(); assertEquals("one paint pass after show: " + paints, 1, paints.size())` with `setVisible(true); awaitNativePaint(control, paints, 1, "hidden <atom> reveal did not paint"); assertEquals("one paint pass after show: " + paints, 1, paints.size())`.
4. Keep all original class/package imports, method signatures, hidden 10,000-delta gates, exact zero-paint and one-paint thresholds, repaint counters and exceptions unchanged. No invented `SWT.Paint` events, no `redraw()`, no ignored failures.
5. Build all 14 new Git blobs from verified UTF-8 source and record their exact returned Git SHAs. Create **one atomic tree and commit**, not serial branch updates. Diff check must show exactly the 14 allowlisted test files in this commit, no other files.
6. Keep PR draft; require actual Java/Tycho GTK3 and other target platforms, original/viewport modes, a complete reactor, source API/ABI comparison, and deterministic recipe replay before promotion.

## CI evidence at task start
Combined Grid + Hyperlink native [run #43826450753](https://github.com/hsoliwal/nebula/actions/runs/43826450753) reached MultiSplitPane, failing `hiddenPaneFoldsDeltasAndPaintsOnceOnShow` immediately after `Control.update()`; its dedicated fix is #502.

The repeated source pattern across these 14 candidates is verified. **No compilation or runtime pass of this 14-file recipe is yet claimed.** This branch must never be merged automatically, and future donor or base changes require full SHA reconciliation.
