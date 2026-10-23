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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** GALLERY on RWT: a virtual Nebula Gallery of 100,000 captioned items in one group. */
public final class GalleryExample extends RwtExample {

	static final int ITEMS = 100_000;

	public GalleryExample () {
		super ("GALLERY", "org.eclipse.nebula.widgets.gallery.Gallery");
	}

	@Override
	public Control create (Composite parent) {
		Gallery gallery = new Gallery (parent, SWT.VIRTUAL | SWT.V_SCROLL | SWT.MULTI);
		NoGroupRenderer groups = new NoGroupRenderer ();
		groups.setItemSize (96, 72);
		groups.setAutoMargin (true);
		gallery.setGroupRenderer (groups);
		gallery.setItemRenderer (new DefaultGalleryItemRenderer ());
		gallery.addListener (SWT.SetData, event -> {
			GalleryItem item = (GalleryItem) event.item;
			if (item.getParentItem () == null) {
				item.setText ("All");
				item.setItemCount (ITEMS);
				item.setExpanded (true);
			} else {
				item.setText (RwtSampleData.cell (item.getParentItem ().indexOf (item), 0));
			}
		});
		gallery.setItemCount (1);
		return gallery;
	}

	@Override
	public void change (Control control) {
		((Gallery) control).getItem (0).setText ("All items " + next ());
	}
}
