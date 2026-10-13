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
    lines[i] = re.sub(r'(?<![\w.])list\.', 'list().', line)
s = '\n'.join(lines)

rep('''		createPopup(null, -1);
		initAccessible();''', '''		//FLLDCIM: lazy — the drop-down shell and list are built on first open or first item API use
		initAccessible();''')
rep('''	void createPopup(String[] items, int selectionIndex) {''', '''	/**
	 * @return the drop-down list, building the drop-down on first use
	 */
	List list() {
		if (list == null && text != null && !isDisposed()) {
			createPopup(null, -1);
		}
		return list;
	}

	void createPopup(String[] items, int selectionIndex) {''')
rep('''		if (!drop) {
			popup.setVisible(false);''', '''		if (!drop) {
			if (popup == null) {
				return;
			}
			popup.setVisible(false);''')
rep('''		if (getShell() != popup.getParent()) {
			String[] items = list().getItems();''', '''		list();
		if (getShell() != popup.getParent()) {
			String[] items = list().getItems();''')
rep('''	boolean isDropped() {
		return popup.getVisible();''', '''	boolean isDropped() {
		return popup != null && popup.getVisible();''')
rep('''		if (text.isFocusControl() || arrow.isFocusControl() || list().isFocusControl() || popup.isFocusControl()) {''',
    '''		if (text.isFocusControl() || arrow.isFocusControl() || list != null && list.isFocusControl()
				|| popup != null && popup.isFocusControl()) {''')
rep('''        if (popup.isVisible()) {
            list().redraw();
        }''', '''        if (popup != null && popup.isVisible()) {
            list.redraw();
        }''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched CustomCombo, list() calls:', s.count('list().'))
