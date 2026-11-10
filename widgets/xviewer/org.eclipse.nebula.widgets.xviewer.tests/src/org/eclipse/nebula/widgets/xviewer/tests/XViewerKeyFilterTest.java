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
package org.eclipse.nebula.widgets.xviewer.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.core.model.SortDataType;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerAlign;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget leaf of the XViewer key filters: the Display-wide Ctrl/Alt filters shared by all XViewers stay while any
 * XViewer lives, so disposing the first viewer does not stop Ctrl/Alt tracking of the others.
 */
public class XViewerKeyFilterTest {

	private Display display;
	private Shell shell;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(300, 200);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void disposingTheFirstViewerKeepsCtrlTrackingForTheOthers() {
		XViewer first = viewer("tt.keys.first");
		XViewer second = viewer("tt.keys.second");
		shell.open();
		first.dispose();
		key(SWT.KeyDown, SWT.CTRL);
		assertTrue("Ctrl down is tracked for the viewer still alive", second.isCtrlKeyDown());
		key(SWT.KeyUp, SWT.CTRL);
		assertFalse("Ctrl up is tracked for the viewer still alive", second.isCtrlKeyDown());
		second.dispose();
	}

	private XViewer viewer(String namespace) {
		XViewerColumn name = new XViewerColumn(namespace + ".name", "Name", 200, XViewerAlign.Left, true,
				SortDataType.String, false, null);
		XViewerFactory factory = new XViewerFactory(namespace) {
			@Override
			public boolean isAdmin() {
				return false;
			}
		};
		factory.registerColumns(name);
		return new XViewer(shell, SWT.MULTI | SWT.FULL_SELECTION, factory);
	}

	/* a key event through the Display filters, as a real key press of any control would pass them */
	private void key(int type, int keyCode) {
		Event event = new Event();
		event.keyCode = keyCode;
		shell.notifyListeners(type, event);
	}
}
