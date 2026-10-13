# Nebula superset consolidation, 2026-10-13

## Purpose

`master` of `hsoliwal/nebula` is meant to be one superset: every fix and every wanted feature from
both the integration line and the per-PR merges, with the newer and more complete side winning
where the same code differs. Three merges on master recorded their pull request head as a second
parent but kept the first parent's tree, so the code of those pull requests never reached master
("ancestry-only" merges). Thirty-six pushed branches also still changed master when merged.

Branch `m3/superset-consolidation-20261013` fixes both:

1. It replays the ancestry-only merges as real content: `C = commit-tree <head>^{tree} -p <base>`,
   then `cherry-pick -X ignore-cr-at-eol C`, then conflict resolution.
2. It merges every live branch that still changes master, or records why master already supersedes
   it. Nothing on master is deleted.

Superset rules used for every conflict: keep every feature, fix and test from both sides; where the
same logic differs, keep the later and more complete side and carry over the other side's extra
members; keep the design laws (packed int/long masks and Xmx-aware parent-owned arrays, lazy
objects, viewport and widget as one control, no paint while invisible, visible-and-damaged repaint,
exact edges, redraw never materializes items); the code reads as Nebula/SWT code; no conflict
markers; no line-ending-only changes.

Base: `origin/master` = `b86f35e9a15a414459a9a3b5b7407b7c38fd4dda`.

Status: the branch is pushed and offered as a pull request; the fork's ruleset ("Only hsoliwal may
modify branches") leaves the merge to the owner. The draft release `m3-superset-20261013.2` targets
this branch.

## Ledger

Actions: REPLAYED (ancestry-only merge replayed as content), MERGED (`git merge --no-ff -X
ignore-cr-at-eol`), SUPERSEDED_BY_MASTER (not merged; master already has the newer or the adopted
form; the branch stays as it is), NO_EFFECT (merging changes nothing). Files are the branch's own
delta against its merge base.

| # | Source | PR | SHA | Action | Files | Decision |
|---|---|---|---|---|---|---|
| 1 | `ancestry-only merge eab2a65c (PR head)` | #148 | `d716ac6221` | REPLAYED | 6f, 474+, 21- | GridRowSpanIndex (sparse row-span frontier, hidden-column semantics), Grid, GridItem, GridItemDataVisualizer and tests were missing from master; replayed clean as 8fff47df. |
| 2 | `ancestry-only merge 5bdd304f (PR head)` | #153 | `2f3066f1ef` | NO_EFFECT | 0f | Replaying 2f3066f1ef against c133b55333 changes nothing: master already has GridIssue700_Test's threshold search. Recorded only. |
| 3 | `ancestry-only merge b86f35e9 (integration head)` | #159 | `cb969381e4` | REPLAYED | 4f, 424+ | Adds the integration records (.m3/integration-20261013/INTEGRATION.md, MERGES.tsv, build-integration.sh, jni_check.py); 38 conflicts, master side kept, see below. 016eb527. |
| 4 | `m3/grid-internal-heap-growth-20261013` | #161 (draft) | `2741bf433d` | MERGED | 7f, 208+, 32- | Xmx-aware atomic multi-lane growth before packed-row mutation, sparse billion-row RowBitLanes masks, overflow-safe insertion, int-boundary HeapGrowth; Grid 1.2.1 -> 1.3.0. Clean. 53f79b9f. |
| 5 | `m3/grid-row-addressed-data-20261013` | - | `b8ec22c3d1` | MERGED | 4f, 378+, 4- | Row-addressed Grid visual data (GridRowAddressedItemDataVisualizer, default visualizer). 3 Grid.java conflicts kept both sides; integrated with PR #148's span index, residency and column window (see fixes). 7e4a8198. |
| 6 | `m3/current-master-full-recipe-reproof-20261012` | #146 (closed) | `7f1a44da5e` | MERGED | 1f, 53+ | m3/proof/current-master-full-reproof-20261012.md, a proof-request record (it states that it is a trigger only). Must-merge per the consolidation order; PR #146 was closed without an execution proof, and the record claims none. Clean. a01e4e62. |
| 7 | `docs/development-guide-20261005` | - | `e211c35785` | MERGED | 2f, 80+ | docs/DEVELOPMENT_GUIDE.md plus a README link; README conflict kept master's licensing section and the link. 23ba7e3e. |
| 8 | `m3/declarative-local-artifact-bootstrap-20261005` | #55 (closed) | `2182d5690b` | SUPERSEDED_BY_MASTER | 1f, 2+, 2- | CI edit (install instead of compile in m3-declarative-openrewrite step 2). PR #55 was closed as superseded by merged #59, which master carries: step 3 runs the proof through m3/reactor.xml -pl recipe-first -am. Merged first, then dropped from this branch before push (history rebuilt, see fixes). |
| 9 | `m3/signal-chain-current-synexia-20261005` | #53 (merged at 35b9dd2d) | `2e34660815` | SUPERSEDED_BY_MASTER | 1f, 2+, 2- | Its one commit after the merged #53 head is the same install edit as PR #55; superseded by #59 on master. |
| 10 | `m3/second-pass-signal-chain-20261004` | #49 (merged at 1c9ab032) | `5fef90affd` | MERGED | 2f, 2- | Drops the blank line at end of file in m3/README.md and m3/docs/second-pass-signal-chain.md (git diff --check). Whitespace, not line endings. 30e9cc99. |
| 11 | `m3/verified-transfer-oracle-20261011` | #139 (merged at 22c0fe53) | `cb858949f3` | MERGED | 2f, 6+ | Hosted verified-transfer proof request and fail-closed state TSVs beside master's current-master-transfer-oracle.tsv. New files. e87f2a6a. |
| 12 | `m3/visible-range-runtime-final-current-20261004` | - | `2762d3500d` | MERGED | 4f, 447+, 91- | 8 visible-range event/paint/screenshot contract tests, runtime-tests.tsv, RUNTIME_GATES.md, failIfNoTests, full-grid-contracts profile; pom and test conflicts kept both sides (17 tests). 805d008f. |
| 13 | `m3/svgloader-length-atomization-20261004` | - | `0bc534cffe` | MERGED | 2f, 102+, 21- | SvgLoaderLengthTests added. The SvgLoader.java edit is SUPERSEDED_BY_MASTER: master seals SvgLoader.java (sha256 23d730f0...) as the raw preimage of NebulaM3SvgLoaderLengthConvergenceRecipe, which produces the same atomization as a candidate. 7c84bc15. |
| 14 | `m3/method-signal-evidence-20261004` | - | `e2f4e99ee7` | MERGED | 37f, 4551+, 25- | Method-coordinate inventory/search/JNI review evidence and its sealed recipe. Seven of its eight target files on master equal the recipe's sealed preimages and merge to its postimages. Adapted to master's OpenRewrite 8.90.4 (see fixes); workflow gets master's concurrency and fork guard. dc4aeb0f. |
| 15 | `aix/nebula-catalogue-apply-20261004` | #41 (closed) | `757c653a80` | SUPERSEDED_BY_MASTER | 5f, 569+ | Master has the same catalogue application (CATALOGUE_APPLICATION.md, apply_catalogue.py, plan) with the later refreshed base pins (62ef3a81, 85f3f66e); the branch only carries the older pin 887cf825. |
| 16 | `codex/m3-standard-openrewrite-output-20261007` | #107 (merged) | `000560f402` | SUPERSEDED_BY_MASTER | 11f, 557+, 336- | Formatting-only recipe output (split declarations, braces). 8 of 11 files are on master byte-for-byte and CalendarCombo carries the same reformat plus later fixes; the rest (BadgedLabel, CustomCombo) would put 4-space indentation into tab-indented SWT files and conflicts with master's 2026-10-13 SWT idiom pass of CustomCombo. |
| 17 | `m3/sealed-offline-owner-20261004` | #44 (merged at 74f97f3e) | `84d3eaef5a` | SUPERSEDED_BY_MASTER | 11f, 557+, 336- | Same content as codex/m3-standard-openrewrite-output-20261007 (merge of #107); same decision. |
| 18 | `codex/openrewrite-recipes-20261004` | - | `600282de02` | SUPERSEDED_BY_MASTER | 410f, 107955+ | Checked-in copy of the Synexia recipe crate (.rewrite/recipes, re2j, OpenRewrite 8.71 shims). Master's M3_CANONICAL_RECIPE_HOME_V1 makes hsoliwal/com.synexia the only recipe implementation home (local role ADAPTER_FIXTURE_PROOF, authoritative_recipe_implementation_allowed=false) and binds it by commit (install_bound_synexia_recipe.sh). |
| 19 | `feat/m3-recipe-first-bootstrap-20261004` | #2 (closed) | `22371b1a0f` | SUPERSEDED_BY_MASTER | 6f, 14+ | First recipe-first sidecar (.m3/openrewrite-recipes, one-line minified sources). Master's m3/recipe-first module is its evolved, tested successor. |
| 20 | `m3/atomize-patternize-control-plane-20261004` | #6 (closed) | `0e84589644` | SUPERSEDED_BY_MASTER | 20f, 2311+, 73- | com.m3.rewrite sidecar (M3Java21ConvergenceRecipe, pure-int atom recipes, m3/pom.xml, m3-nebula-convergence.yml). Master has the renamed successors (org.eclipse.nebula.m3.rewrite.NebulaM3*), m3/reactor.xml and m3-nebula-recipe-first.yml. |
| 21 | `m3/typed-hierarchy-recipes-20261004` | #4 (closed) | `a56d517251` | SUPERSEDED_BY_MASTER | 4f, 104+ | .m3/atom-pattern overlay; 98% of its lines are on master, which carries the later policy values. |
| 22 | `m3/nebula-canonical-recipe-dag-20261004` | #13 (closed) | `cf2260ff27` | SUPERSEDED_BY_MASTER | 40f, 6091+, 26- | 90% of its lines are on master. The rest belongs to NebulaM3RepositoryReviewRecipeDag, which master's compile-bootstrap recipe removed on purpose (its sealed before-image still imports the DAG), plus old pins in the workflow, poms and PLAN.tsv. |
| 23 | `m3/a3-mastery-receipt-gate-20261006` | - | `0950c39b9e` | SUPERSEDED_BY_MASTER | 1f, 108+ | Doc for a receipt that the NebulaM3A3Apply command line must require. Master's later contract keeps that front door ungated (m3-nebula-recipe-first step 6b runs it without a receipt); no code for the receipt exists. |
| 24 | `m3/mastery-v6-gate-20261005` | - | `94a4b1c0e1` | SUPERSEDED_BY_MASTER | 1f, 73+ | Doc for NebulaM3MasteryV6Gate in front of NebulaM3A3Apply; same reason as the receipt gate. |
| 25 | `m3/portable-mastery-fanin-gate-20261006` | - | `864d9212aa` | SUPERSEDED_BY_MASTER | 13f, 2657+, 14- | Makes NebulaM3A3Apply fail closed without a V6 mastery receipt. Master's NebulaM3A3Apply (2026-10-07 baseline) and its workflow step 6b use the ungated run; merging would break that step. |
| 26 | `m3/current-master-proof-20261004` | #33 (closed) | `7e3877f67f` | SUPERSEDED_BY_MASTER | 1f, 17+ | Proof trigger for the 2026-10-04 master. Master has the later CURRENT_MASTER_PROOF_20261011, CURRENT_MASTER_REPROOF_20261013 and the 2026-10-12 full reproof (merged above). |
| 27 | `m3/final-proof-grid-v2-custody-repair-20261007` | #104 (closed) | `fd8589d378` | SUPERSEDED_BY_MASTER | 1f, 40+, 21- | Master's 7693f09c ("Preserve exact canonical V2 Grid source in the M3 custody gate") solves the same V2 custody check, preservation-only, with the receipt hash. |
| 28 | `m3/final-proven-transfer-20261006` | #72 (closed) | `cc609bc53d` | SUPERSEDED_BY_MASTER | 1f, 2+, 5- | Master's m3-nebula-recipe-first.yml already lists m3-final-proven-transfer.yml in its triggers and custody lane and runs the capture steps without if: always(). |
| 29 | `m3/pinned-openrewrite-root-gates-20261005` | #58 (closed) | `c9ae6b59a5` | SUPERSEDED_BY_MASTER | 1f, 5+, 5- | Master already calls org.openrewrite.maven:rewrite-maven-plugin:6.46.1 by full coordinates (plus -DskipMavenParsing=true). |
| 30 | `m3/second-pass-canonical-integration-20261005` | #54 (merged at 1bbb2f92) | `a1a1f360fd` | SUPERSEDED_BY_MASTER | 2f, 28+, 10- | Pins the second pass to Synexia 1cd10864; master pins the later 11234300. Its install edit is superseded by #59 (see PR #55). |
| 31 | `m3/second-pass-ci-proof-fix-20261005` | #56 (closed) | `f9de6101fe` | SUPERSEDED_BY_MASTER | 10f, 564+, 169- | NebulaM3SecondPassBinding with 2026-10-05 pins; master's binding was updated on 2026-10-13 (83e51502) to the later upstream commit. |
| 32 | `m3/second-pass-review-dag-20261005` | - | `9d0b83d410` | SUPERSEDED_BY_MASTER | 12f, 718+, 14- | 69% of its lines are on master; master's review DAG binding (SECOND_PASS_REVIEW_DAG.md, policy pins e2999c9a/PR 8973) is the later form. |
| 33 | `m3/scope-authority-ladder-20261006` | - | `4d90786cd7` | SUPERSEDED_BY_MASTER | 11f, 402+, 12- | 94% on master; the PLAN.tsv scope-ladder rows are on master renumbered (VISIBILITY..LIBRARY_API_FAN_IN), NebulaM3RefactorScope is on master. |
| 34 | `m3/scope-authority-ladder-v2-20261006` | #75 (closed) | `8498aee383` | SUPERSEDED_BY_MASTER | 9f, 231+, 9- | 96% on master; same remainder as v1. |
| 35 | `m3/nebula-parent-openrewrite-profile-20261004` | - | `7b3efc0a44` | SUPERSEDED_BY_MASTER | 1f, 39+ | Adds profile m3-atomize-patternize to the parent POM; master's parent POM already has a profile with that id (a second one would be a duplicate profile id). |
| 36 | `m3/openrewrite-parent-wiring-20261004` | - | `0549075b23` | SUPERSEDED_BY_MASTER | 1f, 39+ | Same profile id as above, with version properties; master already has the profile. |
| 37 | `m3/nebula-grid-baseline-verify-20261004` | #21 (closed) | `f95bdb322c` | SUPERSEDED_BY_MASTER | 1f, 29+ | A/B control workflow running the Grid Tycho closure on every PR. Master's guarded Java CI (maven.yaml with #156 load guards) builds and tests the same closure; adding it back would restore the unbounded Tycho runs #156/#157 removed. |
| 38 | `m3/nebula-packed-occurrence-control-20261004` | #20 (closed) | `9898f748d0` | SUPERSEDED_BY_MASTER | 1f, 31+ | Same as the baseline-verify control workflow, path-filtered to Grid. |
| 39 | `m3/visible-range-runtime-gates-current-20261004` | - | `049ffbd74b` | SUPERSEDED_BY_MASTER | 4f, 340+, 157- | Earlier form of visible-range-runtime-final-current (7 of its 10 tests, older RUNTIME_GATES.md); the final-current branch is merged above. |

Counts: MERGED 9, NO_EFFECT 1, REPLAYED 2, SUPERSEDED_BY_MASTER 27 (39 sources).

### Conflicted files and how both sides were kept

**PR #159 replay** (`cb969381e4` against base `d756dc1c89`, 27 add/add and 11 both-modified). The
integration branch merged PRs #150-#158 at 16:53-17:16 +0530; master received the same PRs later
through their own merges with later heads (SWT idiom pass `e55b3491`, `2f300607`, test-run records).
In every conflict block the master side is that later version, and with the master side kept each
of the 38 files is byte-identical to master, so no integration-side line that master lacks was lost.
The four integration records (`.m3/integration-20261013/`) are added.

| File | Master side kept because |
|---|---|
| `.m3/nebula-viz-decimation-20261013/harness/ExampleRunner.java` | master posts the capture from a timer thread with asyncExec (WM_TIMER starves while a snippet keeps repainting); integration used timerExec |
| `.m3/nebula-viz-decimation-20261013/harness/XYGraphBench.java` | master adds the screen-vs-Control.print pixel diff metric (screenVsPrint) |
| `.m3/tree-table-viewport-20261013/TESTS_TO_RUN.md` | master records the 2026-10-13 runs (#154 OK 2 tests, #155 OK 4 tests) and the showcase cell; integration still said NOT_RUN |
| `widgets/calendarcombo/.../CalendarComboPopupLocationTest.java` | identical apart from master's licence Contributors lines |
| `widgets/calendarcombo/.../CustomCombo.java` | master's SWT idiom pass: plain comments instead of //FLLDCIM tags; code identical |
| `widgets/compositetable/.../CompositeTableViewport_Test.java` | identical apart from master's licence Contributors lines |
| `widgets/compositetable/.../InternalCompositeTable.java` | master allocates the drag catch-up and row-flush Runnables once (no lambda per paint/drag) and tolerates the null end-of-drag event; same slider behaviour |
| `widgets/ctree/.../CTreeViewport_Test.java` | identical apart from master's licence Contributors lines |
| `widgets/ctree/.../CTree.java` | plain comments instead of //FLLDCIM tags; code identical |
| `widgets/ctree/.../CTreeBitLanes.java` | licence Contributors lines and a plain comment; code identical |
| `widgets/ctree/.../CTreeItem.java` | master has the full SWT Javadoc (@return, @exception SWTException) on getData/setData |
| `widgets/ctree/.../CTreeLayout.java` | plain comment instead of a //FLLDCIM tag; code identical |
| `widgets/ctreecombo/.../CTreeComboViewport_Test.java` | identical apart from master's licence Contributors lines |
| `widgets/ctreecombo/.../CTreeCombo.java` | plain comments instead of //FLLDCIM tags; code identical |
| `widgets/oscilloscope/.../OscilloscopeEnvelopeTest.java` | identical apart from master's licence Contributors lines |
| `widgets/oscilloscope/.../EnvelopeRing.java` | identical apart from master's licence Contributors lines |
| `widgets/oscilloscope/.../Oscilloscope.java` | master reports bad arguments through SWT.error (ERROR_INVALID_ARGUMENT, ERROR_NULL_ARGUMENT, ERROR_INVALID_RANGE) and has full Javadoc; integration threw raw IllegalArgumentException and had no null check |
| `widgets/oscilloscope/.../OscilloscopeDispatcher.java` | master has the full Javadoc of isParked (@since 1.5) |
| `widgets/tablecombo/.../TableComboClosePopupTests.java` | identical apart from master's licence Contributors lines |
| `widgets/tablecombo/.../CheckTableCombo.java` | plain comments instead of //FLLDCIM tags; code identical |
| `widgets/tablecombo/.../TableCombo.java` | plain comments instead of //FLLDCIM tags; code identical |
| `widgets/visualization/.../raster/ColorMapRaster.java` | master reports worker failures through SWT.error(ERROR_UNSPECIFIED, cause); licence Contributors lines |
| `widgets/visualization/.../raster/WindowArrayWrapper.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/DecimationCoreTest.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../figures/TraceDecimationDifferentialTest.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/AxisMapping.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/DecimatedTrace.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/PackedColumns.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/PackedMinMaxPyramid.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/PolylineBuilder.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/TraceDecimation.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/TraceDecimator.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../decimation/TraceView.java` | identical apart from master's licence Contributors lines |
| `widgets/visualization/.../dataprovider/PackedXYDataProvider.java` | master reports bad arguments through SWT.error (null and range checks) with full @param/@exception Javadoc; licence Contributors lines |
| `widgets/visualization/.../figures/TraceDecimationPainter.java` | identical apart from master's licence Contributors lines |
| `widgets/xviewer/.../tests/XViewerViewport_Test.java` | identical apart from master's licence Contributors lines |
| `widgets/xviewer/.../XViewer.java` | plain comments instead of //FLLDCIM tags; @since 1.1 matches Bundle-Version 1.1.0 (integration said 1.2) |
| `widgets/xviewer/.../XViewerViewportContentProvider.java` | licence Contributors lines, @since 1.1 (Bundle-Version 1.1.0), plain comment |

**Other conflicts**

| Source | File | How both sides were kept |
|---|---|---|
| `m3/grid-row-addressed-data-20261013` | `widgets/grid/.../Grid.java` | 3 blocks (clearItems, sparse remove, disposeSparseVirtualItems): PR #148's rowSpanIndex.clear() / rowsChanged() and the branch's clearDataRows() / removeDataRow(index) both kept, span index first |
| `docs/development-guide-20261005` | `README.md` | master's M3/Synexia licensing boundary section kept; the "Fork development guide" link section follows it |
| `m3/visible-range-runtime-final-current-20261004` | `widgets/grid/org.eclipse.nebula.widgets.grid.test/pom.xml` | master's screenshot systemProperties and 24 default includes kept; branch's failIfNoTests, full-grid-contracts profile and updated comment added |
| `m3/visible-range-runtime-final-current-20261004` | `widgets/grid/.../GridVisibleRangeSupport_Test.java` | master's 9 tests and helpers unchanged; the branch's 8 other tests added in tab style on a second, listener-free support created after the fixture settles; they reuse master's flushPaint/snapshot (same PNG names plus evidence); their delta oracle uses master's corrected removed-column payload; 17 tests |
| `m3/method-signal-evidence-20261004` | `m3/README.md` | master's second-pass sections kept; the method-signal evidence link follows |
| `m3/svgloader-length-atomization-20261004` | `widgets/cwt/.../SvgLoader.java` | no textual conflict; master's sealed preimage kept (see ledger), the test is added |

## Integration fixes made while merging

1. **Row-addressed visual data and PR #148 row spans (7e4a8198).** The row-addressed branch makes
   `GridRowAddressedItemDataVisualizer` the default visualizer. Three master features were gated on
   the exact class `GridItemDataVisualizer` and would have switched off for every default Grid: the
   PR #148 sparse row-span frontier (falling back to the scan that materializes every row above the
   viewport), cold-item residency eviction (`RESIDENCY_EVICT_COLD`), and the column window.
   `Grid.usesBuiltInDataVisualizer()` now admits both built-in visualizers. The row-addressed
   visualizer reports row-span changes, owner removal, column insert/remove and clear-all to the
   Grid's `GridRowSpanIndex` the way `GridItemDataVisualizer` does, `Grid.removeItem` drops the span
   owner on the row-addressed path, and span owners stay resident under eviction
   (`GridRowSpanIndex.owns`) so the index never holds a released facade.
2. **PR #148 test compile (ac045c33).** `GridPaintPreparation_Test` passed its message as the third
   argument of `assertEquals` (JUnit 5 order) in a JUnit 4 bundle, so the class did not compile. The
   message moves first; the assertion is unchanged. ac045c33 also added two new Grid tests to the
   bundle's default Tycho includes; a458ae11 reverts that, because `GridRowSpanIndex_Test` fails one
   assertion at the PR head itself (see Verification) and `GridVirtualItemListCapacity_Test` needs a
   Display. The includes stay as the PRs left them.
3. **Method-signal evidence on OpenRewrite 8.90.4.** The branch was written against an older
   OpenRewrite. In 8.90.4 `withId` no longer returns `SourceFile` in a call chain, and files the
   scanning recipe generates are visited in the same cycle without a scan record. The recipe now
   rebinds a `SourceFile` variable step by step and accepts a file that is already at its reviewed
   postimage before the scan check, which is exactly how master's
   `NebulaM3DeclarativeConvergenceControlRecipe` handles both. Its workflow gains master's
   concurrency group and fork guard (#156) and calls the rewrite plugin by full coordinates with
   `-DskipMavenParsing=true`, like master's recipe-first workflow.
4. **SvgLoader (7c84bc15).** Only the test lands; `SvgLoader.java` keeps the bytes master's
   recipe-first workflow seals.
5. **PR #55 install edit dropped before push.** `m3/declarative-local-artifact-bootstrap-20261005`
   and the identical commit on `m3/signal-chain-current-synexia-20261005` were merged at first and
   then found superseded: PR #55 was closed in favour of the merged #59, which master carries. The
   unpushed branch was rebuilt with `git commit-tree` from `23ba7e3e`: every later commit keeps its
   message, author, second parent and tree, except that `.github/workflows/m3-declarative-openrewrite.yml` stays
   master's blob `dd7e1ca9`.

## Verification

### Gates

| Gate | Result | Evidence |
|---|---|---|
| git diff --check origin/master..HEAD | PASS | 0 lines reported |
| conflict markers (git grep '^(<<<<<<<\|>>>>>>>\|=======$)' over java/md/xml/yml/tsv/properties/sh/py) | PASS | 0 matches |
| line-ending-only changes | PASS | 0 of 67 changed files differ only in line endings; git diff and git diff --ignore-cr-at-eol report the same 6660+/88-; all added or modified files are i/lf w/lf |
| control characters in changed files | PASS | none |
| sealed SvgLoader preimage | PASS | sha256 23d730f01826b3d5c3de5d9f5299f2db11574ae3c482956b20928aaa676f9a08, as pinned by m3-nebula-recipe-first.yml |
| origin/master is an ancestor | PASS | b86f35e9 unchanged at push time |
| no master content deleted | PASS | no file deleted; 88 lines removed net against master, each one rewritten by the branch that owns it (per commit: #148 replay 21, #161 32, method-signal 25, row-addressed 8, visible-range 2, JUnit 4 assertion 2, EOF blank lines 2) |

### Compile (javac 21.0.9, official SWT 3.135.0)

| Unit | --release 21 | Bundle RequiredExecutionEnvironment | Sources | Errors | Warnings |
|---|---|---|---|---|---|
| `org.eclipse.nebula.visualization.widgets` | OK | --release 11 OK | 52 | 0 | 24 |
| `org.eclipse.nebula.visualization.xygraph` | OK | --release 11 OK | 107 | 0 | 0 |
| `org.eclipse.nebula.widgets.calendarcombo` | OK | --release 11 OK | 19 | 0 | 2 |
| `org.eclipse.nebula.widgets.compositetable` | OK | --release 11 OK | 55 | 0 | 11 |
| `org.eclipse.nebula.widgets.ctree` | OK | --release 11 OK | 9 | 0 | 3 |
| `org.eclipse.nebula.widgets.ctreecombo` | OK | --release 11 OK | 5 | 0 | 0 |
| `org.eclipse.nebula.widgets.grid` | OK | --release 17 OK | 102 | 0 | 0 |
| `org.eclipse.nebula.widgets.oscilloscope` | OK | --release 11 OK | 8 | 0 | 30 |
| `org.eclipse.nebula.widgets.tablecombo` | OK | --release 11 OK | 9 | 0 | 0 |
| `org.eclipse.nebula.widgets.xviewer` | OK | --release 11 OK | 96 | 0 | 9 |
| `org.eclipse.nebula.widgets.xviewer.core` | OK | --release 11 OK | 14 | 0 | 1 |
| `org.eclipse.nebula.visualization.widgets.tests` (tests) | OK | - | 14 | 0 | 0 |
| `org.eclipse.nebula.visualization.xygraph.test` (tests) | OK | - | 5 | 0 | 0 |
| `org.eclipse.nebula.widgets.calendarcombo.tests` (tests) | OK | - | 1 | 0 | 0 |
| `org.eclipse.nebula.widgets.compositetable.tests` (tests) | OK | - | 11 | 0 | 0 |
| `org.eclipse.nebula.widgets.ctree.tests` (tests) | OK | - | 1 | 0 | 0 |
| `org.eclipse.nebula.widgets.ctreecombo.tests` (tests) | OK | - | 1 | 0 | 0 |
| `org.eclipse.nebula.widgets.grid.test` (tests) | OK | - | 40 | 0 | 0 |
| `org.eclipse.nebula.widgets.oscilloscope.tests` (tests) | OK | - | 2 | 0 | 0 |
| `org.eclipse.nebula.widgets.tablecombo.test` (tests) | OK | - | 1 | 0 | 0 |
| `org.eclipse.nebula.widgets.xviewer.tests` (tests) | OK | - | 3 | 0 | 0 |
| `org.eclipse.nebula.cwt` | OK | - | 55 | 0 | 0 |
| `org.eclipse.nebula.cwt.tests (tests)` | OK | - | 12 | 0 | 0 |

All 80 javac warnings ([removal], [dep-ann]) are in bundles whose sources this consolidation does not change; they are on master already. Grid, the only product bundle the consolidation changes, has none.

Not compiled (dependency missing locally): `TableComboTests.java`, `TableComboViewerTests.java` (SWTBot (org.eclipse.swtbot.go) not available locally).

### Headless JUnit 4

| Status | Classes | Tests |
|---|---|---|
| PASS | 14 | 73 |
| FAIL | 1 | 3 |
| NOT_RUN | 35 | - |

| Class | Status | Tests | Failures | Note |
|---|---|---|---|---|
| `org.eclipse.nebula.widgets.grid.GridPaintDAG_Test` | PASS | 6 | 0 |  |
| `org.eclipse.nebula.widgets.grid.GridRowSpanIndex_Test` | FAIL | 3 | 1 | Pre-existing in PR #148: sparseFrontierFindsEarliestCoveringSpanAtDistantViewport expects 500000, gets 499995 (GridRowSpanIndex_Test.java:24); reproduced with the PR head d716ac6221 sources alone (.m3/superset-consolidation-20261013/evidence/GridRowSpanIndex_Test-at-d716ac6221.log); the class own linear oracle agrees with 499995. Not merge-caused; not in the default Tycho includes. |
| `org.eclipse.nebula.widgets.grid.GridViewportDistillation_Test` | PASS | 11 | 0 |  |
| `org.eclipse.nebula.widgets.grid.model.HeapGrowth_Test` | PASS | 6 | 0 |  |
| `org.eclipse.nebula.widgets.grid.model.RowBitLanes_Test` | PASS | 4 | 0 |  |
| `org.eclipse.nebula.widgets.grid.model.RowGrouping_Test` | PASS | 2 | 0 |  |
| `org.eclipse.nebula.widgets.grid.model.RowPermutation_Test` | PASS | 13 | 0 |  |
| `org.eclipse.nebula.widgets.grid.model.RowSelection_Test` | PASS | 2 | 0 |  |
| `org.eclipse.nebula.widgets.grid.model.RowStateLanes_Test` | PASS | 3 | 0 |  |
| `org.eclipse.nebula.widgets.calendarcombo.CalendarComboPopupLocationTest` | PASS | 4 | 0 |  |
| `org.eclipse.nebula.visualization.internal.xygraph.decimation.DecimationCoreTest` | PASS | 6 | 0 |  |
| `org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferTest` | PASS | 6 | 0 |  |
| `org.eclipse.nebula.visualization.xygraph.linearscale.AxisTest` | PASS | 1 | 0 |  |
| `org.eclipse.nebula.visualization.xygraph.linearscale.TickFactoryTest` | PASS | 8 | 0 |  |
| `org.eclipse.nebula.visualization.widgets.figures.CircularBufferTest` | PASS | 1 | 0 |  |

NOT_RUN (need a Display; this run is headless only): `GridBlitScroll_Test`, `GridBodyDefer_Test`, `GridCheckLanes_Test`, `GridColumnGroup_Test`, `GridColumnWindow_Test`, `GridColumn_Test`, `GridFixedColumn_Test`, `GridGCProxy_Test`, `GridIssue700_Test`, `GridItem_Test`, `GridModelJobs_Test`, `GridPaintPreparation_Test`, `GridPinnedRows_Test`, `GridPrefetch_Test`, `GridResidency_Test`, `GridRowPermutation_Test`, `GridScrollbarSettle_Test`, `GridSetDataRedraw_Test`, `GridViewportCoordinates_Test`, `GridVirtualHeights_Test`, `GridVirtualItemListCapacity_Test`, `GridVisibleRangeSupport_Test`, `Grid_Test`, `XViewerViewport_Test`, `CTreeViewport_Test`, `CTreeComboViewport_Test`, `TableComboClosePopupTests`, `TableComboTests`, `TableComboViewerTests`, `CompositeTableViewport_Test`, `CompositeTable_267316Test`, `TraceDecimationDifferentialTest`, `AbstractWidgetTest`, `OscilloscopeTest`, `OscilloscopeEnvelopeTest`.

### M3 tooling reactor (`mvn -f m3/reactor.xml`)

| Suite | Tests | Failures | Errors | Status | Note |
|---|---|---|---|---|---|
| test-compile (openrewrite-line-terminators, recipe-first) | - | - | - | PASS | baseline before the m3 merges: BUILD SUCCESS |
| NebulaM3MethodSignalEvidenceRecipeTest, NebulaM3InventoryRecipeTest, NebulaM3RepositoryReviewRecipeTest (merged tree) | 9 | 0 | 0 | PASS | mvn -pl recipe-first -am test after the OpenRewrite 8.90.4 adaptation |

### Build-log report (file:line:severity)

`BUILD-LOG-REPORT.tsv` in `.m3/superset-consolidation-20261013/` holds every parsed javac and JUnit row (130 rows: FAIL 2, NOTE 34, PASS 14, WARNING 80).

| File | Line | Severity | Rule / test | Message |
|---|---|---|---|---|
| `org/eclipse/nebula/widgets/grid/GridRowSpanIndex_Test.java` | 24 | FAIL | sparseFrontierFindsEarliestCoveringSpanAtDistantViewport | java.lang.AssertionError: expected:<500000> but was:<499995> |

## Jars (`*-m3-20261013.2.jar`)

Built by `.m3/superset-consolidation-20261013/build-superset-nebula.sh` (an adapted copy of
`.m3/integration-20261013/build-integration.sh`; the original is unchanged) from source commit
`dc4aeb0f123b3853cb2e817984ddc7cc4e54f241`, `javac --release 21` against the official
`org.eclipse.swt.win32.win32.x86_64-3.135.0.jar` plus the platform jars the prior integration build
used. The build started before the SWT superset jar
(`org.eclipse.swt.win32.win32.x86_64-m3-20261013.2.jar`, sha256 `76e42716...`) was ready, so the jars are
compiled against the official SWT jar, as the prior integration jars were; a compile check of the eleven bundles against the SWT superset jar was attempted but NOT_RUN: the memory gate refused it (machine-wide memory exhaustion from external git processes). Each jar carries its bundle manifest with `Bundle-Version: <base>.m3-20261013-2`,
`Implementation-Version`, the `M3-*` source mapping entries (`M3-Source-Repository`,
`M3-Source-Branch`, `M3-Source-Commit`, `M3-Source-Bundle`, `M3-Mapping`, `M3-Mapping-Classes`,
`M3-Consolidation-Ledger`, `M3-Compile-SWT`) and `META-INF/m3/MAPPING.tsv` (class entry, source
path, source blob, last mainline commit of the consolidation branch, and the ledger sources of this consolidation that touched the
file). `MAPPING.tsv` beside the jars is the union of the eleven.

| Jar | SHA-256 |
|---|---|
| `org.eclipse.nebula.visualization.widgets-1.1.0-m3-20261013.2.jar` | `da5f34433b8b62929a9b8998d852faeb1c7e53c09bbdf0b36e27b7cf23adc388` |
| `org.eclipse.nebula.visualization.xygraph-3.2.0-m3-20261013.2.jar` | `b9232ae67edf3b3dfa874b2046ff6086d3128571b69fba4def64963255bcc319` |
| `org.eclipse.nebula.widgets.calendarcombo-1.1.0-m3-20261013.2.jar` | `189e3307294015f591ed2d948f9811b2c92d084ccd73b92923847da61e7806ff` |
| `org.eclipse.nebula.widgets.compositetable-1.1.0-m3-20261013.2.jar` | `9081024ea2640922e9824d8e0d4d231f8a435d7c8b38fbac3558a33413199f2a` |
| `org.eclipse.nebula.widgets.ctree-0.9.0-m3-20261013.2.jar` | `ed58b8eb8de5366a605dce9cad8913e1d94caf36b8f5ac69e514ee4f6f917d3b` |
| `org.eclipse.nebula.widgets.ctreecombo-1.0.0-m3-20261013.2.jar` | `c349aa8d2d2cbbc44e048a017fe93a9d40d145b2ed7c187e41b0ac725dce77de` |
| `org.eclipse.nebula.widgets.grid-1.3.0-m3-20261013.2.jar` | `75893bf8bc57617e39d4b1788e9befe93baafe8970a342b1e66948adce093251` |
| `org.eclipse.nebula.widgets.oscilloscope-1.5.0-m3-20261013.2.jar` | `be5f5ba6f04652889f0ccfdfd517e44bff1489082041c6a63036a613fe967774` |
| `org.eclipse.nebula.widgets.tablecombo-1.3.0-m3-20261013.2.jar` | `9daec41c8f5c97a280b2569d2a18bc38eaef794050af24885a3bfd7940d2d7a7` |
| `org.eclipse.nebula.widgets.xviewer-1.1.0-m3-20261013.2.jar` | `a282b5744c226d412d46c14059f14b02ee8044f46d5c3934856d5296bed73ef4` |
| `org.eclipse.nebula.widgets.xviewer.core-1.0.0-m3-20261013.2.jar` | `b40abda0606ffc9f9f68c448713449b90f963f6853213a85b91e33b5b8b6108d` |
