import sys

root = sys.argv[1]


def patch(name, pairs):
    p = root + "/" + name
    s = open(p, encoding="utf-8").read()
    for a, b, n in pairs:
        assert s.count(a) == n, (name, a[:90], s.count(a))
        s = s.replace(a, b)
    open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("patched", name)


patch("CompositeTable.java", [
    ("contentPane = new InternalCompositeTable(this, SWT.NULL);",
     "contentPane = new InternalCompositeTable(this, SWT.NO_FOCUS);", 2),
])

patch("InternalCompositeTable.java", [
    # one control: every structural helper is NO_FOCUS; the caller's row editors keep focus
    ("\t\thScroller = new Composite(this, SWT.NULL);", "\t\thScroller = new Composite(this, SWT.NO_FOCUS);", 1),
    ("\t\tcontrolHolder = new Composite(hScroller, SWT.NONE);", "\t\tcontrolHolder = new Composite(hScroller, SWT.NO_FOCUS);", 1),
    ("\t\tvSliderHolder = new Composite(this, SWT.NULL);\n\t\tvSlider = new Slider(vSliderHolder, SWT.VERTICAL);",
     "\t\tvSliderHolder = new Composite(this, SWT.NO_FOCUS);\n\t\tvSlider = new Slider(vSliderHolder, SWT.VERTICAL | SWT.NO_FOCUS);\n"
     "\t\tvSlider.addListener(SWT.FocusIn, e -> returnFocusToRows());", 1),
    ("\t\thSliderHolder = new Composite(this, SWT.NULL);\n\t\thSlider = new Slider(hSliderHolder, SWT.HORIZONTAL);",
     "\t\thSliderHolder = new Composite(this, SWT.NO_FOCUS);\n\t\thSlider = new Slider(hSliderHolder, SWT.HORIZONTAL | SWT.NO_FOCUS);\n"
     "\t\thSlider.addListener(SWT.FocusIn, e -> returnFocusToRows());", 1),
    # no row refresh while hidden either; one catch-up
    ("""	void refreshAllRows() {
		int row = 0;""", """	void refreshAllRows() {
		if (!isVisible() && !flushingRows) {
			refreshDirty = true;
			return;
		}
		refreshDirty = false;
		int row = 0;""", 1),
    ("""	void updateVisibleRows() {
		//FLLDCIM: lazy — no row population while not effectively visible; caught up once on the
		// first paint or on the first public read of rows
		if (!isVisible()) {""", """	void updateVisibleRows() {
		//FLLDCIM: lazy — no row population while not effectively visible; caught up once on the
		// first paint or on the first public read of rows
		if (!isVisible() && !flushingRows) {""", 1),
    ("""	void flushRows() {
		if (rowsDirty && !isDisposed()) {
			rowsDirty = false;
			updateVisibleRowsNow();
		}
	}""", """	void flushRows() {
		if ((rowsDirty || refreshDirty) && !isDisposed()) {
			flushingRows = true;
			try {
				if (rowsDirty) {
					rowsDirty = false;
					updateVisibleRowsNow();
				}
				if (refreshDirty) {
					refreshAllRows();
				}
			} finally {
				flushingRows = false;
			}
		}
	}

	/** A slider never keeps focus: keys and accessibility stay with the table's current row. */
	private void returnFocusToRows() {
		if (isDisposed()) {
			return;
		}
		Control current = getCurrentRowControl();
		if (current != null && !current.isDisposed()) {
			current.setFocus();
		} else {
			parent.forceFocus();
		}
	}""", 1),
    ("""	/** a row update was requested while the table was not visible */
	private boolean rowsDirty;
""", """	/** a row update was requested while the table was not visible */
	private boolean rowsDirty;
	/** a refresh of all rows was requested while the table was not visible */
	private boolean refreshDirty;
	/** a deferred row update or refresh is being caught up */
	private boolean flushingRows;
""", 1),
    ("""			if (rowsDirty) {
				getDisplay().asyncExec(this::flushRows);
			}""", """			if (rowsDirty || refreshDirty) {
				getDisplay().asyncExec(this::flushRows);
			}""", 1),
])
