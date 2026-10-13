import re
import sys

root = sys.argv[1]
p = root + '/CTreeCombo.java'
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
    lines[i] = re.sub(r'(?<![\w.])tree\.', 'tree().', line)
s = '\n'.join(lines)

rep('''		createPopup(null, null);
		initAccessible();''', '''		//FLLDCIM: lazy — the drop-down shell and tree are built on first open, first layout or first item API use
		initAccessible();''')
rep('''	void createPopup(Collection<CTreeComboItem> items, CTreeComboItem selectedItem) {''',
    '''	/**
	 * @return the drop-down tree, building the drop-down (unfiltered, as items were added) on first use
	 */
	Tree tree() {
		if (tree == null && !isDisposed()) {
			createPopup(items, null, false);
		}
		return tree;
	}

	void createPopup(Collection<CTreeComboItem> items, CTreeComboItem selectedItem) {
		createPopup(items, selectedItem, true);
	}

	private void createPopup(Collection<CTreeComboItem> items, CTreeComboItem selectedItem, boolean filter) {''')
rep('''		tree = new Tree(popup, listStyle);
		tree().addTreeListener(hookListener);''', '''		tree = new Tree(popup, listStyle);
		tree.addTreeListener(hookListener);''')
rep('''		if (items != null) {
			createTreeItems(items.toArray(new CTreeComboItem[0]));
		}''', '''		if (items != null) {
			createTreeItems(items.toArray(new CTreeComboItem[0]), filter);
		}''')
rep('''	private void createTreeItems(CTreeComboItem[] items) {
		boolean isReadOnly = (getStyle() & SWT.READ_ONLY) != 0;
''', '''	private void createTreeItems(CTreeComboItem[] items, boolean filter) {
		boolean isReadOnly = !filter || (getStyle() & SWT.READ_ONLY) != 0;
''')
rep('''			createTreeItems(item.getItems());''', '''			createTreeItems(item.getItems(), filter);''')
rep('''		if (drop == isDropped() || !isVisible()) {
			return;
		}
		if (!drop) {
			popup.setVisible(false);''', '''		if (drop == isDropped() || !isVisible()) {
			return;
		}
		if (!drop) {
			if (popup == null) {
				return;
			}
			popup.setVisible(false);''')
rep('''		CTreeComboItem selectionIndex = null;
		if (getShell() != popup.getParent()) {''', '''		CTreeComboItem selectionIndex = null;
		tree();
		if (getShell() != popup.getParent()) {''')
rep('''	boolean isDropped() {
		return popup.getVisible();''', '''	boolean isDropped() {
		return popup != null && popup.getVisible();''')
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched CTreeCombo, tree() calls:', s.count('tree().'))

for name in ('CTreeComboItem.java', 'CTreeComboColumn.java'):
    q = root + '/' + name
    t = open(q, encoding='utf-8').read()
    print(name, 'parent.tree refs:', t.count('parent.tree'))
