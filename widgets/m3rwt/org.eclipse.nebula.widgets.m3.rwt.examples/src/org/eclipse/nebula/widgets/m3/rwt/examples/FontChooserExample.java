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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;

/** FONT_CHOOSER on RWT: FontDialog, opened without blocking (RWT's Dialog.open (DialogCallback)). */
public final class FontChooserExample extends RwtExample {

	public FontChooserExample () {
		super ("FONT_CHOOSER", "org.eclipse.swt.widgets.FontDialog");
	}

	@Override
	public Control create (Composite parent) {
		Button button = new Button (parent, SWT.PUSH);
		button.setText ("Choose");
		FontDialog dialog = new FontDialog (parent.getShell ());
		dialog.setText ("FontDialog");
		dialog.open (returnCode -> button.setText ("Chosen: " + returnCode));
		return button;
	}

	@Override
	public void change (Control control) {
		((Button) control).setText ("Choose again " + next ());
	}
}
