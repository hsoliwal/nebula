/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Synexia contributors - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.oscilloscope.multichannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Disposing a scope releases only what it owns: the Display's shared system colours (the default background and
 * channel foregrounds) and a caller's channel colour stay usable, so a later scope can still be created.
 */
public class OscilloscopeDisposeTest {

	@Test
	public void disposeLeavesBorrowedColorsLive() {
		Display display = Display.getDefault();
		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		Color white = display.getSystemColor(SWT.COLOR_WHITE);
		Color callers = new Color(display, 10, 200, 30);
		Shell shell = new Shell(display);
		try {
			Oscilloscope scope = new Oscilloscope(2, null, shell, SWT.NONE);
			scope.setForeground(1, callers);
			scope.dispose();
			assertFalse("the system background colour belongs to the Display", black.isDisposed()); //$NON-NLS-1$
			assertFalse("the system foreground colour belongs to the Display", white.isDisposed()); //$NON-NLS-1$
			assertFalse("a channel colour set by the caller belongs to the caller", callers.isDisposed()); //$NON-NLS-1$
			Oscilloscope next = new Oscilloscope(1, null, shell, SWT.NONE);
			assertEquals(black.getRGB(), next.getBackground().getRGB());
		} finally {
			shell.dispose();
			callers.dispose();
		}
	}
}
