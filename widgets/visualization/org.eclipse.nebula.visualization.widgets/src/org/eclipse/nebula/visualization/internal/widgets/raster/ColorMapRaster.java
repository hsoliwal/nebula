/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Synexia contributors - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.visualization.internal.widgets.raster;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.eclipse.swt.SWT;

/**
 * Packed colour-map LUT apply over the visible raster: for every output pixel the same 256-entry LUT index as
 * {@code ColorMap.drawImage} ({@code (int) ((v - min) / (max - min) * 255)}, clamped), written as the three bytes
 * {@code ImageData.setPixel} writes for depth 24, straight into the image's byte array. Rows are split across worker
 * threads; the caller waits, so the image is complete when the call returns.
 * <p>
 * The source is either a packed {@code double[]} window ({@code base + y * stride + x}) or, for other wrappers, an
 * {@link Source}. Shared with the GWT/RWT client renderer apart from the thread pool: int/double state; the shrink
 * ratios use the receiver's int arithmetic unchanged (they overflow only for data wider or taller than 32767); row
 * bands are split with int arithmetic only.
 */
public final class ColorMapRaster {

	/** Per-pixel value source for wrappers that are not packed double arrays. */
	public interface Source {
		double get(int index);
	}

	/** {@code false} restores the per-pixel {@code ImageData.setPixel} loop (diagnostics and A/B runs). */
	public static final boolean ENABLED = !"off".equals(System.getProperty( //$NON-NLS-1$
			"org.eclipse.nebula.visualization.widgets.raster")); //$NON-NLS-1$
	private static final int PARALLEL_PIXELS = 1 << 16;
	private static volatile ExecutorService pool;

	private ColorMapRaster() {
	}

	private static ExecutorService pool() {
		ExecutorService p = pool;
		if (p == null) {
			synchronized (ColorMapRaster.class) {
				p = pool;
				if (p == null) {
					int n = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
					ThreadFactory f = r -> {
						Thread t = new Thread(r, "nebula-colormap-raster"); //$NON-NLS-1$
						t.setDaemon(true);
						return t;
					};
					p = Executors.newFixedThreadPool(n, f);
					pool = p;
				}
			}
		}
		return p;
	}

	/**
	 * Fills {@code out} (depth 24, {@code bytesPerLine} per row, {@code outWidth x outHeight}) from the source.
	 *
	 * @param packed     packed values or null to use {@code source}
	 * @param base       index of data (0, 0) in {@code packed}
	 * @param stride     index distance between data rows in {@code packed}
	 * @param source     fallback source indexed {@code y * dataWidth + x}
	 * @param shrink     nearest-neighbour shrink of the data to the output size, as {@code ColorMap.drawImage}
	 */
	public static void apply(double[] packed, int base, int stride, Source source, int dataWidth, int dataHeight,
			double max, double min, int[] lut, byte[] out, int bytesPerLine, int outWidth, int outHeight,
			boolean shrink) {
		int rows = shrink ? outHeight : dataHeight;
		int cols = shrink ? outWidth : dataWidth;
		int xRatio = shrink ? (int) ((dataWidth << 16) / outWidth) + 1 : 0;
		int yRatio = shrink ? (int) ((dataHeight << 16) / outHeight) + 1 : 0;
		if (rows < PARALLEL_PIXELS / Math.max(1, cols)) {
			band(packed, base, stride, source, dataWidth, max, min, lut, out, bytesPerLine, cols, 0, rows, shrink,
					xRatio, yRatio);
			return;
		}
		int parts = Math.min(rows, Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors())));
		Future<?>[] f = new Future<?>[parts];
		for (int p = 0; p < parts; p++) {
			int from = rows / parts * p + Math.min(p, rows % parts);
			int to = rows / parts * (p + 1) + Math.min(p + 1, rows % parts);
			f[p] = pool().submit(() -> band(packed, base, stride, source, dataWidth, max, min, lut, out, bytesPerLine,
					cols, from, to, shrink, xRatio, yRatio));
		}
		for (Future<?> fu : f) {
			try {
				fu.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				SWT.error(SWT.ERROR_UNSPECIFIED, e);
			} catch (java.util.concurrent.ExecutionException e) {
				SWT.error(SWT.ERROR_UNSPECIFIED, e.getCause());
			}
		}
	}

	private static void band(double[] packed, int base, int stride, Source source, int dataWidth, double max,
			double min, int[] lut, byte[] out, int bpl, int cols, int rowFrom, int rowTo, boolean shrink, int xRatio,
			int yRatio) {
		for (int y = rowFrom; y < rowTo; y++) {
			int sy = shrink ? ((y * yRatio) >> 16) : y;
			int o = y * bpl;
			for (int x = 0; x < cols; x++) {
				int sx = shrink ? ((x * xRatio) >> 16) : x;
				double v = packed != null ? packed[base + sy * stride + sx] : source.get(sy * dataWidth + sx);
				int index = (int) ((v - min) / (max - min) * 255);
				if (index < 0) {
					index = 0;
				} else if (index > 255) {
					index = 255;
				}
				int pixel = lut[index];
				out[o] = (byte) ((pixel >> 16) & 0xFF);
				out[o + 1] = (byte) ((pixel >> 8) & 0xFF);
				out[o + 2] = (byte) (pixel & 0xFF);
				o += 3;
			}
		}
	}
}
