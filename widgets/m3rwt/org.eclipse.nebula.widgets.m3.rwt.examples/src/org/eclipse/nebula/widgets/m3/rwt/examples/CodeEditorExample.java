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

import org.eclipse.nebula.widgets.richtext.RichTextEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/** CODE_EDITOR on RWT (rich text only): RAP's RichTextEditor port (CKEditor). */
public final class CodeEditorExample extends RwtExample {

	public CodeEditorExample () {
		super ("CODE_EDITOR", "org.eclipse.nebula.widgets.richtext.RichTextEditor (org.eclipse.rap.nebula.widgets.richtext 4.8.0)");
	}

	@Override
	public Control create (Composite parent) {
		RichTextEditor editor = new RichTextEditor (parent, SWT.BORDER);
		editor.setText ("<p><b>Rich</b> text on RWT</p>");
		return editor;
	}

	@Override
	public void change (Control control) {
		((RichTextEditor) control).setText ("<p>Changed</p>");
	}
}
