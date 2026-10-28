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

import org.eclipse.nebula.widgets.m3.M3ImageView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * An {@link M3ImageView} over a generated 4000 x 3000 image: drag to pan (only exposed strips repaint), Ctrl+wheel to
 * zoom around the pointer. <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the shell's pixels and exits.
 */
public class M3ImageViewSnippet {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("M3ImageView: 4000 x 3000");
		shell.setLayout(new FillLayout());
		ImageData data = new ImageData(4000, 3000, 24, new PaletteData(0xff0000, 0xff00, 0xff));
		int[] row = new int[4000];
		for (int y = 0; y < 3000; y++) {
			for (int x = 0; x < 4000; x++) {
				int ring = (int) Math.hypot(x - 2000, y - 1500) / 40;
				row[x] = (ring % 2 == 0 ? 0x2060c0 : 0xf0c040) ^ ((x / 250 + y / 250) % 2 == 0 ? 0 : 0x101010);
			}
			data.setPixels(0, y, 4000, row, 0);
		}
		Image image = new Image(display, data);
		M3ImageView view = new M3ImageView(shell, SWT.NONE);
		view.setImage(image);
		shell.setSize(640, 480);
		shell.open();
		view.fitToWindow();
		shell.addListener(SWT.Dispose, e -> image.dispose());
		Screenshot.runOrSave(shell, display);
	}
}
