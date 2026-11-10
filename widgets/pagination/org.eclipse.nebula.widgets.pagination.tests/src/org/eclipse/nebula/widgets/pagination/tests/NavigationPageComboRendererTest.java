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
 *******************************************************************************/
package org.eclipse.nebula.widgets.pagination.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.lang.reflect.Field;
import java.util.Locale;

import org.eclipse.nebula.widgets.pagination.PageableController;
import org.eclipse.nebula.widgets.pagination.PaginationHelper;
import org.eclipse.nebula.widgets.pagination.renderers.navigation.NavigationPageComboRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget leaf of the page combo: a page step only moves the combo's selection; the items are rebuilt when the page
 * count or the locale changes.
 */
public class NavigationPageComboRendererTest {

	private static final String MARK = "kept";
	private Display display;
	private Shell shell;
	private PageableController controller;
	private Combo combo;

	@Before
	public void setUp() throws Exception {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		controller = new PageableController(10);
		controller.setLocale(Locale.ENGLISH);
		NavigationPageComboRenderer renderer = new NavigationPageComboRenderer(shell, SWT.NONE, controller);
		controller.setTotalElements(1000); // 100 pages
		controller.setCurrentPage(0);
		Field field = NavigationPageComboRenderer.class.getDeclaredField("pageCombo");
		field.setAccessible(true);
		combo = (Combo) field.get(renderer);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void aPageStepOnlyMovesTheSelection() {
		assertEquals(100, combo.getItemCount());
		combo.setItem(50, MARK); // a rebuild would bring back the formatted text
		controller.setCurrentPage(3);
		assertEquals("a page step selects the page", 3, combo.getSelectionIndex());
		assertEquals("a page step rebuilds no item", MARK, combo.getItem(50));
	}

	@Test
	public void aPageSizeChangeRebuildsTheItems() {
		combo.setItem(5, MARK);
		controller.setPageSize(20); // 50 pages
		assertEquals(50, combo.getItemCount());
		assertEquals(PaginationHelper.getPageText(6, 50, Locale.ENGLISH), combo.getItem(5));
	}

	@Test
	public void aLocaleChangeRebuildsTheItemsOnTheNextStep() {
		combo.setItem(5, MARK);
		controller.setLocale(Locale.FRENCH);
		controller.setCurrentPage(4);
		assertNotEquals("the items are rebuilt for the new locale", MARK, combo.getItem(5));
		assertEquals(PaginationHelper.getPageText(6, 100, Locale.FRENCH), combo.getItem(5));
		assertEquals(4, combo.getSelectionIndex());
	}
}
