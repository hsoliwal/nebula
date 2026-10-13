/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.m3.tt;

import org.eclipse.nebula.widgets.calendarcombo.CalendarCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * CalendarCombo measurement: N flat and N native combos. Usage: CalendarComboBench &lt;image&gt; &lt;combos&gt; &lt;out.tsv&gt;
 */
public final class CalendarComboBench {
	private CalendarComboBench() {
	}

	public static void main(String[] args) throws Exception {
		String image = args[0];
		int combos = Integer.parseInt(args[1]);
		BenchClock clock = new BenchClock(image);
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(10, false));
		shell.setBounds(20, 20, 900, 700);
		clock.memory("baseline");
		long t0 = System.nanoTime();
		for (int i = 0; i < combos; i++) {
			new CalendarCombo(shell, SWT.READ_ONLY | SWT.FLAT);
		}
		clock.put("constructFlat" + combos, BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("shells.afterConstructFlat", display.getShells().length, "count");
		t0 = System.nanoTime();
		for (int i = 0; i < combos; i++) {
			new CalendarCombo(shell, SWT.READ_ONLY);
		}
		clock.put("constructNative" + combos, BenchClock.ms(System.nanoTime() - t0), "ms");
		t0 = System.nanoTime();
		shell.open();
		shell.layout(true, true);
		BenchClock.pump(display);
		clock.put("firstPaint", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("shells.afterPaint", display.getShells().length, "count");
		clock.memory("painted");
		clock.write(args[2]);
		shell.dispose();
		display.dispose();
	}
}
