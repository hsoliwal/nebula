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
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

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
            await(() -> NebulaMobileMain.state != null && surface(current).getWidth() > 0, "Nebula source construction");
            int[] toggle = location(surface(current), 42, 86);
            int[] veto = location(surface(current), 42, 158);
            int[] disabled = location(surface(current), 42, 230);
            int[] fill = location(surface(current), 28, 86);
            await(() -> pixelMatches(veto, 0x007aff), "native grayed-checkbox pixels");
            saveScreen("nebula-android-before.png");
            tap(toggle);
            await(() -> NebulaMobileMain.state.selected && NebulaMobileMain.state.attempts == 1, "real widget selection");
            await(() -> pixelMatches(fill, 0x007aff), "native selected-checkbox fill");
            tap(veto);
            await(() -> NebulaMobileMain.state.attempts == 2, "selection veto listener");
            await(() -> pixelMatches(veto, 0x007aff), "veto preserves grayed selection");
            tap(disabled);
            SystemClock.sleep(150);
            require(NebulaMobileMain.state.attempts == 2, "Disabled control dispatched selection");
            View screen = surface(current);
            int screenLogicalWidth = Math.round(screen.getWidth() / screen.getResources().getDisplayMetrics().density);
            int[] rangeTap = location(screen, Math.max(44, screenLogicalWidth * 3 / 4), 445);
            tap(rangeTap);
            await(() -> NebulaMobileMain.state.scaleEvents == 1
                    && NebulaMobileMain.state.scaleSelection > 25
                    && NebulaMobileMain.state.progressSelection == NebulaMobileMain.state.scaleSelection,
                    "native Scale Selection updates original SWT ProgressBar exactly once");
            int selectedBeforeAccessibility = NebulaMobileMain.state.scaleSelection;
            runOnMainSync(() -> {
                View slider = findSlider(surface(current));
                require(slider != null, "Original SWT Scale has an Android accessibility peer");
                AccessibilityNodeInfo info = slider.createAccessibilityNodeInfo();
                require("android.widget.SeekBar".contentEquals(info.getClassName()),
                        "Scale is advertised as a native Android SeekBar");
                require(slider.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null),
                        "Native accessibility forwards one Scale increment");
            });
            await(() -> NebulaMobileMain.state.scaleEvents == 2
                    && NebulaMobileMain.state.scaleSelection == selectedBeforeAccessibility + 1
                    && NebulaMobileMain.state.progressSelection == NebulaMobileMain.state.scaleSelection,
                    "native Scale accessibility increments original SWT Selection");
            await(() -> NebulaMobileMain.state.listDataLoads > 0
                    && NebulaMobileMain.state.listDataLoads <= 20,
                    "one-billion-row SWT List requests only visible SetData cells");
            int[] listTap = location(screen, 52, 515);
            tap(listTap);
            await(() -> NebulaMobileMain.state.listSelection == 999_999_990
                    && NebulaMobileMain.state.listEvents == 1,
                    "native List tap selects the correct billion-row virtual index");
            int[] selectedRowPixels = location(screen, screenLogicalWidth - 60, 515);
            await(() -> pixelMatches(selectedRowPixels, 0x007aff),
                    "selected original List row paints native selection pixels");
            runOnMainSync(() -> {
                View nativeList = findList(surface(current));
                require(nativeList != null, "Original SWT List owns a native accessibility peer");
                AccessibilityNodeInfo info = nativeList.createAccessibilityNodeInfo();
                require("android.widget.ListView".contentEquals(info.getClassName()),
                        "SWT List is announced as a native Android ListView");
                require(nativeList.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null),
                        "Android native accessibility forwards List viewport paging");
            });
            SystemClock.sleep(200);
            require(NebulaMobileMain.state.listEvents == 1
                    && NebulaMobileMain.state.listSelection == 999_999_990
                    && NebulaMobileMain.state.listDataLoads <= 22,
                    "Accessibility scrolling does not alter Selection or densely load virtual rows");
            require(current.applicationFailure() == null, "SWT application failure: " + current.applicationFailure());
            saveScreen("nebula-android-after.png");
            runOnMainSync(current::finish);
            await(() -> NebulaMobileMain.state.disposed, "Activity destruction disposes Nebula shell");
            result.putString("stream", "NEBULA_ANDROID_NATIVE_PASS: original widgets, Scale/ProgressBar, billion-row List, native accessibility, disposal\n");
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

    // SwtActivity exposes the original Android content root, not getSWTView().
    // Use the real platform view installed by AndroidPlatform.bind().
    private View surface(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            throw new IllegalStateException("SWT Android native root not attached");
        }
        return content.getChildAt(0);
    }

    private View findSlider(View root) {
        CharSequence label = root.getContentDescription();
        if (label != null && label.toString().startsWith("Scale ")) return root;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findSlider(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private View findList(View root) {
        CharSequence label = root.getContentDescription();
        if (label != null && label.toString().equals("List")) return root;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findList(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
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
