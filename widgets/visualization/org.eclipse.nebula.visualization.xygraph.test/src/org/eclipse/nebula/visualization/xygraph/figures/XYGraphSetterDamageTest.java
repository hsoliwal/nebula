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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.Trace.ErrorBarType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Transform;
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
	/** Bounded audit of actual SWT.Paint event rectangles and graphics clipping. */
	private final String[] lastNativeClips = new String[32];
	/** Entry/after observations and capture boundaries; never an inferred stroke state. */
	private final String[] nativeStateLedger = new String[128];
	private int nativeStateRecords;
	private int nativePaintEntries;
	private int pixelCaptures;
	private String capturePhase = "initial";
	/** Owned clip copies share the existing entry counter and capture phase. */
	private final String[] nativePaintStates = new String[lastNativeClips.length];
	private final GC[] pendingPaintContexts = new GC[lastNativeClips.length];
	private final Region[] entryClipRegions = new Region[lastNativeClips.length];
	private final Region[] exitClipRegions = new Region[lastNativeClips.length];
	private final Rectangle[] paintClipDomains = new Rectangle[lastNativeClips.length];
	private String lastPixelCaptureState = "not-captured";
	private String unpairedPaintExit;

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
		// Register before Draw2D so this observes the incoming paint GC.
		canvas.addPaintListener(this::observePaintEntry);
		new LightweightSystem(canvas).setContents(graph);
		canvas.addPaintListener(event -> {
			nativePaints++;
			lastNativePaintNanos = System.nanoTime();
			// This listener runs after Draw2D; its SWT bounds are not the entry
			// native clip or a complete native clip region.
			Rectangle clip = event.gc.getClipping();
			lastNativeClips[(nativePaints - 1) % lastNativeClips.length] =
					"#" + nativePaints + " expose=" + event.x + "," + event.y + ","
					+ event.width + "x" + event.height + " gcClip=" + clip;
			observePaintExit(event);
		});
		shell.open();
		spin();
		awaitNativePaintAfter(0, "initial graph paint");

	}

	@After
	public void tearDown() {
		try {
			shell.dispose();
		} finally {
			releasePaintRegions();
		}
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
		capturePhase = name + "/automatic";
		int before = nativePaints;
		change.run();
		// Require a real SWT Paint from the setter; never synthesize one.
		awaitNativePaintAfter(before, name + " did not repaint automatically");
		int[] shown = pixels();
		ImageData automaticImage = lastPixelCapture;
		String automaticCapture = lastPixelCaptureState;

		capturePhase = name + "/reference-full";
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
				diagnoseNativePixelFailure(name, shown, automaticImage, lastPixelCapture,
						automaticCapture, lastPixelCaptureState);
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
			ImageData automaticImage, ImageData firstFullImage,
			String automaticCapture, String firstFullCapture) {
		int[] firstFull = imagePixels(firstFullImage);
		capturePhase = name + "/reference-no-paint";
		int[] sameScreen = pixels();
		ImageData sameScreenImage = lastPixelCapture;
		String sameScreenCapture = lastPixelCaptureState;
		capturePhase = name + "/reference-full-repeat";
		int priorPaint = nativePaints;
		canvas.redraw();
		awaitNativePaintAfter(priorPaint, name + " diagnostic full redraw did not paint");
		int[] secondFull = pixels();
		ImageData secondFullImage = lastPixelCapture;
		String secondFullCapture = lastPixelCaptureState;

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
		System.err.println("M3_XYGRAPH_NATIVE_DIFF_COORDS phase=auto-vs-full"
				+ " values=" + differenceCoordinates(automatic, firstFull, width, 48));
		System.err.println("M3_XYGRAPH_NATIVE_DIFF_COORDS phase=full-vs-second-full"
				+ " values=" + differenceCoordinates(firstFull, secondFull, width, 48));
		System.err.println("M3_XYGRAPH_NATIVE_PAINT_CLIPS " + lastPaintClips());
		System.err.println("M3_XYGRAPH_NATIVE_CAPTURE_PAINT_STATE " + lastNativeState());
		System.err.println("M3_XYGRAPH_NATIVE_CAPTURE_STATE " + automaticCapture);
		System.err.println("M3_XYGRAPH_NATIVE_CAPTURE_STATE " + firstFullCapture);
		System.err.println("M3_XYGRAPH_NATIVE_CAPTURE_STATE " + sameScreenCapture);
		System.err.println("M3_XYGRAPH_NATIVE_CAPTURE_STATE " + secondFullCapture);
		saveFailureImage(name, "automatic", automaticImage);
		saveFailureImage(name, "reference-full", firstFullImage);
		saveFailureImage(name, "reference-no-paint", sameScreenImage);
		saveFailureImage(name, "reference-full-repeat", secondFullImage);
		reportPaintStates();
	}

	/** Copy entry clip without setting GC state; query membership after all captures. */
	private void observePaintEntry(PaintEvent event) {
		long observed = System.nanoTime();
		int slot = nativePaintEntries++ % nativePaintStates.length;
		pendingPaintContexts[slot] = event.gc;
		nativePaintStates[slot] = "paint=" + nativePaintEntries + " phase=" + capturePhase
				+ " entryNanos=" + observed + " completedBefore=" + nativePaints
				+ " expose=" + event.x + "," + event.y + "," + event.width + "x" + event.height;
		try {
			releaseRegion(entryClipRegions, slot);
			releaseRegion(exitClipRegions, slot);
			paintClipDomains[slot] = canvas.getClientArea();
			entryClipRegions[slot] = new Region(display);
			event.gc.getClipping(entryClipRegions[slot]);
			nativePaintStates[slot] += " entryGcClip=" + event.gc.getClipping()
					+ " domain=" + paintClipDomains[slot];
		} catch (RuntimeException | Error unavailable) {
			releaseRegion(entryClipRegions, slot);
			nativePaintStates[slot] += " entryUnavailable=" + unavailable.getClass().getName();
		}
		recordPaintState("entry", event, nativePaintEntries, observed);
	}

	/** Match by active GC identity so bounded nested paints retain their entry. */
	private void observePaintExit(PaintEvent event) {
		long observed = System.nanoTime();
		for (int offset = 0; offset < Math.min(nativePaintEntries, nativePaintStates.length); offset++) {
			int slot = (nativePaintEntries - 1 - offset) % nativePaintStates.length;
			if (pendingPaintContexts[slot] != event.gc) continue;
			pendingPaintContexts[slot] = null;
			nativePaintStates[slot] += " exitPhase=" + capturePhase + " exitNanos=" + observed
					+ " completedAfter=" + nativePaints;
			try {
				exitClipRegions[slot] = new Region(display);
				event.gc.getClipping(exitClipRegions[slot]);
				nativePaintStates[slot] += " exitGcClip=" + event.gc.getClipping();
			} catch (RuntimeException | Error unavailable) {
				releaseRegion(exitClipRegions, slot);
				nativePaintStates[slot] += " exitUnavailable=" + unavailable.getClass().getName();
			}
			recordPaintState("after", event, nativePaintEntries - offset, observed);
			return;
		}
		unpairedPaintExit = "phase=" + capturePhase + " exitNanos=" + observed
				+ " completedAfter=" + nativePaints + " reason=no-retained-entry";
		recordPaintState("after", event, -1, observed);
	}

	/** Failure-only publication of frozen copies, never of the disposed event GC. */
	private void reportPaintStates() {
		int first = Math.max(0, nativePaintEntries - nativePaintStates.length);
		for (int sequence = first; sequence < nativePaintEntries; sequence++) {
			int slot = sequence % nativePaintStates.length;
			System.err.println("M3_XYGRAPH_NATIVE_PAINT_STATE " + nativePaintStates[slot]
					+ " entryRegion=" + clipRegionWitness(entryClipRegions[slot], paintClipDomains[slot])
					+ " exitRegion=" + clipRegionWitness(exitClipRegions[slot], paintClipDomains[slot]));
		}
		if (unpairedPaintExit != null) {
			System.err.println("M3_XYGRAPH_NATIVE_PAINT_UNPAIRED " + unpairedPaintExit);
		}
	}

	/** Exact integer-point membership within the recorded bounded canvas domain. */
	private static String clipRegionWitness(Region region, Rectangle domain) {
		if (region == null || region.isDisposed() || domain == null) return "unavailable";
		if (domain.width < 0 || domain.height < 0
				|| domain.width > 1_000_000 || domain.height > 1_000_000
				|| (long) domain.width * domain.height > 1_000_000L) return "unavailable:domain-budget";
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException unavailable) {
			return "unavailable:SHA-256";
		}
		byte[] row = new byte[domain.width];
		StringBuilder runs = new StringBuilder();
		int totalRuns = 0;
		int included = 0;
		for (int y = 0; y < domain.height && domain.width > 0; y++) {
			int start = -1;
			for (int x = 0; x <= domain.width; x++) {
				boolean contains = x < domain.width && region.contains(domain.x + x, domain.y + y);
				if (x < domain.width) row[x] = (byte) (contains ? 1 : 0);
				if (contains) {
					included++;
					if (start < 0) start = x;
				} else if (start >= 0) {
					if (totalRuns < 48) {
						if (totalRuns > 0) runs.append(';');
						runs.append(domain.y + y).append(':').append(domain.x + start)
								.append('+').append(x - start);
					}
					totalRuns++;
					start = -1;
				}
			}
			digest.update(row);
		}
		return "{bounds=" + region.getBounds() + ",domain=" + domain + ",included=" + included
				+ ",sha256=" + HexFormat.of().formatHex(digest.digest()) + ",totalRuns=" + totalRuns
				+ ",firstRuns=" + runs + (totalRuns > 48 ? ";truncated" : "") + "}";
	}

	private static void releaseRegion(Region[] regions, int slot) {
		Region region = regions[slot];
		regions[slot] = null;
		if (region != null) region.dispose();
	}

	private void releasePaintRegions() {
		for (int slot = 0; slot < entryClipRegions.length; slot++) {
			try {
				releaseRegion(entryClipRegions, slot);
			} finally {
				releaseRegion(exitClipRegions, slot);
				pendingPaintContexts[slot] = null;
			}
		}
	}

	/** Preserve exact raw pixels for all differing coordinates, bounded in logs. */
	private static String differenceCoordinates(int[] first, int[] second,
			int width, int limit) {
		StringBuilder out = new StringBuilder();
		int emitted = 0;
		for (int i = 0; i < Math.min(first.length, second.length); i++) {
			if (first[i] == second[i]) continue;
			if (emitted >= limit) {
				out.append(" ... more");
				break;
			}
			if (emitted++ > 0) out.append(' ');
			out.append('(').append(i % width).append(',').append(i / width)
					.append(':').append(pixelAt(first, i)).append("->")
					.append(pixelAt(second, i)).append(')');
		}
		return out.toString();
	}

	private String lastPaintClips() {
		StringBuilder result = new StringBuilder();
		int first = Math.max(0, nativePaints - lastNativeClips.length);
		for (int i = first; i < nativePaints; i++) {
			String event = lastNativeClips[i % lastNativeClips.length];
			if (event == null) continue;
			if (!result.isEmpty()) result.append(" | ");
			result.append(event);
		}
		return result.toString();
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
		if (dir == null || dir.isBlank()) {
			// Tycho's test JVM does not necessarily forward Maven -Dproperties.
			// GitHub Actions does forward its own RUNNER_TEMP environment.
			String runnerTemp = System.getenv("RUNNER_TEMP");
			if (runnerTemp != null && !runnerTemp.isBlank()) {
				dir = Path.of(runnerTemp, "nebula-widget-pixels").toString();
			}
		}
		if (dir == null || dir.isBlank()) {
			System.err.println("M3_XYGRAPH_DIAGNOSTIC_PNG_UNAVAILABLE no-output-directory");
			return;
		}
		try {
			Path output = Path.of(dir);
			Files.createDirectories(output);
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] { image };
			Path destination = output.resolve("xygraph-native-"
					+ name.replaceAll("[^A-Za-z0-9_-]", "_") + "-" + phase + ".png");
			loader.save(destination.toString(), SWT.IMAGE_PNG);
			System.err.println("M3_XYGRAPH_NATIVE_PNG_SAVED " + destination);
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
		long captureStarted = System.nanoTime();
		int completedBefore = nativePaints;
		int capture = ++pixelCaptures;
		recordNativeState("capture=" + capture + " boundary=before phase=" + capturePhase
				+ " entries=" + nativePaintEntries + " completed=" + nativePaints
				+ " nanos=" + captureStarted);
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
		long captureEnded = System.nanoTime();
		recordNativeState("capture=" + capture + " boundary=after phase=" + capturePhase
				+ " entries=" + nativePaintEntries + " completed=" + nativePaints
				+ " image=" + image.width + "x" + image.height + " nanos=" + captureEnded);
		lastPixelCaptureState = "capture=" + capture + " phase=" + capturePhase
				+ " startNanos=" + captureStarted + " endNanos=" + captureEnded
				+ " completedBefore=" + completedBefore + " completedAfter=" + nativePaints
				+ " entered=" + nativePaintEntries;
		return all;
	}

	/** Observe only a live event GC; retain no borrowed native handle. */
	private void recordPaintState(String stage, PaintEvent event, int paint, long observed) {
		String prefix = "paint=" + paint + " stage=" + stage
				+ " phase=" + capturePhase + " completed=" + nativePaints
				+ " expose=" + event.x + "," + event.y + "," + event.width + "x" + event.height
				+ " nanos=" + observed + " entries=" + nativePaintEntries
				+ (paint < 0 ? " pairing=UNAVAILABLE:no-retained-entry" : "");
		try {
			GC gc = event.gc;
			LineAttributes line = gc.getLineAttributes();
			float[] clientMatrix = new float[6];
			Transform transform = new Transform(display);
			try {
				gc.getTransform(transform);
				transform.getElements(clientMatrix);
			} finally {
				transform.dispose();
			}
			recordNativeState(prefix + " swtClipBounds=" + gc.getClipping()
					+ " swtClientMatrix=" + Arrays.toString(clientMatrix)
					+ " swtRequestedForegroundRGB=" + gc.getForeground().getRGB()
					+ " swtAlpha=" + gc.getAlpha() + " swtAA=" + gc.getAntialias()
					+ " swtRequestedLine=[width=" + line.width + ",cap=" + line.cap
					+ ",join=" + line.join + ",style=" + line.style
					+ ",dash=" + Arrays.toString(line.dash) + ",offset=" + line.dashOffset
					+ ",miter=" + line.miterLimit + "] " + gtkNativeState(gc));
		} catch (RuntimeException | LinkageError unavailable) {
			recordNativeState(prefix + " gcState=UNAVAILABLE:" + unavailable.getClass().getName());
		}
	}

	/** Public internal queries are resolved only on GTK, retaining other-platform compilation. */
	private static String gtkNativeState(GC gc) {
		if (!"gtk".equals(SWT.getPlatform())) return "cairoState=UNAVAILABLE:non-GTK";
		String unavailable = " cairoDeviceOffset=UNAVAILABLE:no-binding"
				+ " cairoActiveGroupTarget=UNAVAILABLE:no-binding"
				+ " cairoClipRegion=UNAVAILABLE:no-binding"
				+ " cairoAppliedLineAndAlpha=UNAVAILABLE:no-binding"
				+ " nativeStrokePoints=UNAVAILABLE:outside-stroke-owner";
		try {
			Object data = GC.class.getMethod("getGCData").invoke(gc);
			long context = data.getClass().getField("cairo").getLong(data);
			if (context == 0) return "cairoState=UNAVAILABLE:zero-context" + unavailable;
			ClassLoader loader = GC.class.getClassLoader();
			Class<?> cairo = Class.forName("org.eclipse.swt.internal.cairo.Cairo", false, loader);
			double[] matrix = new double[6];
			Arrays.fill(matrix, Double.NaN);
			cairo.getMethod("cairo_get_matrix", long.class, double[].class)
					.invoke(null, context, matrix);
			return "cairoVersion=" + cairo.getMethod("cairo_version").invoke(null)
					+ " cairoMatrix=" + Arrays.toString(matrix)
					+ " cairoAA=" + nativeQuery(cairo, "cairo_get_antialias", context)
					+ originalTargetState(cairo, context) + nativeClipBounds(loader, context)
					+ unavailable;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
			return "cairoState=UNAVAILABLE:" + failure.getClass().getName() + unavailable;
		}
	}

	/** cairo_get_target reports the original target, not a possibly active group target. */
	private static String originalTargetState(Class<?> cairo, long context) {
		try {
			long target = ((Long) nativeQuery(cairo, "cairo_get_target", context)).longValue();
			if (target == 0) return " cairoOriginalTarget=UNAVAILABLE:zero-target";
			int type = ((Integer) nativeQuery(cairo, "cairo_surface_get_type", target)).intValue();
			String extent = "UNAVAILABLE:surface-type-" + type;
			// These type-specific queries are valid only for their observed backend.
			if (type == 0 || type == 3) {
				String backend = type == 0 ? "image" : "xlib";
				int width = ((Integer) nativeQuery(cairo, "cairo_" + backend
						+ "_surface_get_width", target)).intValue();
				int height = ((Integer) nativeQuery(cairo, "cairo_" + backend
						+ "_surface_get_height", target)).intValue();
				extent = width > 0 && height > 0 ? width + "x" + height : "UNAVAILABLE:zero-or-missing-symbol";
			}
			double[] xScale = { Double.NaN };
			double[] yScale = { Double.NaN };
			cairo.getMethod("cairo_surface_get_device_scale", long.class, double[].class, double[].class)
					.invoke(null, target, xScale, yScale);
			String scale = Double.isNaN(xScale[0]) || Double.isNaN(yScale[0])
					? "UNAVAILABLE:query-did-not-return" : xScale[0] + "," + yScale[0];
			return " cairoOriginalTargetType=" + type
					+ " cairoOriginalTargetContent=" + nativeQuery(cairo, "cairo_surface_get_content", target)
					+ " cairoOriginalTargetExtent=" + extent + " cairoOriginalTargetScale=" + scale;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
			return " cairoOriginalTarget=UNAVAILABLE:" + failure.getClass().getName();
		}
	}

	/** Rounded native bounds only: false means empty or an unavailable dynamic symbol. */
	private static String nativeClipBounds(ClassLoader loader, long context) {
		try {
			Class<?> gtk = Class.forName("org.eclipse.swt.internal.gtk.GTK", false, loader);
			if (gtk.getField("GTK4").getBoolean(null)) return " cairoClipBounds=UNAVAILABLE:GTK4";
			Class<?> gdk = Class.forName("org.eclipse.swt.internal.gtk.GDK", false, loader);
			Class<?> rectangle = Class.forName("org.eclipse.swt.internal.gtk.GdkRectangle", false, loader);
			Object bounds = rectangle.getConstructor().newInstance();
			boolean available = ((Boolean) gdk.getMethod("gdk_cairo_get_clip_rectangle", long.class, rectangle)
					.invoke(null, context, bounds)).booleanValue();
			if (!available) return " cairoClipBounds=UNAVAILABLE:empty-or-missing-symbol";
			return " cairoClipBounds=" + rectangle.getField("x").getInt(bounds) + ","
					+ rectangle.getField("y").getInt(bounds) + ","
					+ rectangle.getField("width").getInt(bounds) + "x"
					+ rectangle.getField("height").getInt(bounds);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
			return " cairoClipBounds=UNAVAILABLE:" + failure.getClass().getName();
		}
	}

	private static Object nativeQuery(Class<?> owner, String method, long handle)
			throws ReflectiveOperationException {
		return owner.getMethod(method, long.class).invoke(null, handle);
	}

	private void recordNativeState(String observation) {
		nativeStateLedger[nativeStateRecords++ % nativeStateLedger.length] = observation;
	}

	private String lastNativeState() {
		StringBuilder out = new StringBuilder("records=" + nativeStateRecords
				+ " retained=" + Math.min(nativeStateRecords, nativeStateLedger.length)
				+ " dropped=" + Math.max(0, nativeStateRecords - nativeStateLedger.length));
		for (int i = Math.max(0, nativeStateRecords - nativeStateLedger.length); i < nativeStateRecords; i++) {
			if (!out.isEmpty()) out.append(" | ");
			out.append(nativeStateLedger[i % nativeStateLedger.length]);
		}
		return out.toString();
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
