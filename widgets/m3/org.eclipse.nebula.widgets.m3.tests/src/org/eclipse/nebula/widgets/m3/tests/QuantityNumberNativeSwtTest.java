/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.m3.generated.QuantityNumberNativeSwt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.junit.Test;

/** Native SWT test: actual Spinner state and user-selection callback contract. */
public final class QuantityNumberNativeSwtTest {

    @Test
    public void boundedIntegerNumberUsesOneNativeSpinnerAndStableSelectionCallback() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                List<String> changes = new ArrayList<>();
                List<String> actions = new ArrayList<>();
                Composite content = QuantityNumberNativeSwt.create(
                        shell, SWT.NONE, actions::add,
                        (id, value) -> changes.add(id + "=" + value));
                Control[] widgets = content.getChildren();

                assertEquals(3, widgets.length);
                assertTrue(widgets[0] instanceof Label);
                assertTrue(widgets[1] instanceof Spinner);
                assertTrue(widgets[2] instanceof Button);
                assertEquals("title", widgets[0].getData("m3.web.id"));
                assertEquals("quantity", widgets[1].getData("m3.web.id"));
                assertEquals("save", widgets[2].getData("m3.web.id"));
                assertEquals("Items to ship", ((Label) widgets[0]).getText());

                Spinner spinner = (Spinner) widgets[1];
                assertEquals(0, spinner.getMinimum());
                assertEquals(500, spinner.getMaximum());
                assertEquals(12, spinner.getSelection());
                assertEquals(0, spinner.getDigits());
                assertEquals(1, spinner.getIncrement());
                assertEquals(10, spinner.getPageIncrement());
                assertTrue(changes.isEmpty());
                assertTrue(actions.isEmpty());

                spinner.setSelection(31);
                spinner.notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("quantity=31"), changes);
                assertTrue(actions.isEmpty());

                ((Button) widgets[2]).notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("save"), actions);
                assertEquals(List.of("quantity=31"), changes);
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
