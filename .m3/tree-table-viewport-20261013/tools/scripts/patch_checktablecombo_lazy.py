import re
import sys

p = sys.argv[1]
s = open(p, encoding='utf-8').read()


def rep(a, b, n=1):
    global s
    assert s.count(a) == n, (a[:90], s.count(a))
    s = s.replace(a, b)


lines = s.split('\n')
for i, line in enumerate(lines):
    stripped = line.lstrip()
    if stripped.startswith('*') or stripped.startswith('//') or stripped.startswith('/*'):
        continue
    lines[i] = re.sub(r'(?<![\w.])table\.', 'table().', line)
s = '\n'.join(lines)

rep('''		// initialize the drop down
		createPopup(-1);
''', '''		//FLLDCIM: lazy — the drop-down shell and table are built on first open or first table API use
''')
rep('''	void createPopup(final int selectionIndex) {''', '''	/**
	 * @return the drop-down table, building the drop-down on first use
	 */
	private Table table() {
		if (table == null && text != null && !isDisposed()) {
			createPopup(-1);
		}
		return table;
	}

	void createPopup(final int selectionIndex) {''')
rep('''		// closing the dropDown
		if (!drop) {
			popup.setVisible(false);''', '''		// closing the dropDown
		if (!drop) {
			if (popup == null) {
				return;
			}
			popup.setVisible(false);''')
rep('''		// create a new popup if needed.
		if (getShell() != popup.getParent()) {''', '''		// create a new popup if needed.
		table();
		if (getShell() != popup.getParent()) {''')
rep('''	private boolean isDropped() {
		return popup.getVisible();''', '''	private boolean isDropped() {
		return popup != null && popup.getVisible();''')
rep('''		if (arrow.isFocusControl() || table().isFocusControl() || popup.isFocusControl()) {''',
    '''		if (arrow.isFocusControl() || table != null && table.isFocusControl()
				|| popup != null && popup.isFocusControl()) {''')
rep('''		if (popup.isVisible()) {
			table().redraw();
		}''', '''		if (popup != null && popup.isVisible()) {
			table.redraw();
		}''')
rep('''	public Table getTable() {
		checkWidget();
		return table;''', '''	public Table getTable() {
		checkWidget();
		return table();''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched CheckTableCombo, table() calls:', s.count('table().'))
