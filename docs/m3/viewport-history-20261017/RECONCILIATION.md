# Nebula M3 viewport PR history and additive reconciliation (2026-10-17)

This is a **historical metadata index**, not a claim that every PR diff and head workflow has passed review. The normalized `PR_HISTORY.tsv` lists the 238 returned GitHub PR records, sorted by PR number. It is a point-in-time snapshot. Counts: 214 merged, 24 closed-unmerged, 0 open. Repository: https://github.com/hsoliwal/nebula. Recent retrieved commit: `91c474935eb4a06e46716a0188b9082f0af0eda9` (commit-search ordering; verify exact branch head before promotion).

No existing production file, existing branch, base branch, PR title, license, or test was modified. No rebases, force pushes, merges, deletions or updates to old PRs were performed.

## How to read this ledger

- `merged=true` indicates GitHub PR state, **not** that a particular merged file's postimage is still in today's first-parent tree.
- For each candidate: compare **current source blob SHA**, source owner branch and any preserved checkpoints; classify semantic atoms and dependency closure; rerun stock/viewport differential and exhaustive Java/JNI/native tests at the actual receiving head.
- Closed-unmerged PRs are retained in this index and must be reviewed before deciding whether their deltas were superseded, absorbed via another route, or lost. Do not auto-cherry-pick them.
- Preserve EPL-2.0/Synexia notices and pinned donor attribution. Catalogue LeetCode, HackerRank, GeeksforGeeks and GitHub donors by category, separately from user-owned implementation authority.

## Verified source / branch status

- Current `master` `Grid.java` blob: `161ed50912df44440281dc09a6eb2377c28334f2`, 12,939 lines.
- `m3/grid-viewport-20261016` `Grid.java` blob: `e5061fa904f5c37ea791fffb931892f8ddb96c80`, 12,946 lines.
- Git branch comparison: Grid owner is **5 commits ahead and 11 behind** `master`, not a fast-forward. The five unique commits and their eight changed paths require FILE-level semantic comparison, not a wholesale merge.
- [#235](https://github.com/hsoliwal/nebula/pull/235) reports full Grid 604/604 native tests in both modes for its measured head; also labels the newer G3 range-selection/focus work WIP and not yet built. Those claims must not be extrapolated to today's `master`.
- [#236](https://github.com/hsoliwal/nebula/pull/236) reports Gallery's original mode matches all eight recorded seeds; viewport mode tested against only three fresh seeds, and the full eight-seed re-record and scale-oracle numbers are pending.

## Contract qualifications to close

1. Inventory the Grid G3 branch's five unique commits by source atom and determine if every existing M3 Grid selection contract, range/row mapping, reentry and observable event remains unchanged.
2. Compile and execute an exact-head grid original/viewport/stock oracle comparison, with tests for 1, 0, 10^4 and 10^6 items, variable heights, pinned rows, horizontal columns, selection/focus and mid-scroll structural mutation; no UI-thread O(n) rebuild.
3. Finish Gallery's eight-seed current-stock-to-viewport comparison, normalized pixel/scrollbar recordings, tiny client areas, SWT.VIRTUAL, disposal during paint, resource ownership and original fallback.
4. For both widgets, retain original `Canvas` and renderer contracts, no extra canvas wrapper, and invoke canonical Synexia Maven/OpenRewrite source-sealed recipes; pin donor notices and hashes.
5. Do not silently promote WIP G3 or invent GTK/Cocoa, DPI>100%, GPU or native look coverage absent platform evidence.

## Explicit PR anchors

- [#234](https://github.com/hsoliwal/nebula/pull/234) Grid G1; [#235](https://github.com/hsoliwal/nebula/pull/235) G2 and tests; [#236](https://github.com/hsoliwal/nebula/pull/236) Gallery L7; [#237](https://github.com/hsoliwal/nebula/pull/237) WIP checkpoint; [#238](https://github.com/hsoliwal/nebula/pull/238) attribution; [SWT #197](https://github.com/hsoliwal/eclipse.platform.swt/pull/197) order-statistic donor.

**Promotion: HOLD** for the unverified G3/Gallery leaves. No native UI tests were executed by this audit.
