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
import org.eclipse.swt.widgets.Link;

/** HYPERLINK on RWT: Link. */
public final class HyperlinkExample extends RwtExample {

	public HyperlinkExample () {
		super ("HYPERLINK", "org.eclipse.swt.widgets.Link");
	}

	@Override
	public Control create (Composite parent) {
		Link link = new Link (parent, SWT.NONE);
		link.setText ("Read the <a href=\"https://eclipse.dev/rap/\">RAP documentation</a> or the <a>release notes</a>.");
		return link;
	}

	@Override
	public void change (Control control) {
		((Link) control).setText ("<a>Changed link " + next () + "</a>");
	}
}
