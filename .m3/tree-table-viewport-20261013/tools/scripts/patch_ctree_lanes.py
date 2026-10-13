import re
import sys

root = sys.argv[1]


def patch(name, pairs, regexes=()):
    p = root + '/' + name
    s = open(p, encoding='utf-8').read()
    for a, b in pairs:
        assert s.count(a) == 1, (name, a[:90], s.count(a))
        s = s.replace(a, b)
    for pattern, repl, expected in regexes:
        s, n = re.subn(pattern, repl, s)
        assert n == expected, (name, pattern, n)
    open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched', name)


patch('CTree.java', [
    ('''	List itemList = new ArrayList();
''', '''	List itemList = new ArrayList();

	/** per-item and per-cell boolean state as packed bit lanes */
	final CTreeBitLanes lanes = new CTreeBitLanes();
'''),
])

patch('CTreeItem.java', [
    ('''	boolean painted = false;
''', '''	/** this item's slot in {@link CTree#lanes}: painted and autoHeight live there */
	final int slot;
'''),
    ('''	private boolean autoHeight;
''', ''),
    ('''		super(parent, style);
		this.ctree = parent;
		this.parentItem = parentItem;''', '''		super(parent, style);
		this.ctree = parent;
		this.slot = parent.lanes.allocate();
		this.parentItem = parentItem;'''),
    ('''		return autoHeight;''', '''		return ctree.lanes.get(CTreeBitLanes.ITEM_AUTO_HEIGHT, slot);'''),
    ('''        if (painted) {''', '''        if (ctree.lanes.get(CTreeBitLanes.ITEM_PAINTED, slot)) {'''),
    ('''	void setPainted(boolean painted) {
		if(this.painted != painted) {
			this.painted = painted;''', '''	void setPainted(boolean painted) {
		if(ctree.lanes.get(CTreeBitLanes.ITEM_PAINTED, slot) != painted) {
			ctree.lanes.set(CTreeBitLanes.ITEM_PAINTED, slot, painted);'''),
])

patch('CTreeCell.java', [
    ('''	private   boolean painted = false;
''', '''	/** this cell's slot in {@link CTree#lanes}: painted, hasChild, inited, holdChild, check state live there */
	private final int slot;
'''),
    ('''	private boolean hasChild;
''', ''),
    ('''	private boolean isCheck = false;
''', ''),
    ('''	private boolean isChecked = false;
''', ''),
    ('''	private boolean inited = false;
''', ''),
    ('''	private boolean holdChild = true;
''', ''),
    ('''		this.ctree = item.ctree;
		this.item = item;
		this.style = style;
		if((style & SWT.CHECK) != 0) {
			isCheck = true;
		}''', '''		this.ctree = item.ctree;
		this.item = item;
		this.style = style;
		this.slot = ctree.lanes.allocate();
		ctree.lanes.set(CTreeBitLanes.CELL_HOLD_CHILD, slot, true);
		if((style & SWT.CHECK) != 0) {
			ctree.lanes.set(CTreeBitLanes.CELL_CHECKABLE, slot, true);
		}'''),
], regexes=(
    (r'(?<![\w.])isChecked = ([^;]+);', r'ctree.lanes.set(CTreeBitLanes.CELL_CHECKED, slot, \1);', 3),
    (r'(?<![\w.])isCheck = ([^;]+);', r'ctree.lanes.set(CTreeBitLanes.CELL_CHECKABLE, slot, \1);', 1),
    (r'(?<![\w.])hasChild = ([^;]+);', r'ctree.lanes.set(CTreeBitLanes.CELL_HAS_CHILD, slot, \1);', 2),
    (r'(?<![\w.])inited = ([^;]+);', r'ctree.lanes.set(CTreeBitLanes.CELL_INITED, slot, \1);', 1),
    (r'this\.painted = painted;', r'ctree.lanes.set(CTreeBitLanes.CELL_PAINTED, slot, painted);', 1),
    (r'if\(this\.painted != painted\)', r'if(ctree.lanes.get(CTreeBitLanes.CELL_PAINTED, slot) != painted)', 1),
    (r'return painted;', r'return ctree.lanes.get(CTreeBitLanes.CELL_PAINTED, slot);', 1),
    (r'if \(painted\) \{', r'if (ctree.lanes.get(CTreeBitLanes.CELL_PAINTED, slot)) {', 1),
    (r'(?<![\w.])isChecked(?![\w(])', r'ctree.lanes.get(CTreeBitLanes.CELL_CHECKED, slot)', 2),
    (r'(?<![\w.])isCheck(?![\w(])', r'ctree.lanes.get(CTreeBitLanes.CELL_CHECKABLE, slot)', 5),
    (r'if\(hasChild\)', r'if(ctree.lanes.get(CTreeBitLanes.CELL_HAS_CHILD, slot))', 1),
    (r'if\(!inited\)', r'if(!ctree.lanes.get(CTreeBitLanes.CELL_INITED, slot))', 1),
    (r'if\(holdChild\)', r'if(ctree.lanes.get(CTreeBitLanes.CELL_HOLD_CHILD, slot))', 1),
))
