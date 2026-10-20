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

import org.eclipse.nebula.widgets.oscilloscope.multichannel.Oscilloscope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** CHART on RWT: Nebula's multichannel Oscilloscope with one channel of 400 samples. */
public final class ChartExample extends RwtExample {

	public ChartExample () {
		super ("CHART", "org.eclipse.nebula.widgets.oscilloscope.multichannel.Oscilloscope");
	}

	@Override
	public Control create (Composite parent) {
		Oscilloscope scope = new Oscilloscope (parent, SWT.NONE);
		scope.setTailSize (0, 400);
		for (int i = 0; i < 400; i++) scope.setValue (0, (int) (Math.sin (i / 20.0) * 40));
		return scope;
	}

	@Override
	public void change (Control control) {
		((Oscilloscope) control).setValue (0, 10);
		control.redraw ();
	}
}
