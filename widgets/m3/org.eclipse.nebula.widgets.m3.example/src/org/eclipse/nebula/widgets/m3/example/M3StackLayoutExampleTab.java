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
package org.eclipse.nebula.widgets.m3.example;

import java.util.Arrays;

import org.eclipse.nebula.examples.AbstractExampleTab;
import org.eclipse.nebula.widgets.m3.M3StackLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * Nebula examples tab for {@link M3StackLayout}: 1,000,000 pages, created on first show and kept within a residency
 * budget that the parameters change.
 */
public class M3StackLayoutExampleTab extends AbstractExampleTab {

	static final int PAGES = 1_000_000;

	private M3StackLayout layout;
	private Spinner page;
	private Spinner budget;
	private Label status;

	@Override
	public Control createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		Composite stack = new Composite(area, SWT.BORDER);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layout = new M3StackLayout();
		stack.setLayout(layout);
		layout.setPages(stack, PAGES, M3StackLayoutExampleTab::createPage);
		status = new Label(area, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		show();
		return area;
	}

	static Control createPage(Composite parent, int index) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new FillLayout(SWT.VERTICAL));
		Label label = new Label(page, SWT.CENTER);
		label.setText("Page " + index + " of " + PAGES);
		Text text = new Text(page, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		text.setText("Type here: the text is kept while the page is resident and goes when the page is released.");
		return page;
	}

	private void show() {
		if (layout == null || page == null) {
			return;
		}
		layout.setResidencyBudget(budget.getSelection());
		layout.showPage(page.getSelection());
		status.setText("resident: " + Arrays.toString(layout.getResidentPages()));
	}

	@Override
	public void createParameters(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		new Label(parent, SWT.NONE).setText("Page");
		page = new Spinner(parent, SWT.BORDER);
		page.setMinimum(0);
		page.setMaximum(PAGES - 1);
		page.setPageIncrement(PAGES / 100);
		page.setSelection(PAGES / 2);
		page.addListener(SWT.Selection, e -> show());
		new Label(parent, SWT.NONE).setText("Residency budget");
		budget = new Spinner(parent, SWT.BORDER);
		budget.setMinimum(1);
		budget.setMaximum(64);
		budget.setSelection(M3StackLayout.DEFAULT_RESIDENCY_BUDGET);
		budget.addListener(SWT.Selection, e -> show());
		show();
	}

	@Override
	public String[] createLinks() {
		return new String[] { "<a href=\"https://github.com/hsoliwal/nebula/blob/master/docs/m3/custom-widgets/stack/M3_STACK_LAYOUT.md\">M3StackLayout design</a>" };
	}
}
