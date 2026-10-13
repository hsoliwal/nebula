import sys

root = sys.argv[1]


def patch(name, pairs):
    p = root + '/' + name
    s = open(p, encoding='utf-8').read()
    for pair in pairs:
        a, b = pair[0], pair[1]
        n = pair[2] if len(pair) > 2 else 1
        assert s.count(a) == n, (name, a[:90], s.count(a))
        s = s.replace(a, b)
    open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched', name)


patch('CTree.java', [
    # one control: body and header never take focus; keys, traversal and focus live on the CTree
    ('''		body = new Canvas(this, SWT.BORDER | SWT.NO_BACKGROUND);''',
     '''		//FLLDCIM: one control — the body is a NO_FOCUS helper surface; focus, keys and traversal stay on the CTree
		body = new Canvas(this, SWT.BORDER | SWT.NO_BACKGROUND | SWT.NO_FOCUS);'''),
    ('''		body.addKeyListener(new KeyAdapter() {}); // traverse does not work without this...
		body.addListener(SWT.FocusIn, this);''',
     '''		addKeyListener(new KeyAdapter() {}); // traverse does not work without this...
		addListener(SWT.FocusIn, this);
		addListener(SWT.FocusOut, this);'''),
    ('''		body.addListener(SWT.Traverse, this);
''',
     '''		addListener(SWT.Traverse, this);
'''),
    ('''		case SWT.FocusIn:
		case SWT.FocusOut:
			handleFocus(event.type);
			break;''',
     '''		case SWT.FocusIn:
		case SWT.FocusOut:
			if (event.widget == this) {
				nativeFocusChange(event.type);
			} else {
				handleFocus(event.type);
			}
			break;'''),
    ('''	protected void handleMouseEvents(Event event) {''',
     '''	/**
	 * The CTree itself owns native focus: its FocusIn/FocusOut events already reached
	 * the listeners, so only the internal focus state is updated here.
	 */
	private void nativeFocusChange(int type) {
		if (isDisposed()) {
			return;
		}
		Display display = getDisplay();
		if (type == SWT.FocusIn) {
			if (!hasFocus) {
				hasFocus = true;
				updateFocus();
			}
			display.removeFilter(SWT.FocusIn, filter);
			display.addFilter(SWT.FocusIn, filter);
			return;
		}
		Control focusControl = display.getFocusControl();
		if (focusControl != null && focusControl != this) {
			for (Iterator i = items(false).iterator(); i.hasNext();) {
				if (((CTreeItem) i.next()).contains(focusControl)) {
					return;
				}
			}
		}
		if (hasFocus) {
			hasFocus = false;
			updateFocus();
		}
		display.removeFilter(SWT.FocusIn, filter);
	}

	protected void handleMouseEvents(Event event) {'''),
    # header surfaces hand focus back to the CTree
    ('''			header = new Composite(this, SWT.NONE);''',
     '''			header = new Composite(this, SWT.NO_FOCUS);'''),
    ('''			internalTable = new Table(getHeader(), SWT.NONE);
			internalTable.setLinesVisible(linesVisible);''',
     '''			internalTable = new Table(getHeader(), SWT.NONE);
			internalTable.setLinesVisible(linesVisible);
			internalTable.addListener(SWT.FocusIn, e -> forceFocus());'''),
    # viewport: painted window by binary search over the laid-out bottoms
    ('''	protected List getPaintedItems() {
		int top = getScrollPosition().y;
		int bot = top + body.getClientArea().height;
		int itop = 0;
		int ibot = 0;
		List list = new ArrayList();
		boolean painting = false;
		for(Iterator i = items(false).iterator(); i.hasNext(); ) {
			CTreeItem item = (CTreeItem) i.next();
			Rectangle r = item.getBounds();
			ibot = r.y+r.height;''',
     '''	protected List getPaintedItems() {
		int top = getScrollPosition().y;
		int bot = top + body.getClientArea().height;
		List visible = items(false);
		//FLLDCIM: HPCPOP — skip the rows whose bottom lies above the viewport (O(log n)); same rows as the full scan
		int start = firstBottomAtLeast(visible, top);
		int itop = start == 0 ? 0 : ((CTreeItem) visible.get(start - 1)).getBottom();
		int ibot = 0;
		List list = new ArrayList();
		boolean painting = false;
		paintedStart = start;
		for(ListIterator i = visible.listIterator(start); i.hasNext(); ) {
			CTreeItem item = (CTreeItem) i.next();
			ibot = item.getBottom();'''),
    ('''	public CTreeItem getTopItem() {
		int top = getScrollPosition().y;
		int itop = 0;
		int ibot = 0;
		for(Iterator i = items(false).iterator(); i.hasNext(); ) {
			CTreeItem item = (CTreeItem) i.next();
			Rectangle r = item.getBounds();
			ibot = r.y+r.height;''',
     '''	public CTreeItem getTopItem() {
		int top = getScrollPosition().y;
		List visible = items(false);
		int start = firstBottomAtLeast(visible, top);
		int itop = start == 0 ? 0 : ((CTreeItem) visible.get(start - 1)).getBottom();
		int ibot = 0;
		for(ListIterator i = visible.listIterator(start); i.hasNext(); ) {
			CTreeItem item = (CTreeItem) i.next();
			ibot = item.getBottom();'''),
    ('''	public CTreeItem getParentItem() {''',
     '''	/**
	 * Index of the first visible item whose bottom is at or below {@code y}, when the
	 * visible list is exactly the one last laid out (bottoms are then non-decreasing);
	 * otherwise 0, so callers scan from the start as before.
	 */
	int firstBottomAtLeast(List visible, int y) {
		if (!layout.isLaidOut(visible)) {
			return 0;
		}
		int lo = 0;
		int hi = visible.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (((CTreeItem) visible.get(mid)).getBottom() < y) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	public CTreeItem getParentItem() {'''),
    ('''	List itemList = new ArrayList();
	''',
     '''	List itemList = new ArrayList();

	/** visible-list index of the first painted item, from the last getPaintedItems() */
	private int paintedStart;
	'''),
    ('''			itop = r.y+r.height;''', '''			itop = ibot;''', 2),
    # grid lines: stop once a line can no longer land in the image or the client area
    ('''					int gridline = getGridLineWidth();
					for(Iterator i = items(false).iterator(); i.hasNext(); ) {
						gc.drawLine(r.x, y, r.x+r.width, y);''',
     '''					int gridline = getGridLineWidth();
					List visible = items(false);
					//FLLDCIM: HPCPOP — after a full layout y only grows; lines past the image and client area are invisible
					int limit = layout.isLaidOut(visible) ? Math.max(r.height, ebounds.height) : Integer.MAX_VALUE;
					for(Iterator i = visible.iterator(); i.hasNext(); ) {
						if (y >= limit) {
							break;
						}
						gc.drawLine(r.x, y, r.x+r.width, y);'''),
    ('''			int firstPaintedIndex = items(false).indexOf(paintedItems.get(0));''',
     '''			List visible = items(false);
			int firstPaintedIndex = paintedStart < visible.size() && visible.get(paintedStart) == paintedItems.get(0)
					? paintedStart : visible.indexOf(paintedItems.get(0));'''),
])

patch('CTreeLayout.java', [
    ('''	private boolean fixedHeight = false;
''',
     '''	private boolean fixedHeight = false;

	/** the visible-item list instance whose tops and heights were assigned by the last layoutContent() */
	private List laidOut;
'''),
    ('''	protected void layoutContent() {
		int gridLine = ctree.getGridLineWidth();
		int top = 0;
		for(Iterator i = ctree.items(false).iterator(); i.hasNext(); ) {
			CTreeItem item = (CTreeItem) i.next();
			item.setTop(top);
			item.setHeight(fixedHeight ? getItemHeight() : item.computeHeight());
			top += item.getHeight() + gridLine;
		}
		ctree.body.redraw();
	}''',
     '''	protected void layoutContent() {
		int gridLine = ctree.getGridLineWidth();
		int top = 0;
		List items = ctree.items(false);
		laidOut = null;
		boolean monotonic = gridLine >= 0;
		for(Iterator i = items.iterator(); i.hasNext(); ) {
			CTreeItem item = (CTreeItem) i.next();
			item.setTop(top);
			//FLLDCIM: HPCPOP — no per-item invalidation; the whole body is redrawn once below
			item.setHeightQuiet(fixedHeight ? getItemHeight() : item.computeHeight());
			monotonic &= item.getHeight() >= 0;
			top += item.getHeight() + gridLine;
		}
		laidOut = monotonic ? items : null;
		ctree.body.redraw();
	}

	/**
	 * @return true if {@code items} is the exact list instance laid out last, so its
	 * items' bottoms are non-decreasing
	 */
	boolean isLaidOut(List items) {
		return items != null && items == laidOut;
	}'''),
])

patch('CTreeItem.java', [
    ('''	void setHeight(int height) {
		this.height = height;
		redraw();
	}''',
     '''	void setHeight(int height) {
		this.height = height;
		redraw();
	}

	/** sets the height without invalidating; the caller redraws the whole body */
	void setHeightQuiet(int height) {
		this.height = height;
	}'''),
])
