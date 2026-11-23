/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.terminal;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.junit.Test;

/**
 * Native GTK/SWT embedding and terminal resource lifecycle.
 * Tests must run in Tycho's SWT UI harness, never JUnit's timeout thread.
 */
public final class TerminalWidgetNativeTest {
    @Test public void parentShellDisposalAlsoClosesTerminalClipboard() throws Exception {
        runWidgetLifecycle(true);
    }

    @Test public void directWidgetDisposalAlsoClosesTerminalClipboard() throws Exception {
        runWidgetLifecycle(false);
    }

    private static void runWidgetLifecycle(boolean disposeParent) throws Exception {
        Display display = Display.getDefault();
        assertSame("Terminal control requires the SWT UI thread",
                display.getThread(), Thread.currentThread());
        Shell shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
        shell.setLayout(new FillLayout());
        shell.setSize(500, 300);
        TerminalWidget widget = new TerminalWidget(shell, SWT.NONE);
        ITerminalViewControl terminal = widget.getTerminalControl();
        Clipboard clipboard = terminal.getClipboard();
        assertNotNull(terminal.getControl());
        assertNotNull(terminal.getRootControl());
        assertNotNull(clipboard);
        assertFalse(clipboard.isDisposed());
        assertFalse(terminal.isConnected());
        assertEquals(0, terminal.getConnectors().length);
        widget.setCharset(StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, terminal.getCharset());
        widget.setScrollbackLimit(42);
        assertEquals(42, terminal.getBufferLineLimit());
        assertFalse("An unconnected terminal must not write input",
                widget.paste("not a remote command"));

        try {
            shell.open();
            shell.forceActive();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
            while (System.nanoTime() < deadline && widget.getSize().x == 0) {
                while (display.readAndDispatch()) {
                    // Normal SWT UI event loop.
                }
                Thread.sleep(10);
            }
            assertTrue("Native terminal composite has zero width",
                    widget.getSize().x > 0);
            assertTrue("Native terminal text control has zero width",
                    terminal.getControl().getSize().x > 0);
            if (disposeParent) {
                shell.dispose();
            } else {
                widget.dispose();
            }
            assertTrue(widget.isDisposed());
            assertTrue("Terminal Clipboard leaked when SWT child was disposed",
                    clipboard.isDisposed());
        } finally {
            if (!shell.isDisposed()) {
                shell.dispose();
            }
        }
    }
}
