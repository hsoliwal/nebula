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
import java.util.List;

import org.eclipse.nebula.widgets.ctree.CTree;
import org.eclipse.nebula.widgets.ctree.CTreeColumn;
import org.eclipse.nebula.widgets.ctree.CTreeItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * CTree measurement: R roots x K children. Usage: CTreeBench &lt;image&gt; &lt;roots&gt; &lt;kids&gt; &lt;out.tsv&gt;
 */
public final class CTreeBench {
	private CTreeBench() {
	}

	public static void main(String[] args) throws Exception {
		String image = args[0];
		int roots = Integer.parseInt(args[1]);
		int kids = Integer.parseInt(args[2]);
		BenchClock clock = new BenchClock(image);
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(20, 20, 900, 700);
		clock.put("nodes", (double) roots * (kids + 1), "count");
		clock.memory("baseline");

		long t0 = System.nanoTime();
		CTree tree = tree(shell, roots, kids);
		clock.put("populate", BenchClock.ms(System.nanoTime() - t0), "ms");

		t0 = System.nanoTime();
		shell.open();
		BenchClock.pump(display);
		tree.update();
		BenchClock.pump(display);
		clock.put("firstPaint", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("painted.afterFirstPaint", painted(tree), "rows");
		clock.memory("populated");

		CTreeItem[] rootItems = tree.getItems();
		long[] frames = new long[120];
		for (int i = 0; i < frames.length; i++) {
			int index = (int) ((long) i * (roots - 1) / (frames.length - 1));
			long s = System.nanoTime();
			tree.setTopItem(rootItems[index]);
			tree.update();
			BenchClock.pump(display);
			frames[i] = System.nanoTime() - s;
		}
		clock.frames("scrollFrame", frames);
		tree.setTopItem(rootItems[0]);
		BenchClock.pump(display);
		t0 = System.nanoTime();
		tree.setTopItem(rootItems[roots - 1]);
		tree.update();
		BenchClock.pump(display);
		clock.put("scrollToEnd", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("painted.afterScroll", painted(tree), "rows");

		tree.setTopItem(rootItems[0]);
		BenchClock.pump(display);
		long[] expand = new long[20];
		for (int i = 0; i < expand.length; i++) {
			long s = System.nanoTime();
			rootItems[i].setExpanded(true);
			tree.update();
			BenchClock.pump(display);
			expand[i] = System.nanoTime() - s;
		}
		clock.frames("expandFrame", expand);
		clock.memory("scrolled");

		Shell small = new Shell(display);
		small.setLayout(new FillLayout());
		small.setBounds(40, 40, 600, 500);
		CTree second = tree(small, 1000, 9);
		small.open();
		BenchClock.pump(display);
		t0 = System.nanoTime();
		for (CTreeItem item : second.getItems()) {
			item.setExpanded(true);
		}
		second.update();
		BenchClock.pump(display);
		clock.put("expandAll10k", BenchClock.ms(System.nanoTime() - t0), "ms");
		small.dispose();

		clock.write(args[3]);
		shell.dispose();
		display.dispose();
	}

	static CTree tree(Shell shell, int roots, int kids) {
		CTree tree = new CTree(shell, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		CTreeColumn name = new CTreeColumn(tree, SWT.LEFT);
		name.setText("Name");
		name.setWidth(300);
		tree.setHeaderVisible(true);
		for (int i = 0; i < roots; i++) {
			CTreeItem root = new CTreeItem(tree, SWT.NONE);
			root.setText("n" + i);
			for (int j = 0; j < kids; j++) {
				new CTreeItem(root, SWT.NONE).setText("n" + i + "." + j);
			}
		}
		return tree;
	}

	private static int painted(CTree tree) {
		try {
			Method m = CTree.class.getDeclaredMethod("getPaintedItems");
			m.setAccessible(true);
			return ((List<?>) m.invoke(tree)).size();
		} catch (Exception ex) {
			return -1;
		}
	}
}
