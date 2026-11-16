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
package org.eclipse.nebula.widgets.opal.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.opal.dialog.internal.RepaintCounters;
import org.eclipse.nebula.widgets.opal.dialog.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * ErrorPane: deferred details (formatted and created on first show, reused after), report text, severity, a resize
 * sweep that never formats or creates the details, the modal dialog, errors. Runs in both startup modes.
 */
public class ErrorPaneTest {

	Display display;
	Shell shell;
	ErrorPane pane;
	final IllegalStateException failure = new IllegalStateException("index 1000000 out of bounds", new ArithmeticException("cause"));

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM | SWT.RESIZE);
		shell.setBounds(10, 10, 480, 360);
		shell.setLayout(new FillLayout());
		RepaintCounters.reset();
		pane = new ErrorPane(shell, SWT.NONE);
		pane.setMessage("The table could not be sorted.");
		shell.open();
		TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("ErrorPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void detailsAreCreatedOnFirstShow() {
		pane.setError(failure);
		assertSame(failure, pane.getError());
		if (viewport()) {
			assertNull("no details control before the details are shown", pane.getDetailsText());
			assertEquals(0, RepaintCounters.creates);
		}
		assertFalse(pane.isDetailsVisible());
		pane.setDetailsVisible(true);
		assertTrue(pane.isDetailsVisible());
		Text text = pane.getDetailsText();
		assertNotNull(text);
		assertTrue(text.getText().contains("IllegalStateException"));
		assertTrue(text.getText().contains("Caused by"));
		assertTrue("text " + text.getBounds() + " content " + text.getParent().getBounds() + " visible " + text.getParent().getVisible() + " pane " + text.getParent().getParent().getBounds() + " error pane " + pane.getBounds(), text.getSize().y > 0);
		pane.setDetailsVisible(false);
		pane.setDetailsVisible(true);
		assertSame("the details control is reused", text, pane.getDetailsText());
		assertEquals(1, RepaintCounters.creates);
		pane.setDetails("plain details");
		assertEquals("plain details", text.getText());
		assertNull(pane.getError());
	}

	@Test
	public void reportJoinsMessageAndDetails() {
		assertEquals("The table could not be sorted.", pane.getReport());
		pane.setError(failure);
		String report = pane.getReport();
		assertTrue(report.startsWith("The table could not be sorted."));
		assertTrue(report.contains("index 1000000 out of bounds"));
		pane.setSeverity(SWT.ICON_WARNING);
		assertEquals(SWT.ICON_WARNING, pane.getSeverity());
	}

	@Test
	public void resizeSweepIsAViewportDelta() {
		pane.setError(failure);
		RepaintCounters.reset();
		int height = shell.getSize().y;
		for (int width : TestSupport.sweepWidths(480)) {
			shell.setSize(width, height);
		}
		if (viewport()) {
			assertNull("a resize never creates the details", pane.getDetailsText());
		}
		assertEquals(RepaintCounters.line(), 0, RepaintCounters.creates + RepaintCounters.fullRepaints);
		pane.setDetailsVisible(true);
		Text text = pane.getDetailsText();
		for (int width : TestSupport.sweepWidths(480)) {
			shell.setSize(width, height);
			assertSame(text, pane.getDetailsText());
		}
		assertEquals(RepaintCounters.line(), viewport() ? 1 : 0, RepaintCounters.creates);
	}

	@Test
	public void dialogOpensAndCloses() {
		display.timerExec(200, () -> {
			for (Shell s : display.getShells()) {
				if ("Sort failed".equals(s.getText())) {
					s.close();
				}
			}
		});
		ErrorPane.open(shell, "Sort failed", "The table could not be sorted.", failure);
	}

	@Test
	public void hiddenPaneFoldsChangesAndAppliesThemOnShow() {
		if (!viewport()) {
			return;
		}
		java.util.List<org.eclipse.swt.graphics.Rectangle> paints = TestSupport.recordPaints(pane);
		org.eclipse.swt.widgets.Label message = (org.eclipse.swt.widgets.Label) pane.getChildren()[1];
		org.eclipse.swt.widgets.Composite buttons =
				(org.eclipse.swt.widgets.Composite) pane.getChildren()[2];
		org.eclipse.swt.widgets.Button nativeDetailsButton =
				(org.eclipse.swt.widgets.Button) buttons.getChildren()[1];
		try {
			TestSupport.assertHiddenGate(pane, paints, i -> {
				pane.setMessage("message " + i);
				pane.setSeverity(i % 2 == 0 ? SWT.ICON_WARNING : SWT.ICON_ERROR);
				pane.setError(i % 3 == 0 ? failure : null);
			}, nativeDetailsButton);
		} catch (AssertionError nativePaintFailure) {
			// Preserve the original failed assertion and its stack. Tycho does not
			// archive System.out from the OSGi test, so attach geometry as suppressed
			// evidence to the same AssertionError in the Surefire XML failure trace.
			final StringBuilder nativeGeometry =
					new StringBuilder("ERROR_PANE_NATIVE_PAINT_DIAGNOSTIC pane=");
			try {
				nativeGeometry.append(pane.getClientArea()).append(" events=")
						.append(paints).append('\n');
				appendNativeChildGeometry(pane, pane, nativeGeometry);
			} catch (RuntimeException diagnosticFailure) {
				nativePaintFailure.addSuppressed(diagnosticFailure);
			}
			nativePaintFailure.addSuppressed(new AssertionError(nativeGeometry.toString()));
			throw nativePaintFailure;
		}
		assertEquals("message 9999", message.getText());
		assertEquals("message 9999", pane.getMessage());
		assertEquals(SWT.ICON_ERROR, pane.getSeverity());
		assertSame(failure, pane.getError());
	}


	@Test
	public void nativeChildPaintAllowanceCannotHideBodyDamageOrSecondFullPaint() {
		org.eclipse.swt.widgets.Composite buttons =
				(org.eclipse.swt.widgets.Composite) pane.getChildren()[2];
		org.eclipse.swt.widgets.Button nativeDetailsButton =
				(org.eclipse.swt.widgets.Button) buttons.getChildren()[1];
		org.eclipse.swt.graphics.Point paneSize = pane.getSize();
		org.eclipse.swt.graphics.Rectangle full =
				new org.eclipse.swt.graphics.Rectangle(0, 0, paneSize.x, paneSize.y);
		org.eclipse.swt.graphics.Point position =
				pane.toControl(nativeDetailsButton.toDisplay(0, 0));
		org.eclipse.swt.graphics.Point buttonSize = nativeDetailsButton.getSize();
		org.eclipse.swt.graphics.Rectangle clip =
				new org.eclipse.swt.graphics.Rectangle(position.x, position.y,
						buttonSize.x, buttonSize.y);
		java.util.List<org.eclipse.swt.graphics.Rectangle> paints = new java.util.ArrayList<>();
		paints.add(full);
		paints.add(clip);
		TestSupport.assertOneFullPanePaintWithChildOnly(pane, paints, nativeDetailsButton);
		assertFalse("a client-corner pixel must not be inside the button", clip.contains(0, 0));
		paints.add(new org.eclipse.swt.graphics.Rectangle(0, 0, 1, 1));
		boolean refused = false;
		try {
			TestSupport.assertOneFullPanePaintWithChildOnly(pane, paints, nativeDetailsButton);
		} catch (AssertionError expected) {
			refused = true;
		}
		assertTrue("even one native body pixel outside the button is forbidden", refused);
		paints.clear();
		paints.add(full);
		paints.add(full);
		refused = false;
		try {
			TestSupport.assertOneFullPanePaintWithChildOnly(pane, paints, nativeDetailsButton);
		} catch (AssertionError expected) {
			refused = true;
		}
		assertTrue("two full native pane repaints must fail", refused);
	}


	private static void appendNativeChildGeometry(org.eclipse.swt.widgets.Composite owner,
			org.eclipse.swt.widgets.Composite parent, StringBuilder diagnostic) {
		for (org.eclipse.swt.widgets.Control child : parent.getChildren()) {
			org.eclipse.swt.graphics.Point position = owner.toControl(child.toDisplay(0, 0));
			org.eclipse.swt.graphics.Rectangle bounds = child.getBounds();
			diagnostic.append("ERROR_PANE_NATIVE_CHILD type=").append(child.getClass().getName())
					.append(" client=").append(new org.eclipse.swt.graphics.Rectangle(position.x, position.y,
							bounds.width, bounds.height))
					.append(" visible=").append(child.getVisible())
					.append(" enabled=").append(child.getEnabled()).append('\n');
			if (child instanceof org.eclipse.swt.widgets.Composite) {
				appendNativeChildGeometry(owner, (org.eclipse.swt.widgets.Composite) child, diagnostic);
			}
		}
	}
	@Test
	public void collapsedDetailsFormatAndPushNothingUntilShown() {
		if (!viewport()) {
			return;
		}
		int[] formats = new int[1];
		IllegalStateException counted = new IllegalStateException("counted") {
			private static final long serialVersionUID = 1L;

			@Override
			public void printStackTrace(java.io.PrintWriter writer) {
				formats[0]++;
				super.printStackTrace(writer);
			}
		};
		pane.setDetails("first");
		pane.setDetailsVisible(true);
		Text text = pane.getDetailsText();
		assertEquals("first", text.getText());
		pane.setDetailsVisible(false);
		for (int i = 0; i < 1000; i++) {
			pane.setError(counted);
			pane.setDetails("details " + i);
		}
		pane.setError(counted);
		assertEquals("collapsed: no stack trace formatted", 0, formats[0]);
		assertEquals("collapsed: nothing pushed", "first", text.getText());
		pane.setDetailsVisible(true);
		assertEquals("formatted once when shown", 1, formats[0]);
		assertTrue(text.getText().contains("counted"));
		pane.setDetails("shown");
		assertEquals("shown details are pushed at once", "shown", text.getText());
	}

	@Test
	public void detailsButtonNativeStateIsIdempotentAcrossModelUpdates() {
		// The original two native buttons remain in the existing controls
		// composite; no separate renderer or wrapper is introduced.
		org.eclipse.swt.widgets.Composite buttons =
				(org.eclipse.swt.widgets.Composite) pane.getChildren()[2];
		org.eclipse.swt.widgets.Button detailsButton =
				(org.eclipse.swt.widgets.Button) buttons.getChildren()[1];
		assertEquals("Details >>", detailsButton.getText());
		assertFalse(detailsButton.getEnabled());
		pane.setError(failure);
		assertTrue(detailsButton.getEnabled());
		for (int i = 0; i < 500; i++) {
			pane.setError(failure);
			assertEquals("Details >>", detailsButton.getText());
			assertTrue(detailsButton.getEnabled());
		}
		pane.setDetails("");
		assertFalse(detailsButton.getEnabled());
		pane.setDetails("native details");
		assertTrue(detailsButton.getEnabled());
		pane.setDetailsVisible(true);
		assertEquals("Details <<", detailsButton.getText());
		pane.setDetailsVisible(false);
		assertEquals("Details >>", detailsButton.getText());
	}

	@Test
	public void errors() {
		TestSupport.expect(SWT.ERROR_NULL_ARGUMENT, () -> pane.setMessage(null));
		TestSupport.expect(SWT.ERROR_NULL_ARGUMENT, () -> pane.setDetails(null));
		TestSupport.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setSeverity(SWT.ICON_QUESTION));
		pane.setError(null);
		assertEquals("", pane.getDetails());
	}
}
