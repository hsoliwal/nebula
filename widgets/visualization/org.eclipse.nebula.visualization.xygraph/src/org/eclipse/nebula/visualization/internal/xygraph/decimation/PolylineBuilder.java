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
package org.eclipse.nebula.visualization.internal.xygraph.decimation;

/**
 * Reusable packed {@code x, y} vertex buffer with the exact add rule of
 * {@code org.eclipse.nebula.visualization.xygraph.figures.NoRepeatsPointsList#addPoint(int, int)}: an exact repeat of
 * the last vertex is dropped, and once three or more vertices exist a vertex within one pixel of the last that equals
 * the one before the last is dropped.
 * <p>
 * The scratch array grows by half its size and is reused across decimations; {@link #toArray()} is the only
 * allocation per published polyline. Shared with the GWT/RWT client renderer.
 */
public final class PolylineBuilder {

	private int[] buf = new int[1024];
	private int n;

	public void reset() {
		n = 0;
	}

	/** Number of vertices. */
	public int size() {
		return n >> 1;
	}

	public void add(int x, int y) {
		if (n >= 2) {
			int lx = buf[n - 2];
			int ly = buf[n - 1];
			if (x == lx && y == ly) {
				return;
			}
			if (n >= 6 && Math.abs(x - lx) <= 1 && Math.abs(y - ly) <= 1 && x == buf[n - 4] && y == buf[n - 3]) {
				return;
			}
		}
		if (n + 2 > buf.length) {
			int[] b = new int[buf.length + (buf.length >> 1) + 2];
			System.arraycopy(buf, 0, b, 0, n);
			buf = b;
		}
		buf[n++] = x;
		buf[n++] = y;
	}

	/** Exact-length copy of the vertices ({@code x0, y0, x1, y1, ...}). */
	public int[] toArray() {
		int[] out = new int[n];
		System.arraycopy(buf, 0, out, 0, n);
		return out;
	}
}
