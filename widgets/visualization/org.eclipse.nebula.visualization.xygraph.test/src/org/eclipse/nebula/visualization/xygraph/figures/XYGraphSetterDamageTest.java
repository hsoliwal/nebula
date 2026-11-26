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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.ErrorBarType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
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
	private int nativePaints;
	private long lastNativePaintNanos;
	private ImageData lastPixelCapture;

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
		canvas.addPaintListener(event -> {
			nativePaints++;
			lastNativePaintNanos = System.nanoTime();
		});
		shell.open();
		spin();
		awaitNativePaintAfter(0, "initial graph paint");

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
		int before = nativePaints;
		change.run();
		// Require a real SWT Paint from the setter; never synthesize one.
		awaitNativePaintAfter(before, name + " did not repaint automatically");
		int[] shown = pixels();
		ImageData automaticImage = lastPixelCapture;

		int referenceBefore = nativePaints;
		graph.revalidate();
		canvas.redraw();
		awaitNativePaintAfter(referenceBefore, name + " reference full redraw did not paint");
		// No tolerances: an automatic repaint must have exactly the pixels
		// produced by a subsequently requested complete redraw.
		try {
			assertArrayEquals(name + ": the change shows without another repaint", pixels(), shown);
		} catch (AssertionError original) {
			// The original strict pixel assertion has already failed. Capture
			// native repeatability evidence without changing its verdict.
			try {
				diagnoseNativePixelFailure(name, shown, automaticImage, lastPixelCapture);
			} catch (RuntimeException | Error diagnostic) {
				if (diagnostic != original) original.addSuppressed(diagnostic);
			}
			throw original;
		}
	}

	/**
	 * Diagnostic-only after strict pixel failure. Distinguishes different
	 * images captured without an intervening paint from non-idempotence under
	 * a second explicit native full redraw. The original JUnit failure remains
	 * mandatory and is rethrown after this method, without any pixel tolerance.
	 */
	private void diagnoseNativePixelFailure(String name, int[] automatic,
			ImageData automaticImage, ImageData firstFullImage) {
		int[] firstFull = imagePixels(firstFullImage);
		int[] sameScreen = pixels();
		ImageData sameScreenImage = lastPixelCapture;
		int priorPaint = nativePaints;
		canvas.redraw();
		awaitNativePaintAfter(priorPaint, name + " diagnostic full redraw did not paint");
		int[] secondFull = pixels();
		ImageData secondFullImage = lastPixelCapture;

		int first = firstDifference(automatic, firstFull);
		int width = canvas.getBounds().width;
		System.err.println("M3_XYGRAPH_NATIVE_PIXEL_DIAGNOSTIC"
				+ " setter=" + name
				+ " firstIndex=" + first
				+ " x=" + (first < 0 ? -1 : first % width)
				+ " y=" + (first < 0 ? -1 : first / width)
				+ " autoPixel=" + pixelAt(automatic, first)
				+ " fullPixel=" + pixelAt(firstFull, first)
				+ " rereadPixel=" + pixelAt(sameScreen, first)
				+ " secondFullPixel=" + pixelAt(secondFull, first)
				+ " autoVsFull=" + differences(automatic, firstFull)
				+ " fullVsSameScreen=" + differences(firstFull, sameScreen)
				+ " fullVsSecondFull=" + differences(firstFull, secondFull)
				+ " autoVsSecondFull=" + differences(automatic, secondFull)
				+ " nativePaints=" + nativePaints);
		saveFailureImage(name, "automatic", automaticImage);
		saveFailureImage(name, "reference-full", firstFullImage);
		saveFailureImage(name, "reference-no-paint", sameScreenImage);
		saveFailureImage(name, "reference-full-repeat", secondFullImage);
	}

	private static String pixelAt(int[] values, int at) {
		return at < 0 || at >= values.length ? "none"
				: String.format("0x%08x", values[at]);
	}

	private static int[] imagePixels(ImageData image) {
		int[] result = new int[image.width * image.height];
		for (int y = 0; y < image.height; y++)
			image.getPixels(0, y, image.width, result, y * image.width);
		return result;
	}

	private static int firstDifference(int[] first, int[] second) {
		for (int i = 0; i < Math.min(first.length, second.length); i++)
			if (first[i] != second[i]) return i;
		return first.length == second.length ? -1 : Math.min(first.length, second.length);
	}

	private static int differences(int[] first, int[] second) {
		int count = Math.abs(first.length - second.length);
		for (int i = 0; i < Math.min(first.length, second.length); i++)
			if (first[i] != second[i]) count++;
		return count;
	}

	/** Save the exact native-screen ImageData already captured for the oracle. */
	private static void saveFailureImage(String name, String phase, ImageData image) {
		String dir = System.getProperty("m3.test.pixels.dir");
		if (dir == null || dir.isBlank()) return;
		try {
			Path output = Path.of(dir);
			Files.createDirectories(output);
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { image };
			loader.save(output.resolve("xygraph-native-" + name.replaceAll("[^A-Za-z0-9_-]", "_")
					+ "-" + phase + ".png").toString(), SWT.IMAGE_PNG);
		} catch (IOException | RuntimeException failure) {
			// The original strict native-pixel failure must remain authoritative.
			System.err.println("M3_XYGRAPH_DIAGNOSTIC_PNG_UNAVAILABLE " + failure);
		}
	}

	/** Waits for an actual native paint and a quiet GDK frame interval. */
	private void awaitNativePaintAfter(int before, String message) {
		long started = System.nanoTime();
		long quietSince = started;
		int observed = nativePaints;
		while (nativePaints <= before || System.nanoTime() - quietSince < 200_000_000L) {
			if (System.nanoTime() - started > 5_000_000_000L) {
				throw new AssertionError(message + ": " + nativePaints
						+ " observed Paint events (previously " + before + ")");
			}
			canvas.update();
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5L);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new AssertionError("interrupted awaiting native XYGraph Paint", interrupted);
				}
			}
			if (nativePaints != observed) {
				observed = nativePaints;
				quietSince = lastNativePaintNanos;
			}
		}
	}

	private int[] pixels() {
		Rectangle size = canvas.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(canvas);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData image = shot.getImageData();
		shot.dispose();
		lastPixelCapture = image;
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
