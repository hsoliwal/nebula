// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.m3.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;

/** Actual SWT ownership and static UI semantic tests. Requires an X11 display. */
final class OrdersNativeFormTest {
    @Test
    void fourControlsPreserveTextDisabledStateAndParents() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                Composite form = OrdersNativeForm.create(shell);
                assertSame(shell, form.getParent());
                Control[] children = form.getChildren();
                assertEquals(4, children.length);
                assertTrue(children[0] instanceof Label);
                assertTrue(children[1] instanceof Label);
                assertTrue(children[2] instanceof Button);
                assertTrue(children[3] instanceof Button);
                assertEquals("Orders && shipping", ((Label) children[0]).getText());
                assertEquals("Orders & shipping", children[0].getData("m3.text"));
                assertEquals("A browser && native viewport candidate",
                        ((Label) children[1]).getText());
                assertEquals("A browser & native viewport candidate", children[1].getData("m3.text"));
                assertEquals("Refresh orders", ((Button) children[2]).getText());
                assertEquals("Cancel", ((Button) children[3]).getText());
                assertTrue(children[2].getEnabled());
                assertFalse(children[3].getEnabled());
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }

    @Test
    void separateFormsAndShellDisposalPreserveControlOwnership() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            Control a;
            Control b;
            try {
                Composite first = OrdersNativeForm.create(shell);
                Composite second = OrdersNativeForm.create(shell);
                assertEquals(4, first.getChildren().length);
                assertEquals(4, second.getChildren().length);
                a = first.getChildren()[0];
                b = second.getChildren()[0];
                assertSame(first, a.getParent());
                assertSame(second, b.getParent());
                assertNotSame(a, b);
            } finally {
                shell.dispose();
            }
            assertTrue(a.isDisposed());
            assertTrue(b.isDisposed());
        } finally {
            display.dispose();
        }
    }
}
