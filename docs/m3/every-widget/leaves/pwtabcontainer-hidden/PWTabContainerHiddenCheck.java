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
package org.eclipse.nebula.widgets.opal.preferencewindow;

import java.util.Collections;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget opal PWTabContainer H leaf (opal has no test plugins): resizes of the tab
 * button strip, also while it is hidden, build no background image, and the strip still shows its grey bottom line on
 * white. In the PWTabContainer package because the container is package-private. Run with the bundle classes and an
 * SWT jar on the class path: java org.eclipse.nebula.widgets.opal.preferencewindow.PWTabContainerHiddenCheck.
 * Prints PASSED or throws.
 */
public class PWTabContainerHiddenCheck {

	public static void main(String[] args) {
		Display display = Display.getDefault();
		PreferenceWindow window = PreferenceWindow.create(new HashMap<String, Object>());
		PWTab tab = window.addTab(null, "General");
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		PWTabContainer tabs = new PWTabContainer(holder, SWT.NONE, Collections.singletonList(tab));
		tabs.build();
		Composite strip = (Composite) tabs.getChildren()[0];

		holder.setVisible(false);
		shell.open();
		Image before = strip.getBackgroundImage();
		for (int width = 400; width <= 500; width += 10) {
			shell.setSize(width, 300);
			spin(display, 20);
		}
		Image after = strip.getBackgroundImage();
		check(after == before, "hidden resizes of the tab strip build no background image");

		holder.setVisible(true);
		shell.setSize(520, 300);
		spin(display, 300);
		Rectangle area = strip.getClientArea();
		check(area.height > 2, "the strip is laid out (" + area + ")");
		Image shot = new Image(display, area.width, area.height);
		GC gc = new GC(strip);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		RGB line = data.palette.getRGB(data.getPixel(area.width - 4, area.height - 1));
		RGB above = data.palette.getRGB(data.getPixel(area.width - 4, area.height - 2));
		shot.dispose();
		System.out.println("strip " + area.width + "x" + area.height + ": line " + line + ", above " + above);
		check(line.equals(new RGB(204, 204, 204)), "the strip shows its grey bottom line, got " + line);
		check(above.equals(new RGB(255, 255, 255)), "above the line the strip is white, got " + above);
		shell.dispose();
		System.out.println("PASSED");
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
