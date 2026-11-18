<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Grid: CPU backend / SWT GUI-event thread split

## Scope

Original Nebula Grid and RowPermutation owners are modified in place.
The existing SWT Display/Synchronizer remains the only GUI dispatcher.

```text
SWT UI thread
  GridModelJobs.sortByIntKeysCpuLatest(Grid, keys, descending, Executor)
    -> validate the installed row permutation and capture its version
    -> accept a CPU-prepared IntKeySnapshot (no UI copy), or clone the\n       conventional int[] convenience input on the UI thread
    -> enqueue only latest pending job per Grid
CPU worker
    -> RowPermutation.sortedOrder(keys, count, descending, cancelled)
    -> radix counting/scatter with periodic cooperative cancellation
    -> publish one latest completed result through Display.asyncExec
SWT UI thread
    -> validate Grid alive, same RowPermutation, same model generation,
       matching latest sort token and non-cancelled future
    -> RowPermutation.setOrder and Grid.refreshRowPermutation
    -> complete CompletableFuture
```

**No worker touches `Grid`, a `GridItem`, `Control`, `GC`, image,
native handle, SWT model/view index object or `RowPermutation` instance.**
The only worker algorithm is a static pure primitive-array radix sort.

## Compatibility

`sortByIntKeys` retains its existing small-inline/large-worker behavior and
`sortByIntKeysBusy` remains synchronous. The CPU-only latest-wins behavior
is a new **opt-in** overload. This prevents silent EDT/SWT contract changes
in existing applications. The new method must be called on SWT's UI thread;
the provided `Executor` must actually run on a different thread, not
`Runnable::run`.

This design avoids a hard dependency on M3JDK21 in stock Eclipse/Nebula.
In the integrated M3JDK21 product, `M3ViewportScheduler` supplies the same
generic generation/cancellation contract for other toolkit adapters.

## Lifetime, backpressure, concurrency

Only one CPU drain is admitted for each Grid. While it runs, later requests
replace one pending request, and superseded futures complete `false`.
An already running radix sort polls its cancellation signal at least once
every 4096 elements during initialization/counting/scatter. A completed
result waiting in the SWT queue may be replaced; only the latest is applied.
No per-model-row Java object or native item is created by the sort path.

A `WeakHashMap` of Grid -> lane holds only weak Grid references from lane
values; the active Grid installs an SWT.Dispose listener that invalidates
all queued/active/published jobs. The separate latest-token map holds only
short-lived token objects, not the full key arrays. Both maps are synchronized
so different SWT Displays can submit independently.

A model version check and a latest-sort-request token are separate:
even if the logical row count is unchanged, filtering, sorting, grouping or
row-count mutations invalidate a stale sorted order.

## Thread and input ownership contracts

The convenience int[] overload copies exactly modelCount entries on the UI\nthread, so its capture still costs O(n). For very large models build\n`GridModelJobs.IntKeySnapshot.copyOf(keys, count)` on an independent CPU\nthread before posting the UI command. The snapshot has no mutable accessor,\nand the Grid method only validates identity/version and borrows the frozen\narray. After creation, the caller may mutate its original `keys` buffer; CPU\nwork sees a private clone. The worker sorts no live `RowPermutation`.
`setOrder`, `refreshRowPermutation`, dispose handling, selection, painting,
accessibility, model listeners, native controls and owner-draw remain SWT-only.

When a worker is computing, a stale request may temporarily consume CPU until
the next cancellation check; there is no unsafe `Thread.stop` or forced
modification of native structures. Queued old requests never launch at all.

## Verification

- Original Grid JUnit test class has offload, burst, disposal, rejection, stale
  version and cancellation tests.
- `.github/workflows/grid-model-selection-contract.yml` builds **real** RowPermutation
  and GridModelJobs Java 21 sources with strict SWT-thread stubs and runs
  non-native assertions.
- Source identity and fixed-point Maven/OpenRewrite evidence:
  `hsoliwal/com.synexia` draft PR #18655.

**The headless fixture cannot verify real SWT native behavior**.
Before merging: run SWT/Nebula Tycho and Grid JUnit on Win32/GTK/Cocoa,
owner-draw/selection/focus/accessibility differential, paint reentry,
scroll and resize bursts, huge models, heap/RSS and disposed Display cases.
Do not infer that Tree/Table/LayeredViewport or Swing operations have been
offloaded merely because this Grid sorting path is present.
