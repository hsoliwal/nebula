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
package org.eclipse.nebula.widgets.geomap.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Viewport of an {@link InternalGeoMap} (and so of {@code GeoMap}), aspect H (hidden gating, docs/m3/every-widget).
 * The stock map keeps fetching while hidden: its helper's pool runs every queued tile download (GeoMapHelper 89-99,
 * AsyncImage 66-98) and each finished download posts a tile update that redraws the whole map (AsyncImage 85-93,
 * InternalGeoMap 70-75). Here the helper's tiles go through a {@link GeoMapFetchGate}, parked while the map is hidden
 * (its own or an ancestor's visibility, a zero size, a minimized shell, SWT.PartHidden on the M3 SWT fork, see
 * {@link GeoMapVisibility}): no queued tile starts, the running downloads finish and their arrivals are folded into one
 * redraw at the reveal. The reveal (the next paint, or SWT.PartVisible on the M3 fork) resumes the gate: the waiting
 * tiles of the visible window are fetched once, so the map shows what the stock map shows once its tiles are in.
 * <p>
 * {@code -Dswt.widget.mode.GeoMap=original} (or the global {@code swt.widget.mode=original}) keeps the stock fetching.
 * </p>
 */
final class GeoMapViewport implements Listener {

	/** True unless the startup mode of GeoMap is original. */
	static final boolean ENABLED = WidgetMode.of("GeoMap", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final InternalGeoMap map;
	private final GeoMapVisibility visibility;
	/** The tile fetch gate of the map's helper. */
	final GeoMapFetchGate gate;
	private boolean stale;

	/** Tile arrivals folded while the map was hidden (tests and benches). */
	int hiddenArrivals;
	/** Redraws issued at a reveal for tiles that arrived while hidden (tests and benches). */
	int catchUps;

	GeoMapViewport(final InternalGeoMap map) {
		this.map = map;
		this.gate = GeoMapFetchGate.install(map.geoMapHelper);
		this.visibility = new GeoMapVisibility(map, this);
	}

	/**
	 * A downloaded tile reached the map ({@link InternalGeoMap#tileUpdated(TileRef)}).
	 *
	 * @return true when the map is hidden: the stock redraw is skipped and folded into one at the reveal
	 */
	boolean arrivedHidden() {
		if (map.isDisposed() || sync()) {
			return false;
		}
		stale = true;
		hiddenArrivals++;
		return true;
	}

	@Override
	public void handleEvent(final Event e) {
		if (e.type == SWT.Dispose) {
			visibility.dispose(); // the helper's dispose (InternalGeoMap's dispose listener) disposes the gate
			return;
		}
		if (!sync() || !stale) {
			return;
		}
		stale = false;
		if (e.type == SWT.Paint && e.widget == map && covers(e)) {
			return; // this paint (the stock tile painter ran before this listener) drew the tiles that arrived hidden
		}
		catchUps++;
		map.redraw();
	}

	/* parks the gate while the map is hidden and resumes it when the map is shown; true when shown */
	private boolean sync() {
		if (!visibility.shown()) {
			gate.park();
			return false;
		}
		if (gate.isParked()) {
			final Point size = map.getSize();
			gate.resume(size.x, size.y);
		}
		return true;
	}

	private boolean covers(final Event e) {
		final Point size = map.getSize();
		return e.x <= 0 && e.y <= 0 && e.x + e.width >= size.x && e.y + e.height >= size.y;
	}
}
