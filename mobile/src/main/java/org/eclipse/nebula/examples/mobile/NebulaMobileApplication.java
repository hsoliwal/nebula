/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.examples.mobile;

import org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Runs the unchanged Nebula RoundedCheckbox through SWT's mobile platform source
 * set. Application code uses ordinary SWT owners, layouts and listeners. The
 * Android/iOS host only installs its OS binding and starts this main method on
 * the SWT application thread.
 */
public final class NebulaMobileApplication {
	private NebulaMobileApplication() { }
	public static void main(String[] args) {
		Display display = new Display();
		try {
			Shell shell = new Shell(display);
			shell.setText("Nebula on SWT mobile");
			GridLayout layout = new GridLayout(1, false);
			layout.marginWidth = 24; layout.marginHeight = 24; layout.verticalSpacing = 20;
			shell.setLayout(layout);
			Canvas heading = new Canvas(shell, SWT.NONE);
			GridData headingBounds = new GridData(SWT.FILL, SWT.CENTER, true, false);
			headingBounds.heightHint = 32; heading.setLayoutData(headingBounds);
			heading.addPaintListener(event -> event.gc.drawText("Nebula RoundedCheckbox", 0, 0, true));
			RoundedCheckbox checkbox = new RoundedCheckbox(shell, SWT.NONE);
			GridData checkboxBounds = new GridData(SWT.CENTER, SWT.CENTER, true, false);
			checkboxBounds.widthHint = 112; checkboxBounds.heightHint = 112;
			checkbox.setLayoutData(checkboxBounds);
			Canvas status = new Canvas(shell, SWT.NONE);
			GridData statusBounds = new GridData(SWT.FILL, SWT.FILL, true, true);
			statusBounds.heightHint = 80; status.setLayoutData(statusBounds);
			status.addPaintListener(event -> event.gc.drawText(
				(checkbox.getSelection() ? "Selected" : "Not selected") + "\nTap the circle to change selection.", 0, 0, true));
			checkbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(event ->
				display.asyncExec(() -> { if (!status.isDisposed()) status.redraw(); })));
			checkbox.addListener(SWT.KeyUp, event ->
				display.asyncExec(() -> { if (!status.isDisposed()) status.redraw(); }));
			shell.open();
			while (!shell.isDisposed()) if (!display.readAndDispatch()) display.sleep();
		} finally { if (!display.isDisposed()) display.dispose(); }
	}
}
