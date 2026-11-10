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

import java.lang.reflect.Field;

import org.eclipse.nebula.widgets.richtext.RichTextEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Every-widget check of the embedded RichTextEditor: its Browser lives in a child shell of the window, no descendant of
 * the editor. Disposing an ancestor composite of the editor (a closed view) disposes that shell and its Browser too;
 * before, they stayed alive until the window closed. An explicit dispose() still disposes it.
 */
public class RichTextEmbeddedDisposeCheck {

	public static void main(String[] args) throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(500, 300);
		Composite view = new Composite(shell, SWT.NONE);
		view.setLayout(new FillLayout());
		RichTextEditor editor = new RichTextEditor(view, SWT.EMBEDDED);
		shell.open();
		spin(display, 500);
		Shell embedded = embeddedShell(editor);
		check(!embedded.isDisposed(), "the embedded shell is alive");

		view.dispose();
		spin(display, 200);
		check(editor.isDisposed(), "the editor is disposed with its ancestor");
		check(embedded.isDisposed(), "the embedded shell is disposed with an ancestor of the editor");

		RichTextEditor second = new RichTextEditor(shell, SWT.EMBEDDED);
		Shell secondShell = embeddedShell(second);
		second.dispose();
		check(secondShell.isDisposed(), "an explicit dispose() disposes the embedded shell");

		shell.dispose();
		display.dispose();
		System.out.println("PASSED");
	}

	static Shell embeddedShell(RichTextEditor editor) throws Exception {
		Field field = RichTextEditor.class.getDeclaredField("embeddedShell");
		field.setAccessible(true);
		return (Shell) field.get(editor);
	}

	static void spin(Display display, long millis) throws InterruptedException {
		long end = System.currentTimeMillis() + millis;
		while (System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				Thread.sleep(5);
			}
		}
	}

	static void check(boolean condition, String message) {
		System.out.println((condition ? "ok   " : "FAIL ") + message);
		if (!condition) {
			throw new IllegalStateException(message);
		}
	}
}
