# Every widget is a viewport: leaves done

One row per finished leaf (the ledger `LEDGER.tsv` keeps the inventory as it was read; this file records what changed
since). Gates: `tools/build_bundle.sh --tests <folder>` and `tools/run_bundle_tests.sh` on the hidden desktop, with the
official SWT 3.135.0 jar and with the M3 SWT fork jar (SWT.PartVisible / SWT.PartHidden).

| Date | Widget | Aspect | Change | Gate |
|---|---|---|---|---|
| 2026-10-24 | Grid (nebula/grid) | D | G5: blit scroll for rows of differing heights and tree rows by the row tree's exact pixel delta (branch m3/grid-g5-variable-blit-20261024, efee32d6) | GridBlitScrollVariable_Test 2/2, 287 Grid tests |
| 2026-10-24 | Tiles (nebula/tiles) | H | TilesViewport: the animation timer runs only during a transition while the widget is shown (stock: a 33 ms timer for the widget's whole life, stepping and redrawing while hidden); a hidden transition parks with its wall clock anchored and reaches the stock final frame on reveal; the stock resize debounce no longer throws after dispose in viewport mode | TilesViewportTest 4/4 on three runs (official jar twice, M3 fork once); skipped in the original mode |

## How a leaf is made

1. The ledger row names the shape, the missing aspects and the hooks.
2. New logic goes in new files of the widget's package (`<Widget>Viewport`), the mode switch is the bundle's own copy of
   `internal/WidgetMode.java` (and `SwtLog.java`), as Gallery, CompositeTable and TableCombo do.
3. Hooks into the upstream classes are rows of a recipe in the Synexia crate `m3-viewport-catalogue-recipes`
   (`META-INF/rewrite/m3-nebula-<widget>-viewport.yml`: `M3AddMemberIfMissing`, `M3InsertStatements`), applied with
   `M3PerFileApply`; the second application must report UNCHANGED.
4. The acceptance test lives in a `<bundle>.tests` fragment (package-private access to the viewport counters) and skips
   in the original mode.
