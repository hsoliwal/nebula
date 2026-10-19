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
package org.eclipse.nebula.snippets.gallery;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A virtual Gallery of 1,000,000 items in 10,000 groups of 100: groups and items are created on SWT.SetData when
 * they are first painted or asked for; nothing else is allocated up front. The title shows the time to create and
 * paint. Arguments: the item count (default 1000000); <code>-Dm3.snippet.screenshot=&lt;file.png&gt;</code> saves the
 * shell's pixels and exits.
 */
public class GalleryMillionSnippet {

	static final int GROUP = 100;

	public static void main(String[] args) {
		int count = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Image[] images = { image(display, SWT.COLOR_DARK_BLUE), image(display, SWT.COLOR_DARK_GREEN), image(display, SWT.COLOR_DARK_RED) };
		Gallery gallery = new Gallery(shell, SWT.V_SCROLL | SWT.MULTI | SWT.VIRTUAL);
		DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer();
		groupRenderer.setMinMargin(2);
		groupRenderer.setItemHeight(56);
		groupRenderer.setItemWidth(72);
		groupRenderer.setAutoMargin(true);
		gallery.setGroupRenderer(groupRenderer);
		gallery.setItemRenderer(new DefaultGalleryItemRenderer());
		gallery.addListener(SWT.SetData, e -> {
			GalleryItem item = (GalleryItem) e.item;
			if (item.getParentItem() == null) {
				item.setText("Group " + e.index);
				item.setItemCount(GROUP);
			} else {
				item.setText("Item " + (gallery.indexOf(item.getParentItem()) * GROUP + e.index));
				item.setImage(images[e.index % images.length]);
			}
		});
		long start = System.nanoTime();
		int groups = Math.max(1, count / GROUP);
		gallery.setItemCount(groups);
		for (int g = 0; g < groups; g++) {
			gallery.getItem(g).setExpanded(true);
		}
		shell.setSize(640, 480);
		shell.open();
		gallery.update();
		shell.setText("Gallery: " + groups * GROUP + " virtual items in " + groups + " groups, " + (System.nanoTime() - start) / 1_000_000 + " ms to create and paint");
		runOrSave(shell, display);
		for (Image image : images) {
			image.dispose();
		}
		display.dispose();
	}

	static Image image(Display display, int color) {
		Image image = new Image(display, 32, 32);
		GC gc = new GC(image);
		gc.setBackground(display.getSystemColor(color));
		gc.fillRectangle(0, 0, 32, 32);
		gc.dispose();
		return image;
	}

	/* the event loop, or with -Dm3.snippet.screenshot=<file.png> one settled paint, a PNG of the shell and an exit */
	static void runOrSave(Shell shell, Display display) {
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
