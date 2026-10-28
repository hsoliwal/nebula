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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.m3.M3FindBar;
import org.eclipse.nebula.widgets.m3.M3Searchable;
import org.eclipse.nebula.widgets.m3.internal.M3Counters;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * M3FindBar over 10^6 model rows: first match, count and "k of n", next/previous by the hit lane with and without
 * wrap, match case, superseding generations (only the last search reveals), no allocation per scanned row, the hidden
 * gate (no scan while hidden), a resize sweep, errors. Runs in both startup modes.
 */
public class M3FindBarTest {

	static final int ROWS = 1_000_000;
	static final String[] TEXTS = new String[ROWS];
	static {
		for (int i = 0; i < ROWS; i++) {
			TEXTS[i] = i % 1000 == 7 ? "item " + i + " Needle" : "item " + i;
		}
	}

	Display display;
	Shell shell;
	M3FindBar bar;
	final List<Integer> revealed = new ArrayList<>();
	final List<Integer> selections = new ArrayList<>();
	long reads;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 640, 60);
		bar = new M3FindBar(shell, SWT.NONE);
		bar.setBounds(0, 0, 640, 40);
		bar.setSearchable(M3Searchable.of(() -> ROWS, row -> {
			reads++;
			return TEXTS[row];
		}, revealed::add));
		bar.addListener(SWT.Selection, e -> selections.add(e.index));
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	static boolean viewport() {
		return WidgetMode.of("M3FindBar", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$
	}

	void waitForSearch() {
		for (int i = 0; i < 500 && bar.isSearching(); i++) {
			M3TestSupport.spin(display, 5);
		}
		assertFalse("search finished", bar.isSearching());
	}

	@Test
	public void findsCountsAndSteps() {
		bar.setText("needle");
		waitForSearch();
		assertEquals(7, bar.getCurrentMatch());
		assertEquals(1000, bar.getMatchCount());
		assertEquals("1 of 1000", bar.getStatus());
		assertEquals(1007, bar.findNext());
		assertEquals("2 of 1000", bar.getStatus());
		assertEquals(7, bar.findPrevious());
		assertEquals(999_007, bar.findPrevious());
		assertEquals("wraps forward", 7, bar.findNext());
		bar.setWrap(false);
		assertEquals(-1, bar.findPrevious());
		assertEquals(Integer.valueOf(7), revealed.get(revealed.size() - 1));
	}

	@Test
	public void matchCaseFiltersMatches() {
		bar.setMatchCase(true);
		bar.setText("needle");
		waitForSearch();
		assertEquals(0, bar.getMatchCount());
		assertEquals("no match", bar.getStatus());
		bar.setText("Needle");
		waitForSearch();
		assertEquals(1000, bar.getMatchCount());
	}

	@Test
	public void newerSearchSupersedesTheRunningOne() {
		bar.setText("item 9");
		bar.setText("item 99");
		bar.setText("Needle");
		waitForSearch();
		if (viewport()) {
			assertEquals("only the last generation revealed: " + selections, 1, selections.size());
		} else {
			assertEquals("original mode searches each keystroke at once: " + selections, 3, selections.size());
		}
		assertEquals(Integer.valueOf(7), selections.get(selections.size() - 1));
		assertEquals(1000, bar.getMatchCount());
	}

	@Test
	public void scanningAllocatesNothingPerRow() {
		if (!viewport()) {
			return;
		}
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		bar.setText("x");
		waitForSearch();
		long before = threads.getCurrentThreadAllocatedBytes();
		bar.setText("zzz");
		waitForSearch();
		long bytes = threads.getCurrentThreadAllocatedBytes() - before;
		assertEquals(0, bar.getMatchCount());
		assertTrue("10^6 rows scanned with " + bytes + " bytes allocated", bytes < 512 * 1024);
	}

	@Test
	public void hiddenBarScansNothingUntilShown() {
		if (!viewport()) {
			return;
		}
		bar.setVisible(false);
		M3TestSupport.flush(display);
		M3Counters.reset();
		reads = 0;
		for (int i = 0; i < 10_000; i++) {
			bar.setText(i % 2 == 0 ? "need" : "needle");
		}
		M3TestSupport.spin(display, 50);
		assertEquals("no row read while hidden", 0, reads);
		assertEquals(M3Counters.line(), 0, M3Counters.probes);
		bar.setVisible(true);
		waitForSearch();
		assertEquals(1, M3Counters.reveals);
		assertEquals(1000, bar.getMatchCount());
		assertEquals("needle", bar.getText());
	}

	@Test
	public void resizeSweepStartsNoSearch() {
		bar.setText("needle");
		waitForSearch();
		reads = 0;
		M3Counters.reset();
		int height = bar.getSize().y;
		for (int width : M3TestSupport.sweepWidths(640)) {
			bar.setSize(width, height);
		}
		M3TestSupport.flush(display);
		assertEquals("a resize reads no row", 0, reads);
		assertEquals(M3Counters.line(), 0, M3Counters.fullRepaints);
	}

	@Test
	public void errors() {
		M3StackLayoutTest.expect(SWT.ERROR_NULL_ARGUMENT, () -> bar.setText(null));
		bar.setSearchable(null);
		assertEquals(-1, bar.findNext());
		bar.setText("");
		assertEquals("", bar.getStatus());
	}
}
