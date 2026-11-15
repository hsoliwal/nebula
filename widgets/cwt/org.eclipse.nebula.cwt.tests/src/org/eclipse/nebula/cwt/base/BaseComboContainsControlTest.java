/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.cwt.base;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import junit.framework.TestCase;

/** Verifies focus containment across all child branches of a combo's popup. */
public class BaseComboContainsControlTest extends TestCase {

	private Display display;
	private boolean ownsDisplay;
	private Shell shell;

	@Override
	protected void setUp() {
		display = Display.getCurrent();
		ownsDisplay = display == null;
		if (ownsDisplay) {
			display = new Display();
		}
		shell = new Shell(display);
	}

	@Override
	protected void tearDown() {
		try {
			if (shell != null && !shell.isDisposed()) {
				shell.dispose();
			}
		} finally {
			if (ownsDisplay && display != null && !display.isDisposed()) {
				display.dispose();
			}
		}
	}

	public void testDirectChildAfterUnrelatedComposite() {
		Composite unrelated = new Composite(shell, SWT.NONE);
		Label target = new Label(shell, SWT.NONE);
		unrelated.moveAbove(null);
		assertSame(unrelated, shell.getChildren()[0]);
		assertTrue(BaseCombo.containsControl(target, shell));
	}

	public void testNestedChildAfterUnrelatedComposite() {
		Composite unrelated = new Composite(shell, SWT.NONE);
		new Label(unrelated, SWT.NONE);
		Composite branch = new Composite(shell, SWT.NONE);
		Label target = new Label(branch, SWT.NONE);
		unrelated.moveAbove(null);
		assertSame(unrelated, shell.getChildren()[0]);
		assertTrue(BaseCombo.containsControl(target, shell));
	}

	public void testFindsDescendantAfterAnEmptyNestedBranch() {
		Composite branch = new Composite(shell, SWT.NONE);
		Composite empty = new Composite(branch, SWT.NONE);
		Label target = new Label(branch, SWT.NONE);
		empty.moveAbove(null);
		assertSame(empty, branch.getChildren()[0]);
		assertTrue(BaseCombo.containsControl(target, shell));
	}

	public void testEveryBranchRemainsReachableAfterReordering() {
		Composite[] branches = new Composite[3];
		Label[] targets = new Label[3];
		for (int i = 0; i < branches.length; i++) {
			branches[i] = new Composite(shell, SWT.NONE);
			targets[i] = new Label(branches[i], SWT.NONE);
		}
		for (Composite first : branches) {
			first.moveAbove(null);
			for (Label target : targets) {
				assertTrue(BaseCombo.containsControl(target, shell));
			}
		}
	}

	public void testDirectChildAndFirstBranchRemainSupported() {
		Composite branch = new Composite(shell, SWT.NONE);
		Label target = new Label(branch, SWT.NONE);
		assertTrue(BaseCombo.containsControl(branch, shell));
		assertTrue(BaseCombo.containsControl(target, shell));
	}

	public void testUnrelatedControlIsNotContained() {
		Composite subtree = new Composite(shell, SWT.NONE);
		new Composite(subtree, SWT.NONE);
		Label outside = new Label(shell, SWT.NONE);
		assertFalse(BaseCombo.containsControl(outside, subtree));
	}

	public void testDisposedAndNullAncestorsAreNotContained() {
		Composite disposed = new Composite(shell, SWT.NONE);
		disposed.dispose();
		Label target = new Label(shell, SWT.NONE);
		assertFalse(BaseCombo.containsControl(target, disposed));
		assertFalse(BaseCombo.containsControl(target, null));
	}

	public void testNullAndDisposedControlsAreNotContained() {
		Label disposed = new Label(shell, SWT.NONE);
		disposed.dispose();
		assertFalse(BaseCombo.containsControl(disposed, shell));
		assertFalse(BaseCombo.containsControl(null, shell));
		assertFalse(BaseCombo.containsControl(shell, shell));
	}
}
