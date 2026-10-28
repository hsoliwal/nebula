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

import org.eclipse.nebula.widgets.m3.M3MonthView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link M3MonthView} showing three months with an interval selection (shift-click), flagged release days and
 * unselectable weekends; the label follows the selection.
 * <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3MonthViewSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3MonthView");
		shell.setLayout(new GridLayout(1, false));
		M3MonthView view = new M3MonthView(shell, SWT.MULTI | SWT.BORDER);
		view.setMonthsShown(3, 1);
		LocalDate first = LocalDate.of(2026, 10, 1);
		view.setFirstDisplayedDay(first.toEpochDay());
		view.setToday(LocalDate.of(2026, 10, 20).toEpochDay());
		for (long day = first.toEpochDay(); day < first.plusMonths(3).toEpochDay(); day++) {
			int dow = LocalDate.ofEpochDay(day).getDayOfWeek().getValue();
			if (dow >= 6) {
				view.setUnselectable(day, true);
			}
			if (LocalDate.ofEpochDay(day).getDayOfMonth() == 15) {
				view.setFlagged(day, true);
			}
		}
		view.setSelectionInterval(LocalDate.of(2026, 10, 20).toEpochDay(), LocalDate.of(2026, 10, 23).toEpochDay());
		Label label = new Label(shell, SWT.NONE);
		label.setText("selected " + LocalDate.ofEpochDay(view.getSelectionStart()) + " .. " + LocalDate.ofEpochDay(view.getSelectionEnd()));
		view.addListener(SWT.Selection, e -> label.setText("selected " + LocalDate.ofEpochDay(view.getSelectionStart()) + " .. "
				+ LocalDate.ofEpochDay(view.getSelectionEnd())));
		shell.pack();
		shell.open();
		Screenshot.runOrSave(shell, display);
	}
}
