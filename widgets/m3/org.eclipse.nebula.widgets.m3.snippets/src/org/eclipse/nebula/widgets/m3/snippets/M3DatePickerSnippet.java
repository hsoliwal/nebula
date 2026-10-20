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
package org.eclipse.nebula.widgets.m3.snippets;

import java.time.LocalDate;

import org.eclipse.nebula.widgets.m3.M3DatePicker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Two {@link M3DatePicker}s (start and end); the drop-down month view of the end picker flags the start date. Type a
 * date and press Enter, or open the drop-down with the arrow (or Alt+Down).
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3DatePickerSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3DatePicker");
		shell.setLayout(new GridLayout(2, false));
		new Label(shell, SWT.NONE).setText("Start:");
		M3DatePicker start = new M3DatePicker(shell, SWT.NONE);
		new Label(shell, SWT.NONE).setText("End:");
		M3DatePicker end = new M3DatePicker(shell, SWT.NONE);
		start.setDate(LocalDate.of(2026, 10, 20).toEpochDay());
		end.setDate(LocalDate.of(2026, 11, 3).toEpochDay());
		start.addListener(SWT.Selection, e -> end.getMonthView().setFlagged(start.getDate(), true));
		end.getMonthView().setFlagged(start.getDate(), true);
		shell.pack();
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
