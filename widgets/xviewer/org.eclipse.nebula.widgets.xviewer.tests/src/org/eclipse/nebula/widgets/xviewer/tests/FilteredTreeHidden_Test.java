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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.nebula.widgets.xviewer.util.XViewerDisplay;
import org.eclipse.nebula.widgets.xviewer.util.internal.FilteredTreeComposite;
import org.eclipse.nebula.widgets.xviewer.util.internal.PatternFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget H leaf of FilteredTreeComposite: filter changes while the tree is hidden refresh nothing (before: one
 * full tree refresh per change); the reveal refreshes once, to the rows of the same filter applied while visible.
 */
public class FilteredTreeHidden_Test {

	static final String[] NAMES = { "alpha", "albatross", "beta", "gamma", "alder", "delta", "alpine", "omega" };

	static final class Counting implements ITreeContentProvider {
		int elements;

		@Override
		public Object[] getElements(Object input) {
			elements++;
			return (Object[]) input;
		}

		@Override
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	private Display display;
	private Shell shell;
	private Composite holder;
	private FilteredTreeComposite tree;
	private Counting content;

	@Before
	public void setUp() {
		display = Display.getDefault();
		XViewerDisplay.setStandaloneXViewer(true, display);
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(300, 400);
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		tree = new FilteredTreeComposite(holder, SWT.BORDER, new PatternFilter());
		content = new Counting();
		tree.getViewer().setContentProvider(content);
		tree.getViewer().setLabelProvider(new LabelProvider());
		tree.getViewer().setInput(NAMES);
		shell.open();
		pump();
	}

	@After
	public void tearDown() {
		shell.dispose();
		XViewerDisplay.setStandaloneXViewer(false, display);
	}

	@Test
	public void hiddenFilterChangesRefreshOnceOnReveal() {
		holder.setVisible(false);
		pump();
		content.elements = 0;
		for (String text : new String[] { "a", "al", "alp", "al", "a", "al" }) {
			tree.getFilterControl().setText(text);
		}
		pump();
		assertEquals("hidden filter changes refresh nothing", 0, content.elements);

		holder.setVisible(true);
		pump();
		assertTrue("the reveal refreshes once, got " + content.elements, content.elements >= 1 && content.elements <= 2);
		List<String> shown = rows();
		tree.getFilterControl().setText("a");
		pump();
		tree.getFilterControl().setText("al");
		pump();
		assertEquals("the same rows as the filter applied while visible", rows(), shown);
		assertTrue(shown.contains("alpha") && !shown.contains("beta"));
	}

	private List<String> rows() {
		List<String> texts = new ArrayList<>();
		for (TreeItem item : tree.getViewer().getTree().getItems()) {
			texts.add(item.getText());
		}
		return texts;
	}

	private void pump() {
		long end = System.currentTimeMillis() + 400;
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
