/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.nebula.visualization.xygraph.figures;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.nebula.visualization.xygraph.dataprovider.ISample;
import org.eclipse.nebula.visualization.xygraph.dataprovider.PackedXYDataProvider;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Differential test of the decimated trace paint against the per-sample loop of the same {@link Trace}: for seeded
 * random data and views (zoom, pan, inverted and log axes, clipped edges, duplicates, flat runs, spikes) every
 * {@code drawPolyline} call, the hot sample list and the {@code Control.print} pixels must be identical.
 * <p>
 * Run with {@code -Dorg.eclipse.nebula.visualization.xygraph.decimation.minSamples=2} and
 * {@code -Dorg.eclipse.nebula.visualization.xygraph.decimation.waitMillis=60000} so small views take the decimated
 * path and the UI waits for every worker result.
 */
public class TraceDecimationDifferentialTest {

	static final class RecordingGraphics extends SWTGraphics {
		final List<int[]> polylines = new ArrayList<>();

		RecordingGraphics(GC gc) {
			super(gc);
		}

		@Override
		public void drawPolyline(PointList points) {
			polylines.add(points.toIntArray().clone());
			super.drawPolyline(points);
		}
	}

	private Display display;
	private Shell shell;
	private Canvas canvas;
	private XYGraph graph;
	private Trace trace;
	private PackedXYDataProvider provider;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
		LightweightSystem lws = new LightweightSystem(canvas);
		graph = new XYGraph();
		graph.setShowLegend(false);
		lws.setContents(graph);
		provider = new PackedXYDataProvider();
		trace = new Trace("t", graph.getPrimaryXAxis(), graph.getPrimaryYAxis(), provider); //$NON-NLS-1$
		graph.addTrace(trace);
		graph.getPrimaryXAxis().setAutoScale(false);
		graph.getPrimaryYAxis().setAutoScale(false);
		shell.setSize(800, 600);
		shell.open();
		settle();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	private void settle() {
		while (display.readAndDispatch()) {
			// drain
		}
	}

	/** Paints the trace once into an offscreen image and returns the recorded polylines. */
	private List<int[]> record() {
		Image img = new Image(display, 4, 4);
		GC gc = new GC(img);
		RecordingGraphics g = new RecordingGraphics(gc);
		try {
			trace.paint(g);
		} finally {
			g.dispose();
			gc.dispose();
			img.dispose();
		}
		return g.polylines;
	}

	private static List<double[]> hot(Trace t) {
		List<double[]> out = new ArrayList<>();
		for (ISample s : t.getHotSampleList()) {
			out.add(new double[] { s.getXValue(), s.getYValue() });
		}
		return out;
	}

	private static void assertSamePolylines(String what, List<int[]> a, List<int[]> b) {
		assertEquals(what + " polyline count", a.size(), b.size());
		for (int i = 0; i < a.size(); i++) {
			assertArrayEquals(what + " polyline " + i, a.get(i), b.get(i));
		}
	}

	private static void assertSameHot(String what, List<double[]> a, List<double[]> b) {
		assertEquals(what + " hot count", a.size(), b.size());
		for (int i = 0; i < a.size(); i++) {
			assertArrayEquals(what + " hot " + i, a.get(i), b.get(i), 0.0);
		}
	}

	/** One scenario: same view painted by the loop and by the decimated path. Returns 1 if decimated. */
	private int compare(String what) {
		trace.setDecimationEnabled(false);
		List<int[]> loop = record();
		List<double[]> loopHot = hot(trace);
		trace.setDecimationEnabled(true);
		int before = trace.decimationPainter.decimatedFrames;
		List<int[]> fast = record();
		List<double[]> fastHot = hot(trace);
		assertSamePolylines(what, loop, fast);
		assertSameHot(what, loopHot, fastHot);
		return trace.decimationPainter.decimatedFrames - before;
	}

	static double[][] data(Random r, int n, int shape) {
		double[] x = new double[n];
		double[] y = new double[n];
		double cx = r.nextDouble() * 10 - 5;
		double cy = r.nextGaussian();
		for (int i = 0; i < n; i++) {
			int dup = shape & 1;
			cx += dup == 1 && r.nextInt(4) == 0 ? 0 : r.nextDouble() * (r.nextInt(50) == 0 ? 40 : 1);
			switch (shape >> 1) {
			case 0:
				cy += r.nextGaussian();
				break;
			case 1:
				cy = r.nextInt(30) == 0 ? r.nextGaussian() * 100 : cy + r.nextGaussian() * 0.01;
				break;
			case 2:
				cy = r.nextInt(8) == 0 ? Math.rint(r.nextGaussian() * 4) : cy;
				break;
			default:
				cy = Math.sin(i * 0.001) * 10 + r.nextGaussian() * 0.5;
				break;
			}
			x[i] = cx;
			y[i] = cy;
		}
		return new double[][] { x, y };
	}

	private void view(Random r, double[][] d, int mode) {
		double[] x = d[0];
		double[] y = d[1];
		int n = x.length;
		double xmin = x[0];
		double xmax = x[n - 1];
		double ymin = Double.POSITIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		for (double v : y) {
			ymin = Math.min(ymin, v);
			ymax = Math.max(ymax, v);
		}
		double span = Math.max(xmax - xmin, 1e-9);
		double lo = xmin;
		double hi = xmax;
		switch (mode % 5) {
		case 1: // zoom inside, edges between samples
			lo = xmin + span * r.nextDouble() * 0.4;
			hi = xmax - span * r.nextDouble() * 0.4;
			break;
		case 2: // edges exactly on samples
			lo = x[r.nextInt(n / 3 + 1)];
			hi = x[n - 1 - r.nextInt(n / 3 + 1)];
			break;
		case 3: // pan beyond the data
			lo = xmin - span * 0.3;
			hi = xmax - span * 0.2;
			break;
		case 4: // wider than the data
			lo = xmin - span;
			hi = xmax + span;
			break;
		default:
			break;
		}
		if (hi <= lo) {
			hi = lo + 1;
		}
		double ypad = (ymax - ymin) * (mode >= 5 && mode < 8 ? -0.2 : 0.05) + 1e-9;
		boolean invert = mode % 7 == 6;
		graph.getPrimaryXAxis().setRange(new Range(invert ? hi : lo, invert ? lo : hi));
		graph.getPrimaryYAxis().setRange(new Range(ymin - ypad, ymax + ypad));
		settle();
	}

	@Test
	public void decimatedPolylineEqualsLoopOnRandomViews() {
		Random r = new Random(20261013L);
		int[] sizes = { 2, 3, 17, 160, 1000, 5000, 20000, 70000 };
		int decimated = 0;
		int scenarios = 0;
		for (int round = 0; round < 6; round++) {
			for (int n : sizes) {
				for (int shape = 0; shape < 8; shape++) {
					double[][] d = data(r, n, shape);
					provider.setData(d[0], d[1], n);
					trace.setTraceType(shape % 3 == 0 ? Trace.TraceType.DASH_LINE : Trace.TraceType.SOLID_LINE);
					int mode = r.nextInt(10);
					view(r, d, mode);
					decimated += compare("round " + round + " n " + n + " shape " + shape + " mode " + mode); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					scenarios++;
				}
			}
		}
		System.out.println("TraceDecimationDifferentialTest scenarios=" + scenarios + " decimated=" + decimated); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("decimated path exercised", decimated > scenarios / 3); //$NON-NLS-1$
	}

	@Test
	public void decimatedPolylineEqualsLoopOnLogAxes() {
		Random r = new Random(7L);
		int decimated = 0;
		int scenarios = 0;
		for (int k = 0; k < 40; k++) {
			int n = 500 + r.nextInt(30000);
			double[][] d = data(r, n, r.nextInt(8));
			for (int i = 0; i < n; i++) {
				d[0][i] = Math.exp(d[0][i] * 1e-3) + 1e-3;
				d[1][i] = Math.exp(d[1][i] * 0.05);
			}
			for (int i = 1; i < n; i++) {
				d[0][i] = Math.max(d[0][i], d[0][i - 1]);
			}
			provider.setData(d[0], d[1], n);
			graph.getPrimaryXAxis().setLogScale(k % 2 == 0);
			graph.getPrimaryYAxis().setLogScale(k % 3 != 0);
			view(r, d, r.nextInt(5));
			decimated += compare("log k " + k); //$NON-NLS-1$
			scenarios++;
		}
		graph.getPrimaryXAxis().setLogScale(false);
		graph.getPrimaryYAxis().setLogScale(false);
		System.out.println("log scenarios=" + scenarios + " decimated=" + decimated); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(decimated > 0);
	}

	@Test
	public void appendsReuseThePyramidAndStayEqual() {
		Random r = new Random(11L);
		double[][] d = data(r, 200000, 0);
		int decimated = 0;
		for (int step = 0; step < 20; step++) {
			int from = step * 10000;
			provider.append(java.util.Arrays.copyOfRange(d[0], from, from + 10000),
					java.util.Arrays.copyOfRange(d[1], from, from + 10000), 0, 10000);
			double[][] sofar = { java.util.Arrays.copyOf(d[0], from + 10000), java.util.Arrays.copyOf(d[1], from + 10000) };
			view(r, sofar, step % 5);
			decimated += compare("append step " + step); //$NON-NLS-1$
		}
		assertTrue(decimated >= 15);
	}

	/** After a data replacement and before the next paint the hot list still answers for the last paint. */
	@Test
	public void hotListAfterReplacementMatchesLoop() {
		Random r = new Random(17L);
		double[][] a = data(r, 30000, 2);
		double[][] b = data(r, 20000, 4);
		List<List<double[]>> hot = new ArrayList<>();
		for (boolean decimate : new boolean[] { false, true }) {
			trace.setDecimationEnabled(decimate);
			provider.setData(a[0], a[1], a[0].length);
			view(r, a, 0);
			record();
			provider.setData(b[0], b[1], b[0].length);
			hot.add(hot(trace)); // before any event is dispatched: no repaint has happened yet
			settle();
		}
		assertSameHot("hot after replacement", hot.get(0), hot.get(1)); //$NON-NLS-1$
		assertTrue(hot.get(0).size() > 0);
	}

	/** Hidden widget: data keeps arriving, but nothing is painted, decimated or built until it is shown. */
	@Test
	public void noDecimationWorkWhileHidden() {
		Random r = new Random(13L);
		double[][] d = data(r, 100000, 0);
		provider.setData(d[0], d[1], 50000);
		view(r, new double[][] { java.util.Arrays.copyOf(d[0], 50000), java.util.Arrays.copyOf(d[1], 50000) }, 0);
		canvas.redraw();
		canvas.update();
		settle();
		TraceDecimationPainter p = trace.decimationPainter;
		int decimated = p.decimatedFrames;
		int loop = p.loopFrames;
		long[] timings = p.lastTimings();
		shell.setVisible(false);
		settle();
		for (int k = 0; k < 50; k++) {
			provider.append(java.util.Arrays.copyOfRange(d[0], 50000 + k * 1000, 51000 + k * 1000),
					java.util.Arrays.copyOfRange(d[1], 50000 + k * 1000, 51000 + k * 1000), 0, 1000);
			settle();
		}
		assertEquals("decimated frames while hidden", decimated, p.decimatedFrames); //$NON-NLS-1$
		assertEquals("loop frames while hidden", loop, p.loopFrames); //$NON-NLS-1$
		assertArrayEquals("no worker task while hidden", timings, p.lastTimings()); //$NON-NLS-1$
		shell.setVisible(true);
		canvas.redraw();
		canvas.update();
		settle();
		assertTrue("caught up once when shown", p.decimatedFrames > decimated); //$NON-NLS-1$
	}

	/** Control.print pixels of the whole graph, loop vs decimated, antialias on and off. */
	@Test
	public void printedPixelsAreIdentical() {
		Random r = new Random(5L);
		int decimated = 0;
		for (int k = 0; k < 24; k++) {
			int n = 2000 + r.nextInt(200000);
			double[][] d = data(r, n, k % 8);
			provider.setData(d[0], d[1], n);
			trace.setAntiAliasing(k % 2 == 0);
			trace.setLineWidth(1 + k % 3);
			view(r, d, k % 5);
			trace.setDecimationEnabled(false);
			byte[] loop = print();
			trace.setDecimationEnabled(true);
			int before = trace.decimationPainter.decimatedFrames;
			byte[] fast = print();
			decimated += trace.decimationPainter.decimatedFrames - before > 0 ? 1 : 0;
			assertArrayEquals("print pixels k=" + k, loop, fast); //$NON-NLS-1$
		}
		System.out.println("print scenarios=24 decimated=" + decimated); //$NON-NLS-1$
		assertTrue(decimated > 12);
	}

	private byte[] print() {
		canvas.redraw();
		canvas.update();
		settle();
		org.eclipse.swt.graphics.Point size = canvas.getSize();
		Image img = new Image(display, size.x, size.y);
		GC gc = new GC(img);
		try {
			canvas.print(gc);
		} finally {
			gc.dispose();
		}
		ImageData id = img.getImageData();
		img.dispose();
		return id.data;
	}
}
