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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlDocument;
import org.junit.Test;

/** CPU preparation and ownership transfer require no SWT Display or native resource. */
public class DrawnBrowserPreparationTest {

	@Test
	public void cancelledPreparationReleasesTheSingleWorkerForItsReplacement() throws Exception {
		var entered = new java.util.concurrent.CountDownLatch(1);
		var resume = new java.util.concurrent.CountDownLatch(1);
		var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
		var checks = new java.util.concurrent.atomic.AtomicInteger();
		try (var workers = Executors.newSingleThreadExecutor()) {
			var obsolete = workers.submit(() -> DrawnBrowser.PreparedPage.prepare("<a ".repeat(20_000), () -> {
				if (checks.incrementAndGet() == 32) {
					entered.countDown();
					try {
						if (!resume.await(5, TimeUnit.SECONDS)) throw new AssertionError("cancellation was not released");
					} catch (InterruptedException failure) {
						Thread.currentThread().interrupt();
						throw new AssertionError(failure);
					}
				}
				return cancelled.get();
			}));
			try {
				assertTrue("the old parser must actually reach CPU checkpoints", entered.await(5, TimeUnit.SECONDS));
				var latest = workers.submit(() -> DrawnBrowser.PreparedPage.prepare("<p>latest</p>", () -> false));
				cancelled.set(true);
				resume.countDown();
				assertNull(obsolete.get(5, TimeUnit.SECONDS));
				assertEquals(32, checks.get());
				assertEquals("latest", latest.get(5, TimeUnit.SECONDS).document().text);
			} finally {
				cancelled.set(true);
				resume.countDown();
			}
		}
	}

	@Test
	public void genuineAsyncFailuresRemainOriginalFailures() {
		DrawnBrowser.PreparedPage invalid = DrawnBrowser.PreparedPage.prepare(null, () -> false);
		NullPointerException original = assertThrows(NullPointerException.class, invalid::document);
		assertSame(original, assertThrows(NullPointerException.class, invalid::document));
		var cancellation = new java.util.concurrent.CancellationException("genuine failure");
		DrawnBrowser.PreparedPage failed = DrawnBrowser.PreparedPage.prepare("<p>page</p>", () -> { throw cancellation; });
		assertSame(cancellation, assertThrows(java.util.concurrent.CancellationException.class, failed::document));
		AssertionError error = new AssertionError("genuine error");
		DrawnBrowser.PreparedPage errored = DrawnBrowser.PreparedPage.prepare("<p>page</p>", () -> { throw error; });
		assertSame(error, assertThrows(AssertionError.class, errored::document));
	}

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
			assertTokenizerOriginWhenAvailable(first);
		}
	}
	@Test
	public void anEmptyFailureStackStillRetainsTheExactOriginalException() {
		NullPointerException original = new NullPointerException();
		original.setStackTrace(new StackTraceElement[0]);
		DrawnBrowser.PreparedPage page = DrawnBrowser.PreparedPage.prepare(
				"<p>prepared</p>", () -> { throw original; });
		assertSame(original, assertThrows(NullPointerException.class, page::document));
		assertSame(original, assertThrows(NullPointerException.class, page::document));
		assertTokenizerOriginWhenAvailable(original);

		NullPointerException unrelated = new NullPointerException();
		unrelated.setStackTrace(new StackTraceElement[] {
				new StackTraceElement("unrelated.Source", "run", "Source.java", 1) });
		assertThrows(AssertionError.class, () -> assertTokenizerOriginWhenAvailable(unrelated));
	}

	/** The JVM may omit stack traces at frequently executed implicit throw sites. */
	private static void assertTokenizerOriginWhenAvailable(NullPointerException failure) {
		StackTraceElement[] trace = failure.getStackTrace();
		if (trace.length != 0) {
			assertEquals("org.eclipse.nebula.widgets.drawnbrowser.internal.HtmlTokenizer",
					trace[0].getClassName());
		}
	}

}
