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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument;
import org.junit.Test;

/** CPU preparation and ownership transfer require no SWT Display or native resource. */
public class DrawnBrowserPreparationTest {

	@Test
	public void aWorkerPublishesOneCompletedDocument() throws Exception {
		Thread receiver = Thread.currentThread();
		String html = "<h1>Title</h1><p>CPU &amp; GUI</p><input name='q' value='ready'>";
		try (var workers = Executors.newSingleThreadExecutor()) {
			DrawnBrowser.PreparedPage page = workers.submit(() -> {
				assertNotSame(receiver, Thread.currentThread());
				return DrawnBrowser.PreparedPage.prepare(html);
			}).get(10, TimeUnit.SECONDS);
			HtmlDocument document = page.document();
			assertSame(html, page.text);
			assertEquals("TitleCPU & GUI", document.text);
			assertEquals(1, document.controlCount);
			assertEquals("ready", document.controlValue[0]);
			assertSame("acceptance reuses the completed document", document, page.document());
		}
	}

	@Test
	public void concurrentPreparationsDoNotShareMutableArrays() throws Exception {
		try (var workers = Executors.newFixedThreadPool(2)) {
			var first = workers.submit(() -> DrawnBrowser.PreparedPage.prepare("<p>first</p>"));
			var second = workers.submit(() -> DrawnBrowser.PreparedPage.prepare("<h2>second</h2>"));
			HtmlDocument a = first.get(10, TimeUnit.SECONDS).document();
			HtmlDocument b = second.get(10, TimeUnit.SECONDS).document();
			assertNotSame(a, b);
			assertNotSame(a.kind, b.kind);
			assertNotSame(a.textStart, b.textStart);
			assertEquals("first", a.text);
			assertEquals("second", b.text);
			a.kind[0] = HtmlDocument.RULE;
			assertEquals(HtmlDocument.HEADING, b.kind[0]);
		}
	}

	@Test
	public void anUncheckedFailureIsDeliveredAtAcceptanceWithItsOriginalIdentity() throws Exception {
		try (var workers = Executors.newSingleThreadExecutor()) {
			// Invalid internal input supplies a deterministic parser failure without allocating native resources.
			DrawnBrowser.PreparedPage page = workers.submit(() -> DrawnBrowser.PreparedPage.prepare(null))
					.get(10, TimeUnit.SECONDS);
			NullPointerException first = assertThrows(NullPointerException.class, page::document);
			assertSame(first, assertThrows(NullPointerException.class, page::document));
			assertEquals("org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlTokenizer",
					first.getStackTrace()[0].getClassName());
		}
	}
}
