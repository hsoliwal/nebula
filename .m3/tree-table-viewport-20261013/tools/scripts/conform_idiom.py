"""SWT/Nebula idiom conformance pass (SWT_IDIOM_RULES.md) for the files this leaf adds or changes."""
import sys

root = sys.argv[1]
CONTRIB = (" *\n * Contributors:\n *     Hitesh Soliwal - initial API and implementation\n")
HEADER_END = " * SPDX-License-Identifier: EPL-2.0\n *******************************************************************************/"
NEW_FILES = [
    "widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/XViewerViewportContentProvider.java",
    "widgets/xviewer/org.eclipse.nebula.widgets.xviewer.tests/src/org/eclipse/nebula/widgets/xviewer/tests/XViewerViewport_Test.java",
    "widgets/ctree/org.eclipse.nebula.widgets.ctree/src/org/eclipse/nebula/widgets/ctree/CTreeBitLanes.java",
    "widgets/ctree/org.eclipse.nebula.widgets.ctree.tests/src/org/eclipse/nebula/widgets/ctree/CTreeViewport_Test.java",
    "widgets/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.tests/src/org/eclipse/nebula/widgets/ctreecombo/CTreeComboViewport_Test.java",
    "widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests/src/org/eclipse/nebula/widgets/compositetable/CompositeTableViewport_Test.java",
]


def edit(rel, pairs):
    p = root + "/" + rel
    s = open(p, encoding="utf-8").read()
    for a, b in pairs:
        assert s.count(a) == 1, (rel, a[:80], s.count(a))
        s = s.replace(a, b)
    open(p, "w", encoding="utf-8", newline="\n").write(s)
    print("conformed", rel.split("/")[-1])


# 5. licence headers: SWT/Nebula EPL-2.0 format with a Contributors section
for rel in NEW_FILES:
    p = root + "/" + rel
    s = open(p, encoding="utf-8").read()
    if "Contributors:" not in s.split("*/", 1)[0]:
        edit(rel, [(HEADER_END, " * SPDX-License-Identifier: EPL-2.0\n" + CONTRIB
                    + " *******************************************************************************/")])

# 2. superset additions carry the bundle's next @since (xviewer 1.1.0)
edit("widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/XViewer.java", [
    ("""    * @return true if the tree was created with {@code SWT.VIRTUAL}, so the viewer works as a viewport over the model
    * @since 1.2""", """    * @return true if the tree was created with {@code SWT.VIRTUAL}, so the viewer works as a viewport over the model
    * @since 1.1"""),
    ("""    * viewport over a plain tree content provider
    * @since 1.2""", """    * viewport over a plain tree content provider
    * @since 1.1"""),
])
edit("widgets/xviewer/org.eclipse.nebula.widgets.xviewer/src/org/eclipse/nebula/widgets/xviewer/XViewerViewportContentProvider.java", [
    (""" * @since 1.2
 */
public final class XViewerViewportContentProvider""", """ * @since 1.1
 */
public final class XViewerViewportContentProvider"""),
])

# 1./2. SWT voice for the public CTreeItem overrides
edit("widgets/ctree/org.eclipse.nebula.widgets.ctree/src/org/eclipse/nebula/widgets/ctree/CTreeItem.java", [
    ("""	/**
	 * Item data lives in the tree's slot lanes, not in the item.
	 */
	@Override
	public Object getData() {""", """	/**
	 * Returns the application defined widget data associated with the receiver, or null if it has not been set.
	 * The data is kept in the tree's slot lanes, not in the item.
	 *
	 * @return the widget data
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	 */
	@Override
	public Object getData() {"""),
    ("""	/**
	 * Item data lives in the tree's slot lanes, not in the item.
	 */
	@Override
	public void setData(Object data) {""", """	/**
	 * Sets the application defined widget data associated with the receiver to be the argument. The data is kept in
	 * the tree's slot lanes, not in the item, and is released when the item is disposed.
	 *
	 * @param data the widget data
	 *
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	 */
	@Override
	public void setData(Object data) {"""),
])

# 3. no allocation per drag frame or per paint: reusable runnables
edit("widgets/compositetable/org.eclipse.nebula.widgets.compositetable/src/org/eclipse/nebula/widgets/compositetable/InternalCompositeTable.java", [
    ("""	/** a thumb-drag position is waiting to be applied to the row window */
	private boolean sliderDragPending;
""", """	/** a thumb-drag position is waiting to be applied to the row window */
	private boolean sliderDragPending;

	/** applies the latest thumb-drag position; allocated once, not per drag event */
	private final Runnable sliderDragCatchUp = new Runnable() {
		public void run() {
			sliderDragPending = false;
			if (!isDisposed() && !vSlider.isDisposed()) {
				sliderSelectionListener.widgetDefaultSelected(null);
			}
		}
	};

	/** runs a deferred row update; allocated once, not per paint */
	private final Runnable flushRowsRunnable = new Runnable() {
		public void run() {
			flushRows();
		}
	};
"""),
    ("""				if (!sliderDragPending) {
					sliderDragPending = true;
					getDisplay().asyncExec(() -> {
						sliderDragPending = false;
						if (!isDisposed() && !vSlider.isDisposed()) {
							applySliderSelection();
						}
					});
				}
				return;""", """				if (!sliderDragPending) {
					sliderDragPending = true;
					getDisplay().asyncExec(sliderDragCatchUp);
				}
				return;"""),
    ("""			if (e.detail == SWT.DRAG) {""", """			if (e != null && e.detail == SWT.DRAG) {"""),
    ("""				getDisplay().asyncExec(this::flushRows);""", """				getDisplay().asyncExec(flushRowsRunnable);"""),
])
