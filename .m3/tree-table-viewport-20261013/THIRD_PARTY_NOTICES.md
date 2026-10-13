<!-- SPDX-License-Identifier: EPL-2.0 -->
# Third-party notices: tree/table viewport leaf (2026-10-13)

Code adapted or ported into this leaf comes only from EPL-2.0 sources:

| Source | Commit | Use |
|---|---|---|
| Eclipse Nebula Grid (`widgets/grid`, this repository) | eab2a65c | ADAPT: viewport projection, damage and visible-range patterns |
| hsoliwal/eclipse.platform.swt (`ViewportRuntime.RowWindow`, `VirtualViewportPlanner`, `VirtualTreeTopology`) | 0b6a31ff619b | PORT: int-only row window and packed tree topology patterns (SWT classes are package-private and are not called) |

MIT sources (TanStack Virtual/Table, react-component tree/tree-select/cascader, MUI X free edition, AG Grid Community,
Avalonia TreeDataGrid) and LGPL Qt (`QTreeView::uniformRowHeights`) were consulted as patterns only. No code, comments
or identifiers were copied from them. See `FOSS_REUSE_DECISION.tsv` for commits and paths.
