// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pshelf;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/** Optional real SWT GTK smoke; no fake widget, renderer or native Java stubs. */
public final class PShelfNativeSmoke {
    public static void main(String[] arguments) {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            shell.setText("Nebula PShelf intrinsic geometry proof");
            shell.setLayout(new FillLayout());
            PShelf widget = new PShelf(shell, SWT.BORDER | SWT.SIMPLE);
            PShelfItem[] children = new PShelfItem[8];
            for (int i = 0; i < children.length; i++) {
                children[i] = new PShelfItem(widget, SWT.NONE);
                children[i].setText("Original PShelfItem " + i);
            }
            shell.setSize(550, 650);
            shell.open();
            for (int i = 0; i < children.length; i++) {
                widget.setSelection(children[i]);
                if (widget.getSelection() != children[i]) {
                    throw new AssertionError("selection mismatch after intrinsic layout");
                }
                widget.redraw();
                display.readAndDispatch();
            }
            display.timerExec(300, () -> {
                if (!shell.isDisposed()) shell.dispose();
            });
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) display.sleep();
            }
            System.out.println("PSHELF_NATIVE_SWT_SMOKE_PASS");
        } finally {
            display.dispose();
        }
    }
}
