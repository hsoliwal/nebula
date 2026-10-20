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
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.m3.M3ErrorPane;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3ErrorPane: deferred details (formatted and created on first show, reused after), report text, severity, a resize
 * sweep that never formats or creates the details, the modal dialog, errors. Runs in both startup modes.
 */
public class M3ErrorPaneTest {

	Display display;
	Shell shell;
	M3ErrorPane pane;
	final IllegalStateException failure = new IllegalStateException("index 1000000 out of bounds", new ArithmeticException("cause"));

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM | SWT.RESIZE);
		shell.setBounds(10, 10, 480, 360);
		shell.setLayout(new FillLayout());
		M3Counters.reset();
		pane = new M3ErrorPane(shell, SWT.NONE);
		pane.setMessage("The table could not be sorted.");
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3ErrorPane", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	@Test
	public void detailsAreCreatedOnFirstShow() {
		pane.setError(failure);
		assertSame(failure, pane.getError());
		if (viewport()) {
			assertNull("no details control before the details are shown", pane.getDetailsText());
			assertEquals(0, M3Counters.creates);
		}
		assertFalse(pane.isDetailsVisible());
		pane.setDetailsVisible(true);
		assertTrue(pane.isDetailsVisible());
		Text text = pane.getDetailsText();
		assertNotNull(text);
		assertTrue(text.getText().contains("IllegalStateException"));
		assertTrue(text.getText().contains("Caused by"));
		assertTrue(text.getSize().y > 0);
		pane.setDetailsVisible(false);
		pane.setDetailsVisible(true);
		assertSame("the details control is reused", text, pane.getDetailsText());
		assertEquals(1, M3Counters.creates);
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
		M3Counters.reset();
		int height = shell.getSize().y;
		for (int width : M3TestSupport.sweepWidths(480)) {
			shell.setSize(width, height);
		}
		if (viewport()) {
			assertNull("a resize never creates the details", pane.getDetailsText());
		}
		assertEquals(M3Counters.line(), 0, M3Counters.creates + M3Counters.fullRepaints);
		pane.setDetailsVisible(true);
		Text text = pane.getDetailsText();
		for (int width : M3TestSupport.sweepWidths(480)) {
			shell.setSize(width, height);
			assertSame(text, pane.getDetailsText());
		}
		assertEquals(M3Counters.line(), viewport() ? 1 : 0, M3Counters.creates);
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
		M3ErrorPane.open(shell, "Sort failed", "The table could not be sorted.", failure);
	}

	@Test
	public void hiddenPaneFoldsChangesAndAppliesThemOnShow() {
		if (!viewport()) {
			return;
		}
		java.util.List<org.eclipse.swt.graphics.Rectangle> paints = M3TestSupport.recordPaints(pane);
		org.eclipse.swt.widgets.Label message = (org.eclipse.swt.widgets.Label) pane.getChildren()[1];
		M3TestSupport.assertHiddenGate(pane, paints, i -> {
			pane.setMessage("message " + i);
			pane.setSeverity(i % 2 == 0 ? SWT.ICON_WARNING : SWT.ICON_ERROR);
			pane.setError(i % 3 == 0 ? failure : null);
		});
		assertEquals("message 9999", message.getText());
		assertEquals("message 9999", pane.getMessage());
		assertEquals(SWT.ICON_ERROR, pane.getSeverity());
		assertSame(failure, pane.getError());
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> pane.setMessage(null));
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> pane.setDetails(null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> pane.setSeverity(SWT.ICON_QUESTION));
		pane.setError(null);
		assertEquals("", pane.getDetails());
	}
}
