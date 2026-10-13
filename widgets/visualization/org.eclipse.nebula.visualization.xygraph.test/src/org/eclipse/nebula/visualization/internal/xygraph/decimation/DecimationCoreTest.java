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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.eclipse.nebula.visualization.xygraph.dataprovider.CircularBufferDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.PackedXYDataProvider;
import org.eclipse.nebula.visualization.xygraph.dataprovider.Sample;
import org.eclipse.nebula.visualization.xygraph.figures.NoRepeatsPointsList;
import org.eclipse.nebula.visualization.xygraph.linearscale.Range;
import org.junit.Test;

/** Brute-force checks of the packed decimation atoms (no display needed). */
public class DecimationCoreTest {

	@Test
	public void pyramidRangeMinMaxMatchesScan() {
		Random r = new Random(1);
		for (int n : new int[] { 1, 15, 16, 17, 127, 128, 129, 1000, 4097, 70001 }) {
			double[] d = new double[n];
			for (int i = 0; i < n; i++) {
				d[i] = r.nextInt(20) == 0 ? -0.0 : r.nextGaussian();
			}
			PackedMinMaxPyramid p = PackedMinMaxPyramid.build(d, n);
			double[] out = new double[2];
			for (int k = 0; k < 300; k++) {
				int a = r.nextInt(n);
				int b = a + r.nextInt(n - a);
				p.rangeMinMax(a, b, out);
				double lo = d[a];
				double hi = d[a];
				for (int i = a + 1; i <= b; i++) {
					lo = Math.min(lo, d[i]);
					hi = Math.max(hi, d[i]);
				}
				assertEquals(Double.doubleToLongBits(lo), Double.doubleToLongBits(out[0]));
				assertEquals(Double.doubleToLongBits(hi), Double.doubleToLongBits(out[1]));
			}
		}
	}

	@Test
	public void pyramidNaNPropagates() {
		double[] d = new double[1000];
		d[777] = Double.NaN;
		PackedMinMaxPyramid p = PackedMinMaxPyramid.build(d, d.length);
		double[] out = new double[2];
		p.rangeMinMax(0, 999, out);
		assertEquals(Double.NaN, out[0], 0.0);
		p.rangeMinMax(0, 776, out);
		assertEquals(0.0, out[0], 0.0);
	}

	@Test
	public void pyramidExtendEqualsBuild() {
		Random r = new Random(2);
		double[] d = new double[50000];
		for (int i = 0; i < d.length; i++) {
			d[i] = r.nextGaussian();
		}
		PackedMinMaxPyramid p = PackedMinMaxPyramid.build(d, 100);
		for (int size = 100; size <= d.length; size += 1 + r.nextInt(3000)) {
			p = p.extend(d, size);
			PackedMinMaxPyramid q = PackedMinMaxPyramid.build(d, size);
			double[] a = new double[2];
			double[] b = new double[2];
			for (int k = 0; k < 50; k++) {
				int lo = r.nextInt(size);
				int hi = lo + r.nextInt(size - lo);
				p.rangeMinMax(lo, hi, a);
				q.rangeMinMax(lo, hi, b);
				assertArrayEquals(b, a, 0.0);
			}
		}
	}

	@Test
	public void firstAndLastDiffMatchScan() {
		Random r = new Random(3);
		AxisMapping m = new AxisMapping(false, false, -3, 3, 300, 10, 0, 0);
		for (int n : new int[] { 1, 16, 33, 500, 9000, 66000 }) {
			double[] d = new double[n];
			double v = 0;
			for (int i = 0; i < n; i++) {
				v = r.nextInt(50) == 0 ? r.nextGaussian() : v;
				d[i] = v;
			}
			PackedMinMaxPyramid p = PackedMinMaxPyramid.build(d, n);
			for (int k = 0; k < 300; k++) {
				int a = r.nextInt(n);
				int b = a + r.nextInt(n - a);
				int pixel = m.position(d[a + r.nextInt(b - a + 1)]) + (r.nextInt(5) == 0 ? 1 : 0);
				int first = -1;
				for (int i = a; i <= b && first < 0; i++) {
					first = m.position(d[i]) != pixel ? i : -1;
				}
				int last = -1;
				for (int i = b; i >= a && last < 0; i--) {
					last = m.position(d[i]) != pixel ? i : -1;
				}
				assertEquals("first n=" + n, first, p.firstDiff(a, b, pixel, m)); //$NON-NLS-1$
				assertEquals("last n=" + n, last, p.lastDiff(a, b, pixel, m)); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void builderMatchesNoRepeatsPointsList() {
		Random r = new Random(4);
		for (int k = 0; k < 200; k++) {
			NoRepeatsPointsList ref = new NoRepeatsPointsList();
			PolylineBuilder b = new PolylineBuilder();
			int x = 0;
			int y = 0;
			for (int i = 0; i < 3000; i++) {
				x += r.nextInt(3) - 1;
				y += r.nextInt(3) - 1;
				ref.addPoint(x, y);
				b.add(x, y);
			}
			assertArrayEquals(ref.toIntArray(), b.toArray());
		}
	}

	@Test
	public void packedRangesMatchSampleProvider() {
		Random r = new Random(5);
		for (int k = 0; k < 50; k++) {
			int n = 1 + r.nextInt(500);
			double[] x = new double[n];
			double[] y = new double[n];
			CircularBufferDataProvider ref = new CircularBufferDataProvider(true);
			ref.setBufferSize(n);
			for (int i = 0; i < n; i++) {
				x[i] = r.nextInt(10) == 0 ? -0.0 : r.nextGaussian() * (k % 3 == 0 ? -1 : 1);
				y[i] = r.nextInt(17) == 0 ? Double.NaN : r.nextGaussian();
				ref.addSample(new Sample(x[i], y[i]));
			}
			PackedXYDataProvider p = new PackedXYDataProvider();
			p.setData(x, y, n);
			for (boolean pos : new boolean[] { false, true }) {
				assertRange(ref.getXDataMinMax(pos), p.getXDataMinMax(pos));
				assertRange(ref.getYDataMinMax(pos), p.getYDataMinMax(pos));
			}
		}
	}

	private static void assertRange(Range a, Range b) {
		assertEquals(Double.doubleToLongBits(a.getLower()), Double.doubleToLongBits(b.getLower()));
		assertEquals(Double.doubleToLongBits(a.getUpper()), Double.doubleToLongBits(b.getUpper()));
	}
}
