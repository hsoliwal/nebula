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

import org.eclipse.nebula.widgets.m3.M3StatusBar;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** STATUS_BAR on RWT: M3StatusBar with three drawn slots; the resize grip is off (it needs pointer moves). */
public final class StatusBarExample extends RwtExample {

	public StatusBarExample () {
		super ("STATUS_BAR", "org.eclipse.nebula.widgets.m3.M3StatusBar");
	}

	@Override
	public Control create (Composite parent) {
		M3StatusBar bar = new M3StatusBar (parent, SWT.NONE);
		bar.setText (bar.addSlot (M3StatusBar.FILL, 0), "Ready");
		bar.setText (bar.addSlot (M3StatusBar.FIXED, 120), "Line 1, Column 1");
		bar.setText (bar.addSlot (M3StatusBar.PREFERRED, 0), "UTF-8");
		bar.setGrip (false);
		return bar;
	}

	@Override
	public void change (Control control) {
		((M3StatusBar) control).setText (0, "Saved");
	}
}
