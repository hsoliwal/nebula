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

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The info popup plane: hover cards and click popovers for the header, body (cell or row) and footer of a list, table,
 * tree or grid, anchored to the bounds of what they describe (brief behaviours catalogue, "Info popups on header, body
 * and footer"). One popup per owner control, one floating shell reused for every anchor.
 * <p>
 * The owner says what is under a point and where it is through {@link Anchors} (packed long ids from
 * {@link M3Placement#anchor(int, int, int)}: kind {@link #HEADER}, {@link #BODY} or {@link #FOOTER}, row, column);
 * {@link #forTable(Table, Content)} and {@link #forTree(Tree, Content)} provide them for SWT tables and trees, and the
 * Table, Tree and Grid tracks provide them for their footers. The popup's content is built by {@link Content} only
 * when the popup opens, from the model: nothing exists per cell, row or column. Placement is
 * {@link M3Placement#place} (preferred side, flip, shift inside the monitor).
 * </p>
 * <p>
 * Behaviour: a hover card opens after the show delay when the pointer rests on an anchor and closes after the hide
 * delay when it leaves both the anchor and the card (the pointer may move into the card); a click popover opens on a
 * click and stays until Escape, a click elsewhere in the owner, the owner shell's deactivation or the anchor leaving the
 * view. Keyboard: {@link #showFocused()} (bound by the owner, Ctrl+I by default in the factories) opens the popover for
 * the focused anchor and moves the focus into it; Escape closes it and returns the focus. While the popup is closed its
 * shell is hidden, so viewports inside it fold their deltas.
 * </p>
 * <p>
 * Native headers: a stock Table or Tree header is a separate native window on some platforms and does not deliver
 * mouse moves to the table, so header hover cards need a drawn header (M3 and viewport Table/Tree tracks) or
 * {@link #show(long)} from the column's own events; body and footer anchors work everywhere.
 * </p>
 */
public final class M3InfoPopup {

	/** Anchor kind: a column header. */
	public static final int HEADER = 1;
	/** Anchor kind: a cell or row of the body. */
	public static final int BODY = 2;
	/** Anchor kind: a footer cell. */
	public static final int FOOTER = 3;
	/** No anchor. */
	public static final long NONE = -1L;

	/** Trigger: open on hover after the show delay. */
	public static final int HOVER = 1;
	/** Trigger: open on a click. */
	public static final int CLICK = 2;

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3InfoPopup"); //$NON-NLS-1$

	/** What is under a point of the owner, and where an anchor is. Implemented by the owner widget or its track. */
	public interface Anchors {

		/**
		 * @param x owner client x
		 * @param y owner client y
		 * @return the packed anchor under the point, or {@link M3InfoPopup#NONE}
		 */
		long anchorAt(int x, int y);

		/**
		 * @param anchor a packed anchor
		 * @return its bounds in owner client coordinates, or null when it is not shown (scrolled out, collapsed)
		 */
		Rectangle bounds(long anchor);

		/** @return the anchor that has the keyboard focus, or {@link M3InfoPopup#NONE} */
		default long focused() {
			return NONE;
		}
	}

	/** Builds the popup's content for an anchor, from the model, when the popup opens. */
	@FunctionalInterface
	public interface Content {

		/**
		 * Fills the popup body (a Composite with a FillLayout by default; set another layout as needed).
		 *
		 * @param parent the popup body, emptied before each call
		 * @param anchor the packed anchor
		 * @param trigger HOVER or CLICK
		 * @return false to show nothing for this anchor
		 */
		boolean fill(Composite parent, long anchor, int trigger);
	}

	private final Control owner;
	private final Anchors anchors;
	private final Content content;
	private int triggers = HOVER | CLICK, showDelay = 600, hideDelay = 300, side = M3Placement.BOTTOM, gap = 4;
	private Shell shell;
	private Composite body;
	private long anchor = NONE, pending = NONE;
	private int openedBy;
	private final int[] placed = new int[3];
	private final Runnable showPending = this::showPending, hidePending = this::hideIfAway;
	private final Listener ownerListener = this::ownerEvent, filter = this::displayFilter;

	/**
	 * Attaches a popup plane to an owner.
	 *
	 * @param owner the control whose header, body or footer is described
	 * @param anchors what is under a point and where it is
	 * @param content builds the popup's content
	 */
	public M3InfoPopup(Control owner, Anchors anchors, Content content) {
		if (owner == null || anchors == null || content == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.owner = owner;
		this.anchors = anchors;
		this.content = content;
		for (int type : new int[] { SWT.MouseMove, SWT.MouseExit, SWT.MouseDown, SWT.KeyDown, SWT.Dispose }) {
			owner.addListener(type, ownerListener);
		}
		owner.getShell().addListener(SWT.Deactivate, ownerListener);
	}

	/** A popup plane for a table: header and cell anchors, Ctrl+I opens the popover of the selected row's cell. */
	public static M3InfoPopup forTable(Table table, Content content) {
		M3InfoPopup popup = new M3InfoPopup(table, new TableAnchors(table), content);
		return popup;
	}

	/** A popup plane for a tree: header and cell anchors (row = index among the visible items from the top item). */
	public static M3InfoPopup forTree(Tree tree, Content content) {
		return new M3InfoPopup(tree, new TreeAnchors(tree), content);
	}

	/** Sets which triggers open the popup: HOVER, CLICK or both. */
	public void setTriggers(int triggers) {
		this.triggers = triggers;
	}

	/**
	 * Sets the hover delays.
	 *
	 * @param show milliseconds the pointer must rest on an anchor, at least 0
	 * @param hide milliseconds after the pointer left anchor and card, at least 0
	 */
	public void setDelays(int show, int hide) {
		if (show < 0 || hide < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		showDelay = show;
		hideDelay = hide;
	}

	/** Sets the preferred side, one of the M3Placement sides, and the gap to the anchor. */
	public void setPlacement(int side, int gap) {
		if (side < M3Placement.BOTTOM || side > M3Placement.LEFT || gap < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.side = side;
		this.gap = gap;
	}

	/** @return the anchor shown, or NONE */
	public long getAnchor() {
		return isVisible() ? anchor : NONE;
	}

	/** @return true while the popup is open */
	public boolean isVisible() {
		return shell != null && !shell.isDisposed() && shell.isVisible();
	}

	/** @return the popup shell (created on first open), or null */
	public Shell getShell() {
		return shell;
	}

	/**
	 * Opens the popup for an anchor as a click popover.
	 *
	 * @param anchorId the packed anchor
	 * @return true when it opened (the anchor is shown and has content)
	 */
	public boolean show(long anchorId) {
		return open(anchorId, CLICK);
	}

	/** Opens the popover for the focused anchor and moves the focus into it. */
	public boolean showFocused() {
		long focused = anchors.focused();
		if (focused == NONE || !open(focused, CLICK)) {
			return false;
		}
		shell.setFocus();
		return true;
	}

	/** Closes the popup. */
	public void hide() {
		owner.getDisplay().timerExec(-1, showPending);
		owner.getDisplay().timerExec(-1, hidePending);
		pending = NONE;
		if (isVisible()) {
			boolean refocus = openedBy == CLICK && shell.getDisplay().getFocusControl() != null
					&& isInside(shell.getDisplay().getFocusControl());
			shell.setVisible(false);
			owner.getDisplay().removeFilter(SWT.MouseDown, filter);
			anchor = NONE;
			if (refocus && !owner.isDisposed()) {
				owner.setFocus();
			}
		}
	}

	/** Moves the open popup to its anchor's current bounds (after a scroll or resize), or closes it when not shown. */
	public void reposition() {
		if (!isVisible()) {
			return;
		}
		Rectangle bounds = anchors.bounds(anchor);
		if (bounds == null) {
			hide();
			return;
		}
		locate(bounds);
	}

	private boolean open(long anchorId, int trigger) {
		if (anchorId == NONE || owner.isDisposed()) {
			return false;
		}
		Rectangle bounds = anchors.bounds(anchorId);
		if (bounds == null) {
			return false;
		}
		ensureShell(trigger);
		for (Control child : body.getChildren()) {
			child.dispose();
		}
		body.setLayout(new FillLayout());
		if (!content.fill(body, anchorId, trigger)) {
			hide();
			return false;
		}
		M3Counters.creates++;
		anchor = anchorId;
		openedBy = trigger;
		shell.pack();
		locate(bounds);
		if (!shell.isVisible()) {
			shell.setVisible(true);
			owner.getDisplay().addFilter(SWT.MouseDown, filter);
		}
		LOG.debug(() -> "info popup " + M3Placement.kindOf(anchorId) + " row " + M3Placement.rowOf(anchorId) + " column " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ M3Placement.columnOf(anchorId) + (trigger == HOVER ? " (hover)" : " (click)")); //$NON-NLS-1$ //$NON-NLS-2$
		return true;
	}

	private void ensureShell(int trigger) {
		if (shell != null && !shell.isDisposed()) {
			return;
		}
		shell = new Shell(owner.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.BORDER);
		shell.setLayout(new FillLayout());
		body = new Composite(shell, SWT.NONE);
		shell.addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				e.doit = false;
				hide();
			}
		});
		shell.addListener(SWT.MouseExit, e -> scheduleHide());
		shell.addListener(SWT.MouseEnter, e -> owner.getDisplay().timerExec(-1, hidePending));
	}

	private void locate(Rectangle bounds) {
		Point topLeft = owner.toDisplay(bounds.x, bounds.y);
		Point size = shell.getSize();
		Rectangle area = owner.getMonitor().getClientArea();
		M3Placement.place(topLeft.x, topLeft.y, bounds.width, bounds.height, size.x, size.y, area.x, area.y, area.width, area.height, side, gap,
				placed);
		shell.setLocation(placed[0], placed[1]);
	}

	private boolean isInside(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			if (c == shell) {
				return true;
			}
		}
		return false;
	}

	private void ownerEvent(Event event) {
		switch (event.type) {
		case SWT.MouseMove:
			if ((triggers & HOVER) == 0 || isVisible() && openedBy == CLICK) {
				return;
			}
			long under = anchors.anchorAt(event.x, event.y);
			if (isVisible() && under == anchor) {
				owner.getDisplay().timerExec(-1, hidePending);
				return;
			}
			if (under != pending) {
				pending = under;
				owner.getDisplay().timerExec(under == NONE ? -1 : showDelay, showPending);
			}
			if (isVisible()) {
				scheduleHide();
			}
			break;
		case SWT.MouseExit:
			pending = NONE;
			owner.getDisplay().timerExec(-1, showPending);
			if (isVisible() && openedBy == HOVER) {
				scheduleHide();
			}
			break;
		case SWT.MouseDown:
			if (event.button != 1) {
				return;
			}
			long clicked = anchors.anchorAt(event.x, event.y);
			if ((triggers & CLICK) != 0 && clicked != NONE && (clicked != anchor || !isVisible())) {
				open(clicked, CLICK);
			} else if (isVisible() && clicked != anchor) {
				hide();
			}
			break;
		case SWT.KeyDown:
			if (event.keyCode == SWT.ESC && isVisible()) {
				hide();
			} else if ((event.stateMask & SWT.MOD1) != 0 && (event.keyCode == 'i' || event.keyCode == 'I')) {
				showFocused();
			}
			break;
		case SWT.Deactivate:
			if (isVisible() && openedBy == HOVER) {
				hide();
			}
			break;
		case SWT.Dispose:
			hide();
			owner.getShell().removeListener(SWT.Deactivate, ownerListener);
			if (shell != null && !shell.isDisposed()) {
				shell.dispose();
			}
			break;
		default:
			break;
		}
	}

	private static int scrolled(org.eclipse.swt.widgets.ScrollBar bar) {
		return bar == null ? 0 : -bar.getSelection();
	}

	/* a mouse press outside owner and popup closes a click popover */
	private void displayFilter(Event event) {
		if (!isVisible() || !(event.widget instanceof Control)) {
			return;
		}
		Control target = (Control) event.widget;
		if (!isInside(target) && target != owner) {
			hide();
		}
	}

	private void showPending() {
		long target = pending;
		if (target != NONE && !owner.isDisposed()) {
			open(target, HOVER);
		}
	}

	private void scheduleHide() {
		if (openedBy == HOVER) {
			owner.getDisplay().timerExec(hideDelay, hidePending);
		}
	}

	private void hideIfAway() {
		if (!isVisible() || openedBy != HOVER) {
			return;
		}
		Display display = owner.getDisplay();
		Point pointer = display.getCursorLocation();
		if (shell.getBounds().contains(pointer)) {
			return;
		}
		Rectangle bounds = anchors.bounds(anchor);
		if (bounds != null) {
			Point at = owner.toControl(pointer);
			if (bounds.contains(at)) {
				return;
			}
		}
		hide();
	}

	/* header cells by column bounds, body cells by item hit test; row = item index */
	private static final class TableAnchors implements Anchors {

		private final Table table;

		TableAnchors(Table table) {
			this.table = table;
		}

		@Override
		public long anchorAt(int x, int y) {
			int header = table.getHeaderVisible() ? table.getHeaderHeight() : 0;
			int column = columnAt(x);
			if (y < header) {
				return column < 0 ? NONE : M3Placement.anchor(HEADER, -1, column);
			}
			TableItem item = table.getItem(new Point(x, y));
			if (item == null) {
				return NONE;
			}
			return M3Placement.anchor(BODY, table.indexOf(item), Math.max(0, column));
		}

		private int columnAt(int x) {
			int count = table.getColumnCount();
			if (count == 0) {
				return 0;
			}
			int[] order = table.getColumnOrder();
			int left = scrolled(table.getHorizontalBar());
			for (int index : order) {
				TableColumn column = table.getColumn(index);
				int width = column.getWidth();
				if (x >= left && x < left + width) {
					return index;
				}
				left += width;
			}
			return -1;
		}

		@Override
		public Rectangle bounds(long id) {
			int column = M3Placement.columnOf(id), row = M3Placement.rowOf(id);
			if (M3Placement.kindOf(id) == HEADER) {
				int left = scrolled(table.getHorizontalBar());
				for (int index : table.getColumnOrder()) {
					int width = table.getColumn(index).getWidth();
					if (index == column) {
						return new Rectangle(left, 0, width, table.getHeaderHeight());
					}
					left += width;
				}
				return null;
			}
			if (row < 0 || row >= table.getItemCount()) {
				return null;
			}
			Rectangle cell = table.getColumnCount() == 0 ? table.getItem(row).getBounds() : table.getItem(row).getBounds(column);
			Rectangle client = table.getClientArea();
			return cell.intersects(client) ? cell : null;
		}

		@Override
		public long focused() {
			int index = table.getSelectionIndex();
			return index < 0 ? NONE : M3Placement.anchor(BODY, index, 0);
		}
	}

	/* header cells by column bounds, body cells by item hit test; row = visible-item index from the top item */
	private static final class TreeAnchors implements Anchors {

		private final Tree tree;

		TreeAnchors(Tree tree) {
			this.tree = tree;
		}

		@Override
		public long anchorAt(int x, int y) {
			int header = tree.getHeaderVisible() ? tree.getHeaderHeight() : 0;
			int column = columnAt(x);
			if (y < header) {
				return column < 0 ? NONE : M3Placement.anchor(HEADER, -1, column);
			}
			TreeItem item = tree.getItem(new Point(x, y));
			if (item == null) {
				return NONE;
			}
			int row = (y - header) / Math.max(1, tree.getItemHeight());
			return M3Placement.anchor(BODY, row, Math.max(0, column));
		}

		private int columnAt(int x) {
			int count = tree.getColumnCount();
			if (count == 0) {
				return 0;
			}
			int left = scrolled(tree.getHorizontalBar());
			for (int index : tree.getColumnOrder()) {
				TreeColumn column = tree.getColumn(index);
				if (x >= left && x < left + column.getWidth()) {
					return index;
				}
				left += column.getWidth();
			}
			return -1;
		}

		@Override
		public Rectangle bounds(long id) {
			int column = M3Placement.columnOf(id);
			int header = tree.getHeaderVisible() ? tree.getHeaderHeight() : 0;
			if (M3Placement.kindOf(id) == HEADER) {
				int left = scrolled(tree.getHorizontalBar());
				for (int index : tree.getColumnOrder()) {
					int width = tree.getColumn(index).getWidth();
					if (index == column) {
						return new Rectangle(left, 0, width, header);
					}
					left += width;
				}
				return null;
			}
			int y = header + M3Placement.rowOf(id) * tree.getItemHeight() + 1;
			TreeItem item = tree.getItem(new Point(Math.max(1, tree.getClientArea().width / 2), y));
			if (item == null) {
				return null;
			}
			return tree.getColumnCount() == 0 ? item.getBounds() : item.getBounds(column);
		}
	}
}
