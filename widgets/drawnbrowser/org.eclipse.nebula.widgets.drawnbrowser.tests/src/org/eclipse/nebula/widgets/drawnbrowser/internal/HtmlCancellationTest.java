/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/** Explicit cancellation is distinct from parser failure and from a caller's interrupt policy. */
public class HtmlCancellationTest {

	@Test
	public void largeInputsObserveCancellationWithoutPublishingPartialDocuments() {
		String longText = "x".repeat(100_000);
		for (String html : new String[] {
				longText, "<a " + longText, "<script>" + longText, "<!--" + longText + "-->",
				"<!" + longText + ">", "<p title='" + longText + "'>body</p>",
				"<p>&#" + "9".repeat(100_000) + ";</p>", "<pre>" + longText + "</pre>",
				"<p>block</p>".repeat(10_000) }) {
			AtomicInteger checks = new AtomicInteger();
			assertNull(HtmlDocument.parse(html, () -> checks.incrementAndGet() == 20));
			assertEquals("cancellation must unwind at the observing checkpoint", 20, checks.get());
		}
	}

	@Test
	public void alreadyCancelledInputIsNotRead() {
		assertNull(HtmlDocument.parse(null, () -> true));
	}

	@Test
	public void ordinaryParsingPreservesTheCallersInterruptStatus() {
		boolean interrupted = Thread.interrupted();
		try {
			Thread.currentThread().interrupt();
			assertEquals("still parsed", HtmlDocument.parse("<p>still parsed</p>").text);
			assertEquals("explicit", HtmlDocument.parse("<p>explicit</p>", () -> false).text);
			assertTrue("parsing must not consume the caller's interrupt", Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
			if (interrupted) Thread.currentThread().interrupt();
		}
	}

	@Test
	public void uncancelledPreparationRetainsEveryDocumentField() throws Exception {
		String html = "<style>div p { color: red } .fixed { position: absolute; left: 2px; width: 80px }</style>"
				+ "<div><p class='fixed'>CPU &amp; GUI</p><pre> x\n y</pre></div>"
				+ "<form><input name='q' value='ready'><select><option>one</option></select></form>"
				+ "<table><tr><td>A</td><td>B</td></tr></table>";
		assertDocumentsEqual(HtmlDocument.parse(html), HtmlDocument.parse(html, () -> false));
	}

	@Test
	public void chunkBoundaryTerminatorsQuotesAndEntitiesMatchOrdinaryParsing() throws Exception {
		for (int length = 1018; length <= 1030; length++) {
			String padding = "x".repeat(length);
			String html = "<!--" + padding + "--><!" + padding + "><p><a href='" + padding
					+ "&amp;next'>" + padding + "&amp;tail</a></p>";
			HtmlDocument asynchronous = HtmlDocument.parse(html, () -> false);
			assertDocumentsEqual(HtmlDocument.parse(html), asynchronous);
			assertEquals(padding + "&tail", asynchronous.text);
			assertEquals(padding + "&next", asynchronous.href[0]);
		}
	}

	@Test
	public void asynchronousParsingRetainsConcurrentFlexShrinkAndBasisSemantics() throws Exception {
		String html = "<style>.fixed { flex: none; width: 120px } .soft { flex: 0 .25 200px }</style>"
				+ "<div style='display:flex;flex-shrink:0'><div class='fixed'>fixed</div>"
				+ "<div class='soft'>soft</div>anonymous</div>";
		HtmlDocument asynchronous = HtmlDocument.parse(html, () -> false);
		assertDocumentsEqual(HtmlDocument.parse(html), asynchronous);
		assertEquals(3, asynchronous.blockCount);
		assertEquals(0, asynchronous.cellShrink[0], 0);
		assertEquals(.25, asynchronous.cellShrink[1], 0);
		assertEquals("container factor must not leak to an anonymous item", 1, asynchronous.cellShrink[2], 0);
		assertEquals(120, asynchronous.cellBasis[0]);
		assertEquals(200, asynchronous.cellBasis[1]);
		assertEquals(-1, asynchronous.cellBasis[2]);
	}

	private static void assertDocumentsEqual(HtmlDocument ordinary, HtmlDocument asynchronous) throws Exception {
		for (var field : HtmlDocument.class.getFields()) {
			if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				assertTrue(field.getName(), java.util.Objects.deepEquals(field.get(ordinary), field.get(asynchronous)));
			}
		}
	}

	@Test
	public void genuineSupplierFailuresKeepTheirIdentityEvenIfNamedCancellation() {
		CancellationException cancellation = new CancellationException("external failure");
		IllegalStateException runtime = new IllegalStateException("external failure");
		AssertionError error = new AssertionError("external failure");
		assertSame(cancellation, assertThrows(CancellationException.class,
				() -> HtmlDocument.parse("<p>page</p>", () -> { throw cancellation; })));
		assertSame(runtime, assertThrows(IllegalStateException.class,
				() -> HtmlDocument.parse("<p>page</p>", () -> { throw runtime; })));
		assertSame(error, assertThrows(AssertionError.class,
				() -> HtmlDocument.parse("<p>page</p>", () -> { throw error; })));
	}

	@Test
	public void styleMatchingObservesCancellationWithinBoundedAncestorVisits() {
		AtomicBoolean armed = new AtomicBoolean();
		AtomicInteger checks = new AtomicInteger(), visits = new AtomicInteger();
		IllegalStateException cancelled = new IllegalStateException("checkpoint");
		HtmlStyles styles = new HtmlStyles(() -> {
			if (armed.get() && checks.incrementAndGet() == 16) throw cancelled;
		});
		styles.addSheet(".missing .target { color: red }");
		HtmlStyles.Path path = new HtmlStyles.Path() {
			@Override public int depth() { return 100_000; }
			@Override public int siblings(int up) { return 0; }
			@Override public String tag(int up, int back) { visits.incrementAndGet(); return "p"; }
			@Override public String attribute(int up, int back, String name) {
				return name.equals("class") ? up == 0 ? "target" : "other" : null;
			}
		};
		armed.set(true);
		assertSame(cancelled, assertThrows(IllegalStateException.class, () -> styles.compute(HtmlStyles.root(), path, null)));
		assertEquals(16, checks.get());
		assertTrue("no unbounded ancestor walk between checkpoints", visits.get() < 16);
	}

	@Test
	public void longStyleSheetsObserveCancellationBetweenDeclarations() {
		AtomicInteger checks = new AtomicInteger();
		IllegalStateException cancelled = new IllegalStateException("checkpoint");
		HtmlStyles styles = new HtmlStyles(() -> {
			if (checks.incrementAndGet() == 16) throw cancelled;
		});
		assertSame(cancelled, assertThrows(IllegalStateException.class,
				() -> styles.addSheet("p {" + "color: red;".repeat(10_000) + "}")));
		assertEquals(16, checks.get());
	}
}
