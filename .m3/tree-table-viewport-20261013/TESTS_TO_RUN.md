<!-- SPDX-License-Identifier: EPL-2.0 -->
# Tests to run when tests resume

FIX-ONLY mode (2026-10-13, user in an interview): no windows, no JUnit, no Tycho. Everything below is written and
compile-checked (javac 21, `-J-Xmx512m`) unless marked otherwise. Scripts live in the build workspace
`C:\m3-work-c\nebula-tt-build\scripts` (`run_all.sh`, `diff.sh`, `junit.sh`, `snippets.sh`, `bench.sh`); they are mirrored
in this run directory's `harness/` and `tools/`. Run one JVM at a time, memory-gated.

## Already run (before FIX-ONLY mode), evidence in `evidence/`

| Run | Result |
|---|---|
| Differential probe, ctree (receiver `01ad43be` minus later item-data lanes) | 434 rows: 418 EQUAL, 11 EQUAL_NONINVOKED, 0 DIFF, 5 NONDETERMINISTIC_BASELINE (GC handle id, focus timing) |
| Differential probe, tablecombo | 271 rows: 218 EQUAL, 53 EQUAL_NONINVOKED, 0 DIFF, 0 NONDETERMINISTIC_BASELINE |
| Differential probe, calendarcombo | 370 rows: 346 EQUAL, 24 EQUAL_NONINVOKED, 0 DIFF, 0 NONDETERMINISTIC_BASELINE |
| Differential probe, compositetable | 628 rows: 175 EQUAL, 139 EQUAL_NONINVOKED, 0 DIFF, 314 NONDETERMINISTIC_BASELINE; the first pre-image pass ran in a 1920x1009 host, so pre2 vs post was compared directly: 624/628 equal, the 4 others are wall-clock readings (`getCurrentTime`, `getDate`, `getStartTime`, `getEndTime`) |
| Differential probe, ctreecombo (before `01ad43be`) | 141 rows: 5 EQUAL, 136 DIFF, all from `getSize` 44 -> 75 (lazily built native items carried labels). Fixed in `01ad43be` (bare native items until the first drop, as the eager widget); **re-run required** |

## To run

1. Differential probes (post pass only; pre passes are cached by probe key):
   - `sh scripts/diff.sh ctreecombo CTreeComboFixture visible` — expect 0 DIFF after `01ad43be`.
   - `sh scripts/diff.sh xviewer XViewerFixture visible` — never completed on the final code (runs were stopped twice).
   - `sh scripts/diff.sh ctree CTreeFixture visible` — re-run for the item-data lanes (`09230ca3`).
   - `sh scripts/diff.sh tablecombo TableComboFixture visible`, `calendarcombo`, `compositetable` — re-run for the
     lazy accessibility attach (`83ad9672`) if the first run predates it (the recorded runs used `83ad9672`).
2. JUnit (`sh scripts/run_all.sh junit`):
   - `XViewerViewport_Test` (1,000,000 nodes, materialized-row bound, one request per row, `getData` identity, eager vs
     viewport ordering/filtering, round trip with pixel hash), `DummyTest` before and after.
   - `CTreeViewport_Test` (binary-searched painted window vs full scan at every 7 px, 1,000,000 nodes, round trip with
     pixel hash, item data in lanes with identity, body never takes focus). `-Xmx3g`.
   - `CompositeTableViewport_Test` (1,000,000-row collection, rows built only when shown, editor value/focus/caret round
     trip with pixel hash), and the existing `CompositeTableTest`, `CompositeTable_267316Test`, `NativeHeaderTest`,
     `CalendarControlTestSuite` before and after (upstream skips them, see EclipseNebula/nebula#396).
   - tablecombo `TableComboTests`, `TableComboViewerTests`: need SWTBot, not in the local repository — NOT_RUN until
     SWTBot is available.
3. Snippets and examples smoke run, before and after (`sh scripts/run_all.sh snippets`): CTreeComboSnippet,
   CheckTableComboSnippet, TableComboSnippet, TableComboViewerSnippet1, MyXViewerTest,
   XViewerStyledStringLableProviderTest, CalendarComboTester, Tester.
4. Benches, before and after (`sh scripts/run_all.sh bench`): xviewer 100,000 roots x 9 (1,000,000 nodes), ctree
   100,000 x 9 (`-Xmx3g`), tablecombo 100,000 rows + 200 combos, ctreecombo 10,000 x 9 + 200 combos (eager and
   `SWT.VIRTUAL`), calendarcombo 100 + 100 combos, compositetable 1,000,000 rows. Then `sh scripts/collect.sh`.
5. Synexia crate: `mvn -B -o -ntp -f synexia-openrewrite-recipes/recipe-crates/nebula-tree-table-viewport-20261013/pom.xml verify`.

## Upstream issue PRs (each from `origin/master`)

| PR | Test | Status |
|---|---|---|
| hsoliwal/nebula#154 — EclipseNebula/nebula#705 `setClosePopupAfterSelection(false)` | `TableComboClosePopupTests` (2 tests, opens a shell) | written, compile-checked, NOT_RUN |
| hsoliwal/nebula#155 — EclipseNebula/nebula#612 calendar pop-up monitor | `CalendarComboPopupLocationTest` (4 tests, pure geometry) | written, compile-checked, NOT_RUN; multi-monitor manual check NOT_RUN |

GTK and Cocoa: NOT_RUN for everything.
