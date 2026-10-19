# Custom widget matrix (SWT, Nebula, Swing, SwingX, JavaFX and GitHub widget libraries)

Status: matrix v1, 2026-10-19. Owner: custom-widgets track. Every number below is produced by the scripts in
`tools/`; nothing is estimated by hand.

## What is here

| File | Rows | Content |
|---|---|---|
| `LIBRARIES.tsv` | 178 | every library considered: slug, licence, stars, last push, archived, toolkit, Java files listed, widget classes matched, source, licence route |
| `WIDGET_MATRIX.tsv` | 68 | one row per widget concept, ranked: SwingX class, Nebula class, other library classes, stock SWT, stock Swing, stock JavaFX, virtualizable, library count, missing from stock, M3 plan, licence route, priority, owner |
| `SEARCH_HITS.tsv` | 1,312 | every repository the GitHub searches returned, with the queries that found it and why it was kept, excluded or filtered out |
| `ENHANCED_API_MAP.tsv` | see file | Swing rows handed to the enhanced-API Swing track (same columns as that track's map) |
| `tools/` | | `github_search.sh`, `filter_search.sh`, `harvest_libraries.sh`, `build_matrix.sh` and their inputs |

## Sources (CPU only, no clones)

1. The donor catalogue `K:/m3-work/donors/catalogue/DONOR_CATALOGUE.tsv` (194 rows; the UI toolkits among them), the
   mined links `links/DONOR_LINKS_ALL.tsv` (categories `ui-toolkit`, `ui-toolkit-2`) and `links/USER_LINKS.md`. These stay
   local; only public repository slugs are copied into `tools/libraries.seed.tsv`.
2. The swt-initiative31 organisation (`gh repo list swt-initiative31`): 11 repositories. The six with widget code
   (prototype-swing, prototype-skija, custom-drawn-controls, skija-canvas, prototype-gtk, prototype-skiavcl) are rows;
   prototype-swing maps SWT onto Swing, so it bridges the SWT and Swing columns.
3. GitHub search, 20 queries ranked by stars (topics swt, swt-widgets, eclipse-swt, nebula, swing, java-swing,
   swing-components, swing-library, look-and-feel, javafx-controls, swing-gui, javafx-library, lookandfeel, swt-ui; text
   "swt widget", "swing component library", "swing table", "swing tree table", "virtual list swing", "swt custom
   widgets"): 1,312 distinct repositories. `filter_search.sh` keeps 126 widget-library candidates; 40 of them are
   applications or utilities and are recorded as EXCLUDE with the reason (`tools/excluded.tsv`).
4. The libraries the task named (SwingX lineage arotenberg then wumpz, from `SWINGX_LINEAGE.tsv` on com.synexia #12398;
   FlatLaf extras; Radiance; JIDE common layer; ControlsFX; NatTable; MigLayout; JCalendar; LGoodDatePicker; Glazed Lists;
   WebLaF) and well-known ones the searches miss (JFXtras, GemsFX, CalendarFX, AtlantaFX, MaterialFX, XChart,
   DockingFrames, jxmapviewer2, tablefilter-swing, l2fprod property sheet, pumpernickel, Insubstantial, NetBeans Outline
   and property sheet, IntelliJ platform UI, Eclipse Forms and JFace field assist, RAP RWT, Draw2d/Zest).

For each library `harvest_libraries.sh` reads the GitHub metadata and the Java file tree (`gh api .../git/trees`,
recursive; large repositories by sub-path). Tests, examples and demos are dropped. `build_matrix.sh` matches class names
against the concept patterns in `tools/concepts.tsv` (helper classes such as `*UI`, `*Skin`, `*Model`, `*Renderer`
are excluded first) and writes both tables.

Result: 135 candidate libraries (55 Swing, 35 JavaFX, 31 SWT, 9 multi-toolkit, 4 SWT/Swing bridges, 1 RAP), 97 of
them implement at least one concept; 1,032 (concept, library, class) matches.

## Licence routes

| Route | Libraries | Meaning |
|---|---|---|
| ADAPT (EPL) | EPL-1.0/2.0 | code may be adapted into Nebula/SWT with the notice kept |
| ADAPT (permissive) | Apache-2.0, BSD, MIT, Unlicense | code may be adapted with the notice kept and recorded in FOSS_REUSE_DECISION.tsv |
| PATTERN_ONLY | LGPL, GPL, MPL, CPL, no licence detected | API shape and technique only, never code. SwingX is always shape only (LGPL-2.1) |
| PORT inside M3jdk21 | GPL-2.0 with Classpath exception (OpenJDK, JIDE) | pattern only for SWT/Nebula; same-licence port target inside M3jdk21 `src/**` |
| EXCLUDE | | application, utility or not found |

Counts: 91 ADAPT, 44 PATTERN_ONLY, 40 EXCLUDE, 3 STOCK (column references).

## Ranking

`score = library_count x virtualization weight (Y 3, P 2, N 1) x missing weight (absent from stock SWT and stock Swing 3,
from one of them 2, from neither 1)`. The stack family is P0 by user decision (2026-10-19); then ranks 1-12 are P1,
13-30 P2, the rest P3. `owners.tsv` routes rows that other tracks own.

### P0: stacks (user, 2026-10-19)

| Concept | Plan | Owner |
|---|---|---|
| VIRTUAL_COMPOSITE | `M3VirtualComposite`: SWT.VIRTUAL on Composite as asked in eclipse-platform/eclipse.platform.swt#624; children materialized by the viewport, M3StackLayout pages being the one-child-at-a-time form | custom-widgets (after M3StackLayout) |
| STACK_LAYOUT | `M3StackLayout`: only the top control is materialized; the other children are parked or released and recreated through a client factory under an LRU budget (M3BoundedCache pattern). Swing row `M3CardLayout` handed off | custom-widgets (first leaf) |
| TAB_FOLDER_RICH | thousands of tabs: virtual tab list (lanes for text, image, state), chevron and overflow from the visible window, CTabItem facades on demand. Reuses the all-widgets CTabFolder mediation | all-widgets + custom-widgets |
| EDITOR_VIEW_STACK | e4 StackRenderer part stacks ride on the two leaves above | jface-viewport agent (platform.ui); coordination only |

### P1

| Rank | Concept | Score | Libraries | SWT side | Swing side |
|---|---|---|---|---|---|
| 1 | AUTOCOMPLETE | 108 | 12 | Nebula TextAssist onto the viewport | hand-off |
| 2 | CHART | 108 | 12 | Nebula XYGraph/Oscilloscope onto the viewport | hand-off |
| 3 | GALLERY | 108 | 12 | Nebula Gallery onto the viewport (step 2, first) | hand-off |
| 4 | TREE_TABLE | 78 | 13 | SWT Tree with columns (SWT tracks) | hand-off (M3TreeTable) |
| 5 | IMAGE_VIEW | 72 | 12 | NEW: M3ImageView | hand-off |
| 6 | FIND_BAR | 54 | 6 | NEW: M3FindBar | hand-off |
| 7 | GANTT_TIMELINE | 54 | 6 | Nebula GanttChart/Timeline onto the viewport | hand-off |
| 8 | HEATMAP | 54 | 6 | NEW: M3HeatMap | hand-off |
| 9 | DATE_PICKER | 50 | 25 | SWT DateTime, Nebula CDateTime (no viewport need) | hand-off |
| 10 | FILTER_ROW | 45 | 5 | NEW: M3FilterRow (with the sort-filter lane) | hand-off |
| 11 | TASK_PANE | 36 | 18 | SWT ExpandBar, Nebula PGroup/PShelf | hand-off |
| 12 | PROPERTY_SHEET | 36 | 6 | Nebula PropertyTable onto the viewport | hand-off |
| 13 | PINNED_COLUMNS | 9 (P1 by owner) | 1 | Nebula Grid already has it (merged hsoliwal/nebula#696 and #165, frozen first column); SWT Table gets a column-window lane | hand-off |

New SWT widgets in rank order (no stock SWT and no Nebula implementation): IMAGE_VIEW, FIND_BAR, HEATMAP, FILTER_ROW,
COLUMN_CONTROL, then the P3 rows (MULTI_SPLIT, STATUS_BAR, NODE_EDITOR, TERMINAL, ERROR_PANE, DOCKING, RIBBON, ...).
60 concepts are missing from stock Swing or P0 and become rows in `ENHANCED_API_MAP.tsv` for the Swing track.

## Work order (custom-widgets)

1. STACK_LAYOUT (P0): `M3StackLayout`, differential against stock `StackLayout`, example, 99% coverage.
2. GALLERY (P1, step 2): Gallery viewport leaf on top of nebula#236 (selection lane, amortized growth), 10^6 example.
3. TAB_FOLDER_RICH virtual tab list: after reading the all-widgets CTabFolder mediation.
4. TABLE_COMBO and CompositeTable (step 2): mode switch, differential and examples on top of nebula#150.
5. NEW widgets in rank order: IMAGE_VIEW, FIND_BAR, HEATMAP, FILTER_ROW, ...
6. Then every remaining row in matrix order, each its own small leaf with a differential, 99% coverage and an example.

## Reproduce

```
bash tools/github_search.sh hits.tsv
bash tools/filter_search.sh hits.tsv > libs_from_search.tsv
bash tools/harvest_libraries.sh <seed + search libraries> <cache>
cp hits.tsv <cache>/search_hits.tsv
bash tools/build_matrix.sh <cache> <out>
```
