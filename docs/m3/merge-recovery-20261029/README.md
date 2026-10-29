<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 Nebula merge-history recovery and enhancement map — 29 October 2026

This **product-facing evidence** is computed from actual GitHub PR metadata,
Git object trees and selected historical source bodies. It does not merge
branches, restore snippets or introduce an SWT runtime layer. Canonical
recipes, donor evaluation and cross-repo convergence belong in
`hsoliwal/com.synexia`; Nebula receives only qualified original Java/SWT
results, tested source assertions and factual source-custody receipts.

## Reproducible snapshot

| Authority | Exact reference |
| --- | --- |
| Nebula `master` | `82b7a52c756e26d6075293043766df23291c00a5` |
| Grid sort/filter branch | `m3/grid-sort-filter-20261019` at `dcd1a229bc9cbfac570e2c6e0de8091ffebb7ad2` |
| RWT column branch | `m3/rwt-column-20261020` at `4821d7dacbab64af2c4023d9de8b2eb7d948c1eb` |
| PR metadata | GitHub REST `/pulls?state=all`, four 100-record pages, 297 unique IDs |
| Path coverage | Git trees for the above exact three commits, `recursive=1`, all `truncated=false` |
| Existing historic audit | `docs/m3/viewport-history-20261017/PR_HISTORY.tsv` and `RECONCILIATION.md` (238-PR snapshot) |

The PR metadata map now has **297** rows: **247** merged directly
into `master`, **26** merged into non-master feature branches, and
**24** closed-unmerged. Non-master merges are *not* necessarily lost:
`STACK_MERGES.tsv` compares all **26 exact recorded PR head SHAs** to
the pinned `master`. **15** PR heads have unique commits not in
master's ancestry; **11** are already ancestral. A unique commit does
not prove its content is absent or superior (a separate path/semantic
cherry-pick can retain equivalent behavior).

Two complete recursive Git trees and per-path comparisons produce
`BRANCH_SOURCE_RECEIPTS.tsv`:

| Source lineage | Absent original paths | Different Git blobs | Total paths in branch delta |
| --- | ---: | ---: | ---: |
| Grid sort/filter | 4 | 1 | 5 |
| RWT column | 65 | 12 | 77 |
| **Total** | **69** | **13** | **82** |

This is an **82-path branch delta**, not a finding of 82 functional
regressions. Any `DIFFERENT_MASTER` entry requires a semantic diff
before a restore. A missing path may be an intentionally discarded
prototype or an unported test. The catalogue's named atoms are
in `RECOVERY_MAP.tsv`; all other RWT source entries stay explicitly
`HOLD_MULTI_FILE`.

## High-confidence source gaps requiring a qualified receiver

### Grid sort/filter — feature-branch-only, not on master

- [#262](https://github.com/hsoliwal/nebula/pull/262) merged into
  `integration/m3-grid-sort-filter-20261019`;
  [#265](https://github.com/hsoliwal/nebula/pull/265) and
  [#269](https://github.com/hsoliwal/nebula/pull/269) merged into the
  `m3/grid-sort-filter-20261019` feature lineage.
- Current source branch contains
  `M3GridRowBinding.java` (Git blob
  `ad3ac6aa82d68cd918df38c51e8b1cd6460f7b35`),
  `M3GridSortHeader.java` (`f72af8a96a2bcb8b35542e965833e858b9e69523`),
  and the original Grid row-binding test
  (`adef55f5db8634c6e42a78033952f8c718f283e5`).
  None exists at that path in the pinned master tree.
- The source depends on distinct `org.eclipse.swt.m3.M3RowView`,
  `M3RowChange` and `M3ColumnSort` contracts. **Do not paste**
  these classes into the Grid bundle or invent a bridging runtime.
  First inventory the SWT owner signatures and current Nebula Grid API;
  then compare row selection, reparenting, sorting/filtering and
  disposal against the original native Grid tests.

### RWT graphics — incomplete preserved source, not a safe merge

- [#263](https://github.com/hsoliwal/nebula/pull/263) introduced the
  RWT column on master; [#264](https://github.com/hsoliwal/nebula/pull/264)
  and [#266](https://github.com/hsoliwal/nebula/pull/266) subsequently
  merged into preserved RWT feature branches, not the master branch.
- Historical `GC.java` Git blob
  `6ff366ef07dc7776bb25278c4fd51ff5fe29646a` implements
  `textExtent(String,int)` by calling `M3GC.textForExtent`.
  The current `master` `GC.java` blob
  `7b7d78dbd08681cd74e772231829f8fe1af068a1` has only
  `textExtent(String)` at that inspected API location.
  **Critically**, the historical first-party `M3GC.java` blob
  `5edb7e5571d501f46a609cd079f4d03cdc4cdcc4` does **not
  define** the called `textForExtent` method. The historical source
  is therefore an **incomplete dependency candidate**, not a
  ready-to-compile implementation.
- The historical `M3CanvasPaint.java` blob
  `cebd94a3968483a8b2d602912453a4d00036669e` is absent from
  master. Its `paintIfShown` switches RAP `CurrentPhase` to
  `PROCESS_ACTION` around deferred damage and restores it in a
  `finally` block. Re-admitting this code requires true RAP
  phase, reentrancy, disposal and transport proofs.
- `RwtExamplesProbeTest.java` and `M3CanvasPaintTest.java`
  are also absent from master. The first filters `rwt.client.`
  operations and measures operation payload bytes. Their original
  dependencies and live browser semantics must be reconstituted
  before claiming a successful renderer benchmark.

## Confirmed non-loss / false-positive paths

- [#297](https://github.com/hsoliwal/nebula/pull/297)
  `Gallery.java` historic head blob `444f9bcefe651a0e07c56f2baae2ee2858d512bc`
  differs from master blob `8051b0efe192a427bbb7d42a21b1b2ad3993fff9`,
  but **normalizing CRLF to LF makes the source text identical**.
  This is not an algorithm to recover.
- [#267](https://github.com/hsoliwal/nebula/pull/267)
  `GridRowAddressedItemDataVisualizer` still counts a retained row
  even if all its cells are null (observed `count++` in today's
  target method). The whole file blob differs, but that specific
  contract survives.
- [#268](https://github.com/hsoliwal/nebula/pull/268)
  `GridGCProxyDeviceRegion_Test.java` matches the historical blob
  `1764fe39562c178e55dd2d95116458e9a552560b` exactly on master.
- [#274](https://github.com/hsoliwal/nebula/pull/274)
  generated `OrdersNativeSwt.java` retains its exact Git blob
  `7e36705ad13432e54a5179003429a94861eb93ca` on master.
  The source-generating recipe remains in separate, open
  [Synexia #13402](https://github.com/hsoliwal/com.synexia/pull/13402)
  and must not be silently moved into the Nebula runtime.

## Serial, file-local qualification plan

The `RECOVERY_MAP.tsv` prioritizes **P0** dependency/contract
closures, **P1** independently recoverable tests/graphics and
**P2/P3** already-retained or false-positive findings. Each named
candidate records source ownership, immutable donor PR, exact blob
where known, expected enhancement, a separate testing gate and
the current `HOLD` or `KEEP_CURRENT` decision.

For an actual enhancement: inventory one source file and its exact
dependency graph, compare the existing original platform contract,
obtain a canonical Synexia recipe/test, re-run it to an idempotent
fixed point, compile at the original Java/SWT/RWT target, and execute
native tests and protocol/viewport differential proofs. Only then
commit **generated original product results** through a separate
Nebula PR. Preserve donor licensing/NOTICE and old PR histories.
A GitHub `merged=true`, or branch `ahead>0`, is **never**
enough to authorize source restoration.

## Exact ledger verification (Java 21)

`tools/M3NebulaMergeRecoveryAudit.java` is a source-only,
credential-free checker, not a transformation recipe:

```sh
cd docs/m3/merge-recovery-20261029
javac --release 21 -Xlint:all -Werror tools/M3NebulaMergeRecoveryAudit.java
java -cp tools M3NebulaMergeRecoveryAudit --self-test
java -cp tools M3NebulaMergeRecoveryAudit \
  PR_HISTORY.tsv STACK_MERGES.tsv BRANCH_SOURCE_RECEIPTS.tsv RECOVERY_MAP.tsv
```

It verifies exact snapshot counts, 26 PR/head relationships, 15
ancestry holds, 82 path statuses, 17 unique candidates, hash
formats, source-to-candidate consistency and fail-closed
qualification decisions. It performs **no Git ref mutations**.
The independent local Java21 self-test passed; the actual
remote-ledger schema/cross-reference check was performed on
the four committed GitHub files. The full Java21 four-input run
is an additional CI gate and is not claimed run locally.

This snapshot is only as current as the pinned GitHub SHAs above.
Further merges may change the result: refresh by new source captures,
not by silently editing source blobs or claiming outdated status.
