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
package org.eclipse.nebula.widgets.datechooser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.eclipse.nebula.widgets.datechooser.internal.HiddenGate;
import org.eclipse.nebula.widgets.datechooser.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A date picker: a text field with the date and a button that drops down an {@link MonthView} (the SWT counterpart
 * of SwingX <code>JXDatePicker</code>, re-derived from its documented behaviour). The date is an epoch day.
 * <p>
 * The drop-down month view is created on first open and reused (hidden while closed, so it takes model changes such as
 * flagged days without painting). Typing a date and pressing Enter or leaving the field parses it; an unparsable text
 * is restored to the current date.
 * </p>
 * <p>
 * While the picker is hidden (its own Hide, a hidden ancestor, a minimized shell, or SWT.PartHidden where SWT has part
 * visibility) the drop-down is closed and does not open, and a new date is not pushed into the native field: the field
 * gets the latest date once when the picker is shown (or when {@link #getTextField()} is called).
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Events: <code>Selection</code> when the date changes (<code>Event.index</code> the
 * epoch day). Startup switch: <code>-Dswt.widget.mode.DatePicker=original</code> pushes every date into the field
 * and leaves the drop-down to the shell.
 * </p>
 */
public class DatePicker extends Composite {

	/** The value of {@link #getDate()} when no date is set. */
	public static final long NO_DATE = Long.MIN_VALUE;

	static final boolean VIEWPORT = WidgetMode.of("DatePicker", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$ //FLLDCIM: MARK — every-widget DatePicker H

	private final Text field;
	private final Button button;
	private DateTimeFormatter format = DateTimeFormatter.ISO_LOCAL_DATE;
	private long date = NO_DATE;
	private Shell popup;
	private MonthView monthView;
	/* null in original mode */
	private final HiddenGate gate;
	/* the date changed while the picker was hidden: the field shows an older date until the reveal */
	private boolean staleField;

	/**
	 * Creates a picker with no date.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public DatePicker(Composite parent, int style) {
		super(parent, style);
		gate = VIEWPORT ? new HiddenGate(this, this::pushField) : null;
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 1;
		setLayout(layout);
		field = new Text(this, SWT.BORDER | SWT.SINGLE);
		GridData fieldData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		fieldData.widthHint = 90;
		field.setLayoutData(fieldData);
		button = new Button(this, SWT.ARROW | SWT.DOWN);
		button.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		button.addListener(SWT.Selection, e -> setPopupVisible(popup == null || !popup.isVisible()));
		field.addListener(SWT.DefaultSelection, e -> commitText());
		field.addListener(SWT.FocusOut, e -> commitText());
		field.addListener(SWT.KeyDown, e -> {
			if (e.keyCode == SWT.ARROW_DOWN && (e.stateMask & SWT.ALT) != 0) {
				setPopupVisible(true);
			}
		});
		Listener closer = e -> {
			if (!isDisposed()) {
				setPopupVisible(false); // the picker hides: so does its drop-down
			}
		};
		Shell shell = getShell();
		if (VIEWPORT) {
			addListener(SWT.Hide, closer);
			shell.addListener(SWT.Hide, closer);
			shell.addListener(SWT.Iconify, closer);
			if (HiddenGate.partHiddenEvent() >= 0) {
				addListener(HiddenGate.partHiddenEvent(), closer);
			}
		}
		addListener(SWT.Dispose, e -> {
			if (!shell.isDisposed()) {
				shell.removeListener(SWT.Hide, closer);
				shell.removeListener(SWT.Iconify, closer);
			}
			if (popup != null && !popup.isDisposed()) {
				popup.dispose();
			}
		});
	}

	/**
	 * Sets the date and shows it in the field.
	 *
	 * @param epochDay the day, or {@link #NO_DATE}
	 */
	public void setDate(long epochDay) {
		checkWidget();
		boolean changed = epochDay != date;
		date = epochDay;
		if (gate != null && !gate.probe()) {
			staleField = true; // hidden: the field gets the date on reveal //FLLDCIM: MARK — every-widget DatePicker H
		} else {
			staleField = true;
			pushField();
		}
		if (monthView != null && epochDay != NO_DATE) {
			monthView.setFirstDisplayedDay(epochDay);
			monthView.setSelection(epochDay);
		}
		if (changed) {
			Event event = new Event();
			event.index = (int) epochDay;
			notifyListeners(SWT.Selection, event);
		}
	}

	/** @return the date as an epoch day, or {@link #NO_DATE} */
	public long getDate() {
		checkWidget();
		return date;
	}

	/**
	 * Sets the format of the field (parsing uses the same format).
	 *
	 * @param format the format of the field
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the format is null
	 */
	public void setFormat(DateTimeFormatter format) {
		checkWidget();
		if (format == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.format = format;
		long current = date;
		date = NO_DATE;
		setDate(current);
	}

	/** @return the text field, showing the current date */
	public Text getTextField() {
		checkWidget();
		pushField();
		return field;
	}

	/** @return the drop-down month view, created on first use; flag days or set limits on it */
	public MonthView getMonthView() {
		checkWidget();
		ensurePopup();
		return monthView;
	}

	/**
	 * Opens or closes the drop-down month view below the field.
	 *
	 * @param visible true to open the month view, false to close it
	 */
	public void setPopupVisible(boolean visible) {
		checkWidget();
		if (!visible) {
			if (popup != null && !popup.isDisposed()) {
				popup.setVisible(false);
			}
			return;
		}
		if (VIEWPORT && !HiddenGate.shown(this)) {
			return; // a hidden picker opens no drop-down
		}
		ensurePopup();
		if (date != NO_DATE) {
			monthView.setFirstDisplayedDay(date);
			monthView.setSelection(date);
		}
		Point size = popup.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle bounds = getBounds();
		Point at = getParent().toDisplay(bounds.x, bounds.y + bounds.height);
		popup.setBounds(at.x, at.y, size.x, size.y);
		popup.setVisible(true);
		monthView.setFocus();
	}

	/** @return true while the drop-down is open */
	public boolean isPopupVisible() {
		checkWidget();
		return popup != null && !popup.isDisposed() && popup.isVisible();
	}

	private void ensurePopup() {
		if (popup != null && !popup.isDisposed()) {
			return;
		}
		popup = new Shell(getShell(), SWT.NO_TRIM | SWT.ON_TOP);
		popup.setLayout(new FillLayout());
		monthView = new MonthView(popup, SWT.SINGLE | SWT.BORDER);
		monthView.addListener(SWT.Selection, e -> {
			if (monthView.getSelectionStart() != Long.MIN_VALUE && monthView.getSelectionStart() != date) {
				setDate(monthView.getSelectionStart());
			}
		});
		monthView.addListener(SWT.DefaultSelection, e -> setPopupVisible(false));
		monthView.addListener(SWT.MouseUp, e -> setPopupVisible(false));
		popup.addListener(SWT.Deactivate, e -> popup.setVisible(false));
		popup.addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				popup.setVisible(false);
				e.doit = false;
			}
		});
	}

	/* the one patch on becoming visible: the latest date into the native field */
	private void pushField() {
		if (staleField) {
			staleField = false;
			field.setText(date == NO_DATE ? "" : format.format(LocalDate.ofEpochDay(date))); //$NON-NLS-1$
		}
	}

	private void commitText() {
		if (staleField) {
			pushField(); // the field holds an older date nobody typed: show the current one, parse nothing
			return;
		}
		String text = field.getText().trim();
		if (text.isEmpty()) {
			setDate(NO_DATE);
			return;
		}
		try {
			setDate(LocalDate.parse(text, format).toEpochDay());
		} catch (DateTimeParseException e) {
			long current = date;
			date = NO_DATE;
			setDate(current);
		}
	}
}
