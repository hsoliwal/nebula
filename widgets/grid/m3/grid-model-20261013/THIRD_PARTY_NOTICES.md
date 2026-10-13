<!-- SPDX-License-Identifier: EPL-2.0 -->
# Third-party notices: Grid model leaf (2026-10-13)

The Grid model classes (`org.eclipse.nebula.widgets.grid.model`) and the Grid changes of this leaf are
written for Nebula. The algorithms follow the donors below. Exact commits, paths, hashes and the
decision for each donor are in `FOSS_REUSE_DECISION.tsv`; the hash of every donor file read is in
`DONOR_FILES.sha256.tsv`.

## Ported code

| file | donor | licence | what was ported |
|---|---|---|---|
| `org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/model/IntPermutationSort.java` | fastutil 8.5.14 `it/unimi/dsi/fastutil/ints/IntArrays.java` (Sebastiano Vigna) | Apache-2.0 | `quickSort(int[], int, int, IntComparator)` with `med3`, `swap`, `vecSwap`, `selectionSort` (Bentley-McIlroy), re-typed to the Grid row comparator. The file keeps the Apache-2.0 header and the original copyright line. |

## Patterns adapted (no code copied)

| donor | licence | licence copy |
|---|---|---|
| TanStack Table `packages/table-core` @95c47684 | MIT | `donor-licenses/TanStack-table.MIT.txt` |
| AG Grid Community `packages/ag-grid-community` @29c901c7 | MIT | `donor-licenses/ag-grid-community.MIT.txt` |
| MUI X DataGrid (free) `packages/x-data-grid` @b48a97ec | MIT | `donor-licenses/mui-x-data-grid.MIT.txt` |
| react-data-grid @f5f8abda | MIT | `donor-licenses/react-data-grid.MIT.txt` |
| Avalonia TreeDataGrid @914572f4 (archived) | MIT | `donor-licenses/Avalonia-TreeDataGrid.MIT.txt` |
| Glide Data Grid @0875d78c | MIT | `donor-licenses/glide-data-grid.MIT.txt` |

Qt `QSortFilterProxyModel` (qtbase @d42af390) is LGPL-3.0/GPL/commercial: read for its
source-to-proxy index mapping idea only, no code copied (`PATTERN_ONLY`). AG Grid Enterprise and
MUI X Pro/Premium are commercial and were not read (`NO_FIT`).
