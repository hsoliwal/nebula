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
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.nebula.widgets.oscilloscope.multichannel.Oscilloscope;
import org.eclipse.nebula.widgets.oscilloscope.multichannel.OscilloscopeDispatcher;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Oscilloscope before/after: {@code parity} prints per-frame hashes (capture modes control_print and
 * display_copyArea) for deterministic drives of the existing API; {@code stream} pushes 1M samples/s for a number
 * of seconds through the dispatcher loop; {@code hidden} counts loop hooks while the shell is minimized.
 * Usage: {@code OscilloscopeBench <mode> <label> [seconds]}. New API (pushSamples) is called by reflection so the
 * same source compiles against the unmodified widget.
 */
public class OscilloscopeBench {

	static final com.sun.management.ThreadMXBean THREADS = (com.sun.management.ThreadMXBean) ManagementFactory
			.getThreadMXBean();
	static String label;

	static void out(String m, Object v) {
		System.out.println(label + "\t" + m + "\t" + v);
	}

	public static void main(String[] args) throws Exception {
		String mode = args[0];
		label = args[1];
		Display display = Display.getDefault();
		if ("parity".equals(mode)) {
			parity(display);
		} else if ("stream".equals(mode)) {
			stream(display, Integer.parseInt(args[2]));
		} else {
			hidden(display);
		}
		display.dispose();
	}

	static void parity(Display display) throws Exception {
		MessageDigest all = MessageDigest.getInstance("SHA-256");
		int frames = 0;
		for (int config = 0; config < 8; config++) {
			// never opened: no OS paint can advance the animation; each print() is exactly one paint
			Shell shell = new Shell(display, SWT.NO_TRIM);
			shell.setLayout(new FillLayout());
			Oscilloscope scope = new Oscilloscope(2, null, shell, SWT.NONE);
			shell.setBounds(40, 40, 520 + config * 13, 180 + config * 7);
			shell.layout(true);
			settle(display);
			for (int c = 0; c < 2; c++) {
				scope.setSteady(c, config % 4 == 1, config % 3 == 0 ? Oscilloscope.STEADYPOSITION_75PERCENT : 200);
				scope.setFade(c, config % 4 == 2);
				scope.setConnect(c, config % 2 == 1);
				scope.setPercentage(c, config >= 4);
				scope.setProgression(c, 1 + config % 3);
				scope.setTailSize(c, config % 4 == 3 ? Oscilloscope.TAILSIZE_MAX : Oscilloscope.TAILSIZE_DEFAULT);
				scope.setAntialias(c, config % 2 == 0);
				scope.setLineWidth(c, 1 + config % 2);
				scope.setBaseOffset(c, c == 0 ? 30 : 70);
			}
			java.util.Random r = new java.util.Random(config);
			for (int f = 0; f < 120; f++) {
				int[] v = new int[1 + r.nextInt(6)];
				for (int i = 0; i < v.length; i++) {
					v[i] = (int) (Math.sin((f * 7 + i) * 0.11) * 40) + r.nextInt(9) - 4;
				}
				scope.setValues(0, v);
				scope.setValue(1, Oscilloscope.HEARTBEAT[f % Oscilloscope.HEARTBEAT.length]);
				byte[] px = paintDirect(display, scope);
				all.update(px);
				if (System.getProperty("perFrame") != null) {
					out("frame_" + config + "_" + f, hex(MessageDigest.getInstance("SHA-256").digest(px)));
				}
				frames++;
			}
			shell.dispose();
		}
		out("frames", frames);
		out("sha256_paint_dispatch_every_frame", hex(all.digest()));
		// on-screen check: one topmost, active shell, captured with GC(display).copyArea (not hash-compared:
		// the scope advances on every OS paint, so the on-screen frame count is not deterministic)
		Shell shell = new Shell(display, SWT.ON_TOP | SWT.NO_TRIM);
		shell.setLayout(new FillLayout());
		Oscilloscope scope = new Oscilloscope(1, null, shell, SWT.NONE);
		shell.setBounds(60, 60, 600, 200);
		shell.open();
		shell.forceActive();
		for (int f = 0; f < 200; f++) {
			scope.setValue(0, Oscilloscope.HEARTBEAT[f % Oscilloscope.HEARTBEAT.length]);
			scope.redraw();
			scope.update();
		}
		byte[] sc = screenCapture(display, scope);
		int lit = 0;
		for (int i = 0; i + 2 < sc.length; i += 3) {
			lit += (sc[i] & 0xff) + (sc[i + 1] & 0xff) + (sc[i + 2] & 0xff) > 200 ? 1 : 0;
		}
		out("display_copyArea_bright_pixels", lit);
		shell.dispose();
	}

	static void stream(Display display, int seconds) throws Exception {
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Oscilloscope scope = new Oscilloscope(1, null, shell, SWT.NONE);
		shell.setSize(1200, 300);
		shell.open();
		settle(display);
		Method push = null;
		try {
			Method spc = Oscilloscope.class.getMethod("setSamplesPerColumn", int.class, int.class);
			push = Oscilloscope.class.getMethod("pushSamples", int.class, int[].class, int.class, int.class);
			spc.invoke(scope, 0, 30000); // 1M samples/s at ~33 columns/s
		} catch (NoSuchMethodException e) {
			// unmodified widget: setValues
		}
		out("api", push == null ? "setValues" : "pushSamples");
		AtomicLong frameNanos = new AtomicLong();
		AtomicInteger framesDrawn = new AtomicInteger();
		long[] frameTimes = new long[seconds * 200];
		long[] frameAlloc = new long[seconds * 200];
		long uiThread = Thread.currentThread().getId();
		OscilloscopeDispatcher dispatcher = new OscilloscopeDispatcher(0, scope) {
			long a0;
			long t0;

			@Override
			public void hookBeforeDraw(Oscilloscope o, int counter) {
				super.hookBeforeDraw(o, counter);
				t0 = System.nanoTime();
				a0 = THREADS.getThreadAllocatedBytes(uiThread);
			}

			@Override
			public void hookAfterDraw(Oscilloscope o, int counter) {
				o.update();
				int i = framesDrawn.getAndIncrement();
				if (i < frameTimes.length) {
					frameTimes[i] = System.nanoTime() - t0;
					frameAlloc[i] = THREADS.getThreadAllocatedBytes(uiThread) - a0;
				}
			}

			@Override
			public boolean getFade() {
				return false;
			}

			@Override
			public boolean isPercentage() {
				return false;
			}
		};
		AtomicBoolean run = new AtomicBoolean(true);
		AtomicLong produced = new AtomicLong();
		Method pushM = push;
		Thread producer = new Thread(() -> {
			int[] block = new int[10000];
			long next = System.nanoTime();
			double phase = 0;
			while (run.get()) {
				for (int i = 0; i < block.length; i++) {
					phase += 0.0007;
					block[i] = (int) (Math.sin(phase) * 60 + Math.sin(phase * 37) * 20);
				}
				try {
					if (pushM != null) {
						pushM.invoke(scope, 0, block, 0, block.length);
					} else {
						scope.setValues(0, block);
					}
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
				produced.addAndGet(block.length);
				next += 10_000_000L;
				long wait = next - System.nanoTime();
				if (wait > 0) {
					java.util.concurrent.locks.LockSupport.parkNanos(wait);
				}
			}
		}, "producer");
		long heap0 = heap();
		dispatcher.dispatch();
		producer.start();
		long cpu0 = THREADS.getThreadCpuTime(producer.getId());
		long uiCpu0 = THREADS.getCurrentThreadCpuTime();
		long end = System.currentTimeMillis() + seconds * 1000L;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		long producerCpu = THREADS.getThreadCpuTime(producer.getId()) - cpu0;
		long uiCpu = THREADS.getCurrentThreadCpuTime() - uiCpu0;
		run.set(false);
		producer.join();
		dispatcher.stop();
		int n = Math.min(framesDrawn.get(), frameTimes.length);
		out("seconds", seconds);
		out("samples_pushed", produced.get());
		out("samples_per_second", (long) (produced.get() / (double) seconds));
		out("frames", framesDrawn.get());
		out("producer_cpu_ms_per_s", f(producerCpu / 1e6 / seconds));
		out("ui_cpu_ms_per_s", f(uiCpu / 1e6 / seconds));
		double[] t = new double[n];
		double[] a = new double[n];
		for (int i = 0; i < n; i++) {
			t[i] = frameTimes[i] / 1e6;
			a[i] = frameAlloc[i];
		}
		stats("frame_ms", t);
		stats("ui_alloc_bytes_per_frame", a);
		out("heap_delta_mb", f((heap() - heap0) / 1048576.0));
		shell.dispose();
	}

	static void hidden(Display display) throws Exception {
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Oscilloscope scope = new Oscilloscope(1, null, shell, SWT.NONE);
		shell.setSize(600, 200);
		shell.open();
		settle(display);
		AtomicInteger hooks = new AtomicInteger();
		AtomicInteger paints = new AtomicInteger();
		scope.addListener(SWT.Paint, e -> paints.incrementAndGet());
		OscilloscopeDispatcher d = new OscilloscopeDispatcher(0, scope) {
			@Override
			public void hookBeforeDraw(Oscilloscope o, int counter) {
				hooks.incrementAndGet();
				super.hookBeforeDraw(o, counter);
			}
		};
		d.dispatch();
		pump(display, 1000);
		int visibleHooks = hooks.get();
		shell.setMinimized(true);
		pump(display, 300);
		hooks.set(0);
		paints.set(0);
		pump(display, 2000);
		int minimizedHooks = hooks.get();
		int minimizedPaints = paints.get();
		shell.setMinimized(false);
		pump(display, 1000);
		int restoredHooks = hooks.get();
		scope.setVisible(false);
		pump(display, 300);
		hooks.set(0);
		pump(display, 2000);
		int hiddenHooks = hooks.get();
		scope.setVisible(true);
		pump(display, 1000);
		int reshownHooks = hooks.get();
		d.stop();
		out("hooks_visible_1s", visibleHooks);
		out("hooks_minimized_2s", minimizedHooks);
		out("paints_minimized_2s", minimizedPaints);
		out("hooks_after_restore_1s", restoredHooks);
		out("hooks_setVisible_false_2s", hiddenHooks);
		out("hooks_after_setVisible_true_1s", reshownHooks);
		shell.dispose();
	}

	static void pump(Display display, long ms) {
		long end = System.currentTimeMillis() + ms;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				display.timerExec(5, () -> {
				});
				display.sleep();
			}
		}
	}

	static void settle(Display d) {
		while (d.readAndDispatch()) {
			// drain
		}
	}

	static byte[] print(Display display, Oscilloscope scope) {
		Point s = scope.getSize();
		Image img = new Image(display, s.x, s.y);
		GC gc = new GC(img);
		scope.print(gc);
		gc.dispose();
		byte[] d = img.getImageData().data;
		img.dispose();
		return d;
	}

	/**
	 * Capture mode paint_dispatch: one SWT.Paint event delivered to the scope with a GC on a black Image. Used for
	 * the frame-by-frame parity because Control.print of this widget (which advances its animation inside paint) was
	 * not reproducible from run to run on the unmodified widget.
	 */
	static byte[] paintDirect(Display display, Oscilloscope scope) {
		Point s = scope.getSize();
		Image img = new Image(display, s.x, s.y);
		GC gc = new GC(img);
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.fillRectangle(0, 0, s.x, s.y);
		org.eclipse.swt.widgets.Event e = new org.eclipse.swt.widgets.Event();
		e.gc = gc;
		e.width = s.x;
		e.height = s.y;
		scope.notifyListeners(SWT.Paint, e);
		gc.dispose();
		byte[] d = img.getImageData().data;
		img.dispose();
		return d;
	}

	/** On-screen pixels of the scope (shell made topmost and active first). */
	static byte[] screenCapture(Display display, Oscilloscope scope) {
		scope.getShell().forceActive();
		settle(display);
		Point s = scope.getSize();
		Point at = scope.toDisplay(0, 0);
		Image img = new Image(display, s.x, s.y);
		GC gc = new GC(display);
		gc.copyArea(img, at.x, at.y);
		gc.dispose();
		byte[] d = img.getImageData().data;
		img.dispose();
		return d;
	}

	static long heap() {
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		Runtime r = Runtime.getRuntime();
		return r.totalMemory() - r.freeMemory();
	}

	static void stats(String name, double[] v) {
		if (v.length == 0) {
			out(name + "_p50", "n/a");
			return;
		}
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
