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
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;

/** TASK_PANE on RWT (RAP-native fallback): ExpandBar with three collapsible sections. */
public final class TaskPaneExample extends RwtExample {

	public TaskPaneExample () {
		super ("TASK_PANE", "org.eclipse.swt.widgets.ExpandBar");
	}

	@Override
	public Control create (Composite parent) {
		ExpandBar bar = new ExpandBar (parent, SWT.V_SCROLL);
		for (int i = 0; i < 3; i++) {
			Label body = new Label (bar, SWT.WRAP);
			body.setText ("Tasks of section " + i);
			ExpandItem item = new ExpandItem (bar, SWT.NONE);
			item.setText ("Section " + i);
			item.setControl (body);
			item.setHeight (60);
			item.setExpanded (i == 0);
		}
		return bar;
	}

	@Override
	public void change (Control control) {
		((ExpandBar) control).getItem (1).setExpanded (true);
	}
}
