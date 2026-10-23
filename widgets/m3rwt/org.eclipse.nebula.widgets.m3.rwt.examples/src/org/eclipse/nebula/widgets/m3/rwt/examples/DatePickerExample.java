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
import org.eclipse.swt.widgets.DateTime;

/** DATE_PICKER on RWT: DateTime with SWT.DATE and SWT.DROP_DOWN (calendar pop-up). */
public final class DatePickerExample extends RwtExample {

	public DatePickerExample () {
		super ("DATE_PICKER", "org.eclipse.swt.widgets.DateTime (SWT.DATE | SWT.DROP_DOWN)");
	}

	@Override
	public Control create (Composite parent) {
		DateTime date = new DateTime (parent, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
		date.setDate (2026, 9, 20);
		return date;
	}

	@Override
	public void change (Control control) {
		((DateTime) control).setDate (2026, 11, next () % 28 + 1);
	}
}
