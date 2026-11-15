// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.function.BooleanSupplier;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

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
            // The original SWT Text is a real child EditText. Exercise an
            // InputConnection rather than mutating the Java Text model directly.
            Text editor = NebulaMobileMain.state.editor;
            editor.getDisplay().syncExec(() -> { });
            int modificationsBeforeIme = NebulaMobileMain.state.textModifications;
            int verificationsBeforeIme = NebulaMobileMain.state.textVerifications;
            AtomicReference<InputConnection> connection = new AtomicReference<>();
            runOnMainSync(() -> {
                EditText nativeEditor = findNativeEditor(surface(current));
                require(nativeEditor != null && nativeEditor.requestFocus(),
                        "SWT Text owns a focusable Android IME native editor");
                connection.set(nativeEditor.onCreateInputConnection(new EditorInfo()));
                require(connection.get() != null
                        && connection.get().commitText("native", 1),
                        "Real Android InputConnection committed text");
            });
            editor.getDisplay().syncExec(() -> { });
            await(() -> NebulaMobileMain.state.nativeText.equals("native")
                    && NebulaMobileMain.state.textModifications == modificationsBeforeIme + 1
                    && NebulaMobileMain.state.textVerifications == verificationsBeforeIme + 1,
                    "Original SWT Text accepts native IME input with exactly one Verify and Modify");
            runOnMainSync(() -> require(connection.get().commitText("!", 1),
                    "Real Android InputConnection attempted vetoed input"));
            editor.getDisplay().syncExec(() -> { });
            await(() -> NebulaMobileMain.state.textVerifications == verificationsBeforeIme + 2,
                    "Original SWT Verify listener receives the vetoed native edit exactly once");
            AtomicReference<String> nativeSnapshot = new AtomicReference<>();
            await(() -> {
                runOnMainSync(() -> nativeSnapshot.set(
                        findNativeEditor(surface(current)).getText().toString()));
                return nativeSnapshot.get().equals("native");
            }, "Verify veto restored the actual Android EditText snapshot");
            require(NebulaMobileMain.state.nativeText.equals("native")
                    && NebulaMobileMain.state.textModifications == modificationsBeforeIme + 1
                    && nativeSnapshot.get().equals("native"),
                    "Verify veto rolled back Android IME edit without sending Modify");
            // Selection-only native changes must reach the original SWT Text
            // without pretending that the text itself was edited.
            // The InputConnection can also queue its final caret snapshot.
            // Finish those IME callbacks before measuring selection-only events.
            editor.getDisplay().syncExec(() -> { });
            int modificationsBeforeSelection = NebulaMobileMain.state.textModifications;
            int verificationsBeforeSelection = NebulaMobileMain.state.textVerifications;
            runOnMainSync(() -> findNativeEditor(surface(current)).setSelection(2));
            await(() -> swtSelectionMatches(editor, 2, 2, ""),
                    "Native caret move updates original SWT Text selection");
            runOnMainSync(() -> findNativeEditor(surface(current)).setSelection(1, 4));
            await(() -> swtSelectionMatches(editor, 1, 4, "ati"),
                    "Native selection range updates original SWT selected text");
            require(NebulaMobileMain.state.textModifications == modificationsBeforeSelection
                    && NebulaMobileMain.state.textVerifications == verificationsBeforeSelection,
                    "Native selection-only changes must not send Verify or Modify");
            editor.getDisplay().syncExec(editor::copy);
            require("ati".equals(nativeClipboardText()),
                    "SWT copy uses the actual native selection range");
            editor.getDisplay().syncExec(editor::cut);
            await(() -> NebulaMobileMain.state.nativeText.equals("nve")
                    && NebulaMobileMain.state.textModifications == modificationsBeforeSelection + 1
                    && NebulaMobileMain.state.textVerifications == verificationsBeforeSelection + 1,
                    "SWT cut removes only the selected native range with one Verify and Modify");
            await(() -> {
                runOnMainSync(() -> nativeSnapshot.set(
                        findNativeEditor(surface(current)).getText().toString()));
                return nativeSnapshot.get().equals("nve")
                        && swtSelectionMatches(editor, 1, 1, "");
            }, "SWT cut publishes the accepted text and collapsed caret to Android");
            // Publishing a Java selection back to Android must not echo a text edit.
            editor.getDisplay().syncExec(() -> editor.setSelection(0, 3));
            runOnMainSync(() -> {
                EditText nativeEditor = findNativeEditor(surface(current));
                require(nativeEditor.getSelectionStart() == 0 && nativeEditor.getSelectionEnd() == 3,
                        "Programmatic SWT selection reaches the native editor");
            });
            editor.getDisplay().syncExec(() -> { });
            require(NebulaMobileMain.state.textModifications == modificationsBeforeSelection + 1
                    && NebulaMobileMain.state.textVerifications == verificationsBeforeSelection + 1,
                    "Programmatic selection publication must not echo Verify or Modify");
            // Open the original SWT Combo through its Android Spinner
            // accessibility action and tap the actual popup List Canvas.
            int[] comboBottom = new int[2];
            runOnMainSync(() -> {
                View combo = findCombo(surface(current));
                require(combo != null, "Original Combo has a native Android peer");
                AccessibilityNodeInfo info = combo.createAccessibilityNodeInfo();
                require("android.widget.Spinner".contentEquals(info.getClassName()),
                        "Original SWT Combo exposes a Spinner accessibility class");
                combo.getLocationOnScreen(comboBottom);
                comboBottom[1] += combo.getHeight();
                require(combo.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null),
                        "Native Combo accessibility activates original popup Shell");
            });
            AtomicReference<View> popupList = new AtomicReference<>();
            await(() -> {
                runOnMainSync(() -> popupList.set(findPopupList(surface(current), comboBottom[1])));
                return popupList.get() != null;
            }, "Native Combo popup List becomes visible");
            int[] secondRow = new int[2];
            runOnMainSync(() -> {
                View view = popupList.get();
                view.getLocationOnScreen(secondRow);
                secondRow[0] += Math.min(28, Math.max(1, view.getWidth() / 2));
                secondRow[1] += view.getHeight() / 2;
            });
            tap(secondRow);
            await(() -> NebulaMobileMain.state.comboSelection == 1
                    && "Second".equals(NebulaMobileMain.state.comboText)
                    && NebulaMobileMain.state.comboEvents == 1,
                    "Native original Combo popup row selection dispatches SWT.Selection once");
            require(current.applicationFailure() == null, "SWT application failure: " + current.applicationFailure());
            saveScreen("nebula-android-after.png");
            runOnMainSync(current::finish);
            await(() -> NebulaMobileMain.state.disposed, "Activity destruction disposes Nebula shell");
            result.putString("stream", "NEBULA_ANDROID_NATIVE_PASS: SWT Combo popup/Selection, billion-row List, Android IME selection/copy/cut/Verify/Modify, accessibility, disposal\n");
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

    private View findCombo(View root) {
        CharSequence label = root.getContentDescription();
        if (label != null && label.toString().startsWith("Combo ")) return root;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View candidate = findCombo(group.getChildAt(i));
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private View findPopupList(View root, int popupBottomThreshold) {
        CharSequence label = root.getContentDescription();
        if (label != null && "List".contentEquals(label) && root.isShown()) {
            int[] position = new int[2];
            root.getLocationOnScreen(position);
            if (position[1] >= popupBottomThreshold - 2 && root.getHeight() > 0) return root;
        }
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View candidate = findPopupList(group.getChildAt(i), popupBottomThreshold);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private EditText findNativeEditor(View root) {
        if (root instanceof EditText editor) return editor;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText result = findNativeEditor(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private boolean swtSelectionMatches(Text editor, int start, int end, String selectedText) {
        AtomicReference<Point> selection = new AtomicReference<>();
        AtomicReference<String> text = new AtomicReference<>();
        editor.getDisplay().syncExec(() -> {
            selection.set(editor.getSelection());
            text.set(editor.getSelectionText());
        });
        return new Point(start, end).equals(selection.get()) && selectedText.equals(text.get());
    }

    private String nativeClipboardText() {
        AtomicReference<String> text = new AtomicReference<>();
        runOnMainSync(() -> {
            ClipboardManager clipboard = (ClipboardManager)
                    getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = clipboard == null ? null : clipboard.getPrimaryClip();
            if (data != null && data.getItemCount() > 0) {
                text.set(data.getItemAt(0).coerceToText(getTargetContext()).toString());
            }
        });
        return text.get();
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
