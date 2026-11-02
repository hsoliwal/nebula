<!-- SPDX-License-Identifier: Apache-2.0 -->
# Additive admission of concurrent duplicate PR #313

State: **DRAFT/HOLD**. This is documentation and historical Git ancestry only; it does not promote the SWT source union to `master`.

Newly inventoried [PR #313](https://github.com/hsoliwal/nebula/pull/313) independently represents the same three source/donor PRs #309, #310, #311 as [superset PR #312](https://github.com/hsoliwal/nebula/pull/312). Its actual head `4eed8d8750d4b784cf1ccad3095204288ebb4161` has Git tree `46b992d6d175af64dabc30e505a18a3c658f30b8` and a verified two-parent merge with #310 at `6af6c73f8fce47da7818a865f52461d6428e5fe1`.

The Git-tree comparison shows the receiver and PR #313 share identical `m3/qualification/history-source-recovery-20261102/` tree SHA `e129c3faa959e8e1dd618deba725941f0a4603d4`: **216 files, no difference and no source recopy**. The other existing `m3/qualification` directories `github-fork-pr-consolidation-20261102`, `grid-row-count-master-20261022`, and `upstream-open-pr-reconciliation-20261102` also retain identical tree SHAs. Receiver #312 additionally has its own `superset-additive-all-20261102` verification records; PR #313 contributes only its independently documented `m3/qualification/nebula-three-pr-superset-20261102/` directory, absent on receiver and carrying exact subtree Git SHA `afbba1563f3b1317368b4fbdc4adf020c2e8d829`.

Admission is **exact**: after writing this specification, add only that single donor subtree to the receiving tree, create a real Git two-parent commit (first: #312 receiving documentation head; second: PR #313 head), and move **only** #312's dedicated integration branch with non-force expected-head lease. Require unchanged SWT source blobs, unchanged archived source tree SHA, no deletions or source-path overwrites and both original PR histories as ancestors. Never infer semantic equivalence from matching category labels; this duplicate has exact Git tree witnesses.

The user requested **SUPERSET ADDITIVE MERGE ALL**. This operation only closes the newly inventoried #313 ancestry against the already implemented Nebula PR family, **not** all branches of all 109 surveyed forks or the entire `hsoliwal/*` estate.

**Release HOLD:** The combined source lineage still includes JavaSE-11-to-17 execution-environment changes and untested SWT/Tycho/GTK/Win/Cocoa/RAP behavior. No master release or breaking Java baseline upgrade is authorized by this Git object merge.
