/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.m3.generated.PreferencesNativeSwt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/**
 * Original SWT display-backed acceptance. Run separately on Win32/GTK/Cocoa.
 * RWT 4.8 web-session/API conformance remains a separate unrun gate.
 */
public class PreferencesNativeSwtTest {

    @Test
    public void selectCreatesOneNativeComboWithDistinctSourceValues() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                List<String> actions = new ArrayList<>();
                List<String> changes = new ArrayList<>();
                Composite form = PreferencesNativeSwt.create(shell, SWT.NONE,
                        actions::add, (id, value) -> changes.add(id + "=" + value));
                Control[] controls = form.getChildren();
                assertEquals(3, controls.length);
                assertTrue(controls[0] instanceof Label);
                assertTrue(controls[1] instanceof Combo);
                assertTrue(controls[2] instanceof Button);
                assertEquals("heading", controls[0].getData("m3.web.id"));
                assertEquals("method", controls[1].getData("m3.web.id"));
                assertEquals("save", controls[2].getData("m3.web.id"));
                assertEquals("Delivery && preferences", ((Label) controls[0]).getText());

                Combo combo = (Combo) controls[1];
                assertArrayEquals(new String[] {"Express & priority", "Economy", "None"},
                        combo.getItems());
                assertEquals(1, combo.getSelectionIndex());
                assertTrue(changes.isEmpty());

                combo.select(0);
                combo.notifyListeners(SWT.Selection, new Event());
                combo.select(1);
                combo.notifyListeners(SWT.Selection, new Event());
                combo.select(2);
                combo.notifyListeners(SWT.Selection, new Event());
                ((Button) controls[2]).notifyListeners(SWT.Selection, new Event());

                assertEquals(List.of("method=overnight", "method=economy", "method="), changes);
                assertEquals(List.of("save"), actions);
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
