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
package org.eclipse.nebula.widgets.oscilloscope.multichannel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Envelope mode: (1) the scope draws, per column, the first/min/max/last of exactly the samples pushed for that
 * column; (2) that four-vertex column covers the same pixels as drawing every sample of the column at the same x
 * (capture mode: offscreen {@code GC} on an {@code Image}, same GC settings as the scope's paint). Aliased lines of
 * width 1 must be identical; other widths and antialiasing report the differing pixel count.
 */
public class OscilloscopeEnvelopeTest {

	/** (1) the drawn vertex groups equal the folds of the pushed columns, in order. */
	@Test
	public void drawnColumnsAreTheFoldsOfThePushedSamples() throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Oscilloscope scope = new Oscilloscope(1, null, shell, SWT.NONE);
		shell.setSize(400, 200);
		shell.open();
		while (display.readAndDispatch()) {
			// settle
		}
		int spc = 37;
		scope.setSamplesPerColumn(0, spc);
		Random r = new Random(3);
		List<int[]> folds = new ArrayList<>();
		int columns = 300;
		for (int c = 0; c < columns; c++) {
			int[] s = new int[spc];
			for (int i = 0; i < spc; i++) {
				s[i] = r.nextInt(120) - 60;
			}
			int min = s[0];
			int max = s[0];
			for (int v : s) {
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
			folds.add(new int[] { s[0], min, max, s[spc - 1] });
			scope.pushSamples(0, s, 0, spc);
		}
		// one column per paint: after `columns` paints the tail ends with the last pushed column
		for (int f = 0; f < columns; f++) {
			scope.redraw();
			scope.update();
		}
		int[] l2 = lines(scope, "l2");
		int[] l1 = lines(scope, "l1");
		int base = scope.getBase(0);
		List<int[]> drawn = new ArrayList<>();
		for (int[] l : new int[][] { l1, l2 }) {
			for (int o = 2; o + 8 <= l.length; o += 8) {
				// drawn y = base - value (values are negated on the way into the tail)
				drawn.add(new int[] { base - l[o + 1], base - l[o + 3], base - l[o + 5], base - l[o + 7] });
			}
		}
		// the newest columns are at the end of the drawing; every non-degenerate drawn column is a pushed fold
		int matched = 0;
		int fi = folds.size() - 1;
		for (int d = drawn.size() - 1; d >= 0 && fi >= 0; d--) {
			int[] g = drawn.get(d);
			if (g[0] == 0 && g[1] == 0 && g[2] == 0 && g[3] == 0) {
				continue;
			}
			int[] want = folds.get(fi--);
			assertArrayEquals("column " + d, want, g);
			matched++;
		}
		System.out.println("envelope columns matched=" + matched); //$NON-NLS-1$
		assertTrue(matched > 100);
		shell.dispose();
	}

	private static int[] lines(Oscilloscope scope, String name) throws Exception {
		Field chanF = Oscilloscope.class.getDeclaredField("chan"); //$NON-NLS-1$
		chanF.setAccessible(true);
		Object data = ((Object[]) chanF.get(scope))[0];
		Field f = data.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return (int[]) f.get(data);
	}

	/** (2) four vertices per column vs every sample, same GC settings as the scope. */
	@Test
	public void envelopeColumnsCoverTheSamePixelsAsAllSamples() {
		Display display = Display.getDefault();
		Random r = new Random(11);
		int exactAliased1 = 0;
		StringBuilder report = new StringBuilder();
		for (int k = 0; k < 24; k++) {
			int spc = 2 + r.nextInt(400);
			int cols = 50 + r.nextInt(500);
			List<Integer> full = new ArrayList<>();
			List<Integer> env = new ArrayList<>();
			double phase = r.nextDouble() * 10;
			int prevLast = 100;
			full.add(9);
			full.add(prevLast);
			env.add(9);
			env.add(prevLast);
			for (int c = 0; c < cols; c++) {
				int x = 10 + c;
				int first = 0;
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				int last = 0;
				for (int i = 0; i < spc; i++) {
					phase += 0.003 * (1 + k % 5);
					int y = 100 + (int) (Math.sin(phase) * 60 + Math.sin(phase * 31) * (k % 3) * 10) + r.nextInt(3);
					if (i == 0) {
						first = y;
					}
					min = Math.min(min, y);
					max = Math.max(max, y);
					last = y;
					full.add(x);
					full.add(y);
				}
				int[] v = { first, min, max, last };
				for (int y : v) {
					env.add(x);
					env.add(y);
				}
			}
			for (int width = 1; width <= 2; width++) {
				for (int aa = 0; aa < 2; aa++) {
					int diff = diff(display, toArray(full), toArray(env), width, aa == 1, cols + 40);
					if (width == 1 && aa == 0) {
						assertEquals("aliased width 1, case " + k, 0, diff); //$NON-NLS-1$
						exactAliased1++;
					}
					report.append(k).append(':').append(width).append(aa == 1 ? "aa" : "").append('=').append(diff) //$NON-NLS-1$ //$NON-NLS-2$
							.append(' ');
				}
			}
		}
		System.out.println("envelope pixel diffs (case:width[aa]=pixels): " + report); //$NON-NLS-1$
		assertEquals(24, exactAliased1);
	}

	private static int[] toArray(List<Integer> l) {
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = l.get(i);
		}
		return a;
	}

	private static int diff(Display display, int[] a, int[] b, int width, boolean aa, int w) {
		int[] pa = render(display, a, width, aa, w);
		int[] pb = render(display, b, width, aa, w);
		int n = 0;
		for (int i = 0; i < pa.length; i++) {
			if (pa[i] != pb[i]) {
				n++;
			}
		}
		return n;
	}

	private static int[] render(Display display, int[] poly, int width, boolean aa, int w) {
		Image img = new Image(display, w, 220);
		GC gc = new GC(img);
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.fillRectangle(0, 0, w, 220);
		gc.setForeground(display.getSystemColor(SWT.COLOR_GREEN));
		gc.setAdvanced(true);
		gc.setAntialias(aa ? SWT.ON : SWT.OFF);
		gc.setLineWidth(width);
		gc.drawPolyline(poly);
		gc.dispose();
		org.eclipse.swt.graphics.ImageData d = img.getImageData();
		img.dispose();
		int[] px = new int[d.width * d.height];
		for (int y = 0; y < d.height; y++) {
			d.getPixels(0, y, d.width, px, y * d.width);
		}
		return px;
	}
}
