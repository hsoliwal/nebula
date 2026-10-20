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
import org.eclipse.swt.widgets.Table;

/** CHECK_LIST on RWT: Table SWT.CHECK, virtual, 100,000 rows. */
public final class CheckListExample extends RwtExample {

	public CheckListExample () {
		super ("CHECK_LIST", "org.eclipse.swt.widgets.Table (SWT.CHECK)");
	}

	@Override
	public Control create (Composite parent) {
		return VirtualTables.create (parent, SWT.CHECK, 100_000, 2);
	}

	@Override
	public void change (Control control) {
		Table table = (Table) control;
		table.setItemCount (table.getItemCount () + 1);
	}
}
