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
import java.util.Random;

import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.nebula.visualization.widgets.datadefinition.ColorMap;
import org.eclipse.nebula.visualization.widgets.datadefinition.ColorMap.PredefinedColorMap;
import org.eclipse.nebula.visualization.widgets.datadefinition.DoubleArrayWrapper;
import org.eclipse.nebula.visualization.widgets.datadefinition.IntArrayWrapper;
import org.eclipse.nebula.visualization.widgets.figures.IntensityGraphFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Before/after byte parity and measurements for ColorMap.drawImage and IntensityGraphFigure.
 * Usage: {@code IntensityBench <figureSide> <frames> <label> <lutSide>}; prints TSV {@code label, metric, value}.
 */
public class IntensityBench {

	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory
			.getThreadMXBean();
	static final com.sun.management.OperatingSystemMXBean OS = (com.sun.management.OperatingSystemMXBean) ManagementFactory
			.getOperatingSystemMXBean();
	static String label;

	static void out(String m, Object v) {
		System.out.println(label + "\t" + m + "\t" + v);
	}

	public static void main(String[] args) throws Exception {
		int side = Integer.parseInt(args[0]);
		int frames = Integer.parseInt(args[1]);
		label = args[2];
		int lutSide = Integer.parseInt(args[3]);
		Display display = Display.getDefault();
		colorMapParity();
		lutScale(lutSide, 1200, 900, true);
		lutScale(lutSide, lutSide, lutSide, false);
		double[] data = new double[side * side];
		for (int y = 0; y < side; y++) {
			for (int x = 0; x < side; x++) {
				double dx = x - side / 3.0;
				double dy = y - side / 2.0;
				data[y * side + x] = Math.sin(Math.sqrt(dx * dx + dy * dy) / 40.0) + ((x * 7 + y * 13) % 17) / 40.0;
			}
		}
		long heap0 = heap();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Canvas canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
		LightweightSystem lws = new LightweightSystem(canvas);
		IntensityGraphFigure fig = new IntensityGraphFigure();
		lws.setContents(fig);
		long t0 = System.nanoTime();
		fig.setMax(1.5);
		fig.setMin(-1);
		fig.setDataWidth(side);
		fig.setDataHeight(side);
		fig.setColorMap(new ColorMap(PredefinedColorMap.JET, true, true));
		fig.setDataArray(data);
		long loadNs = System.nanoTime() - t0;
		shell.setSize(1200, 900);
		long t1 = System.nanoTime();
		shell.open();
		canvas.update();
		settle(display);
		long firstNs = System.nanoTime() - t1;
		out("side", side);
		out("load_ms", f(loadNs / 1e6));
		out("first_paint_ms", f(firstNs / 1e6));
		out("print_sha256_crop0", print(display, canvas));
		double[] t = new double[frames];
		double[] cpu = new double[frames];
		double[] alloc = new double[frames];
		long heapPeak = 0;
		for (int i = 0; i < frames; i++) {
			int c = (i + 1) * (side / (3 * frames));
			fig.setCropLeft(c);
			fig.setCropRight(c / 2);
			fig.setCropTop(c / 3);
			fig.setCropBottom(c);
			long c0 = OS.getProcessCpuTime();
			long a0 = allocated();
			long s = System.nanoTime();
			canvas.redraw();
			canvas.update();
			t[i] = (System.nanoTime() - s) / 1e6;
			cpu[i] = (OS.getProcessCpuTime() - c0) / 1e6;
			alloc[i] = allocated() - a0;
			Runtime r = Runtime.getRuntime();
			heapPeak = Math.max(heapPeak, r.totalMemory() - r.freeMemory());
			if (i == frames / 2) {
				out("print_sha256_crop_mid", print(display, canvas));
			}
		}
		out("print_sha256_crop_end", print(display, canvas));
		out("display_copyArea_sha256_crop_end", screen(display, canvas));
		stats("crop_frame_ms", t);
		stats("crop_cpu_ms_per_frame", cpu);
		stats("crop_alloc_bytes_per_frame", alloc);
		out("heap_retained_mb", f((heap() - heap0) / 1048576.0));
		out("heap_peak_during_crops_mb", f(heapPeak / 1048576.0));
		shell.dispose();
	}

	static void colorMapParity() throws Exception {
		Random r = new Random(9);
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		int k = 0;
		for (PredefinedColorMap pm : PredefinedColorMap.values()) {
			if (pm == PredefinedColorMap.None) {
				continue;
			}
			for (int variant = 0; variant < 4; variant++) {
				ColorMap cm = new ColorMap(pm, variant % 2 == 0, variant < 2);
				int w = 50 + r.nextInt(700);
				int h = 50 + r.nextInt(500);
				double[] d = new double[w * h];
				int[] di = new int[w * h];
				for (int i = 0; i < d.length; i++) {
					d[i] = r.nextGaussian() * 2;
					di[i] = r.nextInt(400) - 100;
				}
				boolean shrink = variant == 1 || variant == 3;
				int ow = shrink ? Math.max(1, w / 3) : w;
				int oh = shrink ? Math.max(1, h / 2) : h;
				ImageData a = cm.drawImage(new DoubleArrayWrapper(d), w, h, 3, -2,
						new ImageData(ow, oh, 24, cm.getPalette()), shrink);
				ImageData b = cm.drawImage(new IntArrayWrapper(di), w, h, 250, -50,
						new ImageData(ow, oh, 24, cm.getPalette()), shrink);
				md.update(a.data);
				md.update(b.data);
				k += 2;
			}
		}
		out("colormap_images", k);
		out("colormap_sha256", hex(md.digest()));
	}

	/** ColorMap.drawImage over a lutSide x lutSide packed image into an outW x outH image, 10 runs. */
	static void lutScale(int side, int outW, int outH, boolean shrink) throws Exception {
		double[] d = new double[side * side];
		for (int i = 0; i < d.length; i++) {
			int x = i % side;
			int y = i / side;
			d[i] = Math.sin(x / 50.0) * Math.cos(y / 70.0) + ((x ^ y) & 15) / 30.0;
		}
		ColorMap cm = new ColorMap(PredefinedColorMap.JET, true, true);
		ImageData img = new ImageData(outW, outH, 24, cm.getPalette());
		DoubleArrayWrapper w = new DoubleArrayWrapper(d);
		double[] t = new double[10];
		double[] alloc = new double[10];
		double[] cpu = new double[10];
		for (int k = 0; k < 10; k++) {
			long a0 = allocated();
			long c0 = OS.getProcessCpuTime();
			long s = System.nanoTime();
			cm.drawImage(w, side, side, 1.6, -1.2, img, shrink);
			t[k] = (System.nanoTime() - s) / 1e6;
			cpu[k] = (OS.getProcessCpuTime() - c0) / 1e6;
			alloc[k] = allocated() - a0;
		}
		String tag = "lut_" + side + "_to_" + outW + "x" + outH;
		out(tag + "_sha256", hex(MessageDigest.getInstance("SHA-256").digest(img.data)));
		stats(tag + "_ms", t);
		stats(tag + "_cpu_ms", cpu);
		stats(tag + "_alloc_bytes", alloc);
	}

	static String print(Display display, Canvas canvas) throws Exception {
		settle(display);
		canvas.redraw();
		canvas.update();
		org.eclipse.swt.graphics.Point s = canvas.getSize();
		Image img = new Image(display, s.x, s.y);
		GC gc = new GC(img);
		canvas.print(gc);
		gc.dispose();
		byte[] data = img.getImageData().data;
		img.dispose();
		return hex(MessageDigest.getInstance("SHA-256").digest(data));
	}

	/** Capture mode display_copyArea: shell forced active, canvas pixels read back from the screen. */
	static String screen(Display display, Canvas canvas) throws Exception {
		canvas.getShell().forceActive();
		canvas.getShell().moveAbove(null);
		canvas.redraw();
		canvas.update();
		settle(display);
		org.eclipse.swt.graphics.Point s = canvas.getSize();
		org.eclipse.swt.graphics.Point at = canvas.toDisplay(0, 0);
		Image img = new Image(display, s.x, s.y);
		GC gc = new GC(display);
		gc.copyArea(img, at.x, at.y);
		gc.dispose();
		byte[] data = img.getImageData().data;
		img.dispose();
		return hex(MessageDigest.getInstance("SHA-256").digest(data));
	}

	static void settle(Display d) {
		while (d.readAndDispatch()) {
			// drain
		}
	}

	static long heap() {
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		Runtime r = Runtime.getRuntime();
		return r.totalMemory() - r.freeMemory();
	}

	static long allocated() {
		long sum = 0;
		for (long b : THREADS.getThreadAllocatedBytes(THREADS.getAllThreadIds())) {
			sum += Math.max(0, b);
		}
		return sum;
	}

	static void stats(String name, double[] v) {
		double[] s = v.clone();
		Arrays.sort(s);
		out(name + "_p50", f(s[(s.length - 1) / 2]));
		out(name + "_p95", f(s[(int) Math.floor((s.length - 1) * 0.95)]));
		out(name + "_max", f(s[s.length - 1]));
	}

	static String hex(byte[] d) {
		StringBuilder sb = new StringBuilder();
		for (byte b : d) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	static String f(double d) {
		return String.format(Locale.ROOT, "%.2f", d);
	}
}
