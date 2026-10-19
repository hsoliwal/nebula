# M3FindBar (matrix row FIND_BAR, rank 6, P1)

Missing from stock SWT (Table and Tree have native type-ahead only) and stock Swing (JTree/JList prefix navigation
only); 6 libraries implement it: SwingX `JXFindBar` / `JXFindPanel` / `Searchable` (LGPL: API shape only), NetBeans
`QuickSearch` (Apache-2.0, ADAPT), IntelliJ `SpeedSearch` (Apache-2.0, ADAPT), JIDE `Searchable` (GPL+CPE, pattern),
shellfx. Swing twin: `javax.swing.M3FindBar` (row CW-FIND_BAR in `../ENHANCED_API_MAP.tsv`).

## API shape taken from the donors (shape only, no code)

SwingX: a `Searchable` with `search(pattern, startIndex, backwards)` returning the match index; a bar with
find next / find previous, match case, wrap, a found / not-found message, bound to a target. NetBeans QuickSearch:
an inline popup on typing, `Callback.evaluate(request, response)` over the model. IntelliJ SpeedSearch: a prefix
search over the model's row texts with a highlight of the matched range.

## Contract

`org.eclipse.nebula.widgets.m3.M3FindBar extends Composite` (text, previous, next, match case, whole word, wrap, a
status label).

| Member | Meaning |
|---|---|
| `setSearchable(M3Searchable)` | the model to search: `int size()`, `CharSequence textOf(int index)` and `void reveal(int index)` (the target shows and selects the row) |
| `bind(Table)`, `bind(Tree)`, `bind(List)` (static helpers) | ready-made searchables over the SWT widgets; `textOf` reads the item text of the search column through the widget's index API, never through facades kept by the bar |
| `find(String pattern, int from, boolean backwards)` | the match index or -1; the search is over `textOf` of every index from `from` with wrap when enabled |
| `findNext()` / `findPrevious()` | from the current match |
| `setMatchCase`, `setWholeWord`, `setWrap`, `setIncremental` | options; incremental finds on every keystroke |
| `getMatch()` / `getMatchRange()` | the current match index and the character range in its text |
| `Selection` events | sent when the match changes (`Event.index` = match) |
| `setStatusText`-free: the status is the bar's own `Label` ("12 of 345", "not found") | |

## DOOP data

The bar keeps no copy of the model: a search is a loop over `textOf(i)` with a precomputed lower-cased pattern;
matches are not materialized unless `countMatches()` is asked, and then they are counted, not stored. For a Table
with 10^6 rows a search is O(n) `getItem(i).getText(column)` calls through the viewport (SetData on demand is the
viewport's concern); the bar runs it in slices of 4096 rows on the UI thread through `Display.timerExec`, with a
generation counter so a newer pattern supersedes an older search (superseding-generation debounce).

## Proof

- `M3FindBarTest`: API, options, wrap, backwards, incremental, generation supersession, events, bindings for
  Table/Tree/List (the Tree searchable walks the expanded rows in display order), both modes; a 10^6-row Table
  search is timed.
- Coverage 99% (JaCoCo). Example: `M3FindBarSnippet` (Table with 10^6 virtual rows and the bar above it).
