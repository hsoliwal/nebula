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
package org.eclipse.nebula.widgets.timeline;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.nebula.widgets.timeline.jface.TimelineViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link TimelineViewport} on a display (docs/m3/every-widget, aspect H): a hidden timeline neither refreshes its
 * binding nor lays out or paints Draw2D, and after the reveal it shows what a timeline that stayed shown shows.
 */
public class TimelineViewportTest {

	private Display display;
	private final List<Shell> shells = new ArrayList<>();

	@Before
	public void setUp() {
		assumeTrue("viewport mode", TimelineViewport.ENABLED);
		display = Display.getDefault();
	}

	@After
	public void tearDown() {
		shells.forEach(Shell::dispose);
	}

	private TimelineViewer viewer(int x) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shells.add(shell);
		shell.setLayout(new FillLayout());
		shell.setBounds(x, 10, 450, 300);
		TimelineViewer viewer = new TimelineViewer(shell, SWT.NULL);
		new TimelineDataBinding(viewer, (ITimeline) viewer.getInput()); // model changes refresh the viewer (300 ms job)
		shell.open();
		spin(400);
		return viewer;
	}

	/* the same model in every viewer: seeded events in two tracks of three lanes */
	private static void populate(TimelineViewer viewer) {
		ITimeline model = (ITimeline) viewer.getInput();
		List<ILane> lanes = new ArrayList<>();
		for (String name : new String[] { "Layer 1", "Layer 2" }) {
			ITrack track = model.createTrack(name);
			for (int i = 0; i < 3; i++) {
				lanes.add(track.createLane());
			}
		}
		Random random = new Random(7);
		int position = 0;
		for (int item = 0; item < 40; item++) {
			int offset = random.nextInt(20), width = 1 + random.nextInt(150);
			lanes.get(random.nextInt(lanes.size())).createEvent("Item " + item, "event " + item, position + offset, width,
					TimeUnit.NANOSECONDS);
			position += offset + width;
		}
	}

	/* runs the event loop for at least the given time: the binding job (300 ms) syncExecs into this thread */
	private void spin(long millis) {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private static int[] pixels(Control control) {
		Rectangle size = control.getBounds();
		Image image = new Image(control.getDisplay(), size.width, size.height);
		GC gc = new GC(control);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageData data = image.getImageData();
		image.dispose();
		int[] out = new int[size.width * size.height];
		for (int y = 0; y < size.height; y++) {
			data.getPixels(0, y, size.width, out, y * size.width);
		}
		return out;
	}

	@Test
	public void hiddenTimelineWaitsAndShowsWhatAShownOneShows() {
		TimelineViewer hidden = viewer(10), shown = viewer(480);
		TimelineComposite parked = hidden.getControl();
		List<Object> paints = new ArrayList<>();
		for (Control child : parked.getChildren()) {
			child.addListener(SWT.Paint, e -> paints.add(e));
		}
		int reveals = parked.viewport.reveals, resumed = parked.viewport.jobsResumed; // the shell's first show is a reveal
		parked.setVisible(false);
		populate(hidden);
		populate(shown);
		spin(1500);
		assertTrue("binding jobs parked while hidden: " + parked.viewport.jobsParked, parked.viewport.jobsParked > 0);
		assertEquals("binding jobs of a shown timeline are never parked", 0, shown.getControl().viewport.jobsParked);
		assertEquals("paints while hidden", 0, paints.size());
		parked.setVisible(true);
		spin(1500);
		assertEquals("one reveal", reveals + 1, parked.viewport.reveals);
		assertEquals("the parked binding job scheduled once", resumed + 1, parked.viewport.jobsResumed);
		assertTrue("paints after show", paints.size() > 0);
		assertArrayEquals("pixels after show", pixels(shown.getControl()), pixels(parked));
	}
}
