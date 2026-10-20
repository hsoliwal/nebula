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
package org.eclipse.nebula.widgets.m3.example;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.examples.AbstractExampleTab;
import org.eclipse.nebula.widgets.m3.M3ErrorPane;
import org.eclipse.nebula.widgets.m3.jface.M3StatusPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Nebula examples tab (an Eclipse view) for {@link M3ErrorPane} bound to an Eclipse {@link IStatus} through
 * {@link M3StatusPane}: a multi-status with a failing child; the details (status tree and stack traces) are formatted
 * only when shown.
 */
public class M3ErrorPaneExampleTab extends AbstractExampleTab {

	private M3ErrorPane pane;

	static IStatus status() {
		MultiStatus status = new MultiStatus("org.eclipse.nebula.widgets.m3.example", 0, "Sorting 10^6 rows failed", null); //$NON-NLS-1$ //$NON-NLS-2$
		status.add(Status.info("index built in 1.2 s")); //$NON-NLS-1$
		status.add(Status.error("comparator threw", new IllegalStateException("column 3 has no comparator"))); //$NON-NLS-1$ //$NON-NLS-2$
		return status;
	}

	@Override
	public Control createControl(Composite parent) {
		pane = new M3ErrorPane(parent, SWT.BORDER);
		M3StatusPane.show(pane, status());
		return pane;
	}

	@Override
	public void createParameters(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		Button dialog = new Button(parent, SWT.PUSH);
		dialog.setText("Open as dialog"); //$NON-NLS-1$
		dialog.addListener(SWT.Selection, e -> M3StatusPane.open(parent.getShell(), "Sort failed", status())); //$NON-NLS-1$
	}

	@Override
	public String[] createLinks() {
		return new String[] { "https://github.com/hsoliwal/nebula/tree/m3/nebula-widgets-viewport-20261019/widgets/m3" }; //$NON-NLS-1$
	}
}
