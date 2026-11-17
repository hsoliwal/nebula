/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The drawn browser's navigation: setUrl, links resolved against the page, history, veto, failures. */
public class DrawnBrowserNavigationTest {

	private Display display;
	private Shell shell;
	private DrawnBrowser browser;
	private Path site;
	private final List<String> calls = new ArrayList<>();

	@Before
	public void setUp() throws IOException {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);
		shell.setLayout(new FillLayout());
		browser = new DrawnBrowser(shell, SWT.NONE);
		shell.setBounds(40, 40, 320, 240);
		shell.open();
		site = Files.createTempDirectory("drawnbrowser");
		write("one.html", "<p><a href=\"sub/two.html\">TOTWOTOTWOTOTWOTOTWO</a></p><img src=\"red.png\">");
		write("sub/two.html", "<h1>Page two</h1><p><a href=\"../one.html\">back</a></p>");
		ImageData red = new ImageData(30, 20, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		for (int y = 0; y < 20; y++) for (int x = 0; x < 30; x++) red.setPixel(x, y, 0xFF0000);
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { red };
		loader.save(site.resolve("red.png").toString(), SWT.IMAGE_PNG);
		browser.addLocationListener(new LocationListener() {
			@Override
			public void changing(LocationEvent event) {
				calls.add("changing " + name(event.location));
			}

			@Override
			public void changed(LocationEvent event) {
				calls.add("changed " + name(event.location));
			}
		});
	}

	@After
	public void tearDown() throws IOException {
		shell.dispose();
		try (var files = Files.walk(site)) {
			files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
		}
	}

	private void write(String name, String html) throws IOException {
		Path file = site.resolve(name);
		Files.createDirectories(file.getParent());
		Files.writeString(file, html, StandardCharsets.UTF_8);
	}

	private String url(String name) {
		return site.resolve(name).toUri().toString();
	}

	private static String name(String location) {
		return location.substring(location.lastIndexOf('/') + 1);
	}

	private void await(BooleanSupplier done) {
		long end = System.currentTimeMillis() + 10_000;
		while (!done.getAsBoolean() && System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) Thread.onSpinWait();
		}
		assertTrue("timed out; calls " + calls, done.getAsBoolean());
	}

	private void click(int x, int y) {
		Event e = new Event();
		e.x = x;
		e.y = y;
		e.button = 1;
		browser.notifyListeners(SWT.MouseUp, e);
	}

	private int redPixels() {
		browser.update();
		Image shot = new Image(display, 80, 80);
		GC gc = new GC(browser);
		gc.copyArea(shot, 0, 0);
		gc.dispose();
		ImageData data = shot.getImageData();
		shot.dispose();
		int red = 0;
		for (int y = 0; y < 80; y++) for (int x = 0; x < 80; x++) {
			RGB rgb = data.palette.getRGB(data.getPixel(x, y));
			if (rgb.red > 200 && rgb.green < 60 && rgb.blue < 60) red++;
		}
		return red;
	}

	@Test
	public void setUrlLoadsThePageAndItsRelativeImage() {
		assertTrue(browser.setUrl(url("one.html")));
		await(() -> browser.getUrl().endsWith("one.html"));
		assertTrue(browser.getText().contains("sub/two.html"));
		assertEquals(List.of("changing one.html", "changed one.html"), calls);
		await(() -> redPixels() >= 30 * 20 - 40);
	}

	@Test
	public void linksNavigateAndTheHistoryGoesBackAndForward() {
		browser.setUrl(url("one.html"));
		await(() -> browser.getUrl().endsWith("one.html"));
		assertFalse(browser.isBackEnabled());
		click(6, 8 + 6);
		await(() -> browser.getUrl().endsWith("sub/two.html"));
		assertTrue(browser.isBackEnabled());
		assertFalse(browser.isForwardEnabled());
		assertTrue(browser.back());
		await(() -> browser.getUrl().endsWith("/one.html"));
		assertTrue(browser.isForwardEnabled());
		assertTrue(browser.forward());
		await(() -> browser.getUrl().endsWith("sub/two.html"));
		assertFalse(browser.isForwardEnabled());
		assertEquals(List.of("changing one.html", "changed one.html", "changing two.html", "changed two.html",
				"changing one.html", "changed one.html", "changing two.html", "changed two.html"), calls);
	}

	@Test
	public void aVetoStopsTheNavigation() {
		browser.addLocationListener(LocationListener.changingAdapter(e -> e.doit = false));
		assertFalse(browser.setUrl(url("one.html")));
		for (int i = 0; i < 20; i++) display.readAndDispatch();
		assertEquals("about:blank", browser.getUrl());
		assertEquals(List.of("changing one.html"), calls);
	}

	@Test
	public void aPageThatCannotLoadLeavesTheCurrentOne() {
		browser.setUrl(url("one.html"));
		await(() -> browser.getUrl().endsWith("one.html"));
		browser.setUrl(url("missing.html"));
		long end = System.currentTimeMillis() + 1_000;
		while (System.currentTimeMillis() < end) display.readAndDispatch();
		assertTrue(browser.getUrl().endsWith("one.html"));
		assertEquals(List.of("changing one.html", "changed one.html", "changing missing.html"), calls);
	}

	@Test
	public void setTextShowsThePageAtAboutBlankAndStopDropsALoad() {
		browser.setUrl(url("one.html"));
		browser.setText("<p>local</p>");
		long end = System.currentTimeMillis() + 1_000;
		while (System.currentTimeMillis() < end) display.readAndDispatch();
		assertEquals("about:blank", browser.getUrl());
		assertEquals("<p>local</p>", browser.getText());
		browser.setUrl(url("one.html"));
		browser.stop();
		end = System.currentTimeMillis() + 1_000;
		while (System.currentTimeMillis() < end) display.readAndDispatch();
		assertEquals("about:blank", browser.getUrl());
	}
}
