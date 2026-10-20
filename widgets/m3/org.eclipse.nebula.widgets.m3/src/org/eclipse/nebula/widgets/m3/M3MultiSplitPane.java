/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3;

import java.util.Arrays;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;

/**
 * A pane split into any tree of rows and columns with draggable dividers (the SWT counterpart of SwingX
 * <code>JXMultiSplitPane</code> and its <code>MultiSplitLayout</code>, re-derived from their documented behaviour).
 * Node 0 is the root split; add splits and leaves under it; each leaf shows one child control.
 * <p>
 * The tree is int and double lanes (kind, parent, first child, next sibling, weight, bounds) plus a control lane;
 * there is no object per node. A split divides its length among its children by cumulative rounding of the prefix
 * sums of their weights, so they tile it exactly and moving one divider changes exactly two siblings: a divider drag
 * re-lays out only those two subtrees and repaints only the old and new divider strips; controls whose bounds did not
 * change are not touched. A resize re-lays out from the root but skips every subtree whose bounds stayed the same,
 * and repaints only the dividers that moved and the newly exposed strip.
 * </p>
 * <p>
 * While the pane is hidden (its own Hide, a zero size, a hidden ancestor) weight and structure changes only update
 * the lanes; on becoming visible it lays out once and paints once.
 * </p>
 * <p>
 * Styles: <code>HORIZONTAL</code> (default, the root is a row) or <code>VERTICAL</code> (the root is a column),
 * <code>BORDER</code>. Events: <code>Selection</code> after a divider moved (<code>Event.index</code> the node after
 * the divider). Startup switch: <code>-Dswt.widget.mode.M3MultiSplitPane=original</code> lays out every node and
 * repaints the whole pane on every change. RWT: dragging needs MouseMove, which an RWT server does not receive;
 * {@link #moveDivider(int, int)} works everywhere.
 * </p>
 */
public class M3MultiSplitPane extends Canvas {

	/** Node kind: children side by side, left to right. */
	public static final int ROW = 0;
	/** Node kind: children stacked, top to bottom. */
	public static final int COLUMN = 1;
	/** Node kind: a leaf showing one control. */
	public static final int LEAF = 2;

	static final boolean VIEWPORT = WidgetMode.of("M3MultiSplitPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3MultiSplitPane"); //$NON-NLS-1$

	private int count;
	private int[] kind = new int[8], parent = new int[8], first = new int[8], last = new int[8], next = new int[8];
	private int[] bx = new int[8], by = new int[8], bw = new int[8], bh = new int[8];
	private double[] weight = new double[8];
	private Control[] controls = new Control[8];
	private int divider = 6;
	private final M3HiddenGate gate;
	/* drag state: the node after the dragged divider and the last pointer position along the axis */
	private int dragNode = -1, dragAt;

	/**
	 * Creates a pane whose root split is a row (HORIZONTAL) or a column (VERTICAL).
	 *
	 * @param parent the parent
	 * @param style HORIZONTAL or VERTICAL, BORDER
	 */
	public M3MultiSplitPane(Composite parent, int style) {
		super(parent, (style & ~(SWT.HORIZONTAL | SWT.VERTICAL)) | SWT.DOUBLE_BUFFERED | (VIEWPORT ? SWT.NO_REDRAW_RESIZE : 0));
		gate = new M3HiddenGate(this, this::layoutAll);
		add(-1, (style & SWT.VERTICAL) != 0 ? COLUMN : ROW, 1, null);
		addListener(SWT.Paint, this::paint);
		addListener(SWT.Resize, e -> resized());
		addListener(SWT.MouseDown, this::mouse);
		addListener(SWT.MouseMove, this::mouse);
		addListener(SWT.MouseUp, this::mouse);
	}

	/** The pane places its children itself; a layout cannot be set. */
	@Override
	public void setLayout(Layout layout) {
		checkWidget();
	}

	/** @return the root node, 0 */
	public int getRoot() {
		checkWidget();
		return 0;
	}

	/**
	 * Appends a split under a split.
	 *
	 * @param parent a ROW or COLUMN node
	 * @param kind ROW or COLUMN
	 * @param weight the share of the parent's length, greater than 0
	 * @return the new node
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_RANGE - if the parent is not a node</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if the parent is a leaf, the kind is not ROW or COLUMN, or the weight
	 *                is not positive</li>
	 *                </ul>
	 */
	public int addSplit(int parent, int kind, double weight) {
		checkSplit(parent);
		if (kind != ROW && kind != COLUMN) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		return added(add(parent, kind, checkWeight(weight), null));
	}

	/**
	 * Appends a leaf under a split.
	 *
	 * @param parent a ROW or COLUMN node
	 * @param weight the share of the parent's length, greater than 0
	 * @param control a child of this pane, or null
	 * @return the new node
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE, ERROR_INVALID_ARGUMENT as for addSplit;
	 *                ERROR_INVALID_PARENT - if the control is not a child of this pane
	 */
	public int addLeaf(int parent, double weight, Control control) {
		checkSplit(parent);
		checkControl(control);
		return added(add(parent, LEAF, checkWeight(weight), control));
	}

	/**
	 * Shows a control in a leaf (the previous control is left where it is).
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if not a node; ERROR_INVALID_ARGUMENT - if not a
	 *                leaf; ERROR_INVALID_PARENT - if the control is not a child of this pane
	 */
	public void setControl(int leaf, Control control) {
		checkNode(leaf);
		if (kind[leaf] != LEAF) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		checkControl(control);
		controls[leaf] = control;
		if (control != null && showing()) {
			control.setBounds(bx[leaf], by[leaf], bw[leaf], bh[leaf]);
			M3Counters.moves++;
		}
	}

	/** @return the control of a leaf, or null */
	public Control getControl(int leaf) {
		checkNode(leaf);
		return controls[leaf];
	}

	/**
	 * Sets a node's weight; only the node's parent split is laid out again.
	 *
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if not a node; ERROR_INVALID_ARGUMENT - if not positive
	 */
	public void setWeight(int node, double weight) {
		checkNode(node);
		this.weight[node] = checkWeight(weight);
		if (node != 0 && showing()) {
			relayout(parent[node]);
		}
	}

	/** @return a node's weight */
	public double getWeight(int node) {
		checkNode(node);
		return weight[node];
	}

	/** @return ROW, COLUMN or LEAF */
	public int getKind(int node) {
		checkNode(node);
		return kind[node];
	}

	/** @return a node's parent, -1 for the root */
	public int getParent(int node) {
		checkNode(node);
		return parent[node];
	}

	/** @return the number of nodes, root included */
	public int getNodeCount() {
		checkWidget();
		return count;
	}

	/** @return a node's bounds in client coordinates */
	public Rectangle getNodeBounds(int node) {
		checkNode(node);
		return new Rectangle(bx[node], by[node], bw[node], bh[node]);
	}

	/** @return the divider before a node (between it and its previous sibling); empty for a first child or the root */
	public Rectangle getDividerBounds(int node) {
		checkNode(node);
		return divider(node);
	}

	/**
	 * Sets the divider thickness and lays out again.
	 *
	 * @param pixels at least 1
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if less than 1
	 */
	public void setDividerSize(int pixels) {
		checkWidget();
		if (pixels < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		divider = pixels;
		if (showing()) {
			layoutAll();
			fullRepaint();
		}
	}

	/** @return the divider thickness */
	public int getDividerSize() {
		checkWidget();
		return divider;
	}

	/**
	 * Moves the divider before a node by a number of pixels along its parent's axis (clamped so neither sibling gets a
	 * negative size). Only the node and its previous sibling change.
	 *
	 * @param node a node with a previous sibling
	 * @param delta pixels, positive to the right or down
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if not a node; ERROR_INVALID_ARGUMENT - if the node is
	 *                the root or a first child
	 */
	public void moveDivider(int node, int delta) {
		checkNode(node);
		int before = previous(node);
		if (node == 0 || before < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		boolean row = kind[parent[node]] == ROW;
		int a = row ? bw[before] : bh[before], b = row ? bw[node] : bh[node];
		int moved = Math.max(-a, Math.min(b, delta));
		double sum = weight[before] + weight[node];
		if (a + b > 0) {
			weight[before] = Math.max(1e-9, sum * (a + moved) / (a + b));
			weight[node] = Math.max(1e-9, sum - weight[before]);
		}
		if (showing()) {
			relayout(parent[node]);
		}
		Event event = new Event();
		event.index = node;
		notifyListeners(SWT.Selection, event);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		Point size = preferred(0);
		Rectangle trim = computeTrim(0, 0, wHint == SWT.DEFAULT ? size.x : wHint, hHint == SWT.DEFAULT ? size.y : hHint);
		return new Point(trim.width, trim.height);
	}

	private Point preferred(int node) {
		if (kind[node] == LEAF) {
			M3Counters.measures++;
			return controls[node] == null ? new Point(0, 0) : controls[node].computeSize(SWT.DEFAULT, SWT.DEFAULT);
		}
		int along = 0, across = 0, children = 0;
		for (int c = first[node]; c >= 0; c = next[c]) {
			Point size = preferred(c);
			along += kind[node] == ROW ? size.x : size.y;
			across = Math.max(across, kind[node] == ROW ? size.y : size.x);
			children++;
		}
		along += Math.max(0, children - 1) * divider;
		return kind[node] == ROW ? new Point(along, across) : new Point(across, along);
	}

	private void checkNode(int node) {
		checkWidget();
		if (node < 0 || node >= count) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
	}

	private void checkSplit(int node) {
		checkNode(node);
		if (kind[node] == LEAF) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
	}

	private void checkControl(Control control) {
		if (control != null && control.getParent() != this) {
			SWT.error(SWT.ERROR_INVALID_PARENT);
		}
	}

	private static double checkWeight(double weight) {
		if (!(weight > 0) || Double.isInfinite(weight)) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		return weight;
	}

	private int add(int parentNode, int nodeKind, double nodeWeight, Control control) {
		if (count == kind.length) {
			int n = count * 2;
			kind = Arrays.copyOf(kind, n);
			parent = Arrays.copyOf(parent, n);
			first = Arrays.copyOf(first, n);
			last = Arrays.copyOf(last, n);
			next = Arrays.copyOf(next, n);
			bx = Arrays.copyOf(bx, n);
			by = Arrays.copyOf(by, n);
			bw = Arrays.copyOf(bw, n);
			bh = Arrays.copyOf(bh, n);
			weight = Arrays.copyOf(weight, n);
			controls = Arrays.copyOf(controls, n);
		}
		int node = count++;
		kind[node] = nodeKind;
		parent[node] = parentNode;
		first[node] = last[node] = next[node] = -1;
		bx[node] = by[node] = bw[node] = bh[node] = 0;
		weight[node] = nodeWeight;
		controls[node] = control;
		if (parentNode >= 0) {
			if (last[parentNode] < 0) {
				first[parentNode] = node;
			} else {
				next[last[parentNode]] = node;
			}
			last[parentNode] = node;
		}
		return node;
	}

	private int added(int node) {
		if (showing()) {
			relayout(parent[node]);
		}
		return node;
	}

	private int previous(int node) {
		if (node == 0) {
			return -1;
		}
		int before = -1;
		for (int c = first[parent[node]]; c >= 0 && c != node; c = next[c]) {
			before = c;
		}
		return before;
	}

	private Rectangle divider(int node) {
		int before = previous(node);
		if (before < 0) {
			return new Rectangle(0, 0, 0, 0);
		}
		return kind[parent[node]] == ROW ? new Rectangle(bx[before] + bw[before], by[node], divider, bh[node])
				: new Rectangle(bx[node], by[before] + bh[before], bw[node], divider);
	}

	/* a hidden pane does no native work: changes fold into the gate and the pane lays out once on reveal */
	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	private void fullRepaint() {
		M3Counters.fullRepaints++;
		redraw();
	}

	private void resized() {
		if (VIEWPORT && !gate.visible()) {
			return;
		}
		Rectangle client = getClientArea();
		place(0, client.x, client.y, client.width, client.height, !VIEWPORT, !VIEWPORT);
		if (!VIEWPORT) {
			fullRepaint();
		}
	}

	/* the one window patch on reveal, and a full layout after a divider size change */
	private void layoutAll() {
		M3Counters.fullLayouts++;
		Rectangle client = getClientArea();
		place(0, client.x, client.y, client.width, client.height, true, true);
	}

	/* lays out one split's children inside its current bounds (a weight or divider delta) */
	private void relayout(int split) {
		if (!VIEWPORT) {
			layoutAll();
			fullRepaint();
			return;
		}
		M3Counters.layouts++;
		place(split, bx[split], by[split], bw[split], bh[split], true, false);
	}

	/*
	 * Places a node and, when its bounds changed (or force), its children: the split's length minus the dividers is
	 * divided by cumulative rounding, child i ending at length * (w0 + .. + wi) / W, so the children tile the split
	 * and a change of two adjacent weights with the same sum moves only those two. Unchanged subtrees are skipped. A
	 * divider whose position changed is damaged at its old and new place.
	 */
	private void place(int node, int x, int y, int width, int height, boolean force, boolean all) {
		boolean changed = x != bx[node] || y != by[node] || width != bw[node] || height != bh[node];
		if (!changed && !force && !all) {
			return;
		}
		bx[node] = x;
		by[node] = y;
		bw[node] = width;
		bh[node] = height;
		if (kind[node] == LEAF) {
			if (controls[node] != null && !controls[node].isDisposed() && (changed || all)) {
				controls[node].setBounds(x, y, width, height);
				M3Counters.moves++;
			}
			return;
		}
		boolean row = kind[node] == ROW;
		int children = 0;
		double total = 0;
		for (int c = first[node]; c >= 0; c = next[c]) {
			children++;
			total += weight[c];
		}
		int length = Math.max(0, (row ? width : height) - Math.max(0, children - 1) * divider);
		double seen = 0;
		int start = row ? x : y, given = 0;
		for (int c = first[node]; c >= 0; c = next[c]) {
			seen += weight[c];
			int end = total > 0 ? (int) Math.round(length * seen / total) : 0;
			int size = end - given;
			int oldStart = row ? bx[c] : by[c];
			if (c != first[node] && oldStart != start && VIEWPORT) {
				damageDivider(row, oldStart - divider, node);
				damageDivider(row, start - divider, node);
			}
			if (row) {
				place(c, start, y, size, height, false, all);
			} else {
				place(c, x, start, width, size, false, all);
			}
			given = end;
			start += size + divider;
		}
	}

	private void damageDivider(boolean row, int at, int split) {
		if (row) {
			redraw(at, by[split], divider, bh[split], false);
		} else {
			redraw(bx[split], at, bw[split], divider, false);
		}
	}

	private int dividerAt(int px, int py) {
		for (int node = 1; node < count; node++) {
			Rectangle rect = divider(node);
			if (rect.width > 0 && rect.contains(px, py)) {
				return node;
			}
		}
		return -1;
	}

	private void mouse(Event event) {
		switch (event.type) {
		case SWT.MouseDown:
			if (event.button == 1) {
				dragNode = dividerAt(event.x, event.y);
				if (dragNode >= 0) {
					dragAt = kind[parent[dragNode]] == ROW ? event.x : event.y;
				}
			}
			break;
		case SWT.MouseMove:
			if (dragNode >= 0 && (event.stateMask & SWT.BUTTON1) != 0) {
				int at = kind[parent[dragNode]] == ROW ? event.x : event.y;
				if (at != dragAt) {
					moveDivider(dragNode, at - dragAt);
					dragAt = at;
				}
			} else {
				int node = dividerAt(event.x, event.y);
				setCursor(node < 0 ? null
						: getDisplay().getSystemCursor(kind[parent[node]] == ROW ? SWT.CURSOR_SIZEWE : SWT.CURSOR_SIZENS));
			}
			break;
		case SWT.MouseUp:
			if (dragNode >= 0) {
				LOG.debug(() -> "divider before node " + dragNode + " released"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			dragNode = -1;
			break;
		default:
			break;
		}
	}

	private void paint(Event event) {
		GC gc = event.gc;
		gc.setBackground(getBackground());
		gc.fillRectangle(event.x, event.y, event.width, event.height);
		gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		Rectangle clip = new Rectangle(event.x, event.y, event.width, event.height);
		for (int node = 1; node < count; node++) {
			Rectangle rect = divider(node);
			if (rect.width > 0 && rect.intersects(clip)) {
				int cx = rect.x + rect.width / 2, cy = rect.y + rect.height / 2;
				for (int k = -1; k <= 1; k++) {
					if (rect.width < rect.height) {
						gc.fillRectangle(cx - 1, cy + 4 * k - 1, 2, 2);
					} else {
						gc.fillRectangle(cx + 4 * k - 1, cy - 1, 2, 2);
					}
				}
			}
		}
	}
}
