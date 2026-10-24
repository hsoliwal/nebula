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
package org.eclipse.nebula.widgets.tiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Aspect H of the Tiles viewport (docs/m3/every-widget): the animation timer runs only during a transition while the
 * widget is shown; a transition started or running while the widget is hidden parks, and after the reveal the widget
 * reaches the same final frame as the stock one (frame time is wall-clock based). Skips in the original mode.
 */
public class TilesViewportTest {

	private static final int DURATION = 400;

	private Display display;
	private Shell shell;
	private Composite holder;
	private Tiles<String> tiles;

	@Before
	public void setUp() {
		Assume.assumeTrue("Tiles viewport mode", TilesViewport.ENABLED);
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		tiles = new Tiles<>(holder, SWT.NONE);
		tiles.setAnimationSettings(new TileAnimationSettings(30, DURATION));
		tiles.setItems(items(40));
		tiles.update();
		shell.setSize(400, 300);
		shell.open();
		assertNotNull("viewport installed", tiles.viewport);
		// the stock resize debounce (ResizeListener: a 500 ms timer that reschedules itself once more when it fires
		// exactly 500 ms after the resize) rebuilds the frames and starts a transition about a second after the shell
		// opens: let it start and run to its end, then count from zero
		pump(1300);
		pumpUntilIdle(DURATION * 5);
		tiles.viewport.ticks = 0;
		tiles.viewport.parks = 0;
	}

	@After
	public void tearDown() {
		if (shell != null) {
			shell.dispose();
		}
	}

	@Test
	public void anIdleWidgetRunsNoTimer() {
		pump(300);
		assertFalse("no transition", tiles.m3Animating());
		assertEquals("no animation step while idle", 0, tiles.viewport.ticks);
	}

	@Test
	public void aTransitionTicksWhileShownAndTheTimerStopsAfterIt() {
		startTransition();
		assertTrue(tiles.m3Animating());
		pumpUntilIdle(DURATION * 5);
		assertFalse("the transition ended", tiles.m3Animating());
		final int ticks = tiles.viewport.ticks;
		assertTrue("the transition was stepped: " + ticks, ticks > 0);
		pump(300);
		assertEquals("no step after the transition", ticks, tiles.viewport.ticks);
	}

	@Test
	public void aHiddenTransitionParksAndCatchesUpOnShow() {
		startTransition();
		tiles.setVisible(false);
		pump(DURATION + 300);
		assertEquals("no animation step while hidden", 0, tiles.viewport.ticks);
		assertTrue("parked", tiles.viewport.isParked());
		assertTrue("the transition is still current", tiles.m3Animating());
		tiles.setVisible(true);
		pumpUntilIdle(DURATION * 5);
		assertFalse("the transition ended after the reveal", tiles.m3Animating());
		assertTrue("the first steps after the reveal caught up with the hidden time: " + tiles.viewport.ticks,
				tiles.viewport.ticks <= 3);
	}

	@Test
	public void aTransitionUnderAHiddenAncestorParksAndResumesWhenItShows() {
		holder.setVisible(false);
		startTransition();
		pump(DURATION + 300);
		assertEquals("no animation step under a hidden ancestor", 0, tiles.viewport.ticks);
		assertTrue("parked", tiles.viewport.isParked());
		holder.setVisible(true);
		pumpUntilIdle(DURATION * 5);
		assertFalse("the transition ended after the ancestor showed", tiles.m3Animating());
	}

	private void startTransition() {
		tiles.setItems(items(25));
		tiles.update();
	}

	private static List<String> items(final int n) {
		final List<String> items = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			items.add("item " + i);
		}
		return items;
	}

	private void pump(final long millis) {
		final long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}

	private void pumpUntilIdle(final long maxMillis) {
		final long end = System.currentTimeMillis() + maxMillis;
		while (tiles.m3Animating() && System.currentTimeMillis() < end) {
			pump(20);
		}
	}
}
