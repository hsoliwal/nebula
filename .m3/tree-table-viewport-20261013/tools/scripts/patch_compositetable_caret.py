import sys

p = sys.argv[1]
s = open(p, encoding='utf-8').read()


def rep(a, b, n=1):
    global s
    assert s.count(a) == n, (a[:90], s.count(a))
    s = s.replace(a, b)


rep('''	/** a row update was requested while the table was not visible */
	private boolean rowsDirty;
''', '''	/** a row update was requested while the table was not visible */
	private boolean rowsDirty;

	//FLLDCIM: viewport round trip — the focused editor's caret, kept as ints by collection position
	private final java.util.IdentityHashMap<Control, Integer> shownPosition = new java.util.IdentityHashMap<>();
	private int caretPosition = -1;
	private int caretColumn = -1;
	private int caretStart;
	private int caretEnd;
''')
rep('''	private void fireRefreshEvent(int positionInCollection, Control rowControl) {
		if (numRowsInCollection < 1) {
			return;
		}
		for (Iterator<?> refreshListenersIter = parent.contentProviders.iterator(); refreshListenersIter
				.hasNext();) {
			IRowContentProvider listener = (IRowContentProvider) refreshListenersIter
					.next();
			listener.refresh(parent, positionInCollection, rowControl);
		}
	}''', '''	private void fireRefreshEvent(int positionInCollection, Control rowControl) {
		if (numRowsInCollection < 1) {
			return;
		}
		rememberCaret(rowControl);
		for (Iterator<?> refreshListenersIter = parent.contentProviders.iterator(); refreshListenersIter
				.hasNext();) {
			IRowContentProvider listener = (IRowContentProvider) refreshListenersIter
					.next();
			listener.refresh(parent, positionInCollection, rowControl);
		}
		shownPosition.put(rowControl, Integer.valueOf(positionInCollection));
		restoreCaret(positionInCollection, rowControl);
	}

	/**
	 * Before a row control is refreshed for another collection position, records the
	 * caret of the focused text editor inside it by the position it was showing.
	 */
	private void rememberCaret(Control rowControl) {
		Control focus = getDisplay().getFocusControl();
		Integer shown = shownPosition.get(rowControl);
		if (shown == null || !(rowControl instanceof Composite) || !(focus instanceof org.eclipse.swt.widgets.Text)
				|| focus.getParent() != rowControl) {
			return;
		}
		Control[] columns = ((Composite) rowControl).getChildren();
		for (int column = 0; column < columns.length; column++) {
			if (columns[column] == focus) {
				Point selection = ((org.eclipse.swt.widgets.Text) focus).getSelection();
				caretPosition = shown.intValue();
				caretColumn = column;
				caretStart = selection.x;
				caretEnd = selection.y;
				return;
			}
		}
	}

	/** After a refresh, puts back the caret recorded for that collection position. */
	private void restoreCaret(int positionInCollection, Control rowControl) {
		if (positionInCollection != caretPosition || !(rowControl instanceof Composite)) {
			return;
		}
		Control[] columns = ((Composite) rowControl).getChildren();
		if (caretColumn < columns.length && columns[caretColumn] instanceof org.eclipse.swt.widgets.Text) {
			((org.eclipse.swt.widgets.Text) columns[caretColumn]).setSelection(caretStart, caretEnd);
		}
	}''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched', p)
