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

import org.eclipse.nebula.widgets.compositetable.CompositeTable;
import org.eclipse.nebula.widgets.compositetable.ResizableGridRowLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * CompositeTable measurement over a large collection. Usage: CompositeTableBench &lt;image&gt; &lt;rows&gt; &lt;out.tsv&gt;
 */
public final class CompositeTableBench {
	private CompositeTableBench() {
	}

	/** Row prototype. */
	public static final class Row extends Composite {
		final Text text;

		public Row(Composite parent, int style) {
			super(parent, style);
			setLayout(new ResizableGridRowLayout());
			text = new Text(this, SWT.BORDER | SWT.SINGLE);
		}
	}

	public static void main(String[] args) throws Exception {
		String image = args[0];
		int rows = Integer.parseInt(args[1]);
		BenchClock clock = new BenchClock(image);
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setBounds(20, 20, 600, 700);
		clock.memory("baseline");
		int[] refreshed = { 0 };
		long t0 = System.nanoTime();
		CompositeTable table = new CompositeTable(shell, SWT.NONE);
		new Row(table, SWT.NONE);
		table.addRowContentProvider((sender, offset, row) -> {
			refreshed[0]++;
			((Row) row).text.setText("row " + offset);
		});
		table.setNumRowsInCollection(rows);
		table.setRunTime(true);
		shell.layout(true, true);
		clock.put("populate", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("refreshed.beforeOpen", refreshed[0], "rows");
		t0 = System.nanoTime();
		shell.open();
		BenchClock.pump(display);
		shell.update();
		BenchClock.pump(display);
		clock.put("firstPaint", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("rowControls", table.getRowControls().length, "rows");
		clock.memory("populated");
		long[] frames = new long[120];
		for (int i = 0; i < frames.length; i++) {
			int top = (int) ((long) i * (rows - 1) / (frames.length - 1));
			long s = System.nanoTime();
			table.setTopRow(top);
			shell.update();
			BenchClock.pump(display);
			frames[i] = System.nanoTime() - s;
		}
		clock.frames("scrollFrame", frames);
		table.setTopRow(0);
		BenchClock.pump(display);
		t0 = System.nanoTime();
		table.setTopRow(rows - 1);
		shell.update();
		BenchClock.pump(display);
		clock.put("scrollToEnd", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("rowControls.afterScroll", table.getRowControls().length, "rows");
		clock.memory("scrolled");
		clock.write(args[2]);
		shell.dispose();
		display.dispose();
	}
}
