/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.swt.tests.mobile;

import java.util.ArrayList;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridCanvasReceiver;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.mobile.MobileRuntime;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Real SWT Java owner-contract exercise with the existing headless native
 * transport. This is NOT an Android/iOS device or native screenshot test.
 */
public final class MobileGridCanvasReceiverTest {
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
    private static void pump(Display display) {
        for (int i = 0; i < 100 && display.readAndDispatch(); i++) { }
        display.update();
    }
    private static void pointer(Canvas canvas, int eventType, int y) {
        Event event = new Event();
        event.button = 1;
        event.x = 10;
        event.y = y;
        canvas.notifyListeners(eventType, event);
    }

    public static void main(String[] args) {
        ContractPlatform backend = new ContractPlatform();
        MobileRuntime.install(backend);
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            shell.setBounds(0, 0, 320, 120);
            Canvas canvas = new Canvas(shell, SWT.NONE);
            canvas.setBounds(0, 0, 320, 120);
            ArrayList<Long> selections = new ArrayList<>();
            MobileGridCanvasReceiver receiver = new MobileGridCanvasReceiver(
                    canvas, 1_000_000_000_000L, 2, 24, 2,
                    (row, column) -> "row " + row + " col " + column,
                    selections::add);
            shell.open();
            pump(display);
            check(receiver.window().visibleStart() == 0, "initial viewport");
            check(receiver.window().nativeSlots() <= 9, "bounded native row slots");
            check(receiver.rowAt(25) == 1, "initial row hit test");
            pointer(canvas, SWT.MouseDown, 25);
            pointer(canvas, SWT.MouseUp, 25);
            check(selections.equals(java.util.List.of(1L)) && receiver.selectedRow() == 1, "selection");
            receiver.scrollTo(12);
            pump(display);
            check(receiver.rowAt(12) == 1, "fractional scroll hit test");
            pointer(canvas, SWT.MouseDown, 100);
            pointer(canvas, SWT.MouseMove, 80);
            pointer(canvas, SWT.MouseUp, 80);
            check(receiver.scrollY() == 32 && selections.size() == 1, "drag scroll without click");
            receiver.scrollTo(Long.MAX_VALUE);
            pump(display);
            check(receiver.window().visibleStart() == 999_999_999_999L, "long-index jump");
            check(receiver.window().nativeSlots() <= 9, "long-index bounded cache");
            receiver.setItemCount(0);
            pump(display);
            check(receiver.rowAt(20) == -1 && receiver.window().visibleEnd() == 0, "zero-row reset");
            canvas.dispose();
            check(receiver.isClosed(), "canvas owns receiver disposal");
            receiver.close(); // idempotent
            shell.dispose();
        } finally {
            display.dispose();
            MobileRuntime.uninstall(backend);
        }
        check(backend.controls.isEmpty() && backend.images.isEmpty(), "no native owner leak");
        System.out.println("MobileGridCanvasReceiverTest PASSED");
    }
}
