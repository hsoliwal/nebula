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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

import org.eclipse.nebula.widgets.geomap.GeoMap;
import org.eclipse.nebula.widgets.geomap.TileServer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * {@link GeoMapViewport} on a display (docs/m3/every-widget, aspect H): while the map is hidden no queued tile download
 * starts (the running ones finish and do not redraw), and after the reveal the map fetches the rest once and shows what
 * a map that stayed shown shows. Tiles come from a local HTTP server that holds every download until released.
 */
public class GeoMapViewportTest {

	private static final int ZOOM = 4;

	private Display display;
	private HttpServer server;
	private final Semaphore permits = new Semaphore(0);
	private final AtomicInteger requests = new AtomicInteger();
	private final List<Shell> shells = new ArrayList<>();

	@Before
	public void setUp() throws IOException {
		assumeTrue("viewport mode", GeoMapViewport.ENABLED);
		display = Display.getDefault();
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", this::tile);
		server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
		server.start();
	}

	@After
	public void tearDown() {
		shells.forEach(Shell::dispose);
		permits.release(10_000);
		if (server != null) {
			server.stop(0);
		}
	}

	/* one 256 x 256 PNG per tile, its colour from x and y; held until a permit is released */
	private void tile(HttpExchange exchange) throws IOException {
		requests.incrementAndGet();
		permits.acquireUninterruptibly();
		String[] parts = exchange.getRequestURI().getPath().replace(".png", "").split("/");
		int x = Integer.parseInt(parts[parts.length - 2]), y = Integer.parseInt(parts[parts.length - 1]);
		BufferedImage image = new BufferedImage(GeoMapHelper.TILE_SIZE, GeoMapHelper.TILE_SIZE, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		g.setColor(new java.awt.Color((x * 53) & 0xff, (y * 97) & 0xff, ((x + y) * 31) & 0xff));
		g.fillRect(0, 0, GeoMapHelper.TILE_SIZE, GeoMapHelper.TILE_SIZE);
		g.dispose();
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(image, "png", png);
		exchange.getResponseHeaders().set("Content-Type", "image/png");
		exchange.sendResponseHeaders(200, png.size());
		try (OutputStream out = exchange.getResponseBody()) {
			png.writeTo(out);
		}
	}

	private GeoMap map(int x) {
		Shell shell = new Shell(display, SWT.NO_TRIM);
		shells.add(shell);
		shell.setLayout(new FillLayout());
		shell.setBounds(x, 10, 900, 600);
		GeoMap map = new GeoMap(shell, SWT.NONE, new Point(0, 0), ZOOM);
		map.setTileServer(new TileServer("http://127.0.0.1:" + server.getAddress().getPort() + "/", 0, 19,
				"{0}/{1}/{2}.png"));
		shell.open();
		return map;
	}

	/* runs the event loop until the condition holds (or 10 s) */
	private boolean await(BooleanSupplier condition) {
		long end = System.currentTimeMillis() + 10_000;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
		}
		return condition.getAsBoolean();
	}

	private void spin(long millis) {
		long end = System.currentTimeMillis() + millis;
		await(() -> System.currentTimeMillis() >= end);
	}

	/* the package-private viewport of InternalGeoMap, read through the type that declares it */
	private static GeoMapViewport viewport(GeoMap map) {
		return ((InternalGeoMap) map).viewport;
	}

	private static int[] pixels(GeoMap map) {
		Point size = map.getSize();
		Image image = new Image(map.getDisplay(), size.x, size.y);
		GC gc = new GC(map);
		gc.copyArea(image, 0, 0);
		gc.dispose();
		ImageData data = image.getImageData();
		image.dispose();
		int[] out = new int[size.x * size.y];
		for (int y = 0; y < size.y; y++) {
			data.getPixels(0, y, size.x, out, y * size.x);
		}
		return out;
	}

	@Test
	public void hiddenMapStartsNoQueuedTileAndCatchesUpOnShow() {
		GeoMap map = map(10);
		GeoMapFetchGate gate = viewport(map).gate;
		int threads = map.geoMapHelper.executor.getCorePoolSize();
		assertTrue("downloads running", await(() -> requests.get() == threads));
		int tiles = threads + gate.waiting();
		assertTrue("more tiles than fetch threads: " + tiles, tiles > threads);
		map.setVisible(false);
		assertTrue("parked by the hide", await(gate::isParked));
		permits.release(10_000);
		assertTrue("the running downloads finish", await(() -> viewport(map).hiddenArrivals == threads));
		spin(500);
		assertEquals("no queued tile starts while hidden", threads, gate.started());
		assertEquals("requests while hidden", threads, requests.get());
		map.setVisible(true);
		assertTrue("every tile fetched after the reveal", await(() -> gate.started() == tiles && gate.waiting() == 0));
		assertEquals("no tile fetched twice", tiles, requests.get());
		GeoMap reference = map(920);
		assertTrue("reference tiles", await(() -> viewport(reference).gate.started() == tiles
				&& viewport(reference).gate.waiting() == 0 && requests.get() == 2 * tiles));
		spin(500);
		map.redraw();
		map.update();
		reference.update();
		assertArrayEquals("pixels equal a map that stayed shown", pixels(reference), pixels(map));
	}
}
