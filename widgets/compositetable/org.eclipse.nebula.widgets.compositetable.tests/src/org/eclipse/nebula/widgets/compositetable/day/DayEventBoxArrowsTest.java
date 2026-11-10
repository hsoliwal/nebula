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
package org.eclipse.nebula.widgets.compositetable.day;

import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.nebula.widgets.compositetable.day.internal.DayEditorCalendarableItemControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget D leaf of the day editor's event box: clipping and continued changes repaint the arrow strips (to the
 * pixels of a full repaint), an unchanged value repaints nothing, and a continued change shows its arrows at once.
 */
public class DayEventBoxArrowsTest extends TestCase {

	private Display display;
	private Shell shell;
	private DayEditorCalendarableItemControl box;
	private boolean marginRepainted;
	private int paints;

	@Override
	protected void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		box = new DayEditorCalendarableItemControl(shell, SWT.NONE);
		box.setText("Meeting");
		box.setBounds(10, 10, 160, 60);
		box.addListener(SWT.Paint, e -> {
			paints++;
			// the left margin at mid-height: inside the box, outside the label and the arrow strips
			Region clip = new Region(display);
			e.gc.getClipping(clip);
			marginRepainted |= clip.contains(1, box.getClientArea().height / 2);
			clip.dispose();
		});
		shell.setSize(220, 120);
		shell.open();
		spin();
		box.update();
		spin();
	}

	@Override
	protected void tearDown() throws Exception {
		shell.dispose();
	}

	public void testClippingRepaintsTheArrowStrips() {
		reset();
		box.setClipping(SWT.TOP);
		spin();
		assertTrue("a clipping change repaints", paints > 0);
		assertFalse("a clipping change repaints the arrow strips, not the margin between them", marginRepainted);
		assertSameAsFullRepaint("clipping TOP");
		reset();
		box.setClipping(SWT.TOP);
		spin();
		assertEquals("the same clipping repaints nothing", 0, paints);
	}

	public void testAContinuedChangeShowsItsArrows() {
		reset();
		box.setContinued(SWT.BOTTOM);
		spin();
		assertSameAsFullRepaint("continued BOTTOM");
		reset();
		box.setContinued(SWT.BOTTOM);
		spin();
		assertEquals("the same continued repaints nothing", 0, paints);
	}

	private void reset() {
		paints = 0;
		marginRepainted = false;
	}

	private void assertSameAsFullRepaint(String step) {
		int[] incremental = pixels();
		box.redraw();
		box.update();
		spin();
		assertTrue(step + ": the pixels equal a full repaint", Arrays.equals(incremental, pixels()));
	}

	private int[] pixels() {
		Rectangle area = box.getClientArea();
		Image shot = new Image(display, area.width, area.height);
		GC gc = new GC(box);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int[] all = new int[area.width * area.height];
		for (int y = 0; y < area.height; y++) {
			data.getPixels(0, y, area.width, all, y * area.width);
		}
		return all;
	}

	private void spin() {
		long end = System.currentTimeMillis() + 150;
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
}
