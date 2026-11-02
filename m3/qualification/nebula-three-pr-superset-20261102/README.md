<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula source-preserving additive superset — PRs #309 + #310 + #311

Date: 2026-11-02. **DRAFT / source-custody merge only / product activation HOLD.**

## Immutable parents and public compatibility

- Target default `master` at `c08f92fc093e87b1e8912ef0846d09a659084baf` (not changed here).
- Receiving parent [#311](https://github.com/hsoliwal/nebula/pull/311) `8a91f6ab27e17116e46fbee1aa73ba6fb4e252bf`, Git tree `afe6c9b2b46df0290e221316017a737e1f0f79fe`; this head includes the complete ancestry of the ten-default-head donor consolidation [#309](https://github.com/hsoliwal/nebula/pull/309) at `1f7a870ae41c63c1c8e3f9dbb36d3813549b25c3`.
- Historical custody donor [#310](https://github.com/hsoliwal/nebula/pull/310) `6af6c73f8fce47da7818a865f52461d6428e5fe1`, Git tree `369b80b36159ee994746809b129b715dcd447d22`.
- Exact donor tree under `m3/qualification/history-source-recovery-20261102`: `e129c3faa959e8e1dd618deba725941f0a4603d4`; the parent `m3/qualification` tree is `d4ee082bf7d3780a8772466dba52354693a7a549`. The receiver's corresponding `m3/qualification` tree is `9d0d5f912dcfec14d4c7e02519fa6a0fc962b98b`, and has **no** `history-source-recovery-20261102` entry.
- The donor archive has 216 files (212 exact `.sourceblob` files, four provenance/README/INDEX files) and zero changed/deleted paths against `master`. Recursive Git tree listing of the subtree was **untruncated**, had 212 distinct sourceblob blob IDs, and all 216 blobs are `100644`. The 212 historical variants are data, not active Java/JNI code.
- The receiving #311 branch has 103 changed paths vs master; **zero path collisions** with the 216 donor archive paths. No active SWT/JNI Java source or test file will be overwritten by this archival union.

## Mechanical merge operation

1. Documentation first: commit this immutable plan to an isolated branch based exactly on #311; do not modify #311, #310, #309 or `master`.
2. Read the documentation commit tree SHA. Build a Git tree using that tree as `base_tree_sha`, adding exactly one directory entry: `path=m3/qualification/history-source-recovery-20261102`, `mode=040000`, `type=tree`, `sha=e129c3faa959e8e1dd618deba725941f0a4603d4`.
3. Create a **two-parent** commit with first parent equal to the documentation commit and second parent equal to the exact #310 archive head. Do not use an `ours` merge: the resulting tree must physically retain all 216 donor archive files.
4. Move only the isolated receiving branch via a non-force ref update guarded by the expected documentation-head SHA.
5. Verify both donors are ancestors via parent links; prove Git tree identity for the imported subtree and no deletions or modifications relative to the documentation head; verify #310 donor tree and files have not moved.
6. Create a draft PR against Nebula `master` for review. The complete source union includes #309 + #311 + #310 but remains HOLD until Java baseline, actual SWT Tycho/SWTBot/native GUI platform, provenance/license and runtime/ABI gates execute. Avoid claiming all of the original 109 fork PR branches have been audited.

## Locked behavior and limits

Public SWT/JFace APIs, Java/JDK execution environment, JNI symbols, widget selection/paint events, styling, serialization, runtime resource lifecycle and external behavior remain locked. Previously staged #309 includes JavaSE-11-to-17 manifest settings; do not activate it by merging to master without compatibility assessment. The #707 Grid row-index correction and #708 TableCombo regressions from #311 require actual native UI tests. The 212 historical source blobs must **not** be treated as replacements for live widget files without explicit source-level analysis and separate recipes/tests.

**Status:** source-level, additive Git graph union is authorized; runtime acceptance, product promotion and all-fork-PR closure are not demonstrated by ancestry.
