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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/** SEARCH_FIELD on RWT: Text with SWT.SEARCH, ICON_SEARCH and ICON_CANCEL. */
public final class SearchFieldExample extends RwtExample {

	public SearchFieldExample () {
		super ("SEARCH_FIELD", "org.eclipse.swt.widgets.Text (SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL)");
	}

	@Override
	public Control create (Composite parent) {
		Text text = new Text (parent, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL | SWT.BORDER);
		text.setMessage ("Search");
		return text;
	}

	@Override
	public void change (Control control) {
		((Text) control).setText ("viewport");
	}
}
