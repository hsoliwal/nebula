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
package org.eclipse.nebula.visualization.xygraph.figures;

import static org.junit.Assert.assertArrayEquals;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.ErrorBarType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget D leaf of the XYGraph setters: each Trace error-bar setter and the Legend border and preferred height
 * show at once, to the pixels of a full repaint (before, they showed at the next unrelated repaint).
 */
public class XYGraphSetterDamageTest {

	private Display display;
	private Shell shell;
	private Canvas canvas;
	private XYGraph graph;
	private Trace trace;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setSize(560, 420);
		canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
		canvas.setBounds(0, 0, 520, 380);
		graph = new XYGraph();
		CircularBufferDataProvider data = new CircularBufferDataProvider(false);
		data.setBufferSize(20);
		for (int i = 0; i < 20; i++) {
			data.addSample(new Sample(i, Math.sin(i / 3.0) * 10, 2, 3, 0.4, 0.3));
		}
		trace = new Trace("trace", graph.getPrimaryXAxis(), graph.getPrimaryYAxis(), data);
		trace.setPointStyle(Trace.PointStyle.CIRCLE);
		graph.addTrace(trace);
		graph.getPrimaryXAxis().setRange(-1, 21);
		graph.getPrimaryYAxis().setRange(-16, 16);
		new LightweightSystem(canvas).setContents(graph);
		shell.open();
		spin();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void theErrorBarSettersShowAtOnce() {
		step("setErrorBarEnabled", () -> trace.setErrorBarEnabled(true));
		step("setYErrorBarType", () -> trace.setYErrorBarType(ErrorBarType.PLUS));
		step("setXErrorBarType", () -> trace.setXErrorBarType(ErrorBarType.MINUS));
		step("setErrorBarCapWidth", () -> trace.setErrorBarCapWidth(12));
		step("setErrorBarColor", () -> trace.setErrorBarColor(display.getSystemColor(SWT.COLOR_MAGENTA)));
		step("setDrawYErrorInArea", () -> {
			trace.setTraceType(Trace.TraceType.SOLID_LINE);
			trace.setDrawYErrorInArea(true);
		});
	}

	@Test
	public void theLegendSettersShowAtOnce() {
		Legend legend = graph.getLegendMap().get(graph.getPrimaryYAxis());
		step("Legend.setDrawBorder", () -> legend.setDrawBorder(false));
		step("Legend.setPreferredHeight", () -> legend.setPreferredHeight(60));
	}

	private void step(String name, Runnable change) {
		change.run();
		spin();
		int[] shown = pixels();
		graph.revalidate();
		spin();
		canvas.redraw();
		canvas.update();
		spin();
		assertArrayEquals(name + ": the change shows without another repaint", pixels(), shown);
	}

	private int[] pixels() {
		Rectangle size = canvas.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(canvas);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData image = shot.getImageData();
		shot.dispose();
		int[] all = new int[size.width * size.height];
		for (int y = 0; y < size.height; y++) {
			image.getPixels(0, y, size.width, all, y * size.width);
		}
		return all;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 250;
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
}
