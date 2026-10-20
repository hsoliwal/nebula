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
import org.eclipse.swt.widgets.ToolTip;

/** POPOVER on RWT: a balloon ToolTip shown next to a button. */
public final class PopoverExample extends RwtExample {

	public PopoverExample () {
		super ("POPOVER", "org.eclipse.swt.widgets.ToolTip (SWT.BALLOON)");
	}

	@Override
	public Control create (Composite parent) {
		Button button = new Button (parent, SWT.PUSH);
		button.setText ("Anchor");
		ToolTip tip = new ToolTip (parent.getShell (), SWT.BALLOON | SWT.ICON_INFORMATION);
		tip.setText ("Popover");
		tip.setMessage ("Rich tooltip shown on RWT");
		tip.setAutoHide (false);
		tip.setLocation (40, 40);
		tip.setVisible (true);
		button.setData (tip);
		return button;
	}

	@Override
	public void change (Control control) {
		((ToolTip) control.getData ()).setMessage ("Changed message");
	}
}
