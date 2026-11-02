# SWT Nebula GitHub fork and PR additive-consolidation qualification

Audit date: 2026-11-02. Repository: `hsoliwal/nebula`. Candidate branch: `m3/github-donors-superset-20261031`. Pinned pre-audit head: `c8cbc138701af776d4e8b2f46d6ea60d26c50049`; receiving master: `c08f92fc093e87b1e8912ef0846d09a659084baf`. Original EclipseNebula master: `5d0bcbb9c6e3a118685a5ec91c6f5d8eb7833893`.

## Scope and tree facts

- [PR #309](https://github.com/hsoliwal/nebula/pull/309) reports **109 forks surveyed** and ten default-branch heads ahead of original upstream. That 109 count is inherited from the PR's recorded sweep, **not** independently re-enumerated in this audit.
- The candidate history contains **all ten selected donor heads as ancestors**, two via clean merges and eight via individually resolved two-parent merges. The eight original merge conflicts are **not still pending in the candidate commit graph**. The older PR #309 summary was stale. See `FORK_DONORS.tsv` for the exact donor and resolution SHA ledger.
- From `master` to the pinned candidate, GitHub compare reported 38 commits, 96 changed paths (2,971 additions; 246 deletions), and zero commits behind. Five representative blobs were compared between the donor-resolution ref and the final candidate; all five matched: FloatingText, PropertyTable, Timeline AnnotationFigure, CKEditor filebrowser, and the Maven workflow. This is a sampled identity check, not full path-by-path equivalence.
- Some donor content is **subsumed** rather than duplicated: minova-afis CI caching, Pontesegger's older Timeline import and jmidvidy's upstream-landed XViewer changes have zero net tree delta from the pinned anchor. Their histories remain in the candidate's parent graph.
- A Git merge parent does not by itself prove all donor behavior is functionally present. Conflict decisions in historical merge commit messages must be accepted against current APIs, observable widget behavior, licensing and native SWT/Draw2D tests.

## Upstream PRs and separate receiver branches

At audit time [EclipseNebula/nebula#707](https://github.com/EclipseNebula/nebula/pull/707) was an **open draft**, and [#708](https://github.com/EclipseNebula/nebula/pull/708) was **open**. Their patches are **not** present verbatim in the candidate. Their four relevant production/test signatures were absent. The target fork has divergent partial solutions:
- #707: `GridItem.getCellSize` already uses the receiver instead of reloading its own row and cites issue #700, but the upstream early row-index bounds guard and regression are not present. Both upstream patch hunks fail exact-context applicability on the current Grid fork.
- #708: `TableCombo.internalLayout` already avoids recreating the popup unless its parent shell changes and cites issue #705. The upstream open/restore popup logic and SWTBot regressions are absent; adding another `dropDown(true)` can change event behavior. The current target's test and TableCombo source patch hunks have unique contextual match with line drift, but **textual applicability is not behavioral proof**. Do not blindly transplant it.
- The ten ahead-of-upstream donor forks individually had **zero open PRs** in GitHub search at audit time; that is *not* an exhaustive inventory of 109 fork repositories, all closed/unmerged PRs, or branches not named `master`.
- [hsoliwal/nebula#310](https://github.com/hsoliwal/nebula/pull/310) remains a separate open draft **HOLD** on 212 historical source objects. Do not equate inert custody objects with source restoration or product compatibility.

## Verification and merge gates

1. **NO CI PASS:** GitHub reported zero PR-triggered workflow runs and zero combined-status entries at the pinned #309 head. Individual historical commits describe local `javac`, unit and runtime probes; this audit did not execute or reproduce them. No tests/builds/native SWT runs were executed here.
2. **Compatibility HOLD:** The #309 diff changes multiple `Bundle-RequiredExecutionEnvironment: JavaSE-11` entries to `JavaSE-17`, along with compiler/project metadata. This is a minimum-Java contract change, not automatically additive. Preserve Java 11 consumers or separately authorize and qualify a Java 17 baseline change.
3. **Behavior HOLD:** Run `mvn verify` on the exact current candidate, compile all affected Tycho bundles and assert existing public/protected descriptors, event ordering, resource disposal, native pixel behavior and SWT GTK/Xvfb regressions; qualify applicable Win32/Cocoa/RAP and version compatibility separately. Do not infer a green gate from a workflow file's presence.
4. **Provenance HOLD:** Verify CKEditor source/version/license and EPL-compatible reuse. Review every conflict-resolution decision (especially XViewer deleted method avoidance, Timeline lanes, and TableCombo) against upstream history.
5. **Completeness HOLD:** Inspect every fork's open PR branches, and explicitly classify closed-unmerged PRs as rejected, obsolete, already landed, or eligible; the existing ten-default-head sweep is not an all-PR audit. Use SHA-locked compare and source hashes; perform one leaf and one gate at a time.

The source snapshot, upstream Git repos, PR metadata, compare stats, patches, branch histories and five byte-identity witnesses were **read** in this audit. This commit adds documentation only. **No product code was modified and no merge to `master` was performed by this audit.**
