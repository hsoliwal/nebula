// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.m3.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

/** Generated semantic-id and parent-disposal receiving test for the typed UiBinder form. */
final class OrdersNativeFormGeneratedTest {
    @Test
    void preservesTypedScreenTitleIdsAndButtonState() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Composite form = OrdersNativeForm.create(shell);
                assertSame(shell, form.getParent());
                assertEquals("UiBinder screen", form.getData("m3.title"));
                Control[] controls = form.getChildren();
                assertEquals(4, controls.length);
                assertEquals("title", controls[0].getData("m3.id"));
                assertEquals("description", controls[1].getData("m3.id"));
                assertEquals("refresh", controls[2].getData("m3.id"));
                assertEquals("cancel", controls[3].getData("m3.id"));
                assertTrue(controls[2] instanceof Button);
                assertTrue(controls[3] instanceof Button);
                assertTrue(controls[2].getEnabled());
                assertFalse(controls[3].getEnabled());
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
