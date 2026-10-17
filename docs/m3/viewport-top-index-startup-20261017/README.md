<!-- SPDX-License-Identifier: EPL-2.0 -->
# Grid top-index startup: reproduced, repair pending

This follow-up records a real startup scrolling defect and a rejected repair experiment. **It does not change production code, qualify a repair, or clear the native acceptance gate.** The separate selection-occurrence correction and its all-238-PR history audit remain at `644b9ebfed3779c2df9e5adc6435f70c1c99e8a5`, based on master `91c474935eb4a06e46716a0188b9082f0af0eda9`.

## Observable defect

`Grid.setTopIndex(int)` returns when `vScroll.getVisible()` is false. Before the first paint, the scrollbar can still be hidden even though populated rows exceed the client area. The request is silently lost. The same condition can affect a covered or resized widget whose pending scrollbar update has not painted.

The deterministic reproduction creates a 200 by 200 Grid with 20 collapsed roots, each with three children: 80 logical items and 20 visible rows. It never opens the shell or dispatches a paint. Selecting logical rows 4, 8 and 24, requesting top index 12, and then calling `showSelection()` produces:

| Observation | Unchanged source |
| --- | --- |
| Initial vertical bar visibility | false |
| Requested top index | 12 |
| Immediate top index | 0 |
| Top index after `showSelection()` | 0 |
| Repeat request 12 after bars refresh | 12 |

The final step distinguishes the lost startup request from an invalid row index or insufficient content. `showSelection()` refreshes the bars but reasonably leaves the first selected row at index 4 where it is already visible under top index 0. It cannot recover the earlier ignored request.

This explains the pre-existing `Grid_Test.testShowSelection` failure in the selection PR's 73-method public selection subset: the unchanged baseline and selection correction both pass 72 and fail the same one test. The legacy fixture also leaves shells undisposed, making paint settlement less reliable. That fixture weakness does not erase the direct public setter defect demonstrated here. A separately painted baseline probe in the selection PR reaches 12 and then 4 as expected.

## Why a simple refresh was rejected

A staging-only experiment refreshed obsolete scrollbar geometry in `setTopIndex`, guarded recursive setter entry, preserved frozen scrollbar planes, and revalidated the captured item's identity after returning. The proposed source difference is retained as `rejected-prototype.patch.txt`; it is **not an admitted recipe or an instruction to apply that patch**.

Native scrollbar visibility changes can synchronously invoke application listeners. The callback probe installs an `SWT.Show` listener that disposes the Grid. On the unchanged source, no Show event occurs because the startup request is ignored. On the staging experiment, the refresh enters `ScrollBar.setVisible`, invokes the listener, and then throws a `NullPointerException` inside the GTK SWT implementation because the scrollbar's parent was cleared by disposal. This occurs before control returns to the Grid's post-refresh checks. The candidate process exits 1.

The native experiment used official SWT GTK 3.135.0. It is not a claim that the current SWT source checkout was built or tested for this scenario. The companion SWT source review also observes synchronous Show/Hide and owner Resize paths on GTK and Cocoa and synchronous native resize behavior on Win32. No Windows or Cocoa runtime execution is claimed here.

The repair therefore needs an explicit policy for pending logical scroll requests and callback-safe geometry settlement. Adding an unguarded or only post-guarded `updateScrollbars()` call is not sufficient. A correction must account for disposal, reentrant row mutation, stable item identity, nested frozen planes, and sparse virtual rows while retaining public event and original-mode behavior.

## Evidence and reproduction

`QUALIFICATION.json` pins the unchanged production file, rejected postimage, official runtime artifact and every probe/log. Probe files use a `.java.txt` suffix so they are diagnostic source and are not silently introduced into the product test build. Copy each to its Java filename in a temporary directory and compile it against the Nebula widget/test helper classes and the recorded SWT artifact. `ShowSelectionPrepaintProbe20261017` additionally uses the repository's unchanged `GridTestUtil` helper.

The native executions used Java 21.0.12.1+1, GTK3, a local Xvfb display, `-Xmx512m`, the classic Grid look, and extracted native libraries from the exact SWT artifact. No heap, CPU or native-memory budget is qualified by these small diagnostic runs. The first selection PR records its own measured native runs separately.

The probe runner pattern was:

```sh
xvfb-run -a "$NEBULA_JAVA" -Xmx512m \
  -Djava.library.path="$NEBULA_SWT_NATIVE" \
  -Dorg.eclipse.nebula.widgets.grid.look=classic \
  -cp "$NEBULA_BASE_CLASSES:$NEBULA_PROBE_CLASSES:$NEBULA_SWT_JAR" \
  org.eclipse.nebula.widgets.grid.ShowSelectionPrepaintProbe20261017
```

The callback control substitutes `ShowSelectionCallbackProbe20261017`. To reproduce the rejected experiment in an isolated staging environment, its compiled classes preceded the unchanged dependencies in the classpath. The patch never entered a production checkout. No received OpenRewrite result or generated-source promotion is claimed for it.

## Pending acceptance gates

A successor repair must first be admitted by a new source-pinned canonical `com.synexia` recipe and received through the real Java/OpenRewrite SDK. It must then pass failing-before native cases for startup scrolling and selection, disposal and reentry during geometry updates, model changes from callbacks, frozen planes, and sparse virtual rows. Native screen pixels must show that a prepaint scroll request renders the same intended rows as a settled request. Existing selection contracts, the default Maven/Tycho gate, per-platform runtime lanes and independent resource budgets remain required product gates.

**Status: reproducible defect retained; naive candidate rejected; production repair pending.** This document provides an actionable continuation point and must not be cited as a successful viewport repair.
