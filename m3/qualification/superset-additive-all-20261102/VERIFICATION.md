<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula additive three-open-PR source merge execution record

Dated 2026-11-02. This is a **real new integration-branch merge**, not a merge to `master`, and not a claim that Tycho/SWT/ABI/runtime tests passed.

## Frozen merge identities and executed Git operations

- Incoming source chain (PR #311 including #309): head `8a91f6ab27e17116e46fbee1aa73ba6fb4e252bf`, original Git tree `afe6c9b2b46df0290e221316017a737e1f0f79fe`.
- Independent historical data (PR #310): head `6af6c73f8fce47da7818a865f52461d6428e5fe1`, archive-only directory tree `e129c3faa959e8e1dd618deba725941f0a4603d4`.
- Documentation-first receiving branch commit `5addd94ba62dc268121951e67f7d03f48bbf633e`, tree `26ea314166d913bb87a8419f7b41ab9f8776b0e7`; source receipt `README.md` is retained.
- Additive candidate tree `86b4b6742b317b8175a6f17a8a3ef67d974b3a1e`. Inspected root, `m3` and `m3/qualification` trees before/after: the **only** change was adding the exact archive directory `040000 tree e129c3faa959e8e1dd618deba725941f0a4603d4` below `m3/qualification`. All four existing qualification children retained their exact Git tree entries; all widget/source subtrees retained their identities.
- Actual two-parent Git commit `52dd848e7c299ce01efeaccba062163288dd8302` has parents **[documentation/source `5addd94ba62dc268121951e67f7d03f48bbf633e`, donor `6af6c73f8fce47da7818a865f52461d6428e5fe1`]** and exact candidate tree. Its metadata was read back independently. The new integration branch was moved by non-force fast-forward, with its expected previous commit SHA checked and remote branch ref read back.
- Both parents' complete Git object histories are retained. This is **not** a no-op ancestry-only merge; the 216 inert donor archive paths are physically present in the result.

## Actual after-merge GitHub comparisons

| Comparison | Ahead | Behind | Changed paths | Added | Modified | Removed |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| #311 head → two-parent merge | 4 | 0 | 217 | 217 | 0 | 0 |
| #310 head → two-parent merge | 47 | 0 | 104 | 60 | 44 | 0 |
| #309 head → two-parent merge | 9 | 0 | 222 | 219 | 3 | 0 |

The apparent modifications relative to #310 are pre-existing #309/#311 product source changes, **not edits to its historical objects**. The resulting #309 and #311 source identities are preserved. #310's archive tree SHA proves all 212 historical source variants and its index are retained exactly.

**GitHub comparison-size caveat:** direct `master` → merge comparison returned **300 file rows**, the GitHub compare response cap, and therefore is **not exhaustive**. This record does not represent the capped result as a complete all-file count. Full scope can be decomposed into the independently checked #311 parent (103 changed source/docs paths vs `master`) plus the exact, disjoint archive (216 paths) plus the new `README.md` (one path), for 320 disjoint changed paths at the merge commit; this excludes this subsequent ledger file. To certify the final full-tree diff independently, use Git's uncapped `git diff --name-status` on a complete checkout.

## Remaining merge gates (fail-closed)

- The #309 donor branch changes multiple required JavaSE execution environments from 11 to 17. No compatibility concession was authorized. These changes require baseline-preserving treatment and bundle/manifest/JDK review.
- Tycho/Maven build, exact compiler/java target, SWTBot/GTK Xvfb/Windows/Cocoa GUI event and resource lifetime tests, native regressions, Eclipse API/JNI compatibility, full donor/fork PR branch inventory: **NOT RUN** in the GitHub-only Git Data merge.
- #310 contains only inert data. Historical Java source blobs must never be silently revived as executable product sources.
- Do not mark this review PR ready, or merge it into `master`, until every blocking gate has actual source-compiled and runtime receipts. Git hash/tree equality is necessary source custody evidence, not runtime proof.
