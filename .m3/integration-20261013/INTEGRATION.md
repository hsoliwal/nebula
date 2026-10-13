<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula integration build 2026-10-13

Branch `m3/integration-20261013` puts every open Nebula pull request from 2026-10-13 into one tree, so that one
set of widget jars carries all of the day's Grid, tree/table and visualization work. The jars are built against
the SWT integration build (hsoliwal/eclipse.platform.swt, branch `m3/integration-20261013`). The individual pull
requests remain the units of review. Do not merge this branch before its parts are merged.

- **Base:** `origin/master` at `eab2a65c45`. It already contains #147 and #148.
- **Method:** forward merges only. Each pull request head is merged with `git merge --no-ff`, in the order of
  `CONSOLIDATION_20261013.md` (Synexia #10921). Nothing is rebased.
- **Re-run:** `build-integration.sh merge nebula`. The same script drives the SWT fork.
- **Record:** `MERGES.tsv` lists the head SHA of every pull request and the merge commit that brought it in.

## Merge order

| # | PR | Branch | What it brings |
|---|---|---|---|
| 0 | master | `master` | Base: #147 and #148 |
| 1 | #151 | `m3/grid-model-20261013` | Grid: packed sort/filter/grouping model, column window |
| 2 | #150 | `m3/tree-table-viewport-20261013` | xviewer, ctree, ctreecombo, tablecombo, calendarcombo and compositetable onto the viewport layer |
| 3 | #152 | `m3/nebula-viz-decimation-20261013` | Visualization and oscilloscope: packed decimation viewport |
| 4 | #153 | `m3/grid-issue-700-20261013` | Grid: index out of bounds in `GridItem.getCellSize` during item removal (#700) |
| 5 | #154 | `fix/tablecombo-close-popup-705` | tablecombo honours `setClosePopupAfterSelection(false)` (#705) |
| 6 | #155 | `fix/calendarcombo-popup-monitor-612` | calendarcombo keeps its pop-up on the combo's monitor (#612) |
| 7 | #156 | `ci/workflow-load-guards-20261013` | CI: bound the Actions load |
| 8 | #157 | `ci/workflow-load-guards-aux-20261013` | CI: bound the CodeQL and auxiliary M3 workflow load |
| 9 | #158 | `fix/m3-recipe-first-plan-ordinal-20261013` | CI (m3): recipe-first step 13 requires the current PLAN ordinal |

#156 to #158 were opened while this integration ran, and are merged after the list from the ledger. On the first
pass, #153 was already contained in #151's head. Later heads moved: #151 was merged three times, and #150 and
#152 twice each. Each later merge brought in only the new commits.

## Not merged

| Item | Reason |
|---|---|
| #149, current-master reproof | Optional per the directive. It pins a proof of the current master, which this tree no longer is. Set `INCLUDE_REPROOF=1` to merge it last. |

## Conflicts and resolutions

None. Every merge was clean.
