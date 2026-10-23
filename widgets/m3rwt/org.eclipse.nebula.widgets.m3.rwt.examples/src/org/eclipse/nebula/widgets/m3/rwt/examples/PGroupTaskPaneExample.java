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

import org.eclipse.nebula.widgets.pgroup.PGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** TASK_PANE on RWT with the Nebula look: three PGroups (drawn headers, RAP child controls). */
public final class PGroupTaskPaneExample extends RwtExample {

	public PGroupTaskPaneExample () {
		super ("TASK_PANE", "org.eclipse.nebula.widgets.pgroup.PGroup", "PGROUP");
	}

	@Override
	public Control create (Composite parent) {
		Composite pane = new Composite (parent, SWT.NONE);
		pane.setLayout (new GridLayout ());
		for (int i = 0; i < 3; i++) {
			PGroup group = new PGroup (pane, SWT.SMOOTH);
			group.setText ("Section " + i);
			group.setLayout (new FillLayout ());
			new Label (group, SWT.NONE).setText ("Tasks of section " + i);
			group.setExpanded (i == 0);
		}
		return pane;
	}

	@Override
	public void change (Control control) {
		PGroup group = (PGroup) ((Composite) control).getChildren ()[1];
		group.setExpanded (!group.getExpanded ());
	}
}
