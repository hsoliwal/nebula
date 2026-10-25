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
package org.eclipse.nebula.widgets.geomap.draw2d;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.nebula.widgets.geomap.GeoMapUtil;
import org.eclipse.nebula.widgets.geomap.PointD;
import org.eclipse.nebula.widgets.geomap.TileServer;
import org.eclipse.nebula.widgets.geomap.internal.GeoMapFetchGate;
import org.eclipse.nebula.widgets.geomap.internal.GeoMapHelper;
import org.eclipse.nebula.widgets.geomap.internal.GeoMapVisibility;
import org.eclipse.nebula.widgets.geomap.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Viewport of a {@link MapFigure}, aspect H (hidden gating, docs/m3/every-widget). The stock figure fetches forever
 * once a tile server is set: every image update hands the server to its helper again (MapFigure 148-150), which clears
 * the tile cache (GeoMapHelper 335-338), and bumps the zoom stamp (MapFigure 151, GeoMapHelper 389), so each tile
 * arrival (MapFigure 156-159) invalidates the image, the repaint fetches every window tile again and the image never
 * shows a tile. draw2d keeps that loop running on a hidden canvas, because it paints through its own GC whatever the
 * canvas visibility (BufferedGraphicsSource.getGraphics checks only disposal and size).
 * <p>
 * Here the helper gets the tile server and the zoom only after {@link MapFigure#setTileServer(TileServer)} and
 * {@link MapFigure#setZoomLevel(int)} (once each, as the stock update does), and the position at every update, so an
 * arrival renders the image from the cache and no tile is fetched twice. The helper's tiles go through a
 * {@link GeoMapFetchGate}, parked while the figure is not showing or its {@link FigureCanvas} is hidden (the canvas is
 * found once per root figure among the display's controls, since draw2d keeps no link from a figure to its canvas; see
 * {@link GeoMapVisibility}): no queued tile starts, the running downloads finish and their arrivals do not invalidate
 * the image. The next paint while shown resumes the gate (the waiting tiles of the visible window are fetched once)
 * and renders the image again, once.
 * </p>
 * <p>
 * {@code -Dswt.widget.mode.MapFigure=original} (or the global {@code swt.widget.mode=original}) keeps the stock
 * figure.
 * </p>
 */
final class MapFigureViewport implements Listener {

	/** True unless the startup mode of MapFigure is original. */
	static final boolean ENABLED = WidgetMode.of("MapFigure", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private final MapFigure figure;
	private boolean serverSet;
	private boolean zoomSet;
	private boolean stale;
	/** The tile fetch gate of the figure's helper; null until the helper exists. */
	GeoMapFetchGate gate;
	/* the canvas found for scannedRoot, with the listeners of GeoMapVisibility; null when there is none */
	private GeoMapVisibility canvas;
	private IFigure scannedRoot;

	/** Tile arrivals that did not invalidate the image because the figure was hidden (tests and benches). */
	int hiddenArrivals;
	/** Image renders forced by a paint after tiles arrived while hidden (tests and benches). */
	int catchUps;

	MapFigureViewport(final MapFigure figure) {
		this.figure = figure;
	}

	/** {@link MapFigure#setTileServer(TileServer)}: the next update hands the server to the helper, once. */
	void serverSet() {
		serverSet = true;
	}

	/** {@link MapFigure#setZoomLevel(int)}: the next update hands the zoom to the helper, once. */
	void zoomSet() {
		zoomSet = true;
	}

	/**
	 * MapFigure.updateGeoMapHelper in the viewport mode: creates the helper as the stock method does, and hands it the
	 * tile server and the zoom only after they were set.
	 *
	 * @param helper the figure's helper, null before the first update with a location
	 * @param display the display for a new helper
	 * @param location the figure's location (longitude, latitude), null when not set
	 * @param zoom the figure's zoom level
	 * @param server the figure's tile server, null when not set
	 * @param cacheSize the tile cache size of a new helper
	 * @return the helper, created when there was none and a location is set
	 */
	GeoMapHelper update(final GeoMapHelper helper, final Display display, final PointD location, final int zoom,
			final TileServer server, final int cacheSize) {
		if (location == null) {
			return helper;
		}
		final Point position = GeoMapUtil.computePosition(location, zoom);
		GeoMapHelper h = helper;
		if (h == null) {
			h = new GeoMapHelper(display, position, zoom, cacheSize);
			h.addGeoMapHelperListener(figure);
			gate = GeoMapFetchGate.install(h);
			sync(); // a first update on a hidden canvas: the tiles of the coming render wait for the reveal
			serverSet = true;
			zoomSet = true;
		}
		if (serverSet && server != null) {
			h.setTileServer(server);
			zoomSet = true; // as the stock update: the zoom follows the server (clamped to its maximum)
		}
		if (zoomSet) {
			h.setZoom(zoom);
		}
		serverSet = false;
		zoomSet = false;
		h.setMapPosition(position.x, position.y);
		return h;
	}

	/**
	 * A downloaded tile reached the figure ({@link MapFigure#tileUpdated}).
	 *
	 * @return true when the figure is hidden: the stock image invalidation is skipped, one render follows the reveal
	 */
	boolean arrivedHidden() {
		if (sync()) {
			return false;
		}
		stale = true;
		hiddenArrivals++;
		return true;
	}

	/**
	 * {@link MapFigure#paint}: parks or resumes the gate before the stock paint may update the image.
	 *
	 * @return true when the image must be rendered again for tiles that arrived while the figure was hidden
	 */
	boolean painting() {
		if (!sync() || !stale) {
			return false;
		}
		stale = false;
		catchUps++;
		return true;
	}

	@Override
	public void handleEvent(final Event e) {
		if (e.type == SWT.Dispose) {
			canvas.dispose(); // the canvas is gone: GeoMapVisibility.shown answers false from now on
			return;
		}
		if (sync() && stale) {
			figure.repaint(); // the paint renders the image again (painting)
		}
	}

	/* parks the gate while the figure is hidden and resumes it when shown; true when shown */
	private boolean sync() {
		if (gate == null) {
			return true; // no helper yet: nothing was fetched
		}
		if (!shown()) {
			gate.park();
			return false;
		}
		if (gate.isParked()) {
			final Dimension size = figure.getSize();
			gate.resume(size.width, size.height);
		}
		return true;
	}

	private boolean shown() {
		if (!figure.isShowing()) {
			return false;
		}
		final GeoMapVisibility c = canvas();
		return c == null || c.shown();
	}

	/* the FigureCanvas whose root figure holds the figure, looked up once per root figure */
	private GeoMapVisibility canvas() {
		IFigure root = figure;
		while (root.getParent() != null) {
			root = root.getParent();
		}
		if (root != scannedRoot) {
			scannedRoot = root;
			if (canvas != null) {
				canvas.dispose();
			}
			final Control found = find(Display.getCurrent(), root);
			canvas = found == null ? null : new GeoMapVisibility(found, this);
		}
		return canvas;
	}

	private static Control find(final Display display, final IFigure root) {
		if (display == null) {
			return null;
		}
		for (final Shell shell : display.getShells()) {
			final Control found = find(shell, root);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static Control find(final Composite composite, final IFigure root) {
		for (final Control child : composite.getChildren()) {
			if (child instanceof FigureCanvas
					&& ((FigureCanvas) child).getLightweightSystem().getRootFigure() == root) {
				return child;
			}
			if (child instanceof Composite) {
				final Control found = find((Composite) child, root);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
