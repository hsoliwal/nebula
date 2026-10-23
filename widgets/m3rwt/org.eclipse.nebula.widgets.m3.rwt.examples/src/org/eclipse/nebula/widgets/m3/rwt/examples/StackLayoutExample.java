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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** STACK_LAYOUT on RWT: StackLayout over 50 pages; only the top page is visible. */
public final class StackLayoutExample extends RwtExample {

	public StackLayoutExample () {
		super ("STACK_LAYOUT", "org.eclipse.swt.custom.StackLayout");
	}

	@Override
	public Control create (Composite parent) {
		Composite stack = new Composite (parent, SWT.BORDER);
		StackLayout layout = new StackLayout ();
		stack.setLayout (layout);
		for (int i = 0; i < 50; i++) new Label (stack, SWT.NONE).setText ("Page " + i);
		layout.topControl = stack.getChildren ()[0];
		stack.layout ();
		return stack;
	}

	@Override
	public void change (Control control) {
		Composite stack = (Composite) control;
		((StackLayout) stack.getLayout ()).topControl = stack.getChildren ()[next () % 50];
		stack.layout ();
	}
}
