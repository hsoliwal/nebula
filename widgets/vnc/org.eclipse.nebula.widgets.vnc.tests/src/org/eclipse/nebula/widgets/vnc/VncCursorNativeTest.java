/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Real SWT native cursor creation and direct/parent disposal ownership. */
public final class VncCursorNativeTest {
    @Test public void cursorReplacementAndParentDisposalReleaseNativeHandles() throws Exception {
        Display display = Display.getDefault();
        assertSame("Must execute in SWT UI-harness thread",
                display.getThread(), Thread.currentThread());
        Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
        shell.setLayout(new FillLayout());
        shell.setSize(200, 180);
        VncCanvas canvas = new VncCanvas(shell, SWT.NONE);
        Cursor first = null;
        Cursor next = null;
        try {
            shell.open();
            RfbCursorDecoder.Shape decoded = RfbCursorDecoder.readX(
                    new DataInputStream(new ByteArrayInputStream(new byte[] {
                            (byte) 255, 0, 0, 0, 0, (byte) 255,
                            (byte) 0x80, (byte) 0xc0
                    })), 1, 0, 2, 1);
            RfbClient.CursorFrame shape = new RfbClient.CursorFrame(decoded);
            canvas.installCursor(shape);
            first = canvas.getCursor();
            assertNotNull(first);
            assertFalse(first.isDisposed());
            canvas.installCursor(shape);
            next = canvas.getCursor();
            assertNotSame(first, next);
            assertTrue("Old native Cursor leaked after replacement", first.isDisposed());
            assertFalse(next.isDisposed());
            canvas.installCursor(null);
            assertNull(canvas.getCursor());
            assertTrue("Native Cursor leaked on reset", next.isDisposed());
            canvas.installCursor(shape);
            Cursor onDispose = canvas.getCursor();
            assertFalse(onDispose.isDisposed());
            shell.dispose(); // SWT parent-driven child disposal
            assertTrue(canvas.isDisposed());
            assertTrue("Native cursor leaked on parent shell disposal", onDispose.isDisposed());
        } finally {
            if (!shell.isDisposed()) {
                shell.dispose();
            }
        }
    }
}
