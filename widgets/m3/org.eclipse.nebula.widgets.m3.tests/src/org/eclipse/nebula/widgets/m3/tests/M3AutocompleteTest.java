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
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;

import org.eclipse.nebula.widgets.m3.M3Autocomplete;
import org.eclipse.nebula.widgets.m3.M3Searchable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3Autocomplete: prefix proposals before contains proposals, the limit, keyboard navigation and accept, Escape, the
 * minimum length, superseding keystrokes, no allocation per scanned row over 10^6 model rows.
 */
public class M3AutocompleteTest {

	static final String[] CITIES = { "Hamburg", "Berlin", "Alberta", "Bern", "Munich", "Bergen", "Uberlingen", "Bremen" };
	static final String[] BIG = new String[1_000_000];
	static {
		for (int i = 0; i < BIG.length; i++) {
			BIG[i] = "row " + i;
		}
	}

	Display display;
	Shell shell;
	Text text;
	M3Autocomplete complete;
	int accepted = -1;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(100, 100, 300, 80);
		text = new Text(shell, SWT.BORDER);
		text.setBounds(5, 5, 200, 24);
		complete = new M3Autocomplete(text, M3Searchable.of(() -> CITIES.length, row -> CITIES[row], row -> {
		}));
		text.addListener(SWT.Selection, e -> accepted = e.index);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	void type(String value) {
		text.setText(value);
		M3TestSupport.spin(display, 80);
	}

	void key(int keyCode, char character) {
		Event event = new Event();
		event.keyCode = keyCode;
		event.character = character;
		text.notifyListeners(SWT.KeyDown, event);
	}

	@Test
	public void prefixBeforeContains() {
		type("ber");
		assertTrue(complete.isOpen());
		assertEquals(5, complete.getProposalCount());
		assertEquals("Berlin", CITIES[complete.getProposalRow(0)]);
		assertEquals("Bern", CITIES[complete.getProposalRow(1)]);
		assertEquals("Bergen", CITIES[complete.getProposalRow(2)]);
		assertEquals("Alberta", CITIES[complete.getProposalRow(3)]);
		assertEquals("Uberlingen", CITIES[complete.getProposalRow(4)]);
		complete.setContains(false);
		type("berg");
		assertEquals(1, complete.getProposalCount());
		complete.setLimit(2);
		complete.setContains(true);
		type("e");
		assertEquals(2, complete.getProposalCount());
	}

	@Test
	public void keyboardAcceptsAndEscapeCloses() {
		type("ber");
		key(SWT.ARROW_DOWN, (char) 0);
		assertEquals(1, complete.getHighlighted());
		key(0, '\r');
		assertEquals("Bern", text.getText());
		assertEquals(3, accepted);
		assertFalse(complete.isOpen());
		type("mu");
		assertTrue(complete.isOpen());
		key(SWT.ESC, (char) 27);
		assertFalse(complete.isOpen());
		complete.setMinimumLength(3);
		type("mu");
		assertFalse("below the minimum length", complete.isOpen());
	}

	@Test
	public void keystrokesSupersedeAndScanAllocatesNothingPerRow() {
		M3Autocomplete big = new M3Autocomplete(text, M3Searchable.of(() -> BIG.length, row -> BIG[row], row -> {
		}));
		complete.setMinimumLength(99);
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		type("zz");
		long before = threads.getCurrentThreadAllocatedBytes();
		type("zzz");
		M3TestSupport.spin(display, 400);
		long bytes = threads.getCurrentThreadAllocatedBytes() - before;
		assertEquals(0, big.getProposalCount());
		assertTrue("2 x 10^6 rows scanned with " + bytes + " bytes allocated", bytes < 1024 * 1024);
		text.setText("row 1");
		text.setText("row 12");
		text.setText("row 123");
		M3TestSupport.spin(display, 400);
		assertTrue(big.isOpen());
		assertEquals("row 123", BIG[big.getProposalRow(0)]);
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> new M3Autocomplete(text, null));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_ARGUMENT, () -> big.setLimit(0));
		M3StackLayoutTest.expect(SWT.ERROR_INVALID_RANGE, () -> big.getProposalRow(9999));
	}
}
