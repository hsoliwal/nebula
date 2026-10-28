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

import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * A find bar: a search field, previous and next buttons, match case and wrap options and a "k of n" status, searching
 * an {@link M3Searchable} (the SWT counterpart of SwingX <code>JXFindBar</code>, re-derived from its documented
 * behaviour).
 * <p>
 * Search is incremental and sliced: typing starts a new generation that supersedes the running one (catalogue:
 * superseding-generation debounce); the first match at or after the current row is found and revealed, then the
 * matches are counted, each step scanning at most {@link #SLICE} rows per event loop turn, so 10^6 rows never block the
 * UI. Matching allocates nothing per row (String.regionMatches / indexOf on the row text). Rows are read from the
 * model through the searchable, never from widget items.
 * </p>
 * <p>
 * While the bar is hidden (its own Hide, a zero size, a hidden ancestor, or SWT.PartHidden) a search request only
 * records the pattern; scanning starts when the bar becomes visible. Styles: <code>BORDER</code>. Events:
 * <code>Selection</code> when a match is revealed (<code>Event.index</code> the row). Startup switch:
 * <code>-Dswt.widget.mode.M3FindBar=original</code> scans all rows synchronously on every keystroke.
 * </p>
 */
public class M3FindBar extends Composite {

	/** Rows scanned per event loop turn. */
	public static final int SLICE = 50_000;

	static final boolean VIEWPORT = WidgetMode.of("M3FindBar", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3FindBar"); //$NON-NLS-1$

	private final Text field;
	private final Button previous, next, matchCase, wrap;
	private final Label status;
	private final M3HiddenGate gate;
	private M3Searchable target;
	private String pattern = ""; //$NON-NLS-1$
	/* the revealed match row, the match count (-1 while counting) and the superseding generation */
	private int current = -1, count = -1, generation;
	private boolean searching, pending, settingText;
	/* sorted rows of all matches, filled by the counting slices; valid when count >= 0 */
	private int[] hits = new int[16];
	private int hitCount;

	/**
	 * Creates a find bar with no searchable.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3FindBar(Composite parent, int style) {
		super(parent, style);
		gate = new M3HiddenGate(this, this::revealed);
		GridLayout layout = new GridLayout(7, false);
		layout.marginHeight = 2;
		setLayout(layout);
		new Label(this, SWT.NONE).setText("Find:"); //$NON-NLS-1$
		field = new Text(this, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
		GridData fieldData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		fieldData.minimumWidth = 120;
		field.setLayoutData(fieldData);
		previous = new Button(this, SWT.PUSH);
		previous.setText("Previous"); //$NON-NLS-1$
		next = new Button(this, SWT.PUSH);
		next.setText("Next"); //$NON-NLS-1$
		matchCase = new Button(this, SWT.CHECK);
		matchCase.setText("Match case"); //$NON-NLS-1$
		wrap = new Button(this, SWT.CHECK);
		wrap.setText("Wrap"); //$NON-NLS-1$
		wrap.setSelection(true);
		status = new Label(this, SWT.NONE);
		GridData statusData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		statusData.widthHint = 110;
		status.setLayoutData(statusData);
		field.addListener(SWT.Modify, e -> {
			if (!settingText) {
				search(field.getText());
			}
		});
		field.addListener(SWT.DefaultSelection, e -> findNext());
		previous.addListener(SWT.Selection, e -> findPrevious());
		next.addListener(SWT.Selection, e -> findNext());
		matchCase.addListener(SWT.Selection, e -> search(pattern));
		addListener(SWT.Dispose, e -> generation++);
	}

	/**
	 * Sets what the bar searches; a running search is superseded.
	 *
	 * @param searchable the rows to search, or null
	 */
	public void setSearchable(M3Searchable searchable) {
		checkWidget();
		target = searchable;
		current = -1;
		search(pattern);
	}

	/** @return the searchable, or null */
	public M3Searchable getSearchable() {
		checkWidget();
		return target;
	}

	/**
	 * Sets the search text and searches from the current match.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the text is null
	 */
	public void setText(String text) {
		checkWidget();
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		settingText = true;
		try {
			field.setText(text);
		} finally {
			settingText = false;
		}
		search(text);
	}

	/** @return the search text, never null */
	public String getText() {
		checkWidget();
		return pattern;
	}

	/** Sets whether matching is case sensitive and searches again. */
	public void setMatchCase(boolean value) {
		checkWidget();
		matchCase.setSelection(value);
		search(pattern);
	}

	/** @return true when matching is case sensitive */
	public boolean getMatchCase() {
		checkWidget();
		return matchCase.getSelection();
	}

	/** Sets whether next and previous wrap around the ends. */
	public void setWrap(boolean value) {
		checkWidget();
		wrap.setSelection(value);
	}

	/** @return true when next and previous wrap around */
	public boolean getWrap() {
		checkWidget();
		return wrap.getSelection();
	}

	/** @return the row of the revealed match, or -1 */
	public int getCurrentMatch() {
		checkWidget();
		return current;
	}

	/** @return the number of matching rows, or -1 while they are being counted */
	public int getMatchCount() {
		checkWidget();
		return count;
	}

	/** @return true while a sliced search runs */
	public boolean isSearching() {
		checkWidget();
		return searching;
	}

	/** @return the text of the status label, for example "3 of 120" */
	public String getStatus() {
		checkWidget();
		return status.getText();
	}

	/** Tells the bar that rows were added, removed or changed; the match count is computed again. */
	public void modelChanged() {
		checkWidget();
		search(pattern);
	}

	/**
	 * Finds and reveals the next match after the current one (synchronously).
	 *
	 * @return the row, or -1 when there is none
	 */
	public int findNext() {
		checkWidget();
		return step(1);
	}

	/**
	 * Finds and reveals the previous match before the current one (synchronously).
	 *
	 * @return the row, or -1 when there is none
	 */
	public int findPrevious() {
		checkWidget();
		return step(-1);
	}

	private int step(int direction) {
		if (target == null || pattern.isEmpty()) {
			return -1;
		}
		if (count >= 0) {
			return stepHits(direction);
		}
		int rows = target.getRowCount();
		int found = scan(current + direction, direction, rows);
		if (found < 0 && getWrap() && rows > 0) {
			found = scan(direction > 0 ? 0 : rows - 1, direction, rows);
		}
		if (found >= 0) {
			reveal(found);
		}
		return found;
	}

	/* next or previous match by binary search over the hit lane: O(log n) */
	private int stepHits(int direction) {
		if (count == 0) {
			return -1;
		}
		int at = Arrays.binarySearch(hits, 0, hitCount, current);
		int index;
		if (direction > 0) {
			index = at >= 0 ? at + 1 : -at - 1;
		} else {
			index = at >= 0 ? at - 1 : -at - 2;
		}
		if (index < 0 || index >= hitCount) {
			if (!getWrap()) {
				return -1;
			}
			index = index < 0 ? hitCount - 1 : 0;
		}
		reveal(hits[index]);
		return hits[index];
	}

	/* first matching row from `from` in `direction`, or -1; no allocation per row */
	private int scan(int from, int direction, int rows) {
		boolean caseSensitive = getMatchCase();
		for (int row = from; row >= 0 && row < rows; row += direction) {
			if (matches(target.getText(row), pattern, caseSensitive)) {
				return row;
			}
		}
		return -1;
	}

	static boolean matches(String text, String needle, boolean caseSensitive) {
		if (caseSensitive) {
			return text.indexOf(needle) >= 0;
		}
		int last = text.length() - needle.length();
		for (int i = 0; i <= last; i++) {
			if (text.regionMatches(true, i, needle, 0, needle.length())) {
				return true;
			}
		}
		return false;
	}

	private void reveal(int row) {
		current = row;
		target.reveal(row);
		updateStatus();
		Event event = new Event();
		event.index = row;
		notifyListeners(SWT.Selection, event);
	}

	/* a new generation: supersedes the running search, finds the first match at or after the current row, counts */
	private void search(String text) {
		pattern = text;
		int mine = ++generation;
		count = -1;
		hitCount = 0;
		if (target == null || text.isEmpty()) {
			searching = false;
			count = 0;
			updateStatus();
			return;
		}
		if (!VIEWPORT) {
			int found = scan(Math.max(0, current), 1, target.getRowCount());
			if (found < 0 && getWrap()) {
				found = scan(0, 1, target.getRowCount());
			}
			collect(0, target.getRowCount());
			count = hitCount;
			if (found >= 0) {
				reveal(found);
			} else {
				updateStatus();
			}
			return;
		}
		if (!gate.probe()) {
			pending = true;
			return;
		}
		searching = true;
		LOG.debug(() -> "search generation " + mine + " for '" + text + "'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		int start = Math.max(0, current);
		getDisplay().asyncExec(() -> findSlice(mine, start, start, false));
	}

	/* finds the first match from `start`, wrapping once to 0, SLICE rows per turn, then starts counting */
	private void findSlice(int mine, int start, int row, boolean wrapped) {
		if (isDisposed() || mine != generation) {
			return;
		}
		int rows = target.getRowCount(), end = wrapped ? Math.min(start, rows) : rows;
		int stop = Math.min(end, row + SLICE);
		boolean caseSensitive = getMatchCase();
		for (int r = row; r < stop; r++) {
			if (matches(target.getText(r), pattern, caseSensitive)) {
				reveal(r);
				countSlice(mine, 0);
				return;
			}
		}
		if (stop < end) {
			getDisplay().asyncExec(() -> findSlice(mine, start, stop, wrapped));
		} else if (!wrapped && start > 0 && getWrap()) {
			getDisplay().asyncExec(() -> findSlice(mine, start, 0, true));
		} else {
			countSlice(mine, 0);
		}
	}

	private void countSlice(int mine, int row) {
		if (isDisposed() || mine != generation) {
			return;
		}
		int rows = target.getRowCount(), stop = Math.min(rows, row + SLICE);
		collect(row, stop);
		if (stop < rows) {
			getDisplay().asyncExec(() -> countSlice(mine, stop));
			return;
		}
		count = hitCount;
		searching = false;
		updateStatus();
	}

	/* appends the matching rows of [from, to) to the hit lane (doubling growth, no per-row allocation) */
	private void collect(int from, int to) {
		boolean caseSensitive = getMatchCase();
		for (int r = from; r < to; r++) {
			if (matches(target.getText(r), pattern, caseSensitive)) {
				if (hitCount == hits.length) {
					hits = Arrays.copyOf(hits, hitCount * 2);
				}
				hits[hitCount++] = r;
			}
		}
	}

	private void updateStatus() {
		String text;
		if (pattern.isEmpty()) {
			text = ""; //$NON-NLS-1$
		} else if (count < 0) {
			text = "searching..."; //$NON-NLS-1$
		} else if (count == 0) {
			text = "no match"; //$NON-NLS-1$
		} else {
			text = current >= 0 ? ordinal() + " of " + count : count + " matches"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!text.equals(status.getText())) {
			status.setText(text);
		}
	}

	/* the 1-based position of the current match among all matches, by binary search over the hit lane */
	private int ordinal() {
		return Math.max(0, Arrays.binarySearch(hits, 0, hitCount, current)) + 1;
	}

	/* the one patch on becoming visible: start the search that was requested while hidden */
	private void revealed() {
		if (pending) {
			pending = false;
			search(pattern);
		}
	}
}
