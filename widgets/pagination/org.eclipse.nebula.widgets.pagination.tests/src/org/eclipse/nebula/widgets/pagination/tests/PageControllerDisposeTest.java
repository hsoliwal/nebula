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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Locale;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.nebula.widgets.pagination.PageableController;
import org.eclipse.nebula.widgets.pagination.renderers.navigation.NavigationPageComboRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget leaf of the page controller composites: a renderer leaves a caller-owned controller when it is disposed,
 * also when only an ancestor is disposed (SWT does not call dispose() on the children then), so the next page change
 * does not reach a disposed widget.
 */
public class PageControllerDisposeTest {

	private Display display;
	private PageableController controller;

	@Before
	public void setUp() {
		display = Display.getDefault();
		controller = new PageableController(10);
		controller.setLocale(Locale.ENGLISH);
		controller.setTotalElements(1000);
	}

	@Test
	public void theRendererOfADisposedShellLeavesTheController() throws Exception {
		int baseline = listeners();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new FillLayout());
		NavigationPageComboRenderer renderer = new NavigationPageComboRenderer(parent, SWT.NONE, controller);
		assertEquals(baseline + 1, listeners());

		shell.dispose();
		assertTrue(renderer.isDisposed());
		controller.setCurrentPage(3); // reached the disposed combo before: SWTException (Widget is disposed)
		controller.setTotalElements(2000);
		assertEquals(3, controller.getCurrentPage());
		assertEquals("the disposed renderer left the controller", baseline, listeners());
	}

	@Test
	public void anExplicitlyDisposedRendererLeavesTheController() throws Exception {
		int baseline = listeners();
		Shell shell = new Shell(display);
		NavigationPageComboRenderer renderer = new NavigationPageComboRenderer(shell, SWT.NONE, controller);
		renderer.dispose();
		assertEquals(baseline, listeners());
		controller.setCurrentPage(2);
		shell.dispose();
	}

	private int listeners() throws Exception {
		Field field = PageableController.class.getDeclaredField("pageChangedListeners");
		field.setAccessible(true);
		return ((ListenerList<?>) field.get(controller)).size();
	}
}
