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
package org.eclipse.nebula.widgets.gallery.tests;

import java.util.Arrays;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assume;
import org.junit.Test;

/**
 * Oracle of the Gallery viewport (docs/m3/GALLERY_VIEWPORT.md): what a Gallery with many items costs per operation
 * (create, paint, scroll a page, select and show a far item, hit test, redraw one item, collapse and expand a group,
 * indexOf the last item, remove the first item, append one more item), for explicit items and for SWT.VIRTUAL. Runs
 * against any Gallery and SWT jar; prints one line per size, medians in microseconds. Opt-in:
 * <code>-Dnebula.gallery.oracle=true</code>; sizes with <code>-Dnebula.gallery.oracle.counts=10000,100000</code>
 * (items, in groups of 100).
 */
public class GalleryScaleOracleTest {

	static final int GROUP = 100, APPEND = 200;

	@Test
	public void measure () {
		Assume.assumeTrue ("-Dnebula.gallery.oracle=true is not set", Boolean.getBoolean ("nebula.gallery.oracle"));
		Display display = Display.getDefault ();
		int [] counts = Arrays.stream (System.getProperty ("nebula.gallery.oracle.counts", "10000,100000").split (",")).mapToInt (Integer::parseInt).toArray ();
		Image image = new Image (display, 32, 32);
		GC gc = new GC (image);
		gc.setBackground (display.getSystemColor (SWT.COLOR_DARK_BLUE));
		gc.fillRectangle (0, 0, 32, 32);
		gc.dispose ();
		try {
			for (int count : counts) {
				for (boolean virtual : new boolean [] {false, true}) {
					measure (display, image, count, virtual);
				}
			}
		} finally {
			image.dispose ();
		}
	}

	void measure (Display display, Image image, int count, boolean virtual) {
		Shell shell = new Shell (display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setBounds (40, 40, 640, 480);
		shell.setLayout (new FillLayout ());
		Gallery gallery = new Gallery (shell, SWT.V_SCROLL | SWT.MULTI | (virtual ? SWT.VIRTUAL : SWT.NONE));
		DefaultGalleryGroupRenderer groupRenderer = new DefaultGalleryGroupRenderer ();
		groupRenderer.setMinMargin (2);
		groupRenderer.setItemHeight (56);
		groupRenderer.setItemWidth (72);
		groupRenderer.setAutoMargin (true);
		gallery.setGroupRenderer (groupRenderer);
		gallery.setItemRenderer (new DefaultGalleryItemRenderer ());
		int groups = Math.max (1, count / GROUP);
		if (virtual) {
			gallery.addListener (SWT.SetData, e -> {
				GalleryItem item = (GalleryItem) e.item;
				if (item.getParentItem () == null) {
					/* no setExpanded here: it lays out every group, which asks for the data of the next one (recursion) */
					item.setText ("group " + e.index);
					item.setItemCount (GROUP);
				} else {
					item.setText ("item " + e.index);
					item.setImage (image);
				}
			});
		}
		shell.open ();
		drain (display);
		long start = System.nanoTime ();
		if (virtual) {
			gallery.setItemCount (groups);
			for (int g = 0; g < groups; g++) {
				gallery.getItem (g).setExpanded (true);
			}
		} else {
			for (int g = 0; g < groups; g++) {
				GalleryItem group = new GalleryItem (gallery, SWT.NONE);
				group.setText ("group " + g);
				group.setExpanded (true);
				for (int i = 0; i < GROUP; i++) {
					GalleryItem item = new GalleryItem (group, SWT.NONE);
					item.setText ("item " + i);
					item.setImage (image);
				}
			}
		}
		drain (display);
		gallery.update ();
		long create = (System.nanoTime () - start) / 1000;
		long [] paint = new long [7], scroll = new long [7], select = new long [7], hit = new long [7], redraw = new long [7], collapse = new long [7],
			index = new long [7], remove = new long [7];
		GalleryItem far = gallery.getItem (groups - 1).getItem (GROUP / 2);
		for (int k = 0; k < paint.length; k++) {
			start = System.nanoTime ();
			gallery.redraw ();
			gallery.update ();
			paint [k] = (System.nanoTime () - start) / 1000;
			start = System.nanoTime ();
			int position = gallery.getVerticalBar ().getSelection () + gallery.getVerticalBar ().getPageIncrement ();
			gallery.getVerticalBar ().setSelection (position);
			gallery.getVerticalBar ().notifyListeners (SWT.Selection, new org.eclipse.swt.widgets.Event ());
			gallery.update ();
			scroll [k] = (System.nanoTime () - start) / 1000;
			GalleryItem target = gallery.getItem ((k * 37) % groups).getItem ((k * 11) % GROUP);
			start = System.nanoTime ();
			gallery.setSelection (new GalleryItem [] {target});
			gallery.showItem (target);
			drain (display);
			gallery.update ();
			select [k] = (System.nanoTime () - start) / 1000;
			start = System.nanoTime ();
			for (int x = 0; x < 100; x++) {
				gallery.getItem (new Point (5 + x * 6, 5 + (x * 13) % 470));
			}
			hit [k] = (System.nanoTime () - start) / 1000 / 100;
			start = System.nanoTime ();
			gallery.redraw (target);
			gallery.update ();
			redraw [k] = (System.nanoTime () - start) / 1000;
			GalleryItem group = gallery.getItem ((k * 53) % groups);
			start = System.nanoTime ();
			group.setExpanded (false);
			drain (display);
			gallery.update ();
			group.setExpanded (true);
			drain (display);
			gallery.update ();
			collapse [k] = (System.nanoTime () - start) / 1000;
			start = System.nanoTime ();
			far.getParentItem ().indexOf (far);
			gallery.indexOf (gallery.getItem (groups - 1));
			index [k] = (System.nanoTime () - start) / 1000;
			if (!virtual) {
				start = System.nanoTime ();
				gallery.getItem (0).getItem (0).dispose ();
				drain (display);
				remove [k] = (System.nanoTime () - start) / 1000;
			}
		}
		long append = 0;
		if (!virtual) {
			GalleryItem last = gallery.getItem (groups - 1);
			start = System.nanoTime ();
			for (int i = 0; i < APPEND; i++) {
				GalleryItem item = new GalleryItem (last, SWT.NONE);
				item.setText ("appended " + i);
				item.setImage (image);
			}
			drain (display);
			append = (System.nanoTime () - start) / 1000 / APPEND;
		}
		System.out.println ("gallery-oracle " + mode () + (virtual ? " virtual" : " items") + " " + groups * GROUP + " create " + create + " paint "
			+ median (paint) + " scroll " + median (scroll) + " select " + median (select) + " hit " + median (hit) + " redraw " + median (redraw)
			+ " collapse " + median (collapse) + " indexOf " + median (index) + " remove " + median (remove) + " append " + append);
		shell.dispose ();
	}

	/* the Gallery mode the startup properties ask for, or "stock" for a Gallery without modes */
	static String mode () {
		try {
			Class.forName ("org.eclipse.nebula.widgets.gallery.internal.WidgetMode");
			String value = System.getProperty ("swt.widget.mode.Gallery", System.getProperty ("swt.widget.mode", "viewport"));
			return value.toLowerCase (java.util.Locale.ROOT);
		} catch (ClassNotFoundException e) {
			return "stock";
		}
	}

	static void drain (Display display) {
		while (display.readAndDispatch ()) {
			// dispatch everything queued
		}
	}

	static long median (long [] values) {
		long [] sorted = values.clone ();
		Arrays.sort (sorted);
		return sorted [sorted.length / 2];
	}
}
