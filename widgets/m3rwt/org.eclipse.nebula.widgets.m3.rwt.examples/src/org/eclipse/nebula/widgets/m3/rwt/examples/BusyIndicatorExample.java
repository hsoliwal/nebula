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

import org.eclipse.nebula.widgets.m3.M3BusyLabel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** BUSY_INDICATOR on RWT: M3BusyLabel (drawn throbber and text); the probe uses fixed frames. */
public final class BusyIndicatorExample extends RwtExample {

	public BusyIndicatorExample () {
		super ("BUSY_INDICATOR", "org.eclipse.nebula.widgets.m3.M3BusyLabel");
	}

	@Override
	public Control create (Composite parent) {
		M3BusyLabel label = new M3BusyLabel (parent, SWT.NONE);
		label.setText ("Loading 100,000 rows");
		label.setFrame (3);
		return label;
	}

	@Override
	public void change (Control control) {
		((M3BusyLabel) control).setFrame (next () % 8);
	}
}
