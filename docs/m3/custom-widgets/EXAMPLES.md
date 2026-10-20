# Custom widget examples (every platform)

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

One runnable snippet per widget. The M3 widgets are Java drawn on SWT `Canvas`/`Composite` with public SWT API only,
so the same snippet runs on win32, gtk and cocoa (`compile_platforms.sh` compiles the bundle against all three SWT
source sets). Platform cells per widget: `WIDGET_PLATFORM_MATRIX.tsv`; delta behaviour per widget: `DELTA_GATE.tsv`.

Snippets live in `widgets/m3/org.eclipse.nebula.widgets.m3.snippets/src/org/eclipse/nebula/widgets/m3/snippets/`
(package `org.eclipse.nebula.widgets.m3.snippets`). Build with `build_m3.sh`, run on the hidden desktop with
`ui_lock.sh custom-widgets bash run_m3.sh 180 <log> viewport - main:org.eclipse.nebula.widgets.m3.snippets.<Snippet>`;
`-Dm3.snippet.screenshot=<png>` saves a screenshot and exits (`ui_batch.sh shots`).

| widget | replaces / counterpart of | snippet | shows | screenshot |
|---|---|---|---|---|
| M3StackLayout | SWT `StackLayout`, `java.awt.CardLayout` | `M3StackLayoutSnippet` | 10^6 declared pages, only the top page and an LRU budget resident | `S:/m3-scratch/custom-widgets/screenshots/M3StackLayoutSnippet.png` |
| M3HeatMap | WIDGET_MATRIX HEATMAP (matrix view; no SwingX or stock SWT counterpart) | `M3HeatMapSnippet` | 1000 x 1000 cells in one float lane; scroll, select, arrow keys | `S:/m3-scratch/custom-widgets/screenshots/M3HeatMapSnippet.png` |
| M3BusyLabel | SwingX `JXBusyLabel` | `M3BusyLabelSnippet` | three rings (16/24/32 px, 8/12/16 points), start/stop | `S:/m3-scratch/custom-widgets/screenshots/M3BusyLabelSnippet.png` |
| M3StatusBar | SwingX `JXStatusBar` | `M3StatusBarSnippet` | fixed, preferred (timer-updated row counter), weighted and progress-bar slots, resize grip | `S:/m3-scratch/custom-widgets/screenshots/M3StatusBarSnippet.png` |
| M3CollapsiblePane | SwingX `JXCollapsiblePane` | `M3CollapsiblePaneSnippet` | a vertical options pane and a horizontal side panel, animated toggles | `S:/m3-scratch/custom-widgets/screenshots/M3CollapsiblePaneSnippet.png` |
| M3ErrorPane | SwingX `JXErrorPane` | `M3ErrorPaneSnippet` | embedded pane with an expanded stack trace, and the modal dialog form | `S:/m3-scratch/custom-widgets/screenshots/M3ErrorPaneSnippet.png` |
| M3MultiSplitPane | SwingX `JXMultiSplitPane` | `M3MultiSplitPaneSnippet` | IDE layout: navigator, editor over console, outline; drag any divider | `S:/m3-scratch/custom-widgets/screenshots/M3MultiSplitPaneSnippet.png` |
| M3FindBar | SwingX `JXFindBar`, `JXFindPanel`, `JXSearchPanel` | `M3FindBarSnippet` | find bar over a virtual table of 10^6 rows (texts from the model array, never from items) | `S:/m3-scratch/custom-widgets/screenshots/M3FindBarSnippet.png` |
| M3TaskPane, M3TaskPaneContainer | SwingX `JXTaskPane`, `JXTaskPaneContainer` | `M3TaskPaneSnippet` | three task panes (one special, one with a busy label) in a scrolled container; click a title to collapse | `S:/m3-scratch/custom-widgets/screenshots/M3TaskPaneSnippet.png` |
