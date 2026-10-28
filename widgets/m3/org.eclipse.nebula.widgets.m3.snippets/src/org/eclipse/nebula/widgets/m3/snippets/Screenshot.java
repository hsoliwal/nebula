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
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.snippets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * The event loop of a snippet, or, with <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code>, one settled paint, a
 * PNG of the shell's client area (the documentation screenshots, taken on the hidden desktop) and an exit.
 */
public final class Screenshot {

	private Screenshot() {
	}

	public static void runOrSave(Shell shell, Display display) {
		String file = System.getProperty("m3.snippet.screenshot");
		if (file == null) {
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			return;
		}
		long end = System.currentTimeMillis() + 300;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		Point size = shell.getSize();
		Image image = new Image(display, Math.max(1, size.x), Math.max(1, size.y));
		GC gc = new GC(shell);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageLoader loader = new ImageLoader();
		loader.data = new org.eclipse.swt.graphics.ImageData[] { image.getImageData() };
		loader.save(file, SWT.IMAGE_PNG);
		image.dispose();
		shell.dispose();
	}
}
