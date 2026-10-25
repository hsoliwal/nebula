# Every widget a viewport: waves

Generated from the completeness critique of inventory run wf_14c3f677-472 (2026-10-24, 327 cards in 25 units,
every unit verified; CRITIQUE.json holds the critique, LEDGER.tsv the cards). One wave carries one aspect per pass.

## Wave 1: Shared gate core for each toolkit, plus the generic gate rows every control inherits

**Mechanism.** Land each toolkit's hidden gate once, plus the rows that every control of that toolkit inherits.
- SWT win32 fork:
  - EffectiveVisibility.deferPaintMessage absorbs the WM_ERASEBKGND of a deferred WM_PAINT, together with the Control.WM_ERASEBKGND row at 5989.
  - ScrollBar.SetScrollInfo folds frame repaints behind a per-control RDW_FRAME bit that VisibilityCatchUp.catchUpOne consumes.
  - Spinner routes the WM_PAINT of its EDIT and UPDOWN windows (1242-1244) through the gate.
  - Menu.redraw, at METHOD_START, folds bar redraws while the menu shell is hidden or minimized.
  - ToolTip arms its autopop SetTimer only for autoHide tips and always kills it in releaseWidget.
  - OcclusionTracker parks while no shell of the display is visible.
- Nebula:
  - Ship one gate package per bundle: copies of internal/WidgetMode, M3HiddenGate, M3Counters and SwtLog, following the Tiles 1ea68f80 pattern, because widgets/m3 is not in the nebula-parent reactor.
  - Align the gallery and tiles WidgetMode copies from SKIJA to M3, and make M3Mode answer the realized mode.
- Swing:
  - sun.swing.M3HiddenGate core, with owned-window roots and layout deferral for iconified roots (RepaintManager 471, 807, 1507).
  - SH1: a JComponent.validateTree member before isValidateRoot (4989), and M3HiddenGate.revealed after super.show() in JInternalFrame.show (1741).
  - imageUpdate METHOD_START rows in AbstractButton (2164), JLabel (893) and JSlider (767).
- draw2d: an M3HostVisibilityGate on the canvas's DeferredUpdateManager. It parks sendUpdateRequest (245-253) while the host is hidden or minimized, and drops the dirty regions that the reveal paint covers.
- CWT:
  - EffectGate at AbstractEffect.doEffect METHOD_START (115); apply the same row to the gallery copy.
  - VViewport per top-level VPanel, folding VControl.redraw, VPanel.layout and setBounds.
- JFace:
  - J1: foldIfHidden before M3Viewers.flush in the four StructuredViewer.refresh overloads, in TableViewer.refresh(Object,boolean,boolean) (365) and in AbstractTreeViewer.labelProviderChanged (2474).
  - J2: gate the M3Viewers post and enqueue timers.

**Rationale.** About 25 hook rows and 6 small new files, all size S, close or unblock H for 28 cards, and every later wave depends on them. None of them touches a single widget's paint code. Several uncarded classes close here too: Box and Box.Filler (through SH1), the CWT V-controls (VViewport), the visualization figure widgets (host gate), and the gallery animation copy (EffectGate). The Nebula packaging decision settles the Require-Bundle versus copy conflict between unit leaves before about 60 Nebula leaves depend on it.

Classes (28): Sash, ScrollBar, Spinner, Menu, ToolTip, Shell, JComponent, JFrame, JDialog, JWindow, JPanel, JRootPane, JSplitPane, JToolBar, JMenuBar, AbstractButton, JToggleButton, JCheckBox, JRadioButton, JLabel, AlphaLabel, GrayableButton, AnimationRunner, ScrollingSmoother, VCanvas, BaseCombo, StructuredViewer, M3Viewers

## Wave 2: Hidden-gate rollout, H only: every FIXED drawn widget, every timer owner, every popup owner

**Mechanism.** Each class gets only rows on the wave-1 gates. No damage changes in this wave, so each pass carries one aspect.
- Install the gate in the constructor, before the Paint listener.
- Fold hidden measure, layout, rebuild and native setText/setItems into a dirty flag; getters flush it.
- Park every timerExec, javax.swing.Timer, thread, poll and animation while hidden, and resume once on reveal. Stop on dispose unconditionally.
- Close drop-downs and overlays when the owner or any ancestor hides: Hide listeners on the ancestor chain, PartHidden on the fork.
- On the SWT fork, fold the direct native draws (WM_UPDATEUISTATE, WM_SETTEXT, Slider fRedraw, the PBM marquee, MonthCal ticks) into DIRTY_PAINT and replay them on CATCH_UP.
- Count hiddenDeltas and reveals.
Per-class acceptance: assertHiddenGate, run on the official jar and on the fork. 10^4 hidden deltas must give 0 native work and 0 ticks, then 1 reveal and 1 paint.

**Rationale.** Each class needs 1 to 6 mechanical rows on a shared gate. This converts most of the ledger's H gaps in one sweep: about 120 classes, including the FIXED controls with timers and the popup owners. The list order puts every wrapped widget before its wrapper:
- Table before TableCombo.
- M3CollapsiblePane before M3ErrorPane.
- AbstractCombo before DateChooserCombo.
- CDateTime before its pickers.
- GanttComposite leaf 1 before GanttControlParent.
- Transition before TransitionManager.
- PageContainer and ProgressBar before GeoMapBrowser.
- The BasicTextUI, DefaultCaret, FieldView, PlainView and WrappedPlainView rows of ST-H1 before the text widgets.
The multichannel Oscilloscope dispatcher and the Tiles ResizeListener get probe-based parks, because their H is recorded but incomplete. Card and include in this wave: Notifier and opal FooterArea (timers), FlatCalendarCombo, XViewerFilteredTree, and Annotation and Axis (the XYGraph data fold and zoom timers).

Classes (120): Button, Label, Slider, ProgressBar, DateTime, Group, CoolBar, ToolBar, ExpandBar, TabFolder, Table, TableViewport, List, ComboViewport, Text, StyledText, CTabFolder, AnimatedProgress, TableEditor, TreeEditor, BasicTextUI, DefaultCaret, FieldView, PlainView, WrappedPlainView, JTextComponent, JTextArea, JTextField, JPasswordField, JFormattedTextField, JTextPane, JEditorPane, AsyncBoxView, JButton, JProgressBar, JTree, BasicTableUI, BasicComboPopup, BasicFileChooserUI, JFileChooser, JLayer, JColorChooser, ChoiceWidget, Header, HeapManager, TitledSeparator, PWTabContainer, FlatButton, RoundedToolbar, StarRating, SwitchButton, TriStateSwitchButton, RangeSlider, BaseLED, LED, DotsLed, RoundedCheckbox, RoundedSwitch, SegmentedBar, Stepbar, ProgressCircle, NebulaSlider, Carousel, ImageContainer, ImageSelector, Chips, HeaderControl, TitleControl, ToolbarComposite, NebulaToolbar, NavigationPageGraphics, M3FindBar, M3Toasts, Grid, Gallery, Tiles, PrintPreview, M3HeatMap, CTree, PropertyTable, M3CollapsiblePane, M3ErrorPane, M3TaskPaneContainer, M3StatusBar, PageContainer, GeoMapBrowser, XYGraph, Oscilloscope (multichannel), TimelineComposite, GanttComposite, GanttControlParent, CDateTime, AnalogTimePicker, DiscreteTimePicker, CDatePanel, MonthPick, Transition, TransitionManager, AddAnnotationDialog, AbstractPictureControl, PictureControl, FormPictureControl, AbstractCombo, DateChooserCombo, CalendarCombo, CustomCombo, CalculatorCombo, TextAssist, HorizontalSpinner, FloatingText, RichTextEditor, M3DatePicker, M3InfoPopup, M3Autocomplete, TableCombo, CheckTableCombo, XViewer, FilteredTreeComposite, XFilteredTree, AbstractPageControllerComposite

## Wave 3: Test-only promotions for aspects already in code

**Mechanism.** Write only the missing acceptance test, with no production change: assertHiddenGate plus a damage or pixel oracle. The oracle asserts recorded damage requests or the GC clip region, never PaintEvent bounds.
- Merge the duplicate CalculatorButtonsComposite row and write the missing CalculatorViewport_Test.
- Give RemoveAnnotationDialog its first own test. Keep it PARTIAL until the ComboViewport H leaf and the Annotation listener fix have landed.

**Rationale.** 22 cards can reach DONE with no production lines, but only after wave 2 has closed the H of what they wrap:
- CheckBoxGroup, Calculator and CalculatorButtonsComposite need Button and Label H.
- ReadOnlyStyledText needs StyledText H.
- TableCursor needs Table H.
- JSpinner needs ST-H1 (JFormattedTextField).
- RemoveAnnotationDialog needs ComboViewport H.
Link and Scale also need the native-paint and erase oracle from waves 1 and 2.

Classes (22): Link, Scale, CLabel, CBanner, SashForm, ViewForm, CustomButton, AbstractSelectableRow, AbstractSortableHeader, CheckBoxGroup, CalculatorButtonsComposite, Calculator, ReadOnlyStyledText, TableCursor, JMenu, JMenuItem, JCheckBoxMenuItem, JRadioButtonMenuItem, JPopupMenu, JScrollBar, JSpinner, RemoveAnnotationDialog

## Wave 4: FIXED damage: exact rectangles, clip-culled paint, same-value skips behind a stale bit

**Mechanism.** For each drawn control:
- Damage only the changed part (knob span, switch section, glyph/segment/dot box, item strip, focus ring, text band), inflated for antialiased strokes. Item strips come from a painted lane of x, width and input signature.
- Paint loops cull against the GC clip region.
- A same-value setter skips its repaint only while a stale bit is clear. Setters that never repaint (colours, fonts, borders, setEnabled) set that bit.
- Width-changing item setters damage up to the right edge.
- Item facades damage through their parent's lane.
Tests assert recorded damage requests, because win32 merges damage into one bounding PaintEvent.

**Rationale.** Builds on the wave-2 gate in the same classes. Item facades land with their parent: BreadcrumbItem after Breadcrumb, RoundedToolItem after RoundedToolbar, ToolbarItem after NebulaToolbar, NavigationPageGraphicsItem after NavigationPageGraphics. PasswordRevealer follows EyeButton and Text; M3TaskPane follows M3CollapsiblePane. The uncarded VButton and VLabel (CWT damage) and the visualization figure widgets also belong here. Re-pin the NebulaM3RoundedToolbarStreamRecipe AFTER hash and the RoundedToolItem contract hash in the same commits.

Classes (40): Breadcrumb, BreadcrumbItem, RoundedToolbar, RoundedToolItem, NebulaToolbar, ToolbarItem, NavigationPageGraphics, NavigationPageGraphicsItem, StarRating, SwitchButton, TriStateSwitchButton, FlatButton, RangeSlider, BaseLED, LED, DotsLed, RoundedCheckbox, RoundedSwitch, SegmentedBar, Stepbar, ProgressCircle, BadgedLabel, NebulaSlider, ImageContainer, ImageSelector, Chips, EyeButton, PasswordRevealer, M3Hyperlink, M3TaskPane, EmptyTablePlaceholder, TimeSlot, DayEditorCalendarableItemControl, MonthCalendarableItemControl, SplitButton, ToolBar, BasicComboBoxUI, DiagramComponent, AlphaLabel, GrayableButton

## Wave 5: ROWS: resident row window, logical row lanes, blit scroll, row damage

**Mechanism.** - Only rows on screen plus overscan exist natively or as painted state.
- Row offsets, heights and selection or check state live in lanes (long prefix sums, bitsets); facades outside the window are released.
- A scroll is a blit plus the exposed strip, and the model moves only after the blit.
- A row or cell change damages its projected rectangle.
- refresh, pack, computeSize and selection restore never walk all items.
Order: SWT mediators first, then the row widgets, then the row wrappers.

**Rationale.** Dependencies:
- D in TreeViewport, ListViewport and ComboViewport before PopupList, CCombo, TreeCursor and the JFace viewers J3-J11.
- Tree T4 (plain trees, size L) before the R of FilteredTreeComposite, XFilteredTree and TreeMapper.
- InternalCompositeTable before CompositeTable and AbstractNativeHeader.
- CTreeLayout (setOrigin routing) before CTreeItem and CTreeCell.
- PTWidgetTable before PTWidgetTree and before PropertyTable's R/D.
- JList before JComboBox and FilePane.
ComboViewer stays blocked until the SWT Combo gains a SetData/setItemCount API. Card first: TableItem, TreeItem, TableColumn, TreeColumn, JTableHeader, PageableTable, PageableTree and NavigationPageComboRenderer (O(totalPages) populateCombo).

Classes (47): TreeViewport, ListViewport, ComboViewport, Combo, Tree, TreeCursor, PopupList, CCombo, JList, BasicListUI, JComboBox, JTable, BasicTableUI, BasicTreeUI, FilePane, Grid, GridItem, GridColumn, GridColumnGroup, InternalCompositeTable, CompositeTable, AbstractNativeHeader, CTreeLayout, CTree, CTreeItem, CTreeCell, CTreeColumn, PShelf, PShelfItem, MultiChoice, PTWidgetTable, PTWidgetTree, PropertyTable, DualList, CTreeCombo, CTreeComboItem, CTreeComboColumn, AbstractTableViewer, TableViewer, CheckboxTableViewer, AbstractTreeViewer, TreeViewer, CheckboxTreeViewer, ColumnViewer, AbstractListViewer, ListViewer, ComboViewer

## Wave 6: LINES: line lanes, paint window, damage from the edited line down

**Mechanism.** - Longest-line and line-offset lanes are updated from document ElementChange deltas.
- Paint and layout run from the clip's first line to its last.
- Damage starts at the edited line's top and goes down; no whole-host repaints.
- Wrapped rows are counted per line.
- Text widgets inherit all of this through their views.

**Rationale.** Runs after the wave-2 text gates (ST-H1, StyledText H, Text H). The views come before the widgets that inherit from them. Text R needs an owner decision, because docs/m3/TEXT_VIEWPORT.md deliberately keeps the native EDIT. BidiLayout follows StyledText. PrintPieceCanvas also takes PrintViewer's relayout rows.

Classes (13): PlainView, WrappedPlainView, BoxView, ParagraphView, DefaultHighlighter, JTextArea, JTextPane, JEditorPane, JTextComponent, Text, RichTextViewer, PrintPieceCanvas, BidiLayout

## Wave 7: TILES: tile window over a logical origin, tile damage, blit pan

**Mechanism.** - Only on-screen tiles exist natively or as painted state; native tile images outside the window are released.
- Damage is the tile rectangle.
- A pan is a blit plus the exposed strips, with the model moved after the blit.
- The logical origin is long, and scroll bars are projections of it.

**Rationale.** InternalGeoMap comes before GeoMap and GeoMapViewer, and Gallery before the GalleryItem residency. Launcher retires LauncherLabel in viewport mode, which also resolves the duplicate LauncherLabel rows. MapFigure and M3HeatMap leaf 2 use the wave-1 host gate and logical origins. M3ImageView must blit before it moves the model, fixing the defect its verifier found.

Classes (12): InternalGeoMap, GeoMap, GeoMapViewer, MapFigure, Tiles, Gallery, GalleryItem, M3HeatMap, M3ImageView, Launcher, LauncherLabel, PrintPreview

## Wave 8: AXIS: long logical axis, window of visible cells, ticks and events, strip damage, blit paging

**Mechanism.** - The axis uses long logical coordinates, and only the visible cells, ticks and events are materialized.
- Sweep and append damage covers only the new strips, culled against the clip region.
- Paging and time-axis steps blit, with a full-redraw fallback when the calendar step does not match the pixel delta.
- Event editors use pooled event controls.
- Mappings stay overflow-free past 2^15 and 2^31.

**Rationale.** Order inside the wave:
- GanttComposite leaves 2-5 before GanttChart.
- The AbstractEventEditor EventEditorGate before DayEditor, MonthCalendar, Day and TimeSlice. These also need InternalCompositeTable from wave 5.
- The DateChooser beforeDrop flush before DateChooserCombo.
- The multichannel Oscilloscope before the deprecated facade.
- XYGraph's wave-2 H before Trace and PlotArea.
Axis and Annotation need cards in this wave.

Classes (22): GanttComposite, GanttChart, TimelineComposite, M3MonthView, DatePicker, CalendarComposite, MonthPick, DateChooser, DateChooserCombo, AbstractEventEditor, DayEditor, MonthCalendar, Day, TimeSlice, Oscilloscope (multichannel), Oscilloscope (deprecated), Plotter, XYGraph, Trace, PlotArea, IntensityGraphFigure, JSlider

## Wave 9: CHILDREN: planes beyond the native range, strip damage on resize, damage only for what moved

**Mechanism.** - Children beyond the 16-bit or int range become planes: ViewportPlane on win32, including child Decorations; a logical-origin band with projected bars in JViewport.
- A resize damages only the exposed strips.
- Divider, z-order and selection changes damage only what moved or what the stacking change exposed.
- Hidden layouts fold into one layout on reveal.

**Rationale.** Group and Decorations need only fork-file rows; JViewport (SWG1) comes before JScrollPane. LP1 on JLayeredPane and IF1 on JInternalFrame come before JDesktopPane. PGroupToolItem lands with PGroup and RadioItem with RadioGroup. ColumnBrowserWidget waits for the Table leaves. M3CollapsiblePane and M3TaskPaneContainer need a far-content test, so their G is graded like the other CHILDREN cards. Card first: JInternalFrame.JDesktopIcon, BasicSplitPaneDivider, BasicInternalFrameTitlePane, ScrollableTabViewport and ScrollableTabPanel, and CTabItem.

Classes (19): Decorations, Group, ScrolledComposite, M3CollapsiblePane, M3TaskPaneContainer, M3MultiSplitPane, M3StatusBar, PGroup, PGroupToolItem, CollapsibleButtons, RadioGroup, RadioItem, ColumnBrowserWidget, JViewport, JScrollPane, JTabbedPane, JLayeredPane, JInternalFrame, JDesktopPane

## Wave 10: Wrapper tail and re-grading

**Mechanism.** Three wrappers have a leaf of their own that needs their wrapped widgets closed first:
- TreeMapper: a pooled link window over two plain Trees, after T4 and the host gate.
- XCheckedFilteredTree: check marks restored from JFace item updates instead of paint, after XFilteredTree and the viewer leaves.
- XYGraphConfigDialog: config pages built lazily.
Then re-grade every WRAPPER's present aspects from its wrapped cards, and drop inherited claims that do not hold.

**Rationale.** All 79 open WRAPPER cards wrap PARTIAL or MISSING widgets. Re-grading after waves 2-9 removes the inherited-aspect contradictions in ReadOnlyStyledText, BidiLayout, PopupList, CCombo, TableCursor, TreeCursor, XViewer, FilteredTreeComposite, XFilteredTree, AddAnnotationDialog, Calculator, PasswordRevealer, JSpinner and M3TaskPane.

Classes (3): TreeMapper, XCheckedFilteredTree, XYGraphConfigDialog

## Coverage

The inventory has 327 cards in 25 units: DONE 7, PARTIAL 221, MISSING 55, NOT_APPLICABLE 40, REFUTED 4. I rebuilt the cards from the workflow journal (subagents/workflows/wf_14c3f677-472/journal.jsonl) with the same merge rules as the workflow script, and checked them against the denominators with comm and a script that follows each class's extends chain. All reads were read-only.

Coverage:
- Nebula: all 126 denominator classes have cards.
- Swing: all 46 denominator classes have cards.
- SWT win32: 32 of the 44 Widget subclasses have cards. Missing: Control, Widget, TableItem, TreeItem, TableColumn, TreeColumn, ToolItem, TabItem, ExpandItem, CoolItem, MenuItem, TaskBar.
- SWT custom: 11 of 12. Missing: CTabItem.

Both denominator lists are incomplete. Following the extends chains finds:
- 12 more SWT widgets in Nebula with no card (PageableTable, PageableTree and the pagination renderers, FlatCalendarCombo, XViewerFilteredTree).
- The CWT V-controls.
- The visualization figure widgets, plus Axis and Annotation, both of which run hidden work.
- Swing JTableHeader, Box and the public nested components.
- SWT Browser, GLCanvas, the OLE frames, Java2DCanvas and Tray.

78 cards are not widgets: 12 SWT mediators and helpers, 16 JFace classes, 29 Nebula classes, 21 Swing classes. I count draw2d figures and CWT V-controls as lightweight widgets.

Main inconsistencies:
- Two files have duplicate rows with contradictory verdicts (CalculatorButtonsComposite, LauncherLabel).
- RemoveAnnotationDialog is DONE with no test of its own, over a ComboViewport that is still PARTIAL.
- Wrappers claim aspects their wrapped widget lacks, and no wrapper wraps a DONE widget.
- Units grade H and G by different rules.
- 8 PARTIAL cards have no aspect present.
- Text, Tree, Combo and List behave differently by style but have one widget row plus a mediator row.
- There is no platform column, although many credits hold only on the win32 fork.
- hidden_work counts leave out entries the verifiers added.

Ten waves, split first by aspect, then by shape:
1. Shared gate cores per toolkit: 28 classes, all size S.
2. H rollout for every FIXED drawn widget, timer owner and popup owner: 120 classes.
3. Test-only promotions: 22.
4. FIXED damage: 40.
5. ROWS: 47.
6. LINES: 13.
7. TILES: 12.
8. AXIS: 22.
9. CHILDREN: 19.
10. Wrapper tail and re-grading: 3.

Scratch files are in K:\codex-work\agent-temp\claude\e--ws2-com-synexia\a6de83fe-6489-4056-8689-f1217602e9cc\scratchpad\critic: cards.tsv, cards_full.json, neb_closure.tsv, swing_closure.tsv, parse_cards.py, closure.py, waves.py.

Missing classes: 57; inconsistencies: 31; not widgets: 78 (CRITIQUE.json).
