/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.terminal;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.junit.Test;

/** Real SWT lifecycle acceptance; the Eclipse Terminal stays the sole emulator. */
public final class TerminalNativeLifecycleTest {
    @Test
    public void embeddedTerminalOwnsOriginalControlAndDisposesWithParent() {
        Display display = Display.getCurrent();
        boolean ownsDisplay = display == null;
        if (ownsDisplay) display = new Display();
        Shell shell = null;
        try {
            shell = new Shell(display, SWT.ON_TOP | SWT.SHELL_TRIM);
            shell.setLayout(new FillLayout());
            shell.setSize(480, 240);
            Terminal terminal = new Terminal(shell, SWT.NONE);
            ITerminalViewControl original = terminal.getTerminalControl();
            assertNotNull(original);
            assertNotNull(original.getRootControl());
            assertNotNull(original.getControl());
            assertFalse(original.isDisposed());
            assertFalse(terminal.isConnected());
            shell.open();
            for (int i = 0; i < 10; i++) {
                if (display.readAndDispatch()) { display.update(); }
                display.update();
            }
            terminal.setCharset(StandardCharsets.UTF_8);
            assertEquals(StandardCharsets.UTF_8, original.getCharset());
            terminal.setBufferLineLimit(2048);
            assertEquals(2048, original.getBufferLineLimit());
            terminal.setScrollLock(true);
            assertTrue(original.isScrollLock());
            terminal.setScrollLock(false);
            assertFalse(original.isScrollLock());
            assertFalse(terminal.paste("not connected"));
            terminal.clear();
            shell.dispose();
            assertTrue(original.isDisposed());
        } finally {
            if (shell != null && !shell.isDisposed()) shell.dispose();
            if (ownsDisplay && display != null && !display.isDisposed()) display.dispose();
        }
    }
}
