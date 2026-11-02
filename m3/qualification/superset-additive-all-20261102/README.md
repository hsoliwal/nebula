<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula additive all-open-PR source and historical-custody superset — 2026-11-02

Status **DRAFT / HOLD / NOT RELEASED**. This is the bounded union of the **three verified open PRs** in `hsoliwal/nebula` (#309, #310, #311). "All" here means these inventoried, explicitly named open PRs, **not** every possible unlisted fork PR branch, not approval to weaken SWT/JavaSE compatibility and not a claim of runtime proof. There are no product source renames, removals or overwrites in this archive admission.

## Exact parents and inventory (immutable Git SHA)

1. [PR #309](https://github.com/hsoliwal/nebula/pull/309) — source-superset branch `m3/github-donors-superset-20261031`, head `1f7a870ae41c63c1c8e3f9dbb36d3813549b25c3`; already integrated as a Git ancestor of #311. Its 10 ahead-of-upstream donor heads are present in history; semantic resolution and Java minimum compatibility remain unqualified.
2. [PR #311](https://github.com/hsoliwal/nebula/pull/311) — receiving source tree head `8a91f6ab27e17116e46fbee1aa73ba6fb4e252bf`, tree `afe6c9b2b46df0290e221316017a737e1f0f79fe`. Adds bounded Grid stale-row guard and native TableCombo test/snippet reconciliation from upstream #707/#708. `master` pinned at `c08f92fc093e87b1e8912ef0846d09a659084baf`. The existing source tree is retained **without changes** except for this document and the independent archive directory.
3. [PR #310](https://github.com/hsoliwal/nebula/pull/310) — second donor parent `6af6c73f8fce47da7818a865f52461d6428e5fe1`, Git tree `369b80b36159ee994746809b129b715dcd447d22`. Contains **216 new paths** beneath one inert, absent-in-receiver directory `m3/qualification/history-source-recovery-20261102/`, including 212 historical source blob variants plus indexes. Exact donor directory tree object `e129c3faa959e8e1dd618deba725941f0a4603d4` (`040000 tree`).

## Additive serial merge plan

- Verify the entire archive directory is **absent** in #311. This was checked by walking both pinned Git trees: receiving `m3/qualification/` does not contain the archive; donor has exactly that tree object. Confirmed `compare(#311,#310)` has **216 added, 0 modified, 0 removed**, and donor's 216 paths share the same archive root.
- Publish this documentation on a new branch from the exact #311 head **before** tree/source merging.
- Make a new tree based on that documentation commit's tree, adding **one** existing donor tree object at `m3/qualification/history-source-recovery-20261102`; preserve its subtree SHA, file modes and all 216 paths. Do not copy a sourceblob into executable Java locations.
- Create a **real two-parent Git merge commit**: first parent = the documentation-bearing receiving branch; second parent = #310 exact head. Update only the newly created integration branch, non-force, with its current SHA leased. Existing PR heads and `master` remain unchanged.
- Compare the final tree to both original parents; require exact archive file object identities, no deleted paths, no accidental modifications to the original 103 changed paths from #309+#311, source-history ancestry of #309, #310 and #311, and repeated tree fixed-point identity. Failed checks mean HOLD, not a merge claim.
- Open a review-only PR targeting `master`; retain the original PRs. This does not perform any unqualified default-branch merge.

## Blocking correctness gates

- Original donor #309's JavaSE-11 → JavaSE-17 required execution environment changes need **explicit compatibility resolution**. Eclipse/Tycho/SWT bundles may not change Java baseline silently.
- Run the full original Nebula reactor (compiler, Tycho, JUnit, SWTBot, RAP/Win/Cocoa/GTK/Xvfb) and widget event/resource-leak oracles. No such runtime proof has been executed in the GitHub-only merge environment.
- Validate EPL-2.0/Apache provenance, source-vs-archival-data separation, all fork PR branches beyond the surveyed 109 default-branch heads, SWT public API and baseline behavior.
- Promotion is gated on actual terminal executable CI; GitHub mergeability or source SHA checks alone are insufficient.

No third-party challenge solution source (LeetCode, HackerRank, GeeksforGeeks) is imported. Their problem-category taxonomies remain assigned to the canonical Synexia Java/JNI recipe owner, not copied into this SWT application fork.
