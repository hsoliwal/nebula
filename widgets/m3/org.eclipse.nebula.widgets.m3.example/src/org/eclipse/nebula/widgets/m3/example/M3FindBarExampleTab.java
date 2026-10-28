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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.nebula.examples.AbstractExampleTab;
import org.eclipse.nebula.widgets.m3.M3FindBar;
import org.eclipse.nebula.widgets.m3.jface.M3ViewerSearchable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Nebula examples tab (an Eclipse view) for {@link M3FindBar} bound to a JFace TableViewer through
 * {@link M3ViewerSearchable}: 100,000 elements, the find bar searches the viewer's model in its shown order and selects
 * and reveals matches through the viewer.
 */
public class M3FindBarExampleTab extends AbstractExampleTab {

	static final int ROWS = 100_000;

	private M3FindBar bar;
	private M3ViewerSearchable searchable;

	@Override
	public Control createControl(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(1, false));
		bar = new M3FindBar(area, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		TableViewer viewer = new TableViewer(area, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTable().setHeaderVisible(true);
		for (String title : new String[] { "Order", "Status" }) {
			TableColumn column = new TableColumn(viewer.getTable(), SWT.NONE);
			column.setText(title);
			column.setWidth(160);
		}
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new Labels());
		Integer[] orders = new Integer[ROWS];
		for (int i = 0; i < ROWS; i++) {
			orders[i] = Integer.valueOf(i);
		}
		viewer.setInput(orders);
		searchable = M3ViewerSearchable.of(viewer, 1);
		bar.setSearchable(searchable);
		bar.setText("overdue");
		return area;
	}

	static final class Labels extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			int order = ((Integer) element).intValue();
			return columnIndex == 0 ? "order " + order : order % 997 == 3 ? "overdue" : "open"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Override
	public void createParameters(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		Button next = new Button(parent, SWT.PUSH);
		next.setText("Find next"); //$NON-NLS-1$
		next.addListener(SWT.Selection, e -> bar.findNext());
	}

	@Override
	public String[] createLinks() {
		return new String[] { "https://github.com/hsoliwal/nebula/tree/m3/nebula-widgets-viewport-20261019/widgets/m3" }; //$NON-NLS-1$
	}
}
