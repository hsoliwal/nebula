// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Device-only recovery contracts for the existing SWT Android owner. Events
 * enter through Android View dispatch and pixels come from Canvas/UiAutomation.
 * Compilation alone is not evidence that these assertions have executed.
 */
final class SwtMobileRecoveryContracts {
    private SwtMobileRecoveryContracts() { }

    static void verify(Instrumentation host, Activity activity, Display display) throws Exception {
        AtomicReference<Shell> shell = new AtomicReference<>();
        AtomicReference<Canvas> canvas = new AtomicReference<>();
        AtomicReference<int[]> down = new AtomicReference<>();
        AtomicReference<int[]> up = new AtomicReference<>();
        AtomicReference<int[]> keyDown = new AtomicReference<>();
        AtomicReference<int[]> keyUp = new AtomicReference<>();
        AtomicReference<int[]> move = new AtomicReference<>();
        AtomicInteger ups = new AtomicInteger();
        AtomicInteger enters = new AtomicInteger();
        AtomicInteger exits = new AtomicInteger();
        AtomicInteger focusOuts = new AtomicInteger();
        try {
            display.syncExec(() -> {
                Shell owner = new Shell(display);
                shell.set(owner);
                owner.setBounds(display.getClientArea());
                Composite parent = new Composite(owner, SWT.BORDER);
                parent.setBounds(10, 12, 164, 144);
                Canvas child = new Canvas(parent, SWT.BORDER | SWT.DOUBLE_BUFFERED);
                canvas.set(child);
                child.setBounds(7, 9, 140, 120);
                child.addListener(SWT.Paint, event -> {
                    GC gc = event.gc;
                    gc.setAntialias(SWT.OFF);
                    gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
                    gc.fillRectangle(child.getClientArea());
                    gc.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
                    gc.fillOval(8, 8, 32, 24);
                    gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
                    gc.fillArc(52, 8, 40, 40, 0, 90);
                    gc.setBackground(display.getSystemColor(SWT.COLOR_GREEN));
                    gc.fillRoundRectangle(8, 52, 40, 32, 12, 12);
                    gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
                    gc.drawArc(96, 8, 32, 32, 0, 90);
                    gc.drawRoundRectangle(60, 52, 40, 32, 12, 12);
                });
                child.addListener(SWT.MouseDown, event -> down.set(new int[] {event.x, event.y, event.stateMask}));
                child.addListener(SWT.MouseUp, event -> {
                    up.set(new int[] {event.x, event.y, event.stateMask});
                    ups.incrementAndGet();
                });
                child.addListener(SWT.MouseMove, event -> move.set(new int[] {event.x, event.y, event.stateMask}));
                child.addListener(SWT.MouseEnter, event -> enters.incrementAndGet());
                child.addListener(SWT.MouseExit, event -> exits.incrementAndGet());
                child.addListener(SWT.FocusOut, event -> focusOuts.incrementAndGet());
                child.addListener(SWT.KeyDown, event -> keyDown.set(new int[] {event.keyCode, event.stateMask, event.character}));
                child.addListener(SWT.KeyUp, event -> keyUp.set(new int[] {event.keyCode, event.stateMask, event.character}));
                owner.open();
                require(child.setFocus(), "Recovery Canvas receives SWT focus");
            });

            AtomicReference<View> nativeCanvas = new AtomicReference<>();
            host.runOnMainSync(() -> {
                ViewGroup content = activity.findViewById(android.R.id.content);
                ViewGroup root = (ViewGroup) content.getChildAt(0);
                ViewGroup nativeShell = (ViewGroup) root.getChildAt(root.getChildCount() - 1);
                ViewGroup nativeParent = (ViewGroup) nativeShell.getChildAt(0);
                nativeCanvas.set(nativeParent.getChildAt(0));
            });
            View peer = nativeCanvas.get();
            await(() -> peer.getWidth() > 0 && peer.getHeight() > 0, "Bordered native Canvas layout");
            int[] blue = location(host, peer, 24, 20);
            int[] sector = location(host, peer, 80, 20);
            int[] emptySector = location(host, peer, 64, 36);
            int[] roundedCenter = location(host, peer, 28, 68);
            int[] roundedCorner = location(host, peer, 8, 52);
            await(() -> pixelMatches(host, blue, 0x0000ff), "Actual Canvas oval pixels through both borders");
            await(() -> pixelMatches(host, sector, 0xff0000), "Positive native arc sweep fills the upper-right sector");
            require(pixelMatches(host, emptySector, 0xffffff), "Native arc must not fill the opposite sector");
            require(pixelMatches(host, roundedCenter, 0x00ff00), "Native rounded rectangle fills its center");
            require(pixelMatches(host, roundedCorner, 0xffffff), "Native rounded rectangle leaves its corner empty");

            long touchTime = SystemClock.uptimeMillis();
            MotionEvent first = MotionEvent.obtain(touchTime, touchTime, MotionEvent.ACTION_DOWN, blue[0], blue[1], 0);
            MotionEvent last = MotionEvent.obtain(touchTime, touchTime + 20, MotionEvent.ACTION_UP, blue[0], blue[1], 0);
            try { host.sendPointerSync(first); host.sendPointerSync(last); }
            finally { first.recycle(); last.recycle(); }
            await(() -> ups.get() == 1, "Android touch reaches the original SWT MouseUp listener once");
            require(down.get()[0] == 24 && down.get()[1] == 20
                    && up.get()[0] == 24 && up.get()[1] == 20,
                    "Native input uses the same client origin as bordered Canvas painting");
            require((down.get()[2] & SWT.BUTTON1) == 0, "MouseDown excludes the newly pressed button");
            require((up.get()[2] & SWT.BUTTON1) != 0, "MouseUp includes the just-released button");

            int[][] modifiers = {
                {KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON, SWT.SHIFT},
                {KeyEvent.KEYCODE_SHIFT_RIGHT, KeyEvent.META_SHIFT_ON, SWT.SHIFT},
                {KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON, SWT.CTRL},
                {KeyEvent.KEYCODE_CTRL_RIGHT, KeyEvent.META_CTRL_ON, SWT.CTRL},
                {KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON, SWT.ALT},
                {KeyEvent.KEYCODE_ALT_RIGHT, KeyEvent.META_ALT_ON, SWT.ALT},
                {KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_ON, SWT.COMMAND},
                {KeyEvent.KEYCODE_META_RIGHT, KeyEvent.META_META_ON, SWT.COMMAND}
            };
            for (int[] modifier : modifiers) {
                keyDown.set(null); keyUp.set(null);
                int otherNative = modifier[2] == SWT.CTRL ? KeyEvent.META_SHIFT_ON : KeyEvent.META_CTRL_ON;
                int otherSwt = modifier[2] == SWT.CTRL ? SWT.SHIFT : SWT.CTRL;
                host.runOnMainSync(() -> {
                    require(peer.requestFocus(), "Android peer receives modifier-key focus");
                    long time = SystemClock.uptimeMillis();
                    require(peer.dispatchKeyEvent(new KeyEvent(time, time, KeyEvent.ACTION_DOWN,
                            modifier[0], 0, modifier[1] | otherNative)), "Android modifier KeyDown handled");
                    require(peer.dispatchKeyEvent(new KeyEvent(time, time + 1, KeyEvent.ACTION_UP,
                            modifier[0], 0, otherNative)), "Android modifier KeyUp handled");
                });
                display.syncExec(() -> { });
                require(keyDown.get() != null && keyUp.get() != null, "Both native modifier transitions reached SWT");
                require(keyDown.get()[0] == modifier[2] && keyUp.get()[0] == modifier[2]
                        && keyDown.get()[2] == 0 && keyUp.get()[2] == 0,
                        "Left and right native modifiers map to SWT modifier key codes without characters");
                require((keyDown.get()[1] & modifier[2]) == 0
                        && (keyUp.get()[1] & modifier[2]) != 0
                        && (keyDown.get()[1] & otherSwt) != 0
                        && (keyUp.get()[1] & otherSwt) != 0,
                        "Modifier events preserve other held keys and expose the pre-transition mask");
            }

            int beforeUps = ups.get(), beforeExits = exits.get();
            host.runOnMainSync(() -> {
                long time = SystemClock.uptimeMillis();
                dispatchTouch(peer, time, MotionEvent.ACTION_DOWN, new int[] {7});
                dispatchTouch(peer, time, MotionEvent.ACTION_POINTER_DOWN
                        | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), new int[] {7, 9});
                dispatchTouch(peer, time, MotionEvent.ACTION_POINTER_UP, new int[] {7, 9});
                dispatchTouch(peer, time, MotionEvent.ACTION_UP, new int[] {9});
            });
            display.syncExec(() -> { });
            require(exits.get() == beforeExits + 1 && ups.get() == beforeUps,
                    "Removing the primary contact cancels capture without fabricating MouseUp or Selection");

            move.set(null);
            int beforeEnters = enters.get();
            host.runOnMainSync(() -> {
                MotionEvent enter = pointerEvent(peer, SystemClock.uptimeMillis(), MotionEvent.ACTION_HOVER_ENTER,
                        new int[] {3}, KeyEvent.META_SHIFT_ON,
                        MotionEvent.BUTTON_PRIMARY | MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_SECONDARY,
                        InputDevice.SOURCE_MOUSE);
                MotionEvent hover = pointerEvent(peer, SystemClock.uptimeMillis(), MotionEvent.ACTION_HOVER_MOVE,
                        new int[] {3}, KeyEvent.META_SHIFT_ON,
                        MotionEvent.BUTTON_PRIMARY | MotionEvent.BUTTON_TERTIARY | MotionEvent.BUTTON_SECONDARY,
                        InputDevice.SOURCE_MOUSE);
                try {
                    require(peer.dispatchGenericMotionEvent(enter), "Native mouse enter delivered");
                    require(peer.dispatchGenericMotionEvent(hover), "Native mouse hover delivered");
                } finally { enter.recycle(); hover.recycle(); }
            });
            display.syncExec(() -> { });
            int held = SWT.BUTTON1 | SWT.BUTTON2 | SWT.BUTTON3 | SWT.SHIFT;
            require(move.get() != null && (move.get()[2] & held) == held,
                    "Native hover preserves primary, middle, secondary, and keyboard state");
            for (float client : new float[] {0.25f, 0.45f, 0.75f, 1.0f}) {
                move.set(null);
                host.runOnMainSync(() -> {
                    MotionEvent hover = pointerEvent(peer, SystemClock.uptimeMillis(), MotionEvent.ACTION_HOVER_MOVE,
                            new int[] {3}, 0, 0, InputDevice.SOURCE_MOUSE);
                    float density = peer.getResources().getDisplayMetrics().density;
                    hover.setLocation(peer.getPaddingLeft() + client * density,
                            peer.getPaddingTop() + client * density);
                    try { require(peer.dispatchGenericMotionEvent(hover), "Fractional native hover delivered"); }
                    finally { hover.recycle(); }
                });
                display.syncExec(() -> { });
                int expected = client < 0.5f ? 0 : 1;
                require(move.get() != null && move.get()[0] == expected && move.get()[1] == expected,
                        "Native hover rounds from the actual client inset without double rounding");
            }
            require(enters.get() == beforeEnters + 1,
                    "A native hover entry followed by repeated moves emits MouseEnter exactly once");

            verifyNativeFocusRedirection(display, canvas.get());
            display.syncExec(() -> {
                verifyImageGraphics(display);
                require(canvas.get().setFocus(), "Canvas can refocus before ancestor disable");
            });
            display.syncExec(() -> { });
            int previousFocusOuts = focusOuts.get();
            display.syncExec(() -> canvas.get().getParent().setEnabled(false));
            display.syncExec(() -> {
                require(display.getFocusControl() == null && focusOuts.get() == previousFocusOuts + 1,
                        "Disabling an ancestor clears native-backed SWT focus exactly once");
            });
            saveScreen(host, "nebula-android-recovery.png");
        } finally {
            if (!display.isDisposed()) display.syncExec(() -> {
                Shell owner = shell.get();
                if (owner != null && !owner.isDisposed()) owner.dispose();
            });
        }
    }

    private static void verifyNativeFocusRedirection(Display display, Canvas original) {
        AtomicReference<Canvas> requested = new AtomicReference<>();
        AtomicReference<Canvas> redirected = new AtomicReference<>();
        AtomicReference<org.eclipse.swt.widgets.Listener> redirect = new AtomicReference<>();
        AtomicInteger staleFocusIns = new AtomicInteger();
        AtomicInteger redirectedFocusIns = new AtomicInteger();
        try {
            display.syncExec(() -> {
                Canvas first = new Canvas(original.getParent(), SWT.NONE);
                requested.set(first);
                first.setBounds(88, 86, 20, 20);
                Canvas second = new Canvas(original.getParent(), SWT.NONE);
                redirected.set(second);
                second.setBounds(112, 86, 20, 20);
                first.addListener(SWT.FocusIn, event -> staleFocusIns.incrementAndGet());
                second.addListener(SWT.FocusIn, event -> redirectedFocusIns.incrementAndGet());
                require(original.setFocus(), "Original Canvas owns focus before native redirection");
                org.eclipse.swt.widgets.Listener listener = event -> second.setFocus();
                redirect.set(listener);
                original.addListener(SWT.FocusOut, listener);
                require(!first.setFocus() && display.getFocusControl() == second,
                        "A FocusOut listener redirects the synchronous request to the final SWT owner");
            });
            // Native requestFocus acknowledgements were queued by real Android
            // View callbacks during the preceding owner-thread operation.
            display.syncExec(() -> { });
            display.syncExec(() -> require(display.getFocusControl() == redirected.get()
                    && staleFocusIns.get() == 0 && redirectedFocusIns.get() == 1,
                    "Queued native acknowledgements cannot restore the superseded focus request"));
        } finally {
            if (!display.isDisposed()) display.syncExec(() -> {
                if (!original.isDisposed() && redirect.get() != null)
                    original.removeListener(SWT.FocusOut, redirect.get());
                if (requested.get() != null && !requested.get().isDisposed()) requested.get().dispose();
                if (redirected.get() != null && !redirected.get().isDisposed()) redirected.get().dispose();
                if (!original.isDisposed()) original.setFocus();
            });
        }
    }

    private static void verifyImageGraphics(Display display) {
        Image image = new Image(display, 64, 48);
        GC gc = new GC(image);
        try {
            require(gc.handle != 0, "Original public GC handle is available on Android");
            gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
            gc.fillRectangle(0, 0, 64, 48);
            gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setAntialias(SWT.ON);
            gc.setTextAntialias(SWT.OFF);
            gc.drawString("Sg", 4, 4, true);
            int[] off = textPixelCounts(image.getImageData());
            require(off[0] > 0 && off[1] == 0, "Android text AA stays off while shape AA is on");
            gc.fillRectangle(0, 0, 64, 48);
            gc.setAntialias(SWT.OFF);
            gc.setTextAntialias(SWT.ON);
            gc.drawString("Sg", 4, 4, true);
            require(textPixelCounts(image.getImageData())[1] > 0,
                    "Android text AA stays on while shape AA is off");
            gc.setAdvanced(false);
            require(gc.getAntialias() == SWT.DEFAULT && gc.getTextAntialias() == SWT.DEFAULT,
                    "Disabling advanced graphics restores both default AA states");
        } finally {
            try { gc.dispose(); require(gc.handle == 0, "GC handle clears after native disposal"); }
            finally { image.dispose(); }
        }
    }

    private static int[] textPixelCounts(ImageData image) {
        int black = 0, partial = 0;
        for (int y = 0; y < image.height; y++) for (int x = 0; x < image.width; x++) {
            RGB rgb = image.palette.getRGB(image.getPixel(x, y));
            if (rgb.red == 0 && rgb.green == 0 && rgb.blue == 0) black++;
            else if (rgb.red != 255 || rgb.green != 255 || rgb.blue != 255) partial++;
        }
        return new int[] {black, partial};
    }

    private static void dispatchTouch(View peer, long downTime, int action, int[] ids) {
        MotionEvent event = pointerEvent(peer, downTime, action, ids, 0, 0, InputDevice.SOURCE_TOUCHSCREEN);
        try { require(peer.dispatchTouchEvent(event), "Native multitouch transition delivered"); }
        finally { event.recycle(); }
    }

    private static MotionEvent pointerEvent(View peer, long downTime, int action, int[] ids,
            int metaState, int buttonState, int source) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[ids.length];
        float density = peer.getResources().getDisplayMetrics().density;
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = source == InputDevice.SOURCE_MOUSE
                    ? MotionEvent.TOOL_TYPE_MOUSE : MotionEvent.TOOL_TYPE_FINGER;
            coordinates[i] = new MotionEvent.PointerCoords();
            coordinates[i].x = peer.getPaddingLeft() + Math.round((24 + i * 20) * density);
            coordinates[i].y = peer.getPaddingTop() + Math.round(20 * density);
            coordinates[i].pressure = 1;
            coordinates[i].size = 1;
        }
        return MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, ids.length,
                properties, coordinates, metaState, buttonState, 1, 1, 0, 0, source, 0);
    }

    private static int[] location(Instrumentation host, View peer, int x, int y) {
        int[] result = new int[2];
        host.runOnMainSync(() -> {
            peer.getLocationOnScreen(result);
            float density = peer.getResources().getDisplayMetrics().density;
            result[0] += peer.getPaddingLeft() + Math.round(x * density);
            result[1] += peer.getPaddingTop() + Math.round(y * density);
        });
        return result;
    }

    private static boolean pixelMatches(Instrumentation host, int[] location, int expected) {
        Bitmap image = host.getUiAutomation().takeScreenshot();
        if (image == null) return false;
        try {
            if (location[0] < 0 || location[1] < 0 || location[0] >= image.getWidth() || location[1] >= image.getHeight()) return false;
            int actual = image.getPixel(location[0], location[1]);
            return Math.abs((actual & 255) - (expected & 255)) < 20
                    && Math.abs(((actual >>> 8) & 255) - ((expected >>> 8) & 255)) < 20
                    && Math.abs(((actual >>> 16) & 255) - ((expected >>> 16) & 255)) < 20;
        } finally { image.recycle(); }
    }

    private static void saveScreen(Instrumentation host, String filename) throws Exception {
        Bitmap image = host.getUiAutomation().takeScreenshot();
        require(image != null, "Actual recovery screenshot is available");
        try (FileOutputStream out = new FileOutputStream(new File(host.getTargetContext().getExternalFilesDir(null), filename))) {
            require(image.compress(Bitmap.CompressFormat.PNG, 100, out), "Recovery screenshot encoding succeeded");
        } finally { image.recycle(); }
    }

    private static void await(BooleanSupplier condition, String description) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50);
        require(condition.getAsBoolean(), "Timed out: " + description);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
