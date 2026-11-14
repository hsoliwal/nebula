// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.function.BooleanSupplier;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/** Native screen/touch proof for the existing, unmodified RoundedCheckbox implementation. */
public final class NebulaMobileInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle();
        sendCaseStatus(1, null);
        NebulaMobileActivity activity = null;
        try {
            activity = (NebulaMobileActivity) startActivitySync(new Intent(getTargetContext(), NebulaMobileActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            NebulaMobileActivity current = activity;
            await(() -> current.state != null && current.getSWTView().getWidth() > 0, "Nebula source construction");
            int[] toggle = location(current.getSWTView(), 42, 86);
            int[] veto = location(current.getSWTView(), 42, 158);
            int[] disabled = location(current.getSWTView(), 42, 230);
            int[] fill = location(current.getSWTView(), 28, 86);
            await(() -> pixelMatches(veto, 0x007aff), "native grayed-checkbox pixels");
            saveScreen("nebula-android-before.png");
            tap(toggle);
            await(() -> current.state.selected && current.state.attempts == 1, "real widget selection");
            await(() -> pixelMatches(fill, 0x007aff), "native selected-checkbox fill");
            tap(veto);
            await(() -> current.state.attempts == 2, "selection veto listener");
            await(() -> pixelMatches(veto, 0x007aff), "veto preserves grayed selection");
            tap(disabled);
            SystemClock.sleep(150);
            require(current.state.attempts == 2, "Disabled control dispatched selection");
            require(activity.failure == null, "SWT application failure: " + activity.failure);
            saveScreen("nebula-android-after.png");
            runOnMainSync(current::finish);
            await(() -> current.state.disposed, "Activity destruction disposes Nebula shell");
            result.putString("stream", "NEBULA_ANDROID_NATIVE_PASS: source widget, pixels, selection, veto, disabled, disposal\n");
            result.putString("screenshotDirectory", getTargetContext().getExternalFilesDir(null).getAbsolutePath());
            sendCaseStatus(0, null);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            if (activity != null) { NebulaMobileActivity current = activity; runOnMainSync(current::finish); }
            result.putString("stream", "NEBULA_ANDROID_NATIVE_FAIL: " + failure + "\n");
            sendCaseStatus(-2, failure);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private int[] location(View view, int x, int y) {
        int[] result = new int[2];
        runOnMainSync(() -> {
            view.getLocationOnScreen(result);
            float density = view.getResources().getDisplayMetrics().density;
            result[0] += view.getPaddingLeft() + Math.round(x * density);
            result[1] += view.getPaddingTop() + Math.round(y * density);
        });
        return result;
    }

    private boolean pixelMatches(int[] location, int expected) {
        Bitmap image = getUiAutomation().takeScreenshot();
        if (image == null) return false;
        try {
            if (location[0] < 0 || location[1] < 0 || location[0] >= image.getWidth() || location[1] >= image.getHeight()) return false;
            int actual = image.getPixel(location[0], location[1]);
            return Math.abs((actual & 255) - (expected & 255)) < 20
                    && Math.abs(((actual >>> 8) & 255) - ((expected >>> 8) & 255)) < 20
                    && Math.abs(((actual >>> 16) & 255) - ((expected >>> 16) & 255)) < 20;
        } finally { image.recycle(); }
    }

    private void tap(int[] location) {
        long down = SystemClock.uptimeMillis();
        MotionEvent first = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, location[0], location[1], 0);
        MotionEvent last = MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_UP, location[0], location[1], 0);
        try { sendPointerSync(first); sendPointerSync(last); }
        finally { first.recycle(); last.recycle(); }
    }

    private void saveScreen(String filename) throws Exception {
        Bitmap image = getUiAutomation().takeScreenshot();
        require(image != null, "Missing real screen capture");
        try (FileOutputStream out = new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null), filename))) {
            require(image.compress(Bitmap.CompressFormat.PNG, 100, out), "PNG encoding failed");
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
    private void sendCaseStatus(int code, Throwable failure) {
        Bundle status = new Bundle();
        status.putString("id", "InstrumentationTestRunner");
        status.putString("class", getClass().getName());
        status.putString("test", "nativeCanvasTouchLifecycle");
        status.putInt("numtests", 1);
        status.putInt("current", 1);
        if (failure != null) status.putString("stack", android.util.Log.getStackTraceString(failure));
        sendStatus(code, status);
    }

}
