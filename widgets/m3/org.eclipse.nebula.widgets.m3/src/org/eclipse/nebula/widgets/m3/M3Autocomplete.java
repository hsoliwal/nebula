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

import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * Autocomplete for a text field (behaviours catalogue: autocomplete): proposals are the rows of an
 * {@link M3Searchable} (the model, by index) whose text starts with, or contains, what was typed; they appear in a
 * drop-down list below the field; Up and Down move, Enter or a click accepts (the field takes the proposal and
 * Selection is sent with Event.index the model row), Escape closes. JFace's ContentProposalAdapter builds proposal
 * objects per keystroke; here the matches are an int lane of model rows.
 * <p>
 * Matching is incremental and superseding: each keystroke starts a new generation; the scan reads at most
 * {@link #SLICE} rows per event loop turn, stops at {@link #setLimit(int) the limit} and allocates nothing per row.
 * The drop-down is one reused shell with a virtual table whose items are filled from the match lane only when shown;
 * while it is closed it is hidden. Rows that start with the text come first when prefix matches are preferred.
 * </p>
 */
public final class M3Autocomplete {

	/** Rows scanned per event loop turn. */
	public static final int SLICE = 50_000;

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3Autocomplete"); //$NON-NLS-1$

	private final Text text;
	private final M3Searchable source;
	private Shell popup;
	private Table list;
	private int[] matches = new int[64];
	private int matchCount, limit = 200, generation, minimumLength = 1, visibleRows = 10;
	private boolean contains = true, accepting;
	private String typed = ""; //$NON-NLS-1$

	/**
	 * Attaches autocomplete to a text field.
	 *
	 * @param text the field
	 * @param source the proposals, by model row
	 */
	public M3Autocomplete(Text text, M3Searchable source) {
		if (text == null || source == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.text = text;
		this.source = source;
		text.addListener(SWT.Modify, e -> {
			if (!accepting) {
				propose(text.getText());
			}
		});
		text.addListener(SWT.KeyDown, this::key);
		text.addListener(SWT.FocusOut, e -> text.getDisplay().asyncExec(() -> {
			if (!text.isDisposed() && popup != null && !popup.isDisposed() && !list.isFocusControl()) {
				close();
			}
		}));
		text.addListener(SWT.Dispose, e -> {
			generation++;
			if (popup != null && !popup.isDisposed()) {
				popup.dispose();
			}
		});
	}

	/** Sets the maximum number of proposals, at least 1. */
	public void setLimit(int limit) {
		if (limit < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.limit = limit;
	}

	/** Sets whether a proposal may contain the text anywhere (true) or must start with it (false). */
	public void setContains(boolean contains) {
		this.contains = contains;
	}

	/** Sets how many characters must be typed before proposals appear, at least 0. */
	public void setMinimumLength(int length) {
		if (length < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		minimumLength = length;
	}

	/** @return the number of proposals found for the current text */
	public int getProposalCount() {
		return matchCount;
	}

	/** @return the model row of a proposal */
	public int getProposalRow(int index) {
		if (index < 0 || index >= matchCount) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		return matches[index];
	}

	/** @return true while the drop-down is open */
	public boolean isOpen() {
		return popup != null && !popup.isDisposed() && popup.isVisible();
	}

	/** @return the index of the highlighted proposal, or -1 */
	public int getHighlighted() {
		return isOpen() ? list.getSelectionIndex() : -1;
	}

	/** Closes the drop-down and stops a running scan. */
	public void close() {
		generation++;
		if (isOpen()) {
			popup.setVisible(false);
		}
	}

	/**
	 * Accepts a proposal: the field takes its text, the drop-down closes and Selection is sent to the field's
	 * listeners with Event.index the model row.
	 */
	public void accept(int index) {
		int row = getProposalRow(index);
		accepting = true;
		try {
			text.setText(source.getText(row));
			text.setSelection(text.getText().length());
		} finally {
			accepting = false;
		}
		close();
		Event event = new Event();
		event.index = row;
		text.notifyListeners(SWT.Selection, event);
	}

	private void key(Event event) {
		if (!isOpen()) {
			if (event.keyCode == SWT.ARROW_DOWN) {
				propose(text.getText());
			}
			return;
		}
		int index = list.getSelectionIndex();
		switch (event.keyCode) {
		case SWT.ARROW_DOWN:
			list.setSelection(Math.min(matchCount - 1, index + 1));
			event.doit = false;
			break;
		case SWT.ARROW_UP:
			list.setSelection(Math.max(0, index - 1));
			event.doit = false;
			break;
		case SWT.ESC:
			close();
			event.doit = false;
			break;
		default:
			if (event.character == '\r' && index >= 0) {
				accept(index);
				event.doit = false;
			}
			break;
		}
	}

	/* a new generation: prefix matches first (one pass), then contains matches, SLICE rows per turn, until the limit */
	private void propose(String value) {
		typed = value;
		int mine = ++generation;
		matchCount = 0;
		if (value.length() < minimumLength) {
			close();
			return;
		}
		text.getDisplay().asyncExec(() -> scan(mine, 0, false));
	}

	private void scan(int mine, int from, boolean containsPass) {
		if (mine != generation || text.isDisposed()) {
			return;
		}
		int rows = source.getRowCount(), stop = Math.min(rows, from + SLICE);
		for (int row = from; row < stop && matchCount < limit; row++) {
			String candidate = source.getText(row);
			boolean prefix = candidate.regionMatches(true, 0, typed, 0, typed.length());
			if (containsPass ? !prefix && M3FindBar.matches(candidate, typed, false) : prefix) {
				if (matchCount == matches.length) {
					matches = Arrays.copyOf(matches, matchCount * 2);
				}
				matches[matchCount++] = row;
			}
		}
		if (matchCount < limit && stop < rows) {
			text.getDisplay().asyncExec(() -> scan(mine, stop, containsPass));
		} else if (matchCount < limit && contains && !containsPass) {
			text.getDisplay().asyncExec(() -> scan(mine, 0, true));
		} else {
			show();
		}
	}

	private void show() {
		if (matchCount == 0) {
			close();
			return;
		}
		ensurePopup();
		list.setItemCount(0);
		list.setItemCount(matchCount);
		list.setSelection(0);
		Point origin = text.toDisplay(0, text.getSize().y);
		int height = Math.min(matchCount, visibleRows) * list.getItemHeight() + 2 * list.getBorderWidth() + 2;
		Rectangle area = text.getMonitor().getClientArea();
		int[] placed = new int[3];
		Rectangle field = text.getBounds();
		Point fieldTopLeft = text.getParent().toDisplay(field.x, field.y);
		M3Placement.place(fieldTopLeft.x, fieldTopLeft.y, field.width, field.height, Math.max(field.width, 200), height, area.x, area.y,
				area.width, area.height, M3Placement.BOTTOM, 0, placed);
		popup.setBounds(placed[0], placed[1], Math.max(field.width, 200), height);
		popup.setVisible(true);
		LOG.debug(() -> matchCount + " proposals for '" + typed + "' at " + origin); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void ensurePopup() {
		if (popup != null && !popup.isDisposed()) {
			return;
		}
		popup = new Shell(text.getShell(), SWT.ON_TOP | SWT.TOOL | SWT.NO_TRIM);
		popup.setLayout(new FillLayout());
		list = new Table(popup, SWT.VIRTUAL | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
		list.addListener(SWT.SetData, e -> {
			TableItem item = (TableItem) e.item;
			int index = list.indexOf(item);
			if (index < matchCount) {
				item.setText(source.getText(matches[index]));
			}
		});
		list.addListener(SWT.DefaultSelection, e -> {
			int index = list.getSelectionIndex();
			if (index >= 0) {
				accept(index);
			}
		});
		list.addListener(SWT.MouseUp, e -> {
			int index = list.getSelectionIndex();
			if (index >= 0) {
				accept(index);
			}
		});
	}
}
