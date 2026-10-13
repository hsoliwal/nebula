import sys

p = sys.argv[1]
s = open(p, encoding='utf-8').read()


def rep(a, b, n=1):
    global s
    assert s.count(a) == n, (a[:90], s.count(a))
    s = s.replace(a, b)


rep('''	void updateVisibleRows() {
		// If we don't have our prototype row object yet, bail out''', '''	void updateVisibleRows() {
		//FLLDCIM: lazy — no row population while not effectively visible; caught up once on the
		// first paint or on the first public read of rows
		if (!isVisible()) {
			rowsDirty = true;
			return;
		}
		rowsDirty = false;
		updateVisibleRowsNow();
	}

	/** Runs a row update that was deferred while the table was not visible. */
	void flushRows() {
		if (rowsDirty && !isDisposed()) {
			rowsDirty = false;
			updateVisibleRowsNow();
		}
	}

	private void updateVisibleRowsNow() {
		// If we don't have our prototype row object yet, bail out''')
rep('''	private LinkedList<TableRow> spareRows = new LinkedList<TableRow>();
''', '''	private LinkedList<TableRow> spareRows = new LinkedList<TableRow>();

	/** a row update was requested while the table was not visible */
	private boolean rowsDirty;
''')
rep('''		controlHolder.addListener(SWT.MouseWheel, this);
''', '''		controlHolder.addListener(SWT.MouseWheel, this);
		controlHolder.addListener(SWT.Paint, event -> {
			if (rowsDirty) {
				getDisplay().asyncExec(this::flushRows);
			}
		});
''')
for sig in ('	public int getNumRowsVisible() {\n',
            '	public Point getSelection() {\n',
            '	public void setSelection(int column, int row) {\n',
            '	public Control getCurrentRowControl() {\n',
            '	public Control[] getRowControls() {\n',
            '	public int getControlRow(Control rowControl) {\n'):
    rep(sig, sig + '\t\tflushRows();\n')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched', p)
