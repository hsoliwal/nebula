// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Shell;

/** One shared demo body for the Android and iOS SWT hosts. */
public final class NebulaMobileContents {
    private NebulaMobileContents() {}

    public static final class State {
        public volatile int attempts;
        public volatile boolean selected;
        public volatile boolean disposed;
    }

    public static State create(Shell shell) {
        State state = new State();
        shell.addListener(SWT.Dispose, event -> state.disposed = true);
        Canvas labels = new Canvas(shell, SWT.DOUBLE_BUFFERED);
        RoundedCheckbox selectable = new RoundedCheckbox(shell, SWT.NONE);
        RoundedCheckbox vetoed = new RoundedCheckbox(shell, SWT.NONE);
        RoundedCheckbox disabled = new RoundedCheckbox(shell, SWT.NONE);
        vetoed.setSelection(true);
        vetoed.setGrayed(true);
        disabled.setSelection(true);
        disabled.setEnabled(false);

        selectable.addListener(SWT.Selection, event -> {
            state.attempts++;
            shell.getDisplay().asyncExec(() -> {
                if (!labels.isDisposed()) {
                    state.selected = selectable.getSelection();
                    labels.redraw();
                }
            });
        });
        vetoed.addListener(SWT.Selection, event -> {
            event.doit = false;
            state.attempts++;
            labels.redraw();
        });
        labels.addListener(SWT.Paint, event -> {
            Rectangle area = labels.getClientArea();
            event.gc.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            event.gc.fillRectangle(area);
            event.gc.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_BLACK));
            event.gc.drawText("Nebula on SWT mobile", 16, 16, true);
            event.gc.drawText("Toggle", 80, 76, true);
            event.gc.drawText("Selection veto", 80, 148, true);
            event.gc.drawText("Disabled", 80, 220, true);
            event.gc.drawText("Selection: " + selectable.getSelection(), 16, 292, true);
            event.gc.drawText("Attempts: " + state.attempts, 16, 324, true);
        });
        Runnable layout = () -> {
            Rectangle area = shell.getClientArea();
            labels.setBounds(0, 0, area.width, area.height);
            selectable.setBounds(20, 64, 44, 44);
            vetoed.setBounds(20, 136, 44, 44);
            disabled.setBounds(20, 208, 44, 44);
        };
        shell.addListener(SWT.Resize, event -> layout.run());
        layout.run();
        return state;
    }
}
