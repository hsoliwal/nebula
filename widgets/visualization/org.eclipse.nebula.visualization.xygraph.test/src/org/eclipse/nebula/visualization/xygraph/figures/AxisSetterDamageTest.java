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
 * Every-widget D leaf of the axis setters: the minor tick step hint, the minor ticks and the scale line show at once,
 * to the pixels of a full repaint (before, at the next unrelated repaint).
 */
public class AxisSetterDamageTest {

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
	public void theAxisSettersShowAtOnce() {
		Axis axis = graph.getPrimaryXAxis();
		step("setMinorTickMarkStepHint", () -> axis.setMinorTickMarkStepHint(3));
		step("setMinorTicksVisible", () -> axis.setMinorTicksVisible(false));
		step("setScaleLineVisible", () -> axis.setScaleLineVisible(false));
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
