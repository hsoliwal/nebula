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
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.IDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.Trace;
import org.eclipse.nebula.visualization.xygraph.figures.XYGraph;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Before/after measurement and cross-build pixel parity for XYGraph line traces.
 * <p>
 * Usage: {@code XYGraphBench <points> <provider: circular|packed> <frames> <label>}. Compiles against the unmodified
 * widgets (circular only) and the modified ones (both; packed through reflection so the same source compiles
 * against both). Prints TSV {@code label, metric, value}. Pixel hashes are {@code Control.print} SHA-256 of the whole
 * canvas at fixed views, so equal hashes across builds prove identical rendering.
 */
public class XYGraphBench {

	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory
			.getThreadMXBean();
	static final com.sun.management.OperatingSystemMXBean OS = (com.sun.management.OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean();

	static String label;

	static void out(String metric, Object value) {
		System.out.println(label + "\t" + metric + "\t" + value);
	}

	static long allocatedAllThreads() {
		long[] ids = THREADS.getAllThreadIds();
		long[] bytes = THREADS.getThreadAllocatedBytes(ids);
		long sum = 0;
		for (long b : bytes) {
			sum += Math.max(0, b);
		}
		return sum;
	}

	static long usedHeapAfterGc() {
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		Runtime r = Runtime.getRuntime();
		return r.totalMemory() - r.freeMemory();
	}

	public static void main(String[] args) throws Exception {
		int n = Integer.parseInt(args[0]);
		String kind = args[1];
		int frames = Integer.parseInt(args[2]);
		label = args[3];
		long heap0 = usedHeapAfterGc();
		double[] xs = new double[n];
		double[] ys = new double[n];
		java.util.Random r = new java.util.Random(42);
		double v = 0;
		for (int i = 0; i < n; i++) {
			v += r.nextGaussian();
			if (r.nextInt(100000) == 0) {
				v += r.nextGaussian() * 200;
			}
			xs[i] = i;
			ys[i] = v;
		}
		long heapInput = usedHeapAfterGc() - heap0;

		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Canvas canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
		LightweightSystem lws = new LightweightSystem(canvas);
		XYGraph graph = new XYGraph();
		graph.setShowLegend(false);
		lws.setContents(graph);

		long t0 = System.nanoTime();
		IDataProvider provider;
		if ("packed".equals(kind)) {
			Class<?> c = Class.forName("org.eclipse.nebula.visualization.xygraph.dataprovider.PackedXYDataProvider");
			provider = (IDataProvider) c.getConstructor().newInstance();
			c.getMethod("setData", double[].class, double[].class, int.class).invoke(provider, xs, ys, n);
		} else {
			CircularBufferDataProvider cb = new CircularBufferDataProvider(true);
			cb.setBufferSize(n);
			for (int i = 0; i < n; i++) {
				cb.addSample(new Sample(xs[i], ys[i]));
			}
			provider = cb;
		}
		Trace trace = new Trace("t", graph.getPrimaryXAxis(), graph.getPrimaryYAxis(), provider);
		graph.addTrace(trace);
		graph.getPrimaryXAxis().setAutoScale(false);
		graph.getPrimaryYAxis().setAutoScale(false);
		Range yr = provider.getYDataMinMax();
		double ypad = (yr.getUpper() - yr.getLower()) * 0.05;
		graph.getPrimaryXAxis().setRange(new Range(0, n - 1));
		graph.getPrimaryYAxis().setRange(new Range(yr.getLower() - ypad, yr.getUpper() + ypad));
		long loadNs = System.nanoTime() - t0;
		xs = null;
		ys = null;
		long heapLoaded = usedHeapAfterGc() - heap0;

		shell.setSize(1400, 800);
		long t1 = System.nanoTime();
		shell.open();
		canvas.update();
		while (display.readAndDispatch()) {
			// settle late repaints
		}
		canvas.update();
		long firstPaintNs = System.nanoTime() - t1;
		out("points", n);
		out("provider", kind);
		out("load_ms", fmt(loadNs / 1e6));
		out("first_paint_ms", fmt(firstPaintNs / 1e6));
		out("heap_input_arrays_mb", fmt(heapInput / 1048576.0));
		out("heap_after_load_mb", fmt(heapLoaded / 1048576.0));
		out("print_sha256_full", printHash(display, canvas));

		double[] pan = new double[frames];
		double[] zoom = new double[frames];
		double[] cpuPan = new double[frames];
		double[] allocPan = new double[frames];
		double lo = 0;
		double hi = n - 1;
		// warm-up frame
		frame(display, canvas);
		for (int f = 0; f < frames; f++) {
			double w = hi - lo;
			lo += w * 0.01;
			hi += w * 0.01;
			graph.getPrimaryXAxis().setRange(new Range(lo, hi));
			long cpu0 = OS.getProcessCpuTime();
			long a0 = allocatedAllThreads();
			pan[f] = frame(display, canvas);
			cpuPan[f] = (OS.getProcessCpuTime() - cpu0) / 1e6;
			allocPan[f] = allocatedAllThreads() - a0;
		}
		out("print_sha256_after_pan", printHash(display, canvas));
		double[] cpuZoom = new double[frames];
		double[] allocZoom = new double[frames];
		for (int f = 0; f < frames; f++) {
			double c = (lo + hi) / 2;
			double w = (hi - lo) * (f < frames / 2 ? 0.97 : 1.03);
			lo = c - w / 2;
			hi = c + w / 2;
			graph.getPrimaryXAxis().setRange(new Range(lo, hi));
			long cpu0 = OS.getProcessCpuTime();
			long a0 = allocatedAllThreads();
			zoom[f] = frame(display, canvas);
			cpuZoom[f] = (OS.getProcessCpuTime() - cpu0) / 1e6;
			allocZoom[f] = allocatedAllThreads() - a0;
		}
		out("print_sha256_after_zoom", printHash(display, canvas));
		// repaint with no view change
		double[] same = new double[frames];
		double[] allocSame = new double[frames];
		for (int f = 0; f < frames; f++) {
			long a0 = allocatedAllThreads();
			same[f] = frame(display, canvas);
			allocSame[f] = allocatedAllThreads() - a0;
		}
		stats("pan_frame_ms", pan);
		stats("zoom_frame_ms", zoom);
		stats("repaint_same_view_ms", same);
		stats("pan_cpu_ms_per_frame", cpuPan);
		stats("zoom_cpu_ms_per_frame", cpuZoom);
		stats("pan_alloc_bytes_per_frame", allocPan);
		stats("zoom_alloc_bytes_per_frame", allocZoom);
		stats("same_view_alloc_bytes_per_frame", allocSame);
		out("heap_used_end_mb", fmt(usedHeapAfterGc() / 1048576.0));
		if (n <= 2_000_000) { // the hot list materializes one Sample per visible point when asked
			out("hot_samples", trace.getHotSampleList().size());
		}
		shell.dispose();
		display.dispose();
	}

	static double frame(Display display, Canvas canvas) {
		long t = System.nanoTime();
		canvas.redraw();
		canvas.update();
		return (System.nanoTime() - t) / 1e6;
	}

	static String printHash(Display display, Canvas canvas) throws Exception {
		while (display.readAndDispatch()) {
			// settle
		}
		canvas.redraw();
		canvas.update();
		org.eclipse.swt.graphics.Point size = canvas.getSize();
		Image img = new Image(display, size.x, size.y);
		GC gc = new GC(img);
		canvas.print(gc);
		gc.dispose();
		byte[] data = img.getImageData().data;
		img.dispose();
		byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
		StringBuilder sb = new StringBuilder();
		for (byte b : d) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	static void stats(String name, double[] values) {
		double[] s = values.clone();
		Arrays.sort(s);
		out(name + "_p50", fmt(s[(int) Math.floor((s.length - 1) * 0.5)]));
		out(name + "_p95", fmt(s[(int) Math.floor((s.length - 1) * 0.95)]));
		out(name + "_max", fmt(s[s.length - 1]));
	}

	static String fmt(double d) {
		return String.format(Locale.ROOT, "%.2f", d);
	}
}
