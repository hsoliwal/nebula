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

import org.eclipse.rap.rwt.application.EntryPoint;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Shows one example in a maximized shell without trim. */
final class RwtExampleEntryPoint implements EntryPoint {

	private final RwtExample example;

	RwtExampleEntryPoint (RwtExample example) {
		this.example = example;
	}

	@Override
	public int createUI () {
		Display display = new Display ();
		Shell shell = new Shell (display, SWT.NO_TRIM);
		shell.setMaximized (true);
		shell.setLayout (new FillLayout ());
		example.create (shell);
		shell.layout ();
		shell.open ();
		return 0;
	}
}
