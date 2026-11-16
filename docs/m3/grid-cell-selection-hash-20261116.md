<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula Grid cell selection duplicate hash optimization — 2026-11-16

Intrinsic change in `GridCellSelectionList.inc(Point,int)` only.
At the half-full open-addressed table threshold, duplicate cell
occurrences reuse their existing key slot and keep all three primitive
hash arrays rather than growing on every duplicate. New distinct cells
still grow the table normally. The selected occurrence list, GridItem,
SWT Point copies, duplicate ordering, column counts, first-occurrence
removals, widget events and native resource ownership are unchanged.

Java21 standalone independent proof: 100,000 seeded hash additions and
half-load threshold: old 32-slot key table grows to 64 for a duplicate,
new owner remains 32 until insertion of a new distinct key. The actual
Nebula source is compiled by headless CI with only a test-only Point
stand-in. The original GridCellSelectionList_Test JUnit4 suite gains
a duplicate-capacity / reindex / new-key growth regression.

No Qt/Flutter/JavaFX/WinUI source copied; full Tycho/native and
Android/iOS qualifications are separate release gates.
