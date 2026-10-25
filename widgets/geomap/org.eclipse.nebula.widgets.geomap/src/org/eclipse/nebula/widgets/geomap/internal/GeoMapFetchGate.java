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

import java.util.ArrayDeque;
import java.util.Iterator;

import org.eclipse.swt.graphics.Point;

/**
 * Tile fetch gate of a {@link GeoMapHelper} whose owner (a {@code GeoMap} or a {@code MapFigure}) runs in the viewport
 * mode, aspect H (hidden gating, docs/m3/every-widget). The stock helper hands every new tile to a ThreadPoolExecutor
 * with 4 core threads and an unbounded queue (GeoMapHelper 89-99, AsyncImage 46-53), so the queued downloads keep
 * running while the map is hidden, each of them posts a tile update that redraws the map (AsyncImage 85-93,
 * InternalGeoMap 70-75), and the pool is never shut down (GeoMapHelper 312-316). Here a new tile waits in this gate's
 * FIFO and one reused pump runnable per busy pool thread (at most the stock 4) fetches the waiting tiles in order:
 * <ul>
 * <li>while the owner is shown, the tiles are fetched in the stock order with the stock parallelism;</li>
 * <li>{@link #park()} (the owner is hidden): no waiting tile starts; the downloads already running finish;</li>
 * <li>{@link #resume(int, int)} (the owner is shown again): the waiting tiles that are no longer wanted (an older
 * zoom, no longer the cache's image of their tile, or outside the visible tile window) are dropped and leave the
 * cache, as stock drops the tiles of an older zoom (AsyncImage 68-79); the others are fetched once;</li>
 * <li>{@link #dispose()} drops the waiting tiles and shuts the pool down; idle pool threads also end after the stock
 * keep-alive of 2 s.</li>
 * </ul>
 * submit, park, resume and dispose run on the display thread; the pumps run on the pool threads and touch the FIFO
 * only under this gate's lock.
 */
public final class GeoMapFetchGate {

	private final GeoMapHelper helper;
	private final int threads;
	private final ArrayDeque<AsyncImage> waiting = new ArrayDeque<>();
	private final Runnable pump = this::pump;
	private int pumps;
	private volatile boolean parked;
	private boolean disposed;

	private volatile int started;
	private volatile int dropped;
	private volatile int parks;

	private GeoMapFetchGate(final GeoMapHelper helper) {
		this.helper = helper;
		this.threads = helper.executor.getCorePoolSize();
	}

	/**
	 * Installs a fetch gate on a helper that has fetched nothing yet.
	 *
	 * @param helper the helper of a GeoMap or MapFigure in the viewport mode
	 * @return the gate
	 */
	public static GeoMapFetchGate install(final GeoMapHelper helper) {
		final GeoMapFetchGate gate = new GeoMapFetchGate(helper);
		helper.fetchGate = gate;
		helper.executor.allowCoreThreadTimeOut(true); // idle fetch threads end after the stock keep-alive
		return gate;
	}

	/** In place of the stock executor.execute in AsyncImage's constructor: the tile waits for a pump. */
	void submit(final AsyncImage tile) {
		synchronized (this) {
			if (disposed) {
				return; // the helper is disposed: nothing is fetched any more
			}
			waiting.addLast(tile);
			if (parked || pumps >= threads) {
				return;
			}
			pumps++;
		}
		helper.executor.execute(pump);
	}

	/** The owner is hidden: no waiting tile starts until {@link #resume(int, int)}; the running downloads finish. */
	public void park() {
		if (!parked) {
			parked = true;
			parks++;
		}
	}

	/**
	 * The owner is shown again: drops the waiting tiles that are no longer wanted (see the class comment) and fetches
	 * the others once.
	 *
	 * @param width the width of the owner's map area in pixels
	 * @param height the height of the owner's map area in pixels
	 */
	public void resume(final int width, final int height) {
		if (!parked) {
			return;
		}
		final Point position = helper.getMapPosition();
		final int zoom = helper.getZoom();
		final int x0 = Math.floorDiv(position.x, GeoMapHelper.TILE_SIZE);
		final int y0 = Math.floorDiv(position.y, GeoMapHelper.TILE_SIZE);
		final int x1 = -Math.floorDiv(-(position.x + width), GeoMapHelper.TILE_SIZE);
		final int y1 = -Math.floorDiv(-(position.y + height), GeoMapHelper.TILE_SIZE);
		int start;
		synchronized (this) {
			if (disposed) {
				return;
			}
			parked = false;
			for (final Iterator<AsyncImage> it = waiting.iterator(); it.hasNext();) {
				final AsyncImage tile = it.next();
				final TileRef ref = tile.m3Tile();
				if (tile.m3Stale() || ref.z != zoom || ref.x < x0 || ref.x >= x1 || ref.y < y0 || ref.y >= y1
						|| helper.cache.get(ref) != tile) {
					it.remove();
					helper.cache.remove(ref, tile);
					dropped++;
				}
			}
			start = Math.min(threads - pumps, waiting.size());
			pumps += start;
		}
		for (; start > 0; start--) {
			helper.executor.execute(pump);
		}
	}

	/** GeoMapHelper.dispose: drops the waiting tiles and shuts the pool down (the running downloads finish). */
	void dispose() {
		synchronized (this) {
			disposed = true;
			parked = true;
			waiting.clear();
		}
		helper.executor.shutdown();
	}

	/* a pool thread: fetches waiting tiles until none is left or the gate is parked or disposed */
	private void pump() {
		while (true) {
			final AsyncImage tile;
			synchronized (this) {
				if (parked || disposed || waiting.isEmpty()) {
					pumps--;
					return;
				}
				tile = waiting.pollFirst();
				started++;
			}
			try {
				tile.run();
			} catch (final Throwable e) {
				// as the stock FutureTask around AsyncImage, which keeps what run() throws in a future nobody reads
			}
		}
	}

	/** @return true while the gate is parked (the owner is hidden) */
	public boolean isParked() {
		return parked;
	}

	/** @return the tiles handed to a download so far (tests and benches) */
	public int started() {
		return started;
	}

	/** @return the waiting tiles dropped by {@link #resume(int, int)} so far (tests and benches) */
	public int dropped() {
		return dropped;
	}

	/** @return how often the gate was parked (tests and benches) */
	public int parks() {
		return parks;
	}

	/** @return the tiles waiting now (tests and benches) */
	public synchronized int waiting() {
		return waiting.size();
	}
}
