import sys

root = sys.argv[1]


def patch(name, pairs):
    p = root + '/' + name
    s = open(p, encoding='utf-8').read()
    for a, b in pairs:
        assert s.count(a) == 1, (name, a[:90], s.count(a))
        s = s.replace(a, b)
    open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched', name)


patch('CTreeComboItem.java', [
    ('''	public void buildRealTreeItem(Tree tree, int numberOfColumns) {
		final TreeItem ti;
		if (parentItem != null && parentItem.realTreeItem != null) {
			ti = new TreeItem(parentItem.realTreeItem, getStyle());
		} else {
			ti = new TreeItem(tree, getStyle());
		}
''', '''	public void buildRealTreeItem(Tree tree, int numberOfColumns) {
		final TreeItem ti;
		if (parentItem != null && parentItem.realTreeItem != null) {
			ti = new TreeItem(parentItem.realTreeItem, getStyle());
		} else {
			ti = new TreeItem(tree, getStyle());
		}
		bindRealTreeItem(ti, numberOfColumns);
	}

	/** live list of this item's children, for the virtual drop-down rows */
	List<CTreeComboItem> childList() {
		return childItems;
	}

	/** copies this item's attributes onto {@code ti} and binds it as this item's real tree item */
	void bindRealTreeItem(TreeItem ti, int numberOfColumns) {
'''),
])

patch('CTreeCombo.java', [
    ('''	public CTreeCombo(Composite parent, int style) {
		super(parent, style = checkStyle(style));
''', '''	/** style requested by the caller, before checkStyle masks it (UI-thread confined) */
	private static int requestedStyle;
	/** SWT.VIRTUAL requested: drop-down rows are bound only when the native tree asks for them */
	private final boolean virtualRows;
	private boolean popupFiltered;
	private final java.util.IdentityHashMap<Object, List<CTreeComboItem>> filteredChildren = new java.util.IdentityHashMap<>();

	private static int remember(int style) {
		requestedStyle = style;
		return style;
	}

	public CTreeCombo(Composite parent, int style) {
		super(parent, style = checkStyle(remember(style)));
		virtualRows = (requestedStyle & SWT.VIRTUAL) != 0;
'''),
    ('''		tree = new Tree(popup, listStyle);
		tree.addTreeListener(hookListener);''', '''		tree = new Tree(popup, listStyle | (virtualRows ? SWT.VIRTUAL : SWT.NONE));
		tree.addTreeListener(hookListener);'''),
    ('''		if (items != null) {
			createTreeItems(items.toArray(new CTreeComboItem[0]), filter);
		}

		if (selectedItem != null) {
			tree().setSelection(selectedItem.getRealTreeItem());
		}''', '''		if (items != null && virtualRows) {
			//FLLDCIM: lazy — drop-down rows are bound when the native virtual tree shows them
			popupFiltered = filter && (getStyle() & SWT.READ_ONLY) == 0 && !text.getText().trim().isEmpty();
			filteredChildren.clear();
			tree.addListener(SWT.SetData, event -> bindRow((TreeItem) event.item, event.index));
			tree.setItemCount(visibleChildren(null).size());
		} else if (items != null) {
			createTreeItems(items.toArray(new CTreeComboItem[0]), filter);
		}

		if (selectedItem != null) {
			tree().setSelection(virtualRows ? materialize(selectedItem) : selectedItem.getRealTreeItem());
		}'''),
    ('''	private boolean match(final CTreeComboItem item) {''', '''	private void bindRow(TreeItem row, int index) {
		final TreeItem parentRow = row.getParentItem();
		final CTreeComboItem parentItem = parentRow == null ? null
				: (CTreeComboItem) parentRow.getData(CTreeComboItem.DATA_ID);
		final List<CTreeComboItem> siblings = visibleChildren(parentItem);
		if (index < 0 || index >= siblings.size()) {
			return;
		}
		final CTreeComboItem item = siblings.get(index);
		item.bindRealTreeItem(row, columns.size());
		row.setItemCount(visibleChildren(item).size());
	}

	/** Children shown in the drop-down: all of them, or those passing the typed filter (the eager rule). */
	private List<CTreeComboItem> visibleChildren(CTreeComboItem parentItem) {
		final List<CTreeComboItem> all = parentItem == null ? items : parentItem.childList();
		if (!popupFiltered) {
			return all;
		}
		final Object key = parentItem == null ? (Object) this : parentItem;
		List<CTreeComboItem> shown = filteredChildren.get(key);
		if (shown == null) {
			shown = new ArrayList<CTreeComboItem>();
			for (final CTreeComboItem child : all) {
				if (included(child)) {
					shown.add(child);
				}
			}
			filteredChildren.put(key, shown);
		}
		return shown;
	}

	private boolean included(CTreeComboItem item) {
		if (match(item)) {
			return true;
		}
		for (final CTreeComboItem child : item.childList()) {
			if (included(child)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the native row of {@code item}, binding the rows on its path in a virtual drop-down; an item that is not
	 *         shown keeps whatever real item it had, exactly as the eager drop-down leaves it
	 */
	TreeItem materialize(CTreeComboItem item) {
		final Tree t = tree();
		if (item == null || t == null) {
			return item == null ? null : item.getRealTreeItem();
		}
		final TreeItem real = item.getRealTreeItem();
		if (!virtualRows || real != null && !real.isDisposed()) {
			return real;
		}
		final List<CTreeComboItem> path = new ArrayList<CTreeComboItem>();
		for (CTreeComboItem p = item; p != null; p = p.getParentItem()) {
			path.add(0, p);
		}
		TreeItem row = null;
		CTreeComboItem parentItem = null;
		for (final CTreeComboItem step : path) {
			final int index = visibleChildren(parentItem).indexOf(step);
			if (index < 0) {
				return real;
			}
			row = row == null ? t.getItem(index) : row.getItem(index);
			if (row.getData(CTreeComboItem.DATA_ID) != step) {
				step.bindRealTreeItem(row, columns.size());
				row.setItemCount(visibleChildren(step).size());
			}
			parentItem = step;
		}
		return row;
	}

	private boolean match(final CTreeComboItem item) {'''),
    ('''		return tree().indexOf(item.getRealTreeItem());''', '''		return tree().indexOf(materialize(item));'''),
    ('''				tree().select(item.getRealTreeItem());''', '''				tree().select(materialize(item));'''),
    ('''			items[i] = newItems[i].getRealTreeItem();''', '''			items[i] = materialize(newItems[i]);'''),
    ('''		tree().showItem(item.getRealTreeItem());''', '''		tree().showItem(materialize(item));'''),
])

patch('CTreeComboItem.java', [
    ('''	public void setExpanded(boolean expand) {
		checkWidget();
		parent.tree();''', '''	public void setExpanded(boolean expand) {
		checkWidget();
		parent.materialize(this);'''),
    ('''	public void setItemCount(int count) {
		checkWidget();
		parent.tree();''', '''	public void setItemCount(int count) {
		checkWidget();
		parent.materialize(this);'''),
    ('''	public void clear(int index, boolean all) {
		checkWidget();
		parent.tree();''', '''	public void clear(int index, boolean all) {
		checkWidget();
		parent.materialize(this);'''),
    ('''	public void clearAll(boolean all) {
		checkWidget();
		parent.tree();''', '''	public void clearAll(boolean all) {
		checkWidget();
		parent.materialize(this);'''),
])
