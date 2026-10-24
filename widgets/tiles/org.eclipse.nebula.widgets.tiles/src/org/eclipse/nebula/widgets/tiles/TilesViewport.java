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

import org.eclipse.nebula.widgets.tiles.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/**
 * Viewport of a {@link Tiles} widget, aspect H (hidden gating, docs/m3/every-widget): the stock widget arms a 33 ms
 * timer in its constructor and re-arms it for its whole life, so it ticks about 30 times a second while idle and keeps
 * stepping and redrawing transitions while hidden. Here the timer runs only while a transition frame is current and the
 * widget is shown: it is armed when {@link Tiles#update()} starts a transition, parked while the widget is hidden
 * (its own or an ancestor's visibility, a zero size, a minimized shell, or SWT.PartHidden on the M3 SWT fork), and
 * re-armed by the next Show, Resize, Paint or SWT.PartVisible. A frame's time is wall-clock based
 * ({@link FrameDynamic#tick()}), so the first tick after a reveal catches up with the time spent hidden and the widget
 * shows the same frame as the stock one would.
 * <p>
 * {@code -Dswt.widget.mode.Tiles=original} (or the global {@code swt.widget.mode=original}) keeps the stock timer.
 * </p>
 */
final class TilesViewport<T> {

	/** True unless the startup mode of Tiles is original. */
	static final boolean ENABLED = WidgetMode.of("Tiles", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	/* SWT.PartVisible and Control.isPartVisible of the M3 SWT fork (brief rule 25); -1 and absent on stock SWT */
	private static final int PART_VISIBLE = partEvent("PartVisible"); //$NON-NLS-1$
	private static final java.lang.reflect.Method IS_PART_VISIBLE = partVisibleMethod();

	private final Tiles<T> tiles;
	private final Runnable ticker = this::tick;
	private boolean armed;
	private boolean parked;

	/** Animation steps run (tests and benches). */
	int ticks;
	/** Timer firings that found the widget hidden and parked the animation (tests and benches). */
	int parks;

	TilesViewport(final Tiles<T> tiles) {
		this.tiles = tiles;
		tiles.addListener(SWT.Show, e -> resume());
		tiles.addListener(SWT.Resize, e -> resume());
		// an ancestor shown again: SWT sends no Show to the child, but the first paint after the reveal arrives
		tiles.addListener(SWT.Paint, e -> resume());
		if (PART_VISIBLE >= 0) {
			tiles.addListener(PART_VISIBLE, e -> resume());
		}
		tiles.addListener(SWT.Dispose, e -> tiles.getDisplay().timerExec(-1, ticker));
	}

	/** {@link Tiles#update()} made a transition frame current: tick it while the widget is shown. */
	void animationStarted() {
		parked = false;
		arm();
	}

	/** @return true while the animation is parked because the widget is hidden */
	boolean isParked() {
		return parked;
	}

	private void arm() {
		if (armed || tiles.isDisposed()) {
			return;
		}
		armed = true;
		tiles.getDisplay().timerExec(tiles.getAnimationSettings().getDelta(), ticker);
	}

	private void tick() {
		armed = false;
		if (tiles.isDisposed() || !tiles.m3Animating()) {
			return; // idle: no timer until the next transition
		}
		if (!shown()) {
			// hidden: no frame update and no redraw. The frame's wall clock starts at its first tick (FrameDynamic.tick
			// sets its start time then), so a transition parked before it ever ticked is anchored here; the first tick
			// after the reveal then adds the time spent hidden, as the stock timer's ticks would have.
			tiles.m3Anchor();
			parked = true;
			parks++;
			return;
		}
		ticks++;
		tiles.m3Tick();
		if (tiles.m3Animating()) {
			arm();
		}
	}

	private void resume() {
		if (parked && !tiles.isDisposed() && tiles.m3Animating() && shown()) {
			parked = false;
			arm();
		}
	}

	private boolean shown() {
		if (!tiles.isVisible()) {
			return false; // the widget or an ancestor is hidden (setVisible, tab and stack pages)
		}
		final Point size = tiles.getSize();
		if (size.x <= 0 || size.y <= 0 || tiles.getShell().getMinimized()) {
			return false;
		}
		if (IS_PART_VISIBLE != null) {
			try {
				return (Boolean) IS_PART_VISIBLE.invoke(tiles);
			} catch (final ReflectiveOperationException e) {
				return true;
			}
		}
		return true;
	}

	private static int partEvent(final String name) {
		try {
			return SWT.class.getField(name).getInt(null);
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return -1;
		}
	}

	private static java.lang.reflect.Method partVisibleMethod() {
		try {
			return Control.class.getMethod("isPartVisible"); //$NON-NLS-1$
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
