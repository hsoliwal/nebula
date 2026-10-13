import re
import sys

root = sys.argv[1]
p = root + '/TableCombo.java'
s = open(p, encoding='utf-8').read()


def rep(a, b, n=1):
    global s
    assert s.count(a) == n, (a[:90], s.count(a))
    s = s.replace(a, b)


# 1. every method call on the popup table goes through the lazy accessor (comments untouched)
lines = s.split('\n')
for i, line in enumerate(lines):
    stripped = line.lstrip()
    if stripped.startswith('*') or stripped.startswith('//') or stripped.startswith('/*'):
        continue
    lines[i] = re.sub(r'(?<![\w.])table\.', 'table().', line)
s = '\n'.join(lines)

# 2. focus query must not build the drop-down
rep('''		if (arrow.isFocusControl() || table().isFocusControl() || popup.isFocusControl()) {''',
    '''		if (arrow.isFocusControl() || table != null && table.isFocusControl()
				|| popup != null && popup.isFocusControl()) {''')
# 3. no drop-down at construction: built on first open or first getTable()/item API use
rep('''		// initialize the drop down
		createPopup(-1);
''', '''		//FLLDCIM: lazy — the drop-down shell and table are built on first open or first table API use
''')
rep('''	void createPopup(final int selectionIndex) {''', '''	/**
	 * @return the drop-down table, building the drop-down on first use
	 */
	private Table table() {
		if (table == null && !isDisposed()) {
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
rep('''		if (!getShell().equals(popup.getParent())) {
			// Can happen when a part is detached
			recreatePopup();
		}

		// get the size of the TableCombo.''', '''		table();
		if (!getShell().equals(popup.getParent())) {
			// Can happen when a part is detached
			recreatePopup();
		}

		// get the size of the TableCombo.''')
rep('''	private boolean isDropped() {
		if (!getShell().equals(popup.getParent())) {''', '''	private boolean isDropped() {
		if (popup == null) {
			return false;
		}
		if (!getShell().equals(popup.getParent())) {''')
rep('''		if (popup.isVisible()) {
			table().redraw();
		}''', '''		if (popup != null && popup.isVisible()) {
			table.redraw();
		}''')
rep('''	private void recreatePopup() {''', '''	private void recreatePopup() {
		if (popup == null) {
			// not built yet: it is created under the current shell on first use
			return;
		}''')
rep('''	public Table getTable() {
		checkWidget();
		return table;''', '''	public Table getTable() {
		checkWidget();
		return table();''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched TableCombo, table() calls:', s.count('table().'))

r = root + '/DefaultTableComboRenderer.java'
t = open(r, encoding='utf-8').read()
a = '''		this.tableCombo = tableCombo;
		this.table = tableCombo.getTable();'''
assert t.count(a) == 1
t = t.replace(a, '''		this.tableCombo = tableCombo;''')
t = re.sub(r'(?<![\w.])table\.getItem\(', 'tableCombo.getTable().getItem(', t)
assert 'private Table table;' in t
t = t.replace('	private Table table;\n', '')
open(r, 'w', encoding='utf-8', newline='\n').write(t)
print('patched renderer, remaining table refs:', len(re.findall(r'(?<![\w.])table\b', t)))
