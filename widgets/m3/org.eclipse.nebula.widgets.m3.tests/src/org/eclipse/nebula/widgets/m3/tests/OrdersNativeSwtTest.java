/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.m3.generated.OrdersNativeSwt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Actual SWT display-thread integration test, not a headless signature stub.
 * Qualify separately under Win32, GTK/Xvfb and Cocoa in the native UI lane.
 */
public class OrdersNativeSwtTest {

    @Test
    public void generatesNativeControlsWithStableIdsAndSelectionContracts() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                List<String> actions = new ArrayList<>();
                Composite content = OrdersNativeSwt.create(shell, SWT.NONE, actions::add);
                Control[] children = content.getChildren();
                assertEquals(4, children.length);
                assertEquals("title", children[0].getData("m3.web.id"));
                assertEquals("description", children[1].getData("m3.web.id"));
                assertEquals("refresh", children[2].getData("m3.web.id"));
                assertEquals("cancel", children[3].getData("m3.web.id"));
                assertTrue(children[0] instanceof Label);
                assertTrue(children[1] instanceof Label);
                assertTrue(children[2] instanceof Button);
                assertTrue(children[3] instanceof Button);
                assertEquals("Orders && shipping", ((Label) children[0]).getText());
                assertEquals("A browser && native viewport candidate", ((Label) children[1]).getText());
                assertFalse(((Button) children[3]).isEnabled());
                ((Button) children[2]).notifyListeners(SWT.Selection, new Event());
                ((Button) children[3]).notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("refresh"), actions);
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
