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
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Cancellation must release the existing single loader while the UI retains page and native ownership. */
public class DrawnBrowserCancellationTest {

	private Display display;
	private Shell shell;
	private DrawnBrowser browser;
	private Path obsolete;
	private ExecutorService loader;
	private final AtomicReference<Thread> parser = new AtomicReference<>();
	private int changed;

	@Before
	public void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		browser = new DrawnBrowser(shell, SWT.NONE);
		browser.setText("<p>retained</p>");
		browser.addLocationListener(LocationListener.changedAdapter(event -> changed++));
		obsolete = Files.createTempFile("drawnbrowser-obsolete-", ".html");
		// The existing lenient parser repeatedly scans unmatched tags. This is real CPU work, not a sleep.
		Files.writeString(obsolete, "<a ".repeat(200_000));
		loader = Executors.newSingleThreadExecutor(task -> {
			Thread thread = new Thread(task, "DrawnBrowser cancellation test");
			thread.setDaemon(true);
			parser.set(thread);
			return thread;
		});
		var field = DrawnBrowser.class.getDeclaredField("pageLoader");
		field.setAccessible(true);
		field.set(browser, loader);
	}

	@After
	public void tearDown() throws Exception {
		if (shell != null) shell.dispose();
		if (loader != null) loader.shutdownNow();
		if (obsolete != null) Files.deleteIfExists(obsolete);
	}

	private void startParsing() throws Exception {
		assertTrue(browser.setUrl(obsolete.toUri().toString()));
		long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < limit) {
			Thread thread = parser.get();
			if (thread != null && Arrays.stream(thread.getStackTrace()).anyMatch(frame ->
					frame.getClassName().endsWith(".HtmlTokenizer") && frame.getMethodName().equals("tagEnd"))) return;
			Thread.sleep(1);
		}
		throw new AssertionError("The obsolete page did not reach the real tokenizer scan");
	}

	private void awaitLoader() throws Exception {
		// A subsequent task can run only when that same worker has abandoned the obsolete parse.
		assertTrue(loader.submit(() -> Boolean.TRUE).get(5, TimeUnit.SECONDS).booleanValue());
		while (display.readAndDispatch()) { }
	}

	@Test
	public void stopReleasesTheParserAndKeepsTheCurrentPage() throws Exception {
		startParsing();
		browser.stop();
		awaitLoader();
		assertEquals("<p>retained</p>", browser.getText());
		assertEquals("about:blank", browser.getUrl());
		assertEquals(0, changed);
	}

	@Test
	public void replacementReachesTheUiBeforeTheObsoleteParseCompletes() throws Exception {
		Path latest = Files.createTempFile("drawnbrowser-latest-", ".html");
		try {
			Files.writeString(latest, "<h1>latest</h1>");
			startParsing();
			assertTrue(browser.setUrl(latest.toUri().toString()));
			awaitLoader();
			assertEquals("<h1>latest</h1>", browser.getText());
			assertEquals(latest.toUri().toString(), browser.getUrl());
			assertEquals(1, changed);
		} finally {
			Files.deleteIfExists(latest);
		}
	}

	@Test
	public void synchronousSetTextCancelsTheWorkerAndInstallsBeforeReturning() throws Exception {
		startParsing();
		assertTrue(browser.setText("<p>synchronous</p>"));
		assertEquals("<p>synchronous</p>", browser.getText());
		assertEquals("about:blank", browser.getUrl());
		awaitLoader();
		assertEquals("<p>synchronous</p>", browser.getText());
		assertEquals(0, changed);
	}

	@Test
	public void disposingTheWidgetCancelsItsDetachedParse() throws Exception {
		startParsing();
		browser.dispose();
		assertTrue("disposal must release the obsolete CPU parse", loader.awaitTermination(5, TimeUnit.SECONDS));
		while (display.readAndDispatch()) { }
		assertEquals(0, changed);
	}

	@Test
	public void remotePresentationCancelsTheLocalParseAndRetainsItsImage() throws Exception {
		shell.setLayout(new org.eclipse.swt.layout.FillLayout());
		shell.setBounds(40, 40, 320, 240);
		shell.open();
		boolean[] painted = { false };
		browser.setRemoteFramePaintListener((epoch, revision) -> painted[0] = epoch == 1 && revision == 1);
		startParsing();
		ImageData pixels = new ImageData(2, 2, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		assertTrue(browser.presentRemoteFrame(1, 1, 2, 2, pixels));
		awaitLoader();
		long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!painted[0] && System.nanoTime() < limit) {
			while (display.readAndDispatch()) { }
			browser.update();
			Thread.sleep(1);
		}
		assertTrue("the retained native image must finish painting", painted[0]);
		var area = browser.getClientArea();
		int side = Math.min(area.width, area.height);
		assertTrue("the fitted frame must provide an interior sample", side >= 4);
		// An odd-sized frame's floored midpoint is still in the first source pixel. Sample inside the lower-right quarter.
		int offset = 3 * side / 4;
		assertEquals(new org.eclipse.swt.graphics.Point(1, 1), browser.remoteCssPoint(1, 1,
				(area.width - side) / 2 + offset, (area.height - side) / 2 + offset));
		assertEquals("<p>retained</p>", browser.getText());
		assertEquals(0, changed);
		browser.clearRemoteFrame();
		assertEquals("<p>retained</p>", browser.getText());
	}
}
