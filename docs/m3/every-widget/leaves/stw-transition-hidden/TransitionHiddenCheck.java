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

import org.eclipse.nebula.effects.stw.Transition;
import org.eclipse.nebula.effects.stw.TransitionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Acceptance check of the every-widget STW Transition H leaf (the stw bundle has no test plugin): a transition whose
 * canvas cannot be seen returns at once instead of blocking the UI thread in its frame loop (sleep, redraw, update and
 * readAndDispatch per frame) for its whole length, and a visible transition still draws its frames for its whole
 * length. Run with the bundle classes and an SWT jar on the class
 * path: java TransitionHiddenCheck. Prints PASSED or throws.
 */
public class TransitionHiddenCheck {

	static final long LENGTH = 1500;

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		Composite holder = new Composite(shell, SWT.NONE);
		holder.setBounds(0, 0, 200, 200);
		CTabFolder folder = new CTabFolder(holder, SWT.NONE);
		folder.setBounds(0, 0, 200, 200);
		TransitionManager manager = new TransitionManager(folder);
		Image from = new Image(display, 100, 100);
		Image to = new Image(display, 100, 100);
		shell.setSize(300, 300);
		shell.open();

		int[] hidden = run(manager, from, to, holder, false);
		System.out.println("hidden: " + hidden[0] + " steps, " + hidden[1] + " end frames, " + hidden[2] + " ms");
		// a hidden canvas gets no paint events, so no frame is drawn either way; the frame loop blocked the UI thread
		check(hidden[2] < LENGTH / 3, "a hidden transition returns at once (" + hidden[2] + " ms)");

		int[] shown = run(manager, from, to, holder, true);
		System.out.println("shown: " + shown[0] + " steps, " + shown[1] + " end frames, " + shown[2] + " ms");
		check(shown[0] > 10 && shown[2] >= LENGTH - 100, "a visible transition runs its frames for its whole length");

		from.dispose();
		to.dispose();
		shell.dispose();
		System.out.println("PASSED");
	}

	/* {steps, end frames, elapsed ms} of one transition on a canvas in the holder, the holder visible or hidden */
	static int[] run(TransitionManager manager, Image from, Image to, Composite holder, boolean visible) {
		final int[] counts = new int[2];
		Transition transition = new Transition(manager, 50, LENGTH) {
			@Override
			protected void initTransition(Image f, Image t, GC gc, double direction) {
				// nothing to prepare
			}

			@Override
			protected void stepTransition(long time, Image f, Image t, GC gc, double direction) {
				counts[0]++;
			}

			@Override
			protected void endTransition(Image f, Image t, GC gc, double direction) {
				counts[1]++;
			}
		};
		holder.setVisible(visible);
		Canvas canvas = new Canvas(holder, SWT.DOUBLE_BUFFERED);
		canvas.setBounds(0, 0, 100, 100);
		canvas.moveAbove(null);
		long start = System.currentTimeMillis();
		transition.start(from, to, canvas, 0);
		long elapsed = System.currentTimeMillis() - start;
		canvas.dispose();
		return new int[] { counts[0], counts[1], (int) elapsed };
	}

	static void check(boolean condition, String message) {
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
