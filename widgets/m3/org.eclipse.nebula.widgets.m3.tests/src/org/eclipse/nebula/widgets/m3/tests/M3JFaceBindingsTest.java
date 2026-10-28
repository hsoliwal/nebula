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
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.nebula.widgets.m3.M3ErrorPane;
import org.eclipse.nebula.widgets.m3.M3FindBar;
import org.eclipse.nebula.widgets.m3.jface.M3StatusPane;
import org.eclipse.nebula.widgets.m3.jface.M3ViewerSearchable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JFace bindings: M3ViewerSearchable searches a TableViewer's model in its shown order (filters, then comparator) and
 * reveals through the viewer; M3FindBar finds over it; M3StatusPane shows a MultiStatus with deferred details.
 */
public class M3JFaceBindingsTest {

	Display display;
	Shell shell;
	TableViewer viewer;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display, SWT.NO_TRIM);
		shell.setBounds(10, 10, 400, 300);
		shell.setLayout(new FillLayout());
		viewer = new TableViewer(shell, SWT.VIRTUAL | SWT.BORDER);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return "item " + element;
			}
		});
		viewer.setFilters(new ViewerFilter() {
			@Override
			public boolean select(Viewer v, Object parent, Object element) {
				return ((Integer) element).intValue() % 2 == 0;
			}
		});
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer v, Object a, Object b) {
				return Integer.compare(((Integer) b).intValue(), ((Integer) a).intValue());
			}
		});
		Integer[] input = new Integer[1000];
		for (int i = 0; i < input.length; i++) {
			input[i] = Integer.valueOf(i);
		}
		viewer.setInput(input);
		shell.open();
		M3TestSupport.flush(display);
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void searchableFollowsTheShownOrder() {
		M3ViewerSearchable searchable = M3ViewerSearchable.of(viewer, -1);
		assertEquals(500, searchable.getRowCount());
		assertEquals("item 998", searchable.getText(0));
		assertEquals("item 0", searchable.getText(499));
		searchable.reveal(3);
		assertEquals(Integer.valueOf(992), ((IStructuredSelection) viewer.getSelection()).getFirstElement());
		Shell barShell = new Shell(display, SWT.NO_TRIM);
		M3FindBar bar = new M3FindBar(barShell, SWT.NONE);
		bar.setBounds(0, 0, 400, 40);
		barShell.open();
		bar.setSearchable(searchable);
		bar.setText("item 10");
		for (int i = 0; i < 200 && bar.isSearching(); i++) {
			M3TestSupport.spin(display, 5);
		}
		assertEquals("item 108 .. item 100 and item 10 (even, descending)", 6, bar.getMatchCount());
		assertEquals(Integer.valueOf(108), ((IStructuredSelection) viewer.getSelection()).getFirstElement());
		barShell.dispose();
	}

	@Test
	public void statusPaneDefersTheStatusTree() {
		MultiStatus status = new MultiStatus("test", 0, "Sorting failed", null);
		status.add(Status.error("comparator threw", new IllegalStateException("column 3")));
		M3ErrorPane pane = new M3ErrorPane(shell, SWT.NONE);
		M3StatusPane.show(pane, status);
		assertEquals("Sorting failed", pane.getMessage());
		assertEquals(SWT.ICON_ERROR, pane.getSeverity());
		String details = pane.getDetails();
		assertTrue(details, details.contains("comparator threw") && details.contains("IllegalStateException"));
		M3StatusPane.show(pane, Status.info("all good"));
		assertEquals(SWT.ICON_INFORMATION, pane.getSeverity());
		assertEquals("", pane.getDetails());
		assertTrue(IStatus.OK == Status.OK_STATUS.getSeverity());
	}
}
