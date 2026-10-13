import sys

p = sys.argv[1]
s = open(p, encoding='utf-8').read()


def rep(a, b, n=1):
    global s
    assert s.count(a) == n, (a[:90], s.count(a))
    s = s.replace(a, b)


rep('''	private Color oddLinesForegroundColor, oddLinesBackgroundColor;
''', '''	private Color oddLinesForegroundColor, oddLinesBackgroundColor;
	//FLLDCIM: HPCPOP — SWT.VIRTUAL popup table: rows the table asked for, as a packed bit set
	private boolean virtualTable;
	private int[] materializedRows = new int[0];
	private final Listener lateRowColors = event -> applyLineColors(event.index, (TableItem) event.item);
''')
rep('''		super(parent, newStyle = checkStyle(style));
		hasBorder = (style & SWT.BORDER) == SWT.BORDER;''', '''		super(parent, newStyle = checkStyle(style));
		virtualTable = (style & SWT.VIRTUAL) != 0;
		hasBorder = (style & SWT.BORDER) == SWT.BORDER;''')
rep('''		selectedImage = new Label(this, SWT.NONE);''', '''		selectedImage = new Label(this, SWT.NO_FOCUS);''')
rep('''		table = new Table(popup, SWT.SINGLE | SWT.FULL_SELECTION);
''', '''		table = new Table(popup, SWT.SINGLE | SWT.FULL_SELECTION | (virtualTable ? SWT.VIRTUAL : SWT.NONE));
		if (virtualTable) {
			materializedRows = new int[0];
			table.addListener(SWT.SetData, event -> markMaterialized(event.index));
		}
''')
# computeSize: no item array; virtual tables measure only rows the table has asked for
rep('''			final TableItem[] tableItems = table.getItems();

			final GC gc = new GC(text);''', '''			final GC gc = new GC(text);''')
rep('''			// calculate the maximum text width and image height.
			int index = 0;
			for (final TableItem tableItem : tableItems) {
				currTextWidth = gc.stringExtent(renderer.getLabel(index++)).x;
''', '''			// calculate the maximum text width and image height.
			final int count = table.getItemCount();
			for (int index = 0; index < count; index++) {
				if (virtualTable && !isMaterialized(index)) {
					continue;
				}
				final TableItem tableItem = table.getItem(index);
				currTextWidth = gc.stringExtent(renderer.getLabel(index)).x;
''')
# odd/even colors: nothing to do without colors; virtual tables color asked-for rows, later rows on SetData
rep('''		// Change color for odd & even lines
		for (int i = 0; i < table.getItemCount(); i++) {
			final TableItem item = table.getItem(i);
			if (i % 2 == 0) {''', '''		// Change color for odd & even lines
		final boolean lineColors = oddLinesForegroundColor != null || oddLinesBackgroundColor != null
				|| evenLinesForegroundColor != null || evenLinesBackgroundColor != null;
		if (virtualTable) {
			table.removeListener(SWT.SetData, lateRowColors);
			if (lineColors) {
				table.addListener(SWT.SetData, lateRowColors);
			}
		}
		for (int i = 0, count = lineColors ? table.getItemCount() : 0; i < count; i++) {
			if (virtualTable && !isMaterialized(i)) {
				continue;
			}
			final TableItem item = table.getItem(i);
			if (i % 2 == 0) {''')
rep('''		// get a list of the table items.
		final TableItem[] tableItems = table.getItems();

		final int totalItems = tableItems == null ? 0 : tableItems.length;
''', '''		final int totalItems = table.getItemCount();
''', 3)
rep('''	private void recreatePopup() {''', '''	private void markMaterialized(final int index) {
		if (index < 0) {
			return;
		}
		final int word = index >>> 5;
		if (word >= materializedRows.length) {
			final int[] grown = new int[Math.max(word + 1, materializedRows.length * 2)];
			System.arraycopy(materializedRows, 0, grown, 0, materializedRows.length);
			materializedRows = grown;
		}
		materializedRows[word] |= 1 << (index & 31);
	}

	private boolean isMaterialized(final int index) {
		final int word = index >>> 5;
		return word < materializedRows.length && (materializedRows[word] & 1 << (index & 31)) != 0;
	}

	private void applyLineColors(final int index, final TableItem item) {
		if (item == null || item.isDisposed()) {
			return;
		}
		final Color fg = index % 2 == 0 ? oddLinesForegroundColor : evenLinesForegroundColor;
		final Color bg = index % 2 == 0 ? oddLinesBackgroundColor : evenLinesBackgroundColor;
		if (fg != null) {
			item.setForeground(fg);
		}
		if (bg != null) {
			item.setBackground(bg);
		}
	}

	private void recreatePopup() {''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched', p)
