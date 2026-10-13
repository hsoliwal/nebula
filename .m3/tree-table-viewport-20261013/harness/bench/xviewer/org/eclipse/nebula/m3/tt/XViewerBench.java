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

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.XViewerLabelProvider;
import org.eclipse.nebula.widgets.xviewer.core.model.SortDataType;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerAlign;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

/**
 * XViewer measurement: R roots x K children (R*(K+1) nodes), SWT.VIRTUAL tree.
 * Usage: XViewerBench &lt;image-label&gt; &lt;roots&gt; &lt;childrenPerRoot&gt; &lt;out.tsv&gt;
 */
public final class XViewerBench {
	private XViewerBench() {
	}

	public static void main(String[] args) throws Exception {
		String image = args[0];
		int roots = Integer.parseInt(args[1]);
		int kids = Integer.parseInt(args[2]);
		BenchClock clock = new BenchClock(image);
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		shell.setBounds(20, 20, 900, 700);
		clock.put("nodes", (double) roots * (kids + 1), "count");
		clock.memory("baseline");

		XViewer viewer = viewer(shell, roots, kids);
		Integer[] input = Model.roots(roots);
		long t0 = System.nanoTime();
		viewer.setInputXViewer(input);
		clock.put("populate", BenchClock.ms(System.nanoTime() - t0), "ms");

		Tree tree = viewer.getTree();
		boolean[] painted = { false };
		tree.addListener(SWT.Paint, e -> painted[0] = true);
		t0 = System.nanoTime();
		shell.open();
		long deadline = t0 + 60_000_000_000L;
		while (System.nanoTime() < deadline) {
			if (!display.readAndDispatch()) {
				if (painted[0]) {
					break;
				}
				tree.update();
			}
		}
		clock.put("firstPaint", BenchClock.ms(System.nanoTime() - t0), "ms");
		clock.put("rootItems", tree.getItemCount(), "count");
		materialized(clock, viewer, "materialized.afterFirstPaint");
		clock.memory("populated");

		long[] frames = new long[120];
		for (int i = 0; i < frames.length; i++) {
			int index = (int) ((long) i * (roots - 1) / (frames.length - 1));
			long s = System.nanoTime();
			tree.setTopItem(tree.getItem(index));
			tree.update();
			BenchClock.pump(display);
			frames[i] = System.nanoTime() - s;
		}
		clock.frames("scrollFrame", frames);
		tree.setTopItem(tree.getItem(0));
		tree.update();
		t0 = System.nanoTime();
		tree.setTopItem(tree.getItem(roots - 1));
		tree.update();
		BenchClock.pump(display);
		clock.put("scrollToEnd", BenchClock.ms(System.nanoTime() - t0), "ms");
		materialized(clock, viewer, "materialized.afterScroll");

		tree.setTopItem(tree.getItem(0));
		tree.update();
		long[] expand = new long[60];
		for (int i = 0; i < expand.length; i++) {
			long s = System.nanoTime();
			viewer.setExpandedState(input[i], true);
			tree.update();
			BenchClock.pump(display);
			expand[i] = System.nanoTime() - s;
		}
		clock.frames("expandFrame", expand);
		materialized(clock, viewer, "materialized.afterExpand");
		clock.memory("scrolled");

		Shell small = new Shell(display);
		small.setLayout(new GridLayout(1, false));
		small.setBounds(40, 40, 600, 500);
		XViewer second = viewer(small, 1000, 9);
		second.setInputXViewer(Model.roots(1000));
		small.open();
		BenchClock.pump(display);
		t0 = System.nanoTime();
		second.expandAll();
		second.getTree().update();
		BenchClock.pump(display);
		clock.put("expandAll10k", BenchClock.ms(System.nanoTime() - t0), "ms");
		small.dispose();

		clock.write(args[3]);
		shell.dispose();
		display.dispose();
	}

	private static void materialized(BenchClock clock, XViewer viewer, String metric) {
		try {
			Method m = viewer.getClass().getMethod("getViewportMaterializedCount");
			clock.put(metric, ((Integer) m.invoke(viewer)).intValue(), "rows");
		} catch (NoSuchMethodException ex) {
			clock.put(metric, -2, "rows(no-api)");
		} catch (Exception ex) {
			clock.put(metric, -3, "rows(error)");
		}
	}

	static XViewer viewer(Shell shell, int roots, int kids) {
		Composite parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		XViewerColumn name = new XViewerColumn("tt.name", "Name", 200, XViewerAlign.Left, true, SortDataType.String,
				false, null);
		XViewerColumn value = new XViewerColumn("tt.value", "Value", 100, XViewerAlign.Left, true,
				SortDataType.Integer, false, null);
		XViewerFactory factory = new XViewerFactory("tt.bench") {
			@Override
			public boolean isAdmin() {
				return false;
			}
		};
		factory.registerColumns(name, value);
		XViewer viewer = new XViewer(parent, SWT.VIRTUAL | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER, factory);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new Model(roots, kids));
		viewer.setLabelProvider(new Labels(viewer));
		viewer.setForcePend(true);
		return viewer;
	}

	/** Integer ids: roots 0..R-1, child j of root r is R + r*K + j. */
	static final class Model implements ITreeContentProvider {
		private final int roots;
		private final int kids;

		Model(int roots, int kids) {
			this.roots = roots;
			this.kids = kids;
		}

		static Integer[] roots(int count) {
			Integer[] r = new Integer[count];
			for (int i = 0; i < count; i++) {
				r[i] = Integer.valueOf(i);
			}
			return r;
		}

		@Override
		public Object[] getElements(Object input) {
			return (Object[]) input;
		}

		@Override
		public Object[] getChildren(Object parent) {
			int id = ((Integer) parent).intValue();
			if (id >= roots) {
				return new Object[0];
			}
			Object[] c = new Object[kids];
			for (int j = 0; j < kids; j++) {
				c[j] = Integer.valueOf(roots + id * kids + j);
			}
			return c;
		}

		@Override
		public Object getParent(Object element) {
			int id = ((Integer) element).intValue();
			return id < roots ? null : Integer.valueOf((id - roots) / kids);
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((Integer) element).intValue() < roots && kids > 0;
		}
	}

	static final class Labels extends XViewerLabelProvider {
		Labels(XViewer viewer) {
			super(viewer);
		}

		@Override
		public Image getColumnImage(Object element, XViewerColumn xCol, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, XViewerColumn xCol, int columnIndex) {
			int id = ((Integer) element).intValue();
			return columnIndex == 0 ? "n" + id : Integer.toString(id % 97);
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}
	}
}
