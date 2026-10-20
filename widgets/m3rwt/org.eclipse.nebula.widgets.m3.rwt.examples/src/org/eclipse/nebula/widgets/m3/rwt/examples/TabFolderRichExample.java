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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** TAB_FOLDER_RICH on RWT: CTabFolder with 2,000 closable tabs; only the selected tab has content. */
public final class TabFolderRichExample extends RwtExample {

	static final int TABS = 2_000;

	public TabFolderRichExample () {
		super ("TAB_FOLDER_RICH", "org.eclipse.swt.custom.CTabFolder");
	}

	@Override
	public Control create (Composite parent) {
		CTabFolder folder = new CTabFolder (parent, SWT.BORDER | SWT.CLOSE);
		for (int i = 0; i < TABS; i++) new CTabItem (folder, SWT.NONE).setText ("Tab " + i);
		CTabItem first = folder.getItem (0);
		Label content = new Label (folder, SWT.NONE);
		content.setText ("Content of the selected tab");
		first.setControl (content);
		folder.setSelection (first);
		return folder;
	}

	@Override
	public void change (Control control) {
		new CTabItem ((CTabFolder) control, SWT.NONE).setText ("Added tab");
	}
}
