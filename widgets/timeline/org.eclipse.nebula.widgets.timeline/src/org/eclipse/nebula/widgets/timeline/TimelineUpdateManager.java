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

import org.eclipse.draw2d.DeferredUpdateManager;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.widgets.Canvas;

/**
 * The Draw2D update manager of a {@link TimelineComposite} in the viewport mode (aspect H, {@link TimelineViewport}).
 * The stock DeferredUpdateManager of the LightweightSystem posts an update request (Display.asyncExec) for every batch
 * of figure changes, and the LightweightSystem calls performUpdate on every canvas resize: both lay out the figure tree
 * and repaint, hidden or not. This one parks while the timeline is hidden: no update request is posted, performUpdate
 * neither lays out nor paints, and dirty regions are dropped (the reveal repaints the canvas). Invalid figures stay
 * queued as stock queues them. The reveal resumes once: one update request, and when the reveal is a paint of the whole
 * canvas the layout runs right before that paint, so the parked work costs one paint, not two.
 * <p>
 * performValidation keeps its stock behaviour: it is the synchronous layout a caller asks for.
 * </p>
 */
final class TimelineUpdateManager extends DeferredUpdateManager {

	private final TimelineViewport gate;
	private final Canvas canvas;
	private boolean updateParked;
	private boolean damageParked;
	private boolean absorbing;

	/** Update requests and updates parked while hidden (tests and benches). */
	int updatesParked;
	/** Dirty regions dropped while hidden (tests and benches). */
	int damageDropped;
	/** Reveals that resumed parked updates (tests and benches). */
	int resumes;

	TimelineUpdateManager(final TimelineViewport gate, final Canvas canvas) {
		this.gate = gate;
		this.canvas = canvas;
	}

	@Override
	public synchronized void addDirtyRegion(final IFigure figure, final int x, final int y, final int w, final int h) {
		if (absorbing) {
			return; // the layout of a reveal paint: the paint that follows covers the whole canvas
		}
		if (gate.isHidden()) {
			if (w != 0 && h != 0) {
				damageParked = true;
				damageDropped++;
			}
			return;
		}
		super.addDirtyRegion(figure, x, y, w, h);
	}

	@Override
	protected void sendUpdateRequest() {
		if (gate.isHidden()) {
			park();
			return;
		}
		super.sendUpdateRequest();
	}

	@Override
	public synchronized void performUpdate() {
		if (gate.isHidden()) {
			park();
			return;
		}
		super.performUpdate();
	}

	/**
	 * The timeline shows again (UI thread): one update for everything parked.
	 *
	 * @param paintCoversCanvas the reveal is a paint of the whole canvas, which repaints every figure right after
	 */
	void resume(final boolean paintCoversCanvas) {
		if (updateParked) {
			updateParked = false;
			resumes++;
			if (paintCoversCanvas) {
				absorb();
			}
			super.sendUpdateRequest(); // the stock queued flag is still set when a request was parked: this one clears it
		}
		if (damageParked) {
			damageParked = false;
			if (!paintCoversCanvas) {
				canvas.redraw();
			}
		}
	}

	/* lay out now, inside the reveal paint: its damage is painted by that paint, so it is not queued again */
	private void absorb() {
		absorbing = true;
		try {
			performValidation();
		} finally {
			absorbing = false;
		}
	}

	private void park() {
		updateParked = true;
		updatesParked++;
	}
}
