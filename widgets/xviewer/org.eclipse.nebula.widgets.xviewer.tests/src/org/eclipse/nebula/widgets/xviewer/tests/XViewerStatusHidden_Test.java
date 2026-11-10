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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.core.model.SortDataType;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerAlign;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget H leaf of the XViewer status line: element refreshes while the status line is hidden compute no status
 * (before: one full count per refresh); the reveal, or a read of the status label, computes it once, to the same text.
 */
public class XViewerStatusHidden_Test {

	static final class CountingViewer extends XViewer {
		int computations;

		CountingViewer(Composite parent, XViewerFactory factory) {
			super(parent, SWT.MULTI | SWT.FULL_SELECTION, factory);
		}

		@Override
		public String getStatusString() {
			computations++;
			return "";
		}
	}

	private Display display;
	private Shell shell;
	private Composite parent;
	private CountingViewer viewer;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new GridLayout(1, false));
		shell.setBounds(10, 10, 600, 500);
		parent = new Composite(shell, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		XViewerColumn name = new XViewerColumn("tt.status.name", "Name", 200, XViewerAlign.Left, true,
				SortDataType.String, false, null);
		XViewerFactory factory = new XViewerFactory("tt.status.test") {
			@Override
			public boolean isAdmin() {
				return false;
			}
		};
		factory.registerColumns(name);
		viewer = new CountingViewer(parent, factory);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new XViewerViewport_Test.Model(200, 0));
		viewer.setLabelProvider(new XViewerViewport_Test.Labels(viewer));
		viewer.setInputXViewer(XViewerViewport_Test.Model.roots(200));
		shell.open();
		pump();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void hiddenRefreshesComputeNoStatus() {
		String shown = viewer.getStatusLabel().getText();
		parent.setVisible(false);
		pump();
		viewer.computations = 0;
		for (int i = 0; i < 100; i++) {
			viewer.refresh(Integer.valueOf(i));
		}
		assertEquals("100 hidden refreshes compute no status", 0, viewer.computations);
		assertEquals("a read computes it once, to the same text", shown, viewer.getStatusLabel().getText());
		assertEquals(1, viewer.computations);
	}

	@Test
	public void theRevealComputesTheStatusOnce() {
		parent.setVisible(false);
		pump();
		viewer.computations = 0;
		for (int i = 0; i < 50; i++) {
			viewer.refresh(Integer.valueOf(i));
		}
		parent.setVisible(true);
		pump();
		assertEquals("one computation on reveal", 1, viewer.computations);
		String revealed = viewer.getStatusLabel().getText();
		viewer.updateStatusLabel();
		assertEquals("the revealed text equals a fresh computation", viewer.getStatusLabel().getText(), revealed);
		assertTrue(revealed.contains("200"));
	}

	private void pump() {
		long end = System.currentTimeMillis() + 300;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
