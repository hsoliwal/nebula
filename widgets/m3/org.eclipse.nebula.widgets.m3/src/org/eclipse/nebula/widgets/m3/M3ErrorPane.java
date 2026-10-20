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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.M3HiddenGate;
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * An error pane: a severity icon, a wrapping message, and details (a stack trace or any text) that expand on demand,
 * with a button that copies message and details (the SWT counterpart of SwingX <code>JXErrorPane</code>, re-derived
 * from its documented behaviour). {@link #open(Shell, String, String, Throwable)} shows it in a modal dialog.
 * <p>
 * Details are deferred: the stack trace is formatted and the details text control created only when the details are
 * first shown (in an {@link M3CollapsiblePane}); a new error while the details are hidden only stores the
 * throwable. A resize reflows the message; it never formats, creates or measures the details.
 * While the pane is hidden (its own Hide, a zero size, a hidden ancestor) message, severity and details changes
 * only update the model; on becoming visible the latest values go into the controls once.
 * </p>
 * <p>
 * Styles: <code>BORDER</code>. Startup switch: <code>-Dswt.widget.mode.M3ErrorPane=original</code> creates the details
 * control with the pane and formats every error at once, as a plain implementation would. RWT: no drawing;
 * {@link #copyToClipboard()} needs org.eclipse.swt.dnd.Clipboard, which RWT lacks.
 * </p>
 */
public class M3ErrorPane extends Composite {

	static final boolean VIEWPORT = WidgetMode.of("M3ErrorPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3ErrorPane"); //$NON-NLS-1$

	private static final int DETAIL_LINES = 12;

	private final Label icon, message;
	private final Button detailsButton, copyButton;
	private final M3CollapsiblePane detailsPane;
	private Text detailsText;
	private int severity = SWT.ICON_ERROR;
	private Throwable error;
	/* null while an error waits to be formatted */
	private String details = ""; //$NON-NLS-1$
	private String messageText = ""; //$NON-NLS-1$
	private final M3HiddenGate gate;
	/* model changes made while the pane was hidden, applied to the controls on reveal */
	private boolean staleMessage, staleIcon, staleDetails;

	/**
	 * Creates an error pane with an empty message and no details.
	 *
	 * @param parent the parent
	 * @param style BORDER
	 */
	public M3ErrorPane(Composite parent, int style) {
		super(parent, style);
		gate = new M3HiddenGate(this, this::revealed);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		setLayout(layout);
		icon = new Label(this, SWT.NONE);
		icon.setImage(getDisplay().getSystemImage(severity));
		icon.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		message = new Label(this, SWT.WRAP);
		GridData messageData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		messageData.widthHint = 320;
		message.setLayoutData(messageData);
		Composite buttons = new Composite(this, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		GridLayout buttonLayout = new GridLayout(2, true);
		buttonLayout.marginWidth = buttonLayout.marginHeight = 0;
		buttons.setLayout(buttonLayout);
		copyButton = new Button(buttons, SWT.PUSH);
		copyButton.setText("Copy"); //$NON-NLS-1$
		copyButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		copyButton.addListener(SWT.Selection, e -> copyToClipboard());
		detailsButton = new Button(buttons, SWT.PUSH);
		detailsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		detailsButton.addListener(SWT.Selection, e -> setDetailsVisible(!isDetailsVisible()));
		detailsPane = new M3CollapsiblePane(this, SWT.VERTICAL);
		detailsPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		detailsPane.setAnimated(false);
		detailsPane.setCollapsed(true);
		detailsPane.getContent().setLayout(new FillLayoutWithHint());
		if (!VIEWPORT) {
			materialize();
		}
		updateDetailsButton();
	}

	/**
	 * Shows an error in a modal dialog and returns when the dialog is closed.
	 *
	 * @param parent the parent shell
	 * @param title the dialog title
	 * @param message the message, never null
	 * @param error the error whose stack trace is the details, or null
	 */
	public static void open(Shell parent, String title, String message, Throwable error) {
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText(title == null ? "" : title); //$NON-NLS-1$
		dialog.setLayout(new FillLayout());
		M3ErrorPane pane = new M3ErrorPane(dialog, SWT.NONE);
		pane.setMessage(message);
		pane.setError(error);
		dialog.pack();
		dialog.open();
		Display display = dialog.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Sets the message; only the message is reflowed.
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the message is null
	 */
	public void setMessage(String text) {
		checkWidget();
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (text.equals(messageText)) {
			return;
		}
		messageText = text;
		if (showing()) {
			message.setText(text);
			message.requestLayout();
		} else {
			staleMessage = true;
		}
	}

	/** @return the message, never null */
	public String getMessage() {
		checkWidget();
		return messageText;
	}

	/**
	 * Sets the severity icon.
	 *
	 * @param severity SWT.ICON_ERROR, SWT.ICON_WARNING or SWT.ICON_INFORMATION
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if the value is not one of these
	 */
	public void setSeverity(int severity) {
		checkWidget();
		if (severity != SWT.ICON_ERROR && severity != SWT.ICON_WARNING && severity != SWT.ICON_INFORMATION) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.severity = severity;
		if (showing()) {
			icon.setImage(getDisplay().getSystemImage(severity));
		} else {
			staleIcon = true;
		}
	}

	/** @return the severity icon constant */
	public int getSeverity() {
		checkWidget();
		return severity;
	}

	/**
	 * Sets the error whose stack trace is the details; it is formatted when the details are first needed.
	 *
	 * @param error the error, or null for no details
	 */
	public void setError(Throwable error) {
		checkWidget();
		this.error = error;
		details = error == null ? "" : null; //$NON-NLS-1$
		refreshDetails();
	}

	/** @return the error, or null */
	public Throwable getError() {
		checkWidget();
		return error;
	}

	/**
	 * Sets the details text directly (instead of a stack trace).
	 *
	 * @exception IllegalArgumentException ERROR_NULL_ARGUMENT - if the details are null
	 */
	public void setDetails(String details) {
		checkWidget();
		if (details == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		error = null;
		this.details = details;
		refreshDetails();
	}

	/** @return the details, formatting a pending stack trace now; never null */
	public String getDetails() {
		checkWidget();
		if (details == null) {
			StringWriter writer = new StringWriter();
			error.printStackTrace(new PrintWriter(writer));
			details = writer.toString();
			LOG.debug(() -> "formatted " + details.length() + " characters of details"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return details;
	}

	/** @return message, a blank line and the details: what the copy button puts on the clipboard */
	public String getReport() {
		checkWidget();
		String text = getDetails();
		return text.isEmpty() ? getMessage() : getMessage() + System.lineSeparator() + System.lineSeparator() + text;
	}

	/** Copies {@link #getReport()} to the clipboard. */
	public void copyToClipboard() {
		checkWidget();
		Clipboard clipboard = new Clipboard(getDisplay());
		clipboard.setContents(new Object[] { getReport() }, new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}

	/**
	 * Shows or hides the details, creating the details text control the first time.
	 *
	 * @param visible true to show the details
	 */
	public void setDetailsVisible(boolean visible) {
		checkWidget();
		if (visible) {
			materialize();
		}
		detailsPane.setCollapsed(!visible);
		updateDetailsButton();
		getShell().layout(new Control[] { detailsPane });
	}

	/** @return true when the details are shown */
	public boolean isDetailsVisible() {
		checkWidget();
		return !detailsPane.isCollapsed();
	}

	/** @return the details text control, or null before the details were first shown */
	public Text getDetailsText() {
		checkWidget();
		return detailsText;
	}

	private void materialize() {
		if (detailsText == null) {
			detailsText = new Text(detailsPane.getContent(), SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			detailsText.setText(getDetails());
			M3Counters.creates++;
		}
	}

	private void refreshDetails() {
		if (!showing()) {
			staleDetails = true;
			return;
		}
		if (detailsText != null) {
			detailsText.setText(getDetails());
		}
		updateDetailsButton();
	}

	/* a hidden pane does no native work: changes fold into the gate */
	private boolean showing() {
		return !VIEWPORT || gate.probe();
	}

	/* the one patch on becoming visible: push the latest message, icon and details into the controls */
	private void revealed() {
		if (staleMessage) {
			staleMessage = false;
			message.setText(messageText);
			message.requestLayout();
		}
		if (staleIcon) {
			staleIcon = false;
			icon.setImage(getDisplay().getSystemImage(severity));
		}
		if (staleDetails) {
			staleDetails = false;
			refreshDetails();
		}
	}

	private void updateDetailsButton() {
		detailsButton.setText(isDetailsVisible() ? "Details <<" : "Details >>"); //$NON-NLS-1$ //$NON-NLS-2$
		detailsButton.setEnabled(error != null || details == null || !details.isEmpty());
	}

	/* fills the details composite and asks for DETAIL_LINES lines of text */
	private final class FillLayoutWithHint extends Layout {

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			if (detailsText == null) {
				return new Point(0, 0);
			}
			int lineHeight = detailsText.getLineHeight();
			return new Point(wHint == SWT.DEFAULT ? 400 : wHint, lineHeight * DETAIL_LINES + 8);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			if (detailsText != null) {
				detailsText.setBounds(composite.getClientArea());
			}
		}
	}
}
