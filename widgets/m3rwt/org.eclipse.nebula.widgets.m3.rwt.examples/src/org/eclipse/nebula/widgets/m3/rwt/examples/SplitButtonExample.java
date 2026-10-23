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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/** SPLIT_BUTTON on RWT: ToolItem with SWT.DROP_DOWN (button plus arrow). */
public final class SplitButtonExample extends RwtExample {

	public SplitButtonExample () {
		super ("SPLIT_BUTTON", "org.eclipse.swt.widgets.ToolItem (SWT.DROP_DOWN)");
	}

	@Override
	public Control create (Composite parent) {
		ToolBar bar = new ToolBar (parent, SWT.FLAT);
		ToolItem item = new ToolItem (bar, SWT.DROP_DOWN);
		item.setText ("Run");
		new ToolItem (bar, SWT.PUSH).setText ("Stop");
		return bar;
	}

	@Override
	public void change (Control control) {
		((ToolBar) control).getItem (0).setText ("Run " + next ());
	}
}
