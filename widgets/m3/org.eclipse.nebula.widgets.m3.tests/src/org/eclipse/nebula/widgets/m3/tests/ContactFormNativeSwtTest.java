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

import org.eclipse.nebula.widgets.m3.generated.ContactFormNativeSwt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.Test;

/**
 * Native-screen acceptance test. Run under Win32/GTK/Cocoa with a display;
 * this is not a headless contract stub or proof of RAP browser execution.
 */
public class ContactFormNativeSwtTest {

    @Test
    public void directNativeFormRetainsTypedControlsAndEventValues() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                List<String> actions = new ArrayList<>();
                List<String> changes = new ArrayList<>();
                Composite form = ContactFormNativeSwt.create(shell, SWT.NONE,
                        actions::add, (id, value) -> changes.add(id + "=" + value));
                Control[] controls = form.getChildren();
                assertEquals(5, controls.length);
                assertEquals("title", controls[0].getData("m3.web.id"));
                assertEquals("query", controls[1].getData("m3.web.id"));
                assertEquals("notify", controls[2].getData("m3.web.id"));
                assertEquals("refresh", controls[3].getData("m3.web.id"));
                assertEquals("cancel", controls[4].getData("m3.web.id"));
                assertTrue(controls[0] instanceof Label);
                assertTrue(controls[1] instanceof Text);
                assertTrue(controls[2] instanceof Button);
                assertTrue(controls[3] instanceof Button);
                assertTrue(controls[4] instanceof Button);
                assertEquals("Orders && shipping", ((Label) controls[0]).getText());
                assertEquals("Blue & green", ((Text) controls[1]).getText());
                assertEquals("Notify && alert", ((Button) controls[2]).getText());
                assertTrue(((Button) controls[2]).getSelection());
                assertFalse(((Button) controls[4]).getEnabled());
                assertEquals(List.of(), changes);

                ((Text) controls[1]).setText("New value");
                ((Button) controls[2]).setSelection(false);
                ((Button) controls[2]).notifyListeners(SWT.Selection, new Event());
                ((Button) controls[3]).notifyListeners(SWT.Selection, new Event());
                ((Button) controls[4]).notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("query=New value", "notify=false"), changes);
                assertEquals(List.of("refresh"), actions);
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
