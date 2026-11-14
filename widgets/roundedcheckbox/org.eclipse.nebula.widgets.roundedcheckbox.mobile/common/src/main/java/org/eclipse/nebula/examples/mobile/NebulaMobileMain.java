// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Ordinary SWT entrypoint shared by Android and iOS.
 * Android SwtActivity reads this class from Activity metadata; the iOS
 * IOSPlatform.launch bootstrap invokes its public static main(String[]).
 */
public final class NebulaMobileMain {
    public static volatile NebulaMobileContents.State state;

    private NebulaMobileMain() { }

    public static void main(String[] args) {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            shell.setText("Nebula Mobile");
            Rectangle area = display.getClientArea();
            shell.setBounds(area.x, area.y, area.width, area.height);
            state = NebulaMobileContents.create(shell);
            shell.open();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) display.sleep();
            }
        } finally {
            if (!display.isDisposed()) display.dispose();
        }
    }
}
