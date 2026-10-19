// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.roundedcheckbox;

import java.lang.reflect.Field;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/** Optional actual GTK/SWT Canvas proof; not a substitute for Win32/Cocoa parity. */
public final class RoundedCheckboxNativePaintSmoke {
    private static final class Reentrant extends RoundedCheckbox {
        boolean armed;
        boolean inside;
        int nestedCallbacks;

        Reentrant(Composite parent) { super(parent, SWT.NONE); }

        @Override public Rectangle getClientArea() {
            if (armed && !inside) {
                inside = true;
                armed = false;
                GC nested = new GC(this);
                try {
                    Event callback = new Event();
                    callback.gc = nested;
                    notifyListeners(SWT.Paint, callback);
                    nestedCallbacks++;
                } finally {
                    nested.dispose();
                    inside = false;
                }
            }
            return super.getClientArea();
        }
    }

    public static void main(String[] args) throws Exception {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            shell.setLayout(new FillLayout());
            Reentrant checkbox = new Reentrant(shell);
            shell.setSize(160, 130);
            shell.open();
            GC outer = new GC(checkbox);
            try {
                checkbox.armed = true;
                Event paint = new Event();
                paint.gc = outer;
                checkbox.notifyListeners(SWT.Paint, paint);
                Field field = RoundedCheckbox.class.getDeclaredField("gc");
                field.setAccessible(true);
                if (field.get(checkbox) != outer || checkbox.nestedCallbacks != 1) {
                    throw new AssertionError("original Canvas failed to restore nested borrowed GC");
                }
            } finally {
                outer.dispose();
                shell.dispose();
            }
            System.out.println("ROUNDED_CHECKBOX_NATIVE_SWT_NESTED_PAINT_PASS");
        } finally {
            display.dispose();
        }
    }
}
