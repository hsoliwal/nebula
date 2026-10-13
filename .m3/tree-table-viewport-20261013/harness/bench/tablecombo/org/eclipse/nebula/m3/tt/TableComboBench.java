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

import java.lang.reflect.Method;

import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * TableCombo measurement. Usage: TableComboBench &lt;image&gt; &lt;rows&gt; &lt;combos&gt; &lt;out.tsv&gt;
 * <ul>
 * <li>construct: N empty combos (drop-down cost at construction vs first use)</li>
 * <li>eager: one combo populated with R rows through getTable(); computeSize; open drop-down; scroll to end</li>
 * <li>virtual (post image only): same R rows with SWT.VIRTUAL and SetData</li>
 * </ul>
 */
public final class TableComboBench {
	private TableComboBench() {
	}

	public static void main(String[] args) throws Exception {
		String image = args[0];
		int rows = Integer.parseInt(args[1]);
		int combos = Integer.parseInt(args[2]);
		BenchClock clock = new BenchClock(image);
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		shell.setBounds(20, 20, 600, 500);
		clock.memory("baseline");

		Composite many = new Composite(shell, SWT.NONE);
		many.setLayout(new GridLayout(10, false));
		long t0 = System.nanoTime();
		for (int i = 0; i < combos; i++) {
			new TableCombo(many, SWT.BORDER | SWT.READ_ONLY);
		}
		clock.put("construct" + combos, BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("shells.afterConstruct", display.getShells().length, "count");
		clock.memory("constructed");
		many.dispose();
		BenchClock.pump(display);

		TableCombo combo = new TableCombo(shell, SWT.BORDER | SWT.READ_ONLY);
		t0 = System.nanoTime();
		Table table = combo.getTable();
		new TableColumn(table, SWT.NONE).setWidth(200);
		for (int i = 0; i < rows; i++) {
			new TableItem(table, SWT.NONE).setText("row " + i);
		}
		clock.put("populate", BenchClock.ms(System.nanoTime() - t0), "ms");
		t0 = System.nanoTime();
		shell.open();
		shell.layout(true, true);
		BenchClock.pump(display);
		clock.put("firstPaint", BenchClock.ms(System.nanoTime() - t0), "ms");
		dropAndScroll(clock, display, combo, rows, "eager");
		clock.memory("eager");
		combo.dispose();
		BenchClock.pump(display);

		TableCombo virtual = new TableCombo(shell, SWT.BORDER | SWT.READ_ONLY | SWT.VIRTUAL);
		Table vt = virtual.getTable();
		boolean isVirtual = (vt.getStyle() & SWT.VIRTUAL) != 0;
		clock.put("virtual.supported", isVirtual ? 1 : 0, "bool");
		if (isVirtual) {
			int[] asked = { 0 };
			new TableColumn(vt, SWT.NONE).setWidth(200);
			vt.addListener(SWT.SetData, e -> {
				asked[0]++;
				((TableItem) e.item).setText("row " + e.index);
			});
			t0 = System.nanoTime();
			vt.setItemCount(rows);
			clock.put("virtual.populate", BenchClock.ms(System.nanoTime() - t0), "ms");
			shell.layout(true, true);
			BenchClock.pump(display);
			dropAndScroll(clock, display, virtual, rows, "virtual");
			clock.put("virtual.setDataCalls", asked[0], "rows");
			clock.memory("virtual");
		}
		clock.write(args[3]);
		shell.dispose();
		display.dispose();
	}

	private static void dropAndScroll(BenchClock clock, Display display, TableCombo combo, int rows, String label)
			throws Exception {
		Method drop = TableCombo.class.getDeclaredMethod("dropDown", boolean.class);
		drop.setAccessible(true);
		long t0 = System.nanoTime();
		combo.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		clock.put(label + ".computeSize", BenchClock.ms(System.nanoTime() - t0), "ms");
		t0 = System.nanoTime();
		drop.invoke(combo, Boolean.TRUE);
		combo.getTable().update();
		BenchClock.pump(display);
		clock.put(label + ".openDropDown", BenchClock.ms(System.nanoTime() - t0), "ms");
		Table table = combo.getTable();
		long[] frames = new long[60];
		for (int i = 0; i < frames.length; i++) {
			int index = (int) ((long) i * (rows - 1) / (frames.length - 1));
			long s = System.nanoTime();
			table.setTopIndex(index);
			table.update();
			BenchClock.pump(display);
			frames[i] = System.nanoTime() - s;
		}
		clock.frames(label + ".scrollFrame", frames);
		t0 = System.nanoTime();
		table.setTopIndex(0);
		table.update();
		table.setTopIndex(rows - 1);
		table.update();
		BenchClock.pump(display);
		clock.put(label + ".scrollToEnd", BenchClock.ms(System.nanoTime() - t0), "ms");
		drop.invoke(combo, Boolean.FALSE);
		BenchClock.pump(display);
	}
}
