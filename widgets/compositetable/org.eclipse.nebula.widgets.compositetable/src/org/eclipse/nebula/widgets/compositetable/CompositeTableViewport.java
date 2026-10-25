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
package org.eclipse.nebula.widgets.compositetable;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Viewport of a {@link CompositeTable}, aspect H (hidden gating, docs/m3/every-widget): one hidden gate for the content
 * pane ({@link InternalCompositeTable}). While the table or an ancestor is hidden (its own or an ancestor's visibility,
 * a zero size, a minimized shell, or SWT.PartHidden on the M3 SWT fork) the content pane does no row, layout or key
 * work:
 * <ul>
 * <li>row updates and refreshes of all rows fold into the content pane's dirty flags, and {@code refreshRow} records
 * the collection position in a small int lane (stock calls the content providers at once,
 * InternalCompositeTable.java:786-792);</li>
 * <li>the scroller resize handler (232-260: computeSize, setBounds, a horizontal slider Selection), the content pane
 * relayout (449-457: layout(true) of the header and of every row) and the empty placeholder bounds
 * (EmptyTablePlaceholder.java:100-118) stay pending;</li>
 * <li>{@link CompositeTable#getNumRowsVisible()} answers the count a catch-up would leave, without building rows, so
 * {@link CompositeTable#setTopRow(int)} (CompositeTable.java:580-581) no longer runs the deferred row update;</li>
 * <li>the Display-wide SWT.KeyDown filter (350-353, body 1182-1188) is installed only while the vertical slider is
 * visible and the table is shown: parked on Hide, on SWT.PartHidden or on the first key press that finds the table
 * hidden, installed again by the reveal, and removed on dispose (stock keeps it while hidden and after dispose,
 * 418-422).</li>
 * </ul>
 * The reveal (Show on the official SWT, SWT.PartVisible on the M3 fork, or the first Paint of the content area) catches
 * up once and synchronously, before the rows paint: the scroller, the placeholder, the header once, one row update in
 * which every row is placed and laid out once, then the recorded refreshes still in view. A public row read while
 * hidden (getRowControls, getSelection, ...) catches up the same way, and so does a row focus event that reaches a
 * listener, because the listener gets the row control.
 * <p>
 * {@code -Dswt.widget.mode.CompositeTable=original} (or the global {@code swt.widget.mode=original}) keeps the stock
 * behaviour: no viewport is created.
 * </p>
 */
final class CompositeTableViewport {

	/** True unless the startup mode of CompositeTable is original (the content pane's own switch). */
	static final boolean ENABLED = InternalCompositeTable.VIEWPORT;

	/* SWT.PartVisible, SWT.PartHidden and Control.isPartVisible of the M3 SWT fork (brief rule 25); -1 and absent on stock SWT */
	private static final int PART_VISIBLE = partEvent("PartVisible"); //$NON-NLS-1$
	private static final int PART_HIDDEN = partEvent("PartHidden"); //$NON-NLS-1$
	private static final Method IS_PART_VISIBLE = partVisibleMethod();
	private static final Object[] NO_ARGS = {};

	/** Collection positions of refreshRow calls made while hidden; one more folds into a refresh of all rows. */
	private static final int REFRESH_SLOTS = 32;

	private final InternalCompositeTable table;
	private final CompositeTable parent;
	private final Composite hScroller;
	private final Composite controlHolder;
	private final Listener stockKeyFilter;
	private final Display display;
	private final Listener listener = this::handleEvent;
	private final Listener keyFilter = this::keyDown;
	private final int[] refreshPositions = new int[REFRESH_SLOTS];
	private int refreshCount;
	private boolean refreshAllPending;
	private boolean scrollerPending;
	private boolean placeholderPending;
	private boolean relayoutPending;
	private boolean keyWanted;
	private boolean keyInstalled;
	private boolean sized;
	private boolean catchingUp;
	private boolean revealing;

	/** Row updates, refreshes, layouts and bounds folded while hidden (tests and benches). */
	int deferred;
	/** Catch-ups that ran the folded work (tests and benches). */
	int catchUps;
	/** Reveals that found folded work or a parked key filter (tests and benches). */
	int reveals;
	/** Key presses handed to the stock filter while shown (tests and benches). */
	int keyFilterRuns;
	/** Times the key filter was taken off the Display because the table was hidden (tests and benches). */
	int keyFilterParks;

	CompositeTableViewport(InternalCompositeTable table, CompositeTable parent, Composite hScroller,
			Composite controlHolder, Listener stockKeyFilter) {
		this.table = table;
		this.parent = parent;
		this.hScroller = hScroller;
		this.controlHolder = controlHolder;
		this.stockKeyFilter = stockKeyFilter;
		this.display = table.getDisplay();
		listen();
	}

	private void listen() {
		table.addListener(SWT.Resize, listener);
		table.addListener(SWT.Hide, listener);
		parent.addListener(SWT.Hide, listener);
		// the first paint of the content area after an ancestor is shown (stock SWT sends the children no Show)
		table.addListener(SWT.Paint, listener);
		hScroller.addListener(SWT.Paint, listener);
		controlHolder.addListener(SWT.Paint, listener);
		if (PART_VISIBLE >= 0) {
			// the M3 fork reports PartVisible after its own catch-up of the layouts it deferred while hidden
			table.addListener(PART_VISIBLE, listener);
			table.addListener(PART_HIDDEN, listener);
		} else {
			table.addListener(SWT.Show, listener);
			parent.addListener(SWT.Show, listener);
		}
		table.addListener(SWT.Dispose, listener);
	}

	private void handleEvent(Event e) {
		if (e.type == SWT.Paint || e.type == PART_VISIBLE) {
			resume();
		} else if (e.type == SWT.Resize) {
			Point size = table.getSize();
			sized = size.x > 0 && size.y > 0;
		} else if (e.type == SWT.Show) {
			showing((Control) e.widget);
		} else if (e.type == SWT.Hide || e.type == PART_HIDDEN) {
			parkKeyFilter();
		} else if (e.type == SWT.Dispose) {
			disposed();
		}
	}

	/**
	 * @return true while the table or an ancestor is hidden: not visible, a zero size or a minimized shell, or not
	 *         part-visible on the M3 SWT fork (which covers all three)
	 */
	boolean hidden() {
		if (revealing) {
			return false;
		}
		if (table.isDisposed()) {
			return true;
		}
		if (IS_PART_VISIBLE != null) {
			return !partVisible();
		}
		return !table.isVisible() || !sized || table.getShell().getMinimized();
	}

	private boolean partVisible() {
		try {
			return ((Boolean) IS_PART_VISIBLE.invoke(table, NO_ARGS)).booleanValue();
		} catch (ReflectiveOperationException | RuntimeException e) {
			return table.isVisible();
		}
	}

	/* the folded work runs now: shown, or a catch-up is running it */
	private boolean fold() {
		if (catchingUp || !hidden()) {
			return false;
		}
		deferred++;
		return true;
	}

	/** {@link InternalCompositeTable#updateVisibleRows()}: while hidden the row update folds into rowsDirty. */
	boolean deferRows() {
		return fold();
	}

	/** {@link InternalCompositeTable#refreshAllRows()}: while hidden one refresh of all rows waits, covering the lane. */
	boolean deferRefreshAll() {
		refreshCount = 0; // a refresh of all rows, now or at the catch-up, covers every recorded position
		refreshAllPending = fold();
		return refreshAllPending;
	}

	/** {@link InternalCompositeTable#refreshRow(int)}: while hidden the collection position waits for the catch-up. */
	boolean deferRefreshRow(int row, int topRow) {
		if (!fold()) {
			return false;
		}
		if (!refreshAllPending && topRow > -1 && row >= 0 && !recorded(topRow + row)) {
			record(topRow + row);
		}
		return true;
	}

	private boolean recorded(int position) {
		for (int i = 0; i < refreshCount; i++) {
			if (refreshPositions[i] == position) {
				return true;
			}
		}
		return false;
	}

	private void record(int position) {
		if (refreshCount == REFRESH_SLOTS) {
			table.refreshAllRows(); // hidden: folds into one refresh of all rows (deferRefreshAll empties the lane)
			return;
		}
		refreshPositions[refreshCount++] = position;
	}

	/**
	 * {@link InternalCompositeTable#layoutControlHolder()}: folded while hidden and during a catch-up, which lays out
	 * the header once and leaves each row to the row update that places it.
	 */
	boolean deferLayout() {
		relayoutPending = catchingUp || hidden();
		if (relayoutPending) {
			deferred++;
		}
		return relayoutPending;
	}

	/** The scroller resize handler of the content pane: while hidden the controlHolder bounds wait. */
	boolean deferScrollerResize() {
		scrollerPending = fold();
		return scrollerPending;
	}

	/** {@link EmptyTablePlaceholder} resize: while hidden its bounds wait. */
	boolean deferPlaceholderBounds() {
		placeholderPending = fold();
		return placeholderPending;
	}

	/**
	 * Before a row focus event: a listener gets the row control, so folded row work is caught up first when the event
	 * can reach one (a current row and at least one row focus listener).
	 */
	void beforeRowFocusEvent(int currentRow) {
		if (currentRow >= 0 && !catchingUp && rowsPending() && !parent.rowFocusListeners.isEmpty()) {
			table.flushRows();
		}
	}

	private boolean rowsPending() {
		return scrollerPending || relayoutPending || refreshCount > 0 || table.m3RowsDirty();
	}

	private boolean pending() {
		return rowsPending() || placeholderPending || keyWanted && !keyInstalled;
	}

	/**
	 * Runs the work folded while hidden, once: at the reveal, or at a public read that needs the rows
	 * ({@link InternalCompositeTable#flushRows()}). The stock code does the work, in its own order.
	 */
	void catchUp() {
		if (catchingUp || !rowsPending() && !placeholderPending) {
			return;
		}
		catchingUp = true;
		try {
			catchUps++;
			runScrollerResize();
			runPlaceholderBounds();
			runRelayout();
			table.flushRows(); // one row update (rows placed and laid out once) and one refresh of all rows
			refreshRecorded();
		} finally {
			catchingUp = false;
		}
	}

	private void runScrollerResize() {
		if (scrollerPending) {
			scrollerPending = false;
			table.scrollerResizeHandler.controlResized(null); // may resize controlHolder: its relayout folds (deferLayout)
		}
	}

	private void runPlaceholderBounds() {
		if (placeholderPending) {
			placeholderPending = false;
			EmptyTablePlaceholder placeholder = table.emptyTablePlaceholder;
			if (placeholder != null && !placeholder.isDisposed()) {
				placeholder.m3Bounds();
			}
		}
	}

	private void runRelayout() {
		if (relayoutPending) {
			relayoutPending = false;
			Control header = table.getHeader();
			if (header instanceof Composite && !header.isDisposed()) {
				((Composite) header).layout(true);
			}
		}
	}

	private void refreshRecorded() {
		int count = refreshCount;
		refreshCount = 0;
		int top = table.getTopRow();
		for (int i = 0; i < count; i++) {
			table.refreshRow(refreshPositions[i] - top); // stock refreshes the row only while it is in view
		}
	}

	/**
	 * The count of rows a catch-up would leave, by the arithmetic of the row update
	 * (InternalCompositeTable.java:573-647), without building rows.
	 */
	int plannedRows(int rowCount, boolean rowsDirty, Control rowControl, Control headerControl,
			int numRowsInCollection, int topRow, int maxRowsVisible, boolean placeholder) {
		if (!rowsDirty && !scrollerPending || rowControl == null) {
			return rowCount;
		}
		// controlHolder takes the scroller's height when the scroller resize runs (246, 1416-1419)
		int clientAreaHeight = (scrollerPending ? hScroller : controlHolder).getSize().y;
		int rowHeight = rowControl.getSize().y;
		if (clientAreaHeight <= 0) {
			return rowCount;
		}
		if (rowHeight <= 0) {
			return table.getNumRowsVisible(); // stock divides by the row height (592): its own path, its own exception
		}
		if (headerControl != null) {
			clientAreaHeight -= headerControl.getSize().y + 3;
		}
		if (numRowsInCollection < 1) {
			return placeholder ? rowCount : 0;
		}
		int rows = Math.min(Math.min(clientAreaHeight / rowHeight, numRowsInCollection - topRow), maxRowsVisible);
		return Math.max(rows, 1);
	}

	/** setVSliderVisible: the viewport owns the registration of the stock key filter (installed only while shown). */
	void keyFilterChanged(boolean visible) {
		display.removeFilter(SWT.KeyDown, stockKeyFilter); // the stock code just added it (350) or removed it (352)
		keyWanted = visible;
		if (visible && !hidden()) {
			installKeyFilter();
		} else {
			uninstallKeyFilter();
		}
	}

	private void keyDown(Event e) {
		if (hidden()) {
			parkKeyFilter(); // the first key press that finds the table hidden takes the filter off the Display
			return;
		}
		keyFilterRuns++;
		stockKeyFilter.handleEvent(e);
	}

	private void installKeyFilter() {
		if (!keyInstalled) {
			keyInstalled = true;
			display.addFilter(SWT.KeyDown, keyFilter);
		}
	}

	private void uninstallKeyFilter() {
		if (keyInstalled) {
			keyInstalled = false;
			if (!display.isDisposed()) {
				display.removeFilter(SWT.KeyDown, keyFilter);
			}
		}
	}

	private void parkKeyFilter() {
		if (keyInstalled) {
			uninstallKeyFilter();
			keyFilterParks++;
		}
	}

	/** @return true while the key filter is on the Display (tests) */
	boolean isKeyFilterInstalled() {
		return keyInstalled;
	}

	/* Paint, PartVisible: the table is shown again */
	private void resume() {
		if (pending() && !hidden()) {
			reveal();
		}
	}

	/* Show of the table or the CompositeTable on stock SWT, sent before the window is shown */
	private void showing(Control shown) {
		if (pending() && willShow(shown)) {
			revealing = true;
			try {
				reveal();
			} finally {
				revealing = false;
			}
		}
	}

	private void reveal() {
		reveals++;
		catchUp();
		if (keyWanted) {
			installKeyFilter();
		}
	}

	/* the shown control and the controls between it and the table decide by their own flag, the rest by isVisible */
	private boolean willShow(Control shown) {
		Control outer = shown.getParent();
		if (table.isDisposed() || outer != null && !outer.isVisible() || !sized || table.getShell().getMinimized()) {
			return false;
		}
		for (Control c = table; c != shown; c = c.getParent()) {
			if (!c.getVisible()) {
				return false;
			}
		}
		return true;
	}

	private void disposed() {
		uninstallKeyFilter();
		if (!display.isDisposed()) {
			display.removeFilter(SWT.KeyDown, stockKeyFilter);
		}
		if (!parent.isDisposed()) {
			parent.removeListener(SWT.Hide, listener);
			parent.removeListener(SWT.Show, listener);
		}
		refreshCount = 0;
	}

	private static int partEvent(final String name) {
		try {
			return SWT.class.getField(name).getInt(null);
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return -1;
		}
	}

	private static Method partVisibleMethod() {
		try {
			return Control.class.getMethod("isPartVisible"); //$NON-NLS-1$
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
