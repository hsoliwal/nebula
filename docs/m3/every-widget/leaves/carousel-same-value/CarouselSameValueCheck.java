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

import org.eclipse.nebula.widgets.carousel.Carousel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D check of the Carousel: a programmatic setSelection shows the new image and its dot at once (before,
 * the old image and dot stayed until an unrelated repaint); a new colour repaints to the pixels of a full repaint; the
 * colours and the selection in use repaint nothing (before: one repaint per call).
 */
public class CarouselSameValueCheck {

	static int paints;

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setSize(360, 300);
		Carousel bar = new Carousel(shell, SWT.NONE);
		Image[] slides = new Image[3];
		int[] colours = { SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_YELLOW };
		for (int i = 0; i < slides.length; i++) {
			slides[i] = new Image(display, 200, 120);
			GC gc = new GC(slides[i]);
			gc.setBackground(display.getSystemColor(colours[i]));
			gc.fillRectangle(0, 0, 200, 120);
			gc.dispose();
			bar.addImage(slides[i]);
		}
		bar.setBounds(20, 20, 300, 220);
		Color red = display.getSystemColor(SWT.COLOR_RED);
		countPaints(bar);
		shell.open();
		spin(display);

		bar.setArrowColor(red);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setArrowColor: the pixels equal a full repaint");
		bar.setCircleBackground(red);
		spin(display);
		check(sameAsFullRepaint(display, bar), "setCircleBackground: the pixels equal a full repaint");
		bar.setSelection(1);
		for (int i = 0; i < 10; i++) {
			spin(display); // the slide to the new image is animated
		}
		check(sameAsFullRepaint(display, bar), "setSelection: the pixels equal a full repaint");

		paints = 0;
		bar.setArrowColor(red);
		bar.setCircleBackground(red);
		bar.setSelection(1);
		spin(display);
		System.out.println("paints for the values in use: " + paints);
		check(paints == 0, "the values in use paint nothing, got " + paints);

		shell.dispose();
		for (Image slide : slides) {
			slide.dispose();
		}
		display.dispose();
		System.out.println("PASSED");
	}

	static void countPaints(Control control) {
		control.addListener(SWT.Paint, e -> paints++);
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				countPaints(child);
			}
		}
	}

	static boolean sameAsFullRepaint(Display display, Carousel bar) throws InterruptedException {
		int[] incremental = pixels(display, bar);
		bar.redraw(0, 0, bar.getSize().x, bar.getSize().y, true);
		bar.update();
		spin(display);
		return Arrays.equals(incremental, pixels(display, bar));
	}

	static int[] pixels(Display display, Carousel bar) {
		Rectangle size = bar.getBounds();
		Image shot = new Image(display, size.width, size.height);
		GC gc = new GC(bar);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[size.width * size.height];
		for (int y = 0; y < size.height; y++) {
			data.getPixels(0, y, size.width, all, y * size.width);
		}
		return all;
	}

	static void spin(Display display) throws InterruptedException {
		long end = System.currentTimeMillis() + 200;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				Thread.sleep(5);
			}
		}
	}

	static void check(boolean condition, String message) {
		System.out.println((condition ? "ok   " : "FAIL ") + message);
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
