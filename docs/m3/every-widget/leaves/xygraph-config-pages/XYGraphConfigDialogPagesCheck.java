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
 *******************************************************************************/

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.nebula.visualization.internal.xygraph.toolbar.ITraceConfigPage;
import org.eclipse.nebula.visualization.internal.xygraph.toolbar.XYGraphConfigDialog;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget XYGraphConfigDialog leaf (no test plugin covers the dialog): choosing a trace
 * lays out the page it shows, not the hidden pages of the other traces. Run with the bundle classes, draw2d, jface and an
 * SWT jar on the class path: java XYGraphConfigDialogPagesCheck. Prints PASSED or throws.
 */
public class XYGraphConfigDialogPagesCheck {

	static final int TRACES = 30;

	/* a layout that counts its layout passes and delegates to the page's own layout */
	static final class Counting extends Layout {
		final Layout delegate;
		int layouts;

		Counting(Layout delegate) {
			this.delegate = delegate;
		}

		@Override
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			return (Point) call("computeSize", new Class<?>[] { Composite.class, int.class, int.class, boolean.class },
					composite, wHint, hHint, flushCache);
		}

		@Override
		protected void layout(Composite composite, boolean flushCache) {
			layouts++;
			call("layout", new Class<?>[] { Composite.class, boolean.class }, composite, flushCache);
		}

		Object call(String name, Class<?>[] types, Object... args) {
			try {
				Method method = Layout.class.getDeclaredMethod(name, types);
				method.setAccessible(true);
				return method.invoke(delegate, args);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		XYGraph graph = new XYGraph();
		// hosted as in an application: the graph's colours come from its canvas
		new org.eclipse.draw2d.LightweightSystem(new org.eclipse.swt.widgets.Canvas(shell, SWT.NONE)).setContents(graph);
		graph.setTitle("pages");
		graph.getPrimaryXAxis().setTitle("x");
		graph.getPrimaryYAxis().setTitle("y");
		graph.getPrimaryXAxis().setFormatPattern("0.##"); // set by the first paint otherwise
		graph.getPrimaryYAxis().setFormatPattern("0.##");
		for (int i = 0; i < TRACES; i++) {
			graph.addTrace(new Trace("trace " + i, graph.getPrimaryXAxis(), graph.getPrimaryYAxis(),
					new CircularBufferDataProvider(false)));
		}
		XYGraphConfigDialog dialog = new XYGraphConfigDialog(shell, graph);
		dialog.setBlockOnOpen(false);
		dialog.open();
		spin(display);

		List<?> pages = (List<?>) field(dialog, "traceConfigPageList");
		Counting[] counters = new Counting[pages.size()];
		for (int i = 0; i < counters.length; i++) {
			Composite page = ((ITraceConfigPage) pages.get(i)).getComposite();
			counters[i] = new Counting(page.getLayout());
			page.setLayout(counters[i]);
		}
		Combo traces = (Combo) field(dialog, "traceCombo");
		int chosen = TRACES / 2;
		traces.select(chosen);
		traces.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
		spin(display);

		int hidden = 0;
		for (int i = 0; i < counters.length; i++) {
			if (i != chosen) {
				hidden += counters[i].layouts;
			}
		}
		System.out.println("layouts: shown page " + counters[chosen].layouts + ", hidden pages " + hidden);
		Control shown = ((ITraceConfigPage) pages.get(chosen)).getComposite();
		check(shown.getVisible() && counters[chosen].layouts > 0, "the chosen trace page is shown and laid out");
		check(hidden == 0, "choosing a trace lays out no hidden page (" + hidden + " layouts)");
		dialog.close();
		shell.dispose();
		System.out.println("PASSED");
	}

	static Object field(Object owner, String name) throws ReflectiveOperationException {
		Field field = XYGraphConfigDialog.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(owner);
	}

	static void spin(Display display) {
		long end = System.currentTimeMillis() + 300;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
