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
package org.eclipse.nebula.cwt.animation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.cwt.animation.effects.MoveScrollBar;
import org.eclipse.nebula.cwt.animation.effects.Resize;
import org.eclipse.nebula.cwt.animation.movement.LinearInOut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget H leaf of the cwt effects: an effect whose target cannot be seen (hidden, under a hidden ancestor, or in
 * a minimized shell) applies its end state and completes on its next tick instead of running its frame loop for its
 * whole length; a visible target still animates over the full length.
 */
public class HiddenEffectTest {

	private static final int LENGTH = 2000;
	private Display display;
	private Shell shell;
	private Composite holder;
	private Composite target;

	@Before
	public void setUp() {
		display = Display.getCurrent() != null ? Display.getCurrent() : new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		holder = new Composite(shell, SWT.NONE);
		target = new Composite(holder, SWT.NONE);
		target.setBounds(0, 0, 10, 10);
		shell.setSize(300, 300);
		shell.open();
		spin(100);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void aVisibleTargetAnimatesOverTheWholeLength() {
		final boolean[] stopped = new boolean[1];
		new AnimationRunner().runEffect(resize(stopped));
		spin(200);
		assertFalse("a visible resize is still running after 200 of 2000 ms", stopped[0]);
		assertTrue("a visible resize has not reached its end yet", target.getSize().x < 200);
	}

	@Test
	public void aHiddenTargetTakesItsEndStateOnTheNextTick() {
		target.setVisible(false);
		final boolean[] stopped = new boolean[1];
		new AnimationRunner().runEffect(resize(stopped));
		spin(200);
		assertTrue("the resize of a hidden control completes at once", stopped[0]);
		assertEquals(new Point(200, 200), target.getSize());
	}

	@Test
	public void aTargetUnderAHiddenAncestorTakesItsEndStateOnTheNextTick() {
		holder.setVisible(false);
		final boolean[] stopped = new boolean[1];
		new AnimationRunner().runEffect(resize(stopped));
		spin(200);
		assertTrue("the resize under a hidden ancestor completes at once", stopped[0]);
		assertEquals(new Point(200, 200), target.getSize());
	}

	@Test
	public void aTargetInAMinimizedShellTakesItsEndStateOnTheNextTick() {
		shell.setMinimized(true);
		spin(300);
		if (!shell.getMinimized()) {
			return; // the desktop refused to minimize: nothing to check
		}
		final boolean[] stopped = new boolean[1];
		new AnimationRunner().runEffect(resize(stopped));
		spin(200);
		assertTrue("the resize in a minimized shell completes at once", stopped[0]);
		assertEquals(new Point(200, 200), target.getSize());
	}

	@Test
	public void aSmoothScrollOfAHiddenScrollableEndsAtOnce() {
		final Composite scrollable = new Composite(holder, SWT.V_SCROLL);
		final ScrollBar bar = scrollable.getVerticalBar();
		bar.setValues(0, 0, 100, 10, 1, 10);
		scrollable.setVisible(false);
		final boolean[] stopped = new boolean[1];
		final MoveScrollBar move = new MoveScrollBar(bar, 0, 50, LENGTH, new LinearInOut(), () -> stopped[0] = true, null);
		move.doEffect();
		assertTrue("the smooth scroll of a hidden scrollable ends on its first tick", move.isDone() && stopped[0]);
		assertEquals(50, bar.getSelection());
	}

	private Resize resize(boolean[] stopped) {
		return new Resize(target, new Point(10, 10), new Point(200, 200), LENGTH, new LinearInOut(), () -> stopped[0] = true,
				null);
	}

	private void spin(int milliseconds) {
		final long end = System.currentTimeMillis() + milliseconds;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					return;
				}
			}
		}
	}
}
