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

import org.eclipse.nebula.widgets.ctreecombo.CTreeCombo;
import org.eclipse.nebula.widgets.ctreecombo.CTreeComboItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * CTreeCombo measurement. Usage: CTreeComboBench &lt;image&gt; &lt;roots&gt; &lt;kids&gt; &lt;combos&gt; &lt;out.tsv&gt;
 */
public final class CTreeComboBench {
	private CTreeComboBench() {
	}

	public static void main(String[] args) throws Exception {
		String image = args[0];
		int roots = Integer.parseInt(args[1]);
		int kids = Integer.parseInt(args[2]);
		int combos = Integer.parseInt(args[3]);
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
			new CTreeCombo(many, SWT.BORDER | SWT.READ_ONLY);
		}
		clock.put("construct" + combos, BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("shells.afterConstruct", display.getShells().length, "count");
		clock.memory("constructed");
		many.dispose();
		BenchClock.pump(display);

		CTreeCombo combo = new CTreeCombo(shell, SWT.BORDER | SWT.READ_ONLY);
		clock.put("nodes", (double) roots * (kids + 1), "count");
		t0 = System.nanoTime();
		for (int i = 0; i < roots; i++) {
			CTreeComboItem root = new CTreeComboItem(combo, SWT.NONE);
			root.setText("n" + i);
			for (int j = 0; j < kids; j++) {
				new CTreeComboItem(root, SWT.NONE).setText("n" + i + "." + j);
			}
		}
		clock.put("populate", BenchClock.ms(System.nanoTime() - t0), "ms");
		t0 = System.nanoTime();
		shell.open();
		shell.layout(true, true);
		BenchClock.pump(display);
		clock.put("firstPaint", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.memory("populated");

		Method drop = CTreeCombo.class.getDeclaredMethod("dropDown", boolean.class);
		drop.setAccessible(true);
		long[] opens = new long[5];
		for (int i = 0; i < opens.length; i++) {
			long s = System.nanoTime();
			drop.invoke(combo, Boolean.TRUE);
			BenchClock.pump(display);
			opens[i] = System.nanoTime() - s;
			drop.invoke(combo, Boolean.FALSE);
			BenchClock.pump(display);
		}
		clock.frames("openDropDown", opens);
		clock.memory("opened");
		combo.dispose();
		BenchClock.pump(display);

		CTreeCombo virtual = new CTreeCombo(shell, SWT.BORDER | SWT.READ_ONLY | SWT.VIRTUAL);
		for (int i = 0; i < roots; i++) {
			CTreeComboItem root = new CTreeComboItem(virtual, SWT.NONE);
			root.setText("n" + i);
			for (int j = 0; j < kids; j++) {
				new CTreeComboItem(root, SWT.NONE).setText("n" + i + "." + j);
			}
		}
		shell.layout(true, true);
		BenchClock.pump(display);
		java.lang.reflect.Field treeField = CTreeCombo.class.getDeclaredField("tree");
		treeField.setAccessible(true);
		long[] vopens = new long[5];
		for (int i = 0; i < vopens.length; i++) {
			long s = System.nanoTime();
			drop.invoke(virtual, Boolean.TRUE);
			BenchClock.pump(display);
			vopens[i] = System.nanoTime() - s;
			if (i == 0) {
				org.eclipse.swt.widgets.Tree t = (org.eclipse.swt.widgets.Tree) treeField.get(virtual);
				clock.put("virtual.supported", (t.getStyle() & SWT.VIRTUAL) != 0 ? 1 : 0, "bool");
				int bound = 0;
				for (org.eclipse.swt.widgets.TreeItem row : t.getItems()) {
					if (row.getData("org.eclipse.nebula.widgets.ctreecombo.CTreeComboItem") != null) {
						bound++;
					}
				}
				clock.put("virtual.boundRootRows", bound, "rows");
			}
			drop.invoke(virtual, Boolean.FALSE);
			BenchClock.pump(display);
		}
		clock.frames("virtual.openDropDown", vopens);
		clock.memory("virtual");
		clock.write(args[4]);
		shell.dispose();
		display.dispose();
	}
}
