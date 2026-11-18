/*
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.nebula.widgets.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.junit.Test;

public class TerminalContractTest {
    @Test
    public void terminalIsAnSwtComposite() {
        assertTrue(Composite.class.isAssignableFrom(Terminal.class));
    }

    @Test
    public void constructorAcceptsEclipseTerminalConnectors() throws Exception {
        assertEquals(3, Terminal.class.getConstructor(
                Composite.class, int.class, ITerminalConnector[].class).getParameterCount());
    }
}
