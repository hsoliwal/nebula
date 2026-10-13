<!-- SPDX-License-Identifier: EPL-2.0 -->
# Tree/table family on the viewport layer (2026-10-13)

Leaf of the directive "all widgets in SWT, all in Nebula, all React, all in Qt, all implemented using native and
viewport". Owned widgets: `xviewer`, `ctree`, `ctreecombo`, `tablecombo`, `calendarcombo`, `compositetable`. Worked
serially in that order. Nebula Grid (#128, #140, #144, #148) is the in-repo donor; the SWT fork's
`ViewportRuntime.RowWindow`, `VirtualViewportPlanner` and `VirtualTreeTopology` are package-private and are ported as
patterns only (see `FOSS_REUSE_DECISION.tsv`). Nothing here depends on SWT internals.

## Laws applied to every widget

| Law | How it is met |
|---|---|
| Sparse logical extent; visible rows plus bounded overscan | Native half: `SWT.VIRTUAL` Tree/Table, which asks only for shown rows (`SWT.SetData` / JFace lazy). Custom-drawn half: Grid-style window computed from monotone row bottoms, not from a full scan. |
| No per-row objects until the API asks | Objects are created only for rows the native control requests or the caller's API returns; materialized-row membership is a packed `int[]` bit set. |
| Packed int/bit state, reusable arrays | Bit sets and `int` indices; no `long`, no `Objects.checkIndex`, no `String[].clone()` in shared logic (GWT-safe). |
| One control | Helper surfaces are created with `SWT.NO_FOCUS`; where a native helper can still take focus it hands focus back to the widget. |
| No paint/layout/measurement while not effectively visible | Dirty bit plus one catch-up on first paint or on the first public geometry read. |
| Paint clipped to damage | Work outside the damage rectangle is skipped when its output would be clipped anyway. |
| Public API results identical; additions additive | Pre/post differential probe over every extracted public/protected API row (`NEBULA_TT_API_COVERAGE.tsv`). Every intended difference is listed per widget below. |

## Residency (mirror of the SWT-side `ItemResidency` pattern; no SWT internals used)

Transient rows are thin handles created lazily by the parent; their state lives in the parent's bits and packed
arrays; cold payload is destroyed and recreated; identity is pinned only where the API hands objects to callers;
behaviour is selected by construction bit flags.

| Widget | Transient object | Where its state lives | Cold payload policy | Identity | Construction flag |
|---|---|---|---|---|---|
| xviewer (`SWT.VIRTUAL`) | native `TreeItem` rows | SWT native tree (SWT owns item residency); materialized roots as a packed `int[]` bit set in the bridge | sorted/filtered child arrays per parent: least-recently-used, dropped above `CACHE_BUDGET` (1,048,576 child references) and recomputed on demand | caller model elements are never wrapped; `TreeItem`s pinned by SWT once exposed | `SWT.VIRTUAL` |
| tablecombo (`SWT.VIRTUAL`) | native `TableItem` rows | SWT native table; materialized rows as a packed `int[]` bit set in `TableCombo` | none held beyond the bit set | `TableItem`s pinned by SWT once exposed | `SWT.VIRTUAL` |
| ctreecombo, calendarcombo, tablecombo | drop-down shell and list/tree/table | created by the parent on first use | built on first open/layout/API use | the parent's `getTable()`/`getItems()` contracts pin them once handed out | none (always lazy) |
| compositetable | row editor controls | parent's row window (`rows`/`spareRows`, recycled) | rows beyond the client area are recycled; nothing is built while hidden | rows returned by `getRowControls()` are pinned while in the window | none (always lazy) |
| ctree | `CTreeItem`/`CTreeCell` | still per-object fields | NOT_DONE: items are constructed by callers (`new CTreeItem(...)`) and returned by `getItems()`/`getSelection()`, so the API pins identity; a parent-owned virtual mode needs `setItemCount` (a no-op stub today) and is not attempted in this leaf | pinned by the API | — |

## Per-item state as bit lanes, selection and checks

Laws: every per-item boolean (expanded, has-children, selected, enabled, visible, painted, checked, grayed, ...) is a
bit lane in the parent; selection is packed in the parent and items are materialized only when `getSelection` asks;
check state is a parent bitset. Status per widget:

| Widget | State | Where it lives after this leaf | Status |
|---|---|---|---|
| xviewer | expanded, selected, checked | the native `Tree` (SWT owns the per-item state; nothing per item in Java) | NATIVE |
| xviewer (`SWT.VIRTUAL`) | materialized roots | packed `int[]` bit set in the bridge | DONE |
| tablecombo, CheckTableCombo | selected, checked, materialized rows | the native `Table`; materialized rows as a packed `int[]` bit set | NATIVE / DONE |
| ctreecombo | expanded, selected | the native drop-down `Tree` (rebuilt per drop as before) | NATIVE |
| ctree | item `painted`, `autoHeight`; cell `painted`, `hasChild`, `inited`, `holdChild`, checkable, checked | `CTreeBitLanes` in the `CTree`, indexed by a slot handed out at item/cell creation | DONE |
| ctree | item `enabled`, `visible`; cell `open` (expanded), `selected`, `visible`, `childVisible`, `toggleVisible`, `ghostToggle`, `mouseDown`, `mouseOver`, `mouseOverToggle`, `isGridLine`, `holdControl` | per-object fields | NOT_DONE: these are `protected` fields, i.e. public API for subclasses; moving them would remove API |
| ctree | selection | `protected List selection` of items in selection order | NOT_DONE: protected field, and `getSelection()` and shift-selection depend on insertion order, which a bit set does not keep |
| compositetable | row editors' own state | caller's row controls | N/A (caller-owned controls) |

The lanes use `int` words, not `long[]`, to stay GWT-safe as the platform law requires.

## Opt-in rule for the native virtual path

`SWT.VIRTUAL` is the SWT-native request for a viewport. A widget that wraps a native Tree/Table switches to the virtual
path only when the caller passes `SWT.VIRTUAL`; without it every result is unchanged. With it, rows the native control
has not asked for are not materialized, so item objects the caller could reach only by walking the native control
(for example `TreeItem.getData()` of a never-shown row) are not yet bound. This is the documented contract of
`SWT.VIRTUAL` itself.

## xviewer

- `XViewerViewportContentProvider` (new, public, additive): when the tree has `SWT.VIRTUAL` and the caller gives a
  plain `ITreeContentProvider`, the viewer serves it through JFace's `ILazyTreeContentProvider` protocol. Children are
  filtered by every viewer filter in order and then sorted by the viewer comparator, exactly as
  `AbstractTreeViewer#getSortedChildren` does (including `TreePathViewerSorter`). Sorted child arrays are cached only
  for parents the tree asked about and are dropped on any structural refresh, add, insert or remove.
  `getContentProvider()` returns the bridge; `getDelegate()` returns the caller's provider.
- Additive API: `XViewer#isViewport()`, `XViewer#getViewportMaterializedCount()`.
- Status line: visible-row count in viewport mode visits only items the tree or the viewer API expanded, never the root
  array; the bold status font is created once per viewer (it was created, and leaked, on every update); status layout
  is deferred while the status line is not visible and caught up once on its first paint.
- `getItemUnderMouseClick` fallback computes `row = y / itemHeight` (the uniform-row-height pattern) instead of
  summing over the root array; the result is identical, including `y < 0` and zero item height.
- `refreshColumn` in viewport mode touches only materialized root rows (bit set); unmaterialized rows get the new text
  from the label provider when the tree asks for them.
- Intended difference: the status `StyledText` is created with `SWT.NO_FOCUS` (helper surface), so
  `getStatusLabel().getStyle()` gains that bit.

## ctree

- Painted window and top item by binary search over the laid-out item bottoms (`CTreeLayout#isLaidOut` proves the
  visible list is the exact instance whose tops were assigned, so bottoms are non-decreasing); otherwise the original
  scan runs. The chained-bottom comparisons are unchanged, so the same rows are returned.
- Win32 native-grid line pass stops once a line can land neither in the paint image nor in the client area (same
  pixels; the following horizontal-line loop cannot run in either version).
- `layoutContent` assigns heights without one invalidation per item; the whole body is redrawn once, as before.
- One control: the body `Canvas` and header `Composite` are `SWT.NO_FOCUS`; the CTree owns focus, keys and traversal;
  the native header table hands focus back. Listeners still see one FocusIn/FocusOut per transition (the native one;
  the synthetic re-notification is no longer needed). Intended difference: after a click the display focus control is
  the CTree instead of its internal body canvas.

## Verification

Per widget, all run before (pre-image = `eab2a65c`) and after:

1. `api_surface.py` extracts every public/protected type, constructor, method and field of the main bundle(s).
2. `TtDiff` loads the pre-image twice (baseline determinism) and the post-image once, in separate class loaders over
   one SWT, and probes every API row against a fresh fixture: result or exception, plus a snapshot of every public
   getter of the primary widget. Verdicts: `EQUAL`, `EQUAL_NONINVOKED` (signature equal, not invoked: modal or native
   dialog, no instance), `DIFF`, `NONDETERMINISTIC_BASELINE`.
3. Existing Nebula JUnit tests and the widget's examples/snippets, plus the new viewport tests (1,000,000 nodes for
   xviewer and ctree, materialized-row bound in the style of Nebula #140).
4. Bench: populate, first paint, scroll-frame p50/p95/max, scroll-to-end, expand-frame p50/p95/max, Java heap and
   process private bytes, pre vs post, one JVM per image.

Only Win32 was run. GTK and Cocoa are NOT_RUN.
