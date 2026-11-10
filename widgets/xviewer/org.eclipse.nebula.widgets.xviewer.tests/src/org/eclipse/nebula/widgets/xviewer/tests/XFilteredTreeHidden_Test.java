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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.nebula.widgets.xviewer.util.internal.PatternFilter;
import org.eclipse.nebula.widgets.xviewer.util.internal.dialog.XFilteredTree;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.WorkbenchJob;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Every-widget H leaf of XFilteredTree: filter changes while the tree is hidden schedule no refresh job (before: one
 * full tree refresh per change); the reveal schedules one. The refresh job itself needs a running workbench, so this
 * counts the schedules (getRefreshJobDelay, asked once per schedule) with a refresh job that declines to run.
 */
public class XFilteredTreeHidden_Test {

	static final class CountingTree extends XFilteredTree {
		int schedules;

		CountingTree(Composite parent) {
			super(parent, SWT.BORDER, new PatternFilter());
		}

		@Override
		protected WorkbenchJob doCreateRefreshJob() {
			return new WorkbenchJob("counted refresh") {
				@Override
				public boolean shouldSchedule() {
					return false; // no workbench in this test
				}

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					return Status.OK_STATUS;
				}
			};
		}

		@Override
		protected long getRefreshJobDelay() {
			schedules++;
			return 200;
		}
	}

	private Display display;
	private Shell shell;
	private Composite holder;
	private CountingTree tree;

	@Before
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(300, 400);
		holder = new Composite(shell, SWT.NONE);
		holder.setLayout(new FillLayout());
		tree = new CountingTree(holder);
		tree.getViewer().setContentProvider(new FilteredTreeHidden_Test.Counting());
		tree.getViewer().setLabelProvider(new LabelProvider());
		tree.getViewer().setInput(new String[] { "alpha", "beta", "gamma" });
		shell.open();
		pump();
	}

	@After
	public void tearDown() {
		shell.dispose();
	}

	@Test
	public void hiddenFilterChangesScheduleOneRefreshOnReveal() {
		holder.setVisible(false);
		pump();
		tree.schedules = 0;
		for (String text : new String[] { "a", "al", "alp", "al", "a", "al" }) {
			tree.getFilterControl().setText(text);
		}
		pump();
		assertEquals("hidden filter changes schedule no refresh", 0, tree.schedules);

		holder.setVisible(true);
		pump();
		assertEquals("the reveal schedules one refresh", 1, tree.schedules);

		tree.getFilterControl().setText("alp");
		assertEquals("a visible change schedules at once, as before", 2, tree.schedules);
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
