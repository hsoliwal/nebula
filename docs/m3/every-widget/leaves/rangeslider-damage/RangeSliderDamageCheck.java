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

import java.util.Arrays;

import org.eclipse.nebula.widgets.opal.rangeslider.RangeSlider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget opal RangeSlider D leaf (opal has no test plugins): after each value or knob
 * state change the slider's pixels equal a full repaint, and a small knob move repaints less than the whole control.
 * Horizontal and vertical. Run with the bundle classes and an SWT jar on the class path: java RangeSliderDamageCheck.
 * Prints PASSED or throws.
 */
public class RangeSliderDamageCheck {

	static long damaged;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(420, 420);
		RangeSlider horizontal = slider(shell, SWT.HORIZONTAL, 10, 10, 360, 60);
		RangeSlider vertical = slider(shell, SWT.VERTICAL, 10, 90, 60, 300);
		shell.open();
		spin(display, 300);
		long full = 0;
		for (RangeSlider s : new RangeSlider[] { horizontal, vertical }) {
			String name = s == horizontal ? "horizontal" : "vertical";
			Rectangle area = s.getClientArea();
			full = (long) area.width * area.height;
			int[][] steps = { { 22, 80 }, { 22, 74 }, { 5, 95 }, { 50, 51 }, { 49, 60 } };
			long smallest = Long.MAX_VALUE;
			for (int[] step : steps) {
				damaged = 0;
				s.setSelection(step[0], step[1]);
				spin(display, 60);
				smallest = Math.min(smallest, damaged);
				same(display, s, name + " setSelection(" + step[0] + ", " + step[1] + ")");
			}
			s.setFocus();
			spin(display, 60);
			same(display, s, name + " focus");
			Event move = new Event();
			move.x = area.width / 2;
			move.y = area.height / 2;
			s.notifyListeners(SWT.MouseMove, move);
			spin(display, 60);
			same(display, s, name + " hover");
			System.out.println(name + ": smallest step damage " + smallest + " of " + full + " pixels");
			check(smallest < full, name + ": a small knob move repaints less than the whole control (" + smallest
					+ " of " + full + ")");
		}
		shell.dispose();
		System.out.println("PASSED");
	}

	static RangeSlider slider(Shell shell, int style, int x, int y, int width, int height) {
		RangeSlider slider = new RangeSlider(shell, style);
		slider.setMinimum(0);
		slider.setMaximum(100);
		slider.setSelection(20, 80);
		slider.setBounds(x, y, width, height);
		slider.addListener(SWT.Paint, e -> damaged += (long) e.width * e.height);
		return slider;
	}

	/* the slider's pixels after the change equal its pixels after a full repaint */
	static void same(Display display, RangeSlider slider, String step) {
		int[] incremental = pixels(slider);
		slider.redraw();
		slider.update();
		spin(display, 30);
		int[] repainted = pixels(slider);
		check(Arrays.equals(incremental, repainted), step + ": the pixels equal a full repaint");
	}

	static int[] pixels(RangeSlider slider) {
		Rectangle area = slider.getClientArea();
		Image shot = new Image(slider.getDisplay(), area.width, area.height);
		GC gc = new GC(slider);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] row = new int[area.width * area.height];
		for (int y = 0; y < area.height; y++) {
			data.getPixels(0, y, area.width, row, y * area.width);
		}
		return row;
	}

	static void spin(Display display, int milliseconds) {
		long end = System.currentTimeMillis() + milliseconds;
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

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
