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
package org.eclipse.nebula.widgets.m3.jface;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.nebula.widgets.m3.M3ErrorPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * JFace/Eclipse binding of {@link M3ErrorPane} for an {@link IStatus} (the ErrorDialog use): the status message is the
 * message, the severity picks the icon, and the details (the child statuses and every exception's stack trace) are
 * formatted only when the details are first shown.
 */
public final class M3StatusPane {

	private M3StatusPane() {
	}

	/**
	 * Shows a status in an error pane.
	 *
	 * @param pane the pane
	 * @param status the status
	 */
	public static void show(M3ErrorPane pane, IStatus status) {
		if (pane == null || status == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		pane.setMessage(status.getMessage());
		pane.setSeverity(status.getSeverity() == IStatus.ERROR || status.getSeverity() == IStatus.CANCEL ? SWT.ICON_ERROR
				: status.getSeverity() == IStatus.WARNING ? SWT.ICON_WARNING : SWT.ICON_INFORMATION);
		if (status.isMultiStatus() || status.getException() != null) {
			pane.setError(new StatusDetails(status));
		} else {
			pane.setDetails(""); //$NON-NLS-1$
		}
	}

	/**
	 * Opens a modal dialog with an error pane for a status (the ErrorDialog.openError form) and returns when it closes.
	 */
	public static void open(Shell parent, String title, IStatus status) {
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialog.setText(title == null ? "" : title); //$NON-NLS-1$
		dialog.setLayout(new FillLayout());
		show(new M3ErrorPane(dialog, SWT.NONE), status);
		dialog.pack();
		dialog.open();
		Display display = dialog.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/* a throwable whose stack trace is the status tree: formatted by the pane only when the details are shown */
	private static final class StatusDetails extends Throwable {

		private static final long serialVersionUID = 1L;

		private final transient IStatus status;

		StatusDetails(IStatus status) {
			super(status.getMessage(), null, false, false);
			this.status = status;
		}

		@Override
		public void printStackTrace(PrintWriter writer) {
			append(writer, status, 0);
		}

		private static void append(PrintWriter writer, IStatus status, int depth) {
			writer.print("  ".repeat(depth)); //$NON-NLS-1$
			writer.println(severity(status.getSeverity()) + " " + status.getPlugin() + ": " + status.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
			if (status.getException() != null) {
				StringWriter trace = new StringWriter();
				status.getException().printStackTrace(new PrintWriter(trace));
				writer.print(trace);
			}
			for (IStatus child : status.getChildren()) {
				append(writer, child, depth + 1);
			}
		}

		private static String severity(int severity) {
			switch (severity) {
			case IStatus.ERROR:
				return "ERROR"; //$NON-NLS-1$
			case IStatus.WARNING:
				return "WARNING"; //$NON-NLS-1$
			case IStatus.INFO:
				return "INFO"; //$NON-NLS-1$
			case IStatus.CANCEL:
				return "CANCEL"; //$NON-NLS-1$
			default:
				return "OK"; //$NON-NLS-1$
			}
		}
	}
}
