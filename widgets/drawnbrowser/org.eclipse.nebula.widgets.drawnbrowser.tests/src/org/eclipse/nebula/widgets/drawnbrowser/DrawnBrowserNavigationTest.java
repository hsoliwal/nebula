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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument;
import org.eclipse.nebula.widgets.drawnbrowser.internal.PageHistory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
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
		// Keep the padded 24-bit fixture: a PNG encoder stride defect must fail here, before navigation.
		ImageData[] decoded = new ImageLoader().load(site.resolve("red.png").toString());
		assertEquals(1, decoded.length);
		assertEquals(30, decoded[0].width);
		assertEquals(20, decoded[0].height);
		assertEquals("PNG fixture must retain every red pixel", 30 * 20, redPixels(decoded[0]));
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
		GC gc = new GC(display);
		try {
			Point origin = browser.toDisplay(0, 0);
			gc.copyArea(shot, origin.x, origin.y);
			return redPixels(shot.getImageData());
		} finally {
			gc.dispose();
			shot.dispose();
		}
	}

	private static int redPixels(ImageData data) {
		int red = 0;
		for (int y = 0; y < data.height; y++) for (int x = 0; x < data.width; x++) {
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

	private PageHistory history() throws Exception {
		var field = DrawnBrowser.class.getDeclaredField("pages");
		field.setAccessible(true);
		return (PageHistory) field.get(browser);
	}

	private HtmlDocument document() throws Exception {
		var field = DrawnBrowser.class.getDeclaredField("document");
		field.setAccessible(true);
		return (HtmlDocument) field.get(browser);
	}

	private void arrive(String url, int generation, DrawnBrowser.PreparedPage page) throws Exception {
		var method = DrawnBrowser.class.getDeclaredMethod("arrived", String.class, int.class, int.class,
				DrawnBrowser.PreparedPage.class);
		method.setAccessible(true);
		method.invoke(browser, url, PageHistory.PUSH, generation, page);
	}

	@Test
	public void aPreparedPageIsInstalledOnUiWithoutReparsing() throws Exception {
		String text = "<h1>Prepared</h1><p>viewport</p>";
		DrawnBrowser.PreparedPage page;
		try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
			page = worker.submit(() -> DrawnBrowser.PreparedPage.prepare(text)).get(10, java.util.concurrent.TimeUnit.SECONDS);
		}
		browser.addLocationListener(LocationListener.changedAdapter(event -> {
			assertSame(display.getThread(), Thread.currentThread());
			assertEquals(text, browser.getText());
		}));
		arrive(url("prepared.html"), history().begin(), page);
		assertSame("the UI must accept the worker's completed arrays", page.document(), document());
		assertEquals(text, browser.getText());
		assertEquals(List.of("changed prepared.html"), calls);
	}

	@Test
	public void stopRejectsAPreparedFailureBeforeItTouchesUiState() throws Exception {
		browser.setText("<p>retained</p>");
		HtmlDocument retained = document();
		int generation = history().begin();
		DrawnBrowser.PreparedPage failure = DrawnBrowser.PreparedPage.prepare(null);
		browser.stop();
		arrive(url("stopped.html"), generation, failure);
		assertSame(retained, document());
		assertEquals("<p>retained</p>", browser.getText());
		assertEquals("about:blank", browser.getUrl());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void aNewGenerationRejectsAnOlderPreparedPage() throws Exception {
		int older = history().begin();
		int newer = history().begin();
		DrawnBrowser.PreparedPage page = DrawnBrowser.PreparedPage.prepare("<p>newer</p>");
		arrive(url("newer.html"), newer, page);
		arrive(url("older.html"), older, DrawnBrowser.PreparedPage.prepare("<p>older</p>"));
		assertSame(page.document(), document());
		assertEquals("<p>newer</p>", browser.getText());
		assertEquals(url("newer.html"), browser.getUrl());
		assertEquals(List.of("changed newer.html"), calls);
	}

	@Test
	public void disposalRejectsAPreparedFailureWithoutWidgetAccess() throws Exception {
		int generation = history().begin();
		browser.dispose();
		arrive(url("disposed.html"), generation, DrawnBrowser.PreparedPage.prepare(null));
		assertTrue(calls.isEmpty());
	}
}
