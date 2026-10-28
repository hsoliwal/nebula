# M3StackLayout: a stack layout that materializes only the top control

Matrix row STACK_LAYOUT (P0, user decision 2026-10-19). Bundle `org.eclipse.nebula.widgets.m3`, class
`org.eclipse.nebula.widgets.m3.M3StackLayout`, a subclass of SWT's `org.eclipse.swt.custom.StackLayout`.
Swing twin: `javax.swing.M3StackLayout` (row CW-STACK_LAYOUT in `../ENHANCED_API_MAP.tsv`, handed off).

## Stock behaviour (measured from the source of SWT 3.135.0, `StackLayout.java`)

- `layout` visits every child of the composite, in `getChildren()` order, and calls `setBounds(clientArea - margins)`
  and then `setVisible(child == topControl)` on each one. With n children that is n `setBounds` and n `setVisible`
  native calls per layout, whatever changed.
- `computeSize` calls `computeSize(wHint, hHint, flushCache)` on every child and returns the maximum plus the margins;
  a hint replaces the computed value.
- `flushCache` returns true. Every child is a live native control for the lifetime of the composite.

## What M3StackLayout does

Two ways to use it.

**1. Drop-in (children).** Same fields (`topControl`, `marginWidth`, `marginHeight`), same `computeSize`. `layout`
gives bounds to the top control only and parks every other child: `setVisible(false)` is called only on a child that
is currently visible. A parked child gets its bounds when it becomes the top control.
Native calls per layout: 1 `setBounds` plus one `setVisible` per child that changes, instead of 2n.

**2. Pages (virtual).** `setPages(parent, count, provider)` declares `count` pages, none of them created.
`showPage(index)` creates the page through `M3PageProvider.createPage(parent, index)` when it is not resident, makes
it the top control and lays the parent out. At most `getResidencyBudget()` pages (default 8) stay resident. When the
budget is exceeded the least recently shown page that is not on top is handed to `M3PageProvider.releasePage(index,
page)` (default: `dispose()`), so the client can save its state. A page disposed by the client is dropped and created
again on its next `showPage`. Work per `showPage` is O(budget), independent of `count`; one million pages cost the
same as ten.

## Contract differences (documented, by design)

| Aspect | StackLayout | M3StackLayout |
|---|---|---|
| bounds of a hidden child | updated on every layout | updated when the child becomes the top control |
| Move/Resize events of a hidden child | on every layout that changes its bounds | when the child becomes the top control |
| a hidden child that the client shows with `setVisible(true)` between layouts | shown at the stack area | shown at its last bounds (none if it was never on top) until the next layout hides it again |
| Show/Hide events, visibility, top bounds, computeSize, pixels | | identical (recorded differential) |
| pages (page mode only) | every child is a live control | only resident pages exist; `getChildren()` lists the resident pages; `computeSize` is the maximum over resident pages |

`-Dswt.widget.mode.M3StackLayout=original` (or `-Dswt.widget.mode=original`) turns the layout into the stock
algorithm: every child is laid out on every layout, and in page mode the budget is unlimited (pages are created on
demand and never released).

## Data (DOOP)

Resident pages live in lanes (`internal.M3ResidentPages`): `int[] pages`, `long[] ticks` (last shown), `Control[]
controls`, sized to the budget. The page index is the identity; a `Control` exists only while the page is resident.
No per-page object, no map entry, no allocation per `showPage` after the lanes have grown to the budget.
Eviction picks the smallest tick among the slots that are not on top (exact LRU, deterministic: which page is
released is visible to the client). Pattern donor: M3jdk21 `com.m3.collections.M3BoundedCache` (lanes, no entry
objects); CLOCK is replaced by exact LRU because the release order is observable. See `ALGORITHM_CATALOGUE.tsv`.

## Proof

- `M3StackLayoutDifferentialTest`: seeded scripts (add and dispose children, set `topControl` to a child, to null or
  to a foreign control, change margins, resize, `layout(true|false)`, `computeSize` with and without hints, hide and
  show the parent) run side by side on a composite with `StackLayout` and one with `M3StackLayout`, on the official SWT
  3.135.0 jar on a hidden desktop. Every step compares visibility of every child, the top control's bounds,
  `computeSize`, Show/Hide events and the composite's pixels (WM_PRINT).
- `M3StackLayoutTest`, `M3ResidentPagesTest`: API, errors, budget, eviction order, external dispose, both modes.
- Coverage: JaCoCo over both modes; report in `coverage/`.
- Example: `M3StackLayoutExampleTab` (Nebula examples view) and `M3StackLayoutSnippet` with 1,000,000 pages.

## Status

See the PR (hsoliwal/nebula#261) and `../EXAMPLES.md`.
