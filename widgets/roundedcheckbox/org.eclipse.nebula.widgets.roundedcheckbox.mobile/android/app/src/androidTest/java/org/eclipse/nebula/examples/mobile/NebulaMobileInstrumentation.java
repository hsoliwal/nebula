// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.function.BooleanSupplier;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.android.AndroidPlatform;
import org.eclipse.swt.internal.mobile.MobilePlatform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;

/** Native screen/touch proof for the existing, unmodified RoundedCheckbox implementation. */
public final class NebulaMobileInstrumentation extends Instrumentation {
    /** How long a tap waits for a transient system window, such as the clipboard overlay, to leave its point. */
    private static final long OCCLUDED_TAP_WAIT_MS = 10_000;
    private static final long OCCLUDED_TAP_RETRY_MS = 250;
    /** The message of Android's refusal of a targeted injection that would reach another application's window. */
    private static final String NOT_OWN_WINDOW = "not directed at a window owned by";
    private NebulaMobileActivity activity;
    private String requestedScreenshotDirectory;
    private File screenshotDirectory;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        requestedScreenshotDirectory = arguments == null ? null : arguments.getString("additionalTestOutputDir");
        start();
    }

    /** Exercises the actual AndroidPlatform private generation-tagged registry inside the APK. */
    private static void verifyNativeHandleMinHeap() throws ReflectiveOperationException {
        Class<?> owner = Class.forName("org.eclipse.swt.internal.android.AndroidPlatform$Handles");
        java.lang.reflect.Constructor<?> constructor = owner.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object registry = constructor.newInstance();
        java.lang.reflect.Method add = owner.getDeclaredMethod("add", Object.class);
        java.lang.reflect.Method get = owner.getDeclaredMethod("get", long.class);
        java.lang.reflect.Method remove = owner.getDeclaredMethod("remove", long.class);
        java.lang.reflect.Method replace = owner.getDeclaredMethod("replace", long.class, Object.class);
        java.lang.reflect.Method size = owner.getDeclaredMethod("size");
        for (java.lang.reflect.Method method : new java.lang.reflect.Method[] {add, get, remove, replace, size}) {
            method.setAccessible(true);
        }
        long[] handles = new long[128];
        for (int i = 0; i < handles.length; i++) {
            handles[i] = ((Long)add.invoke(registry, new Object())).longValue();
            require((handles[i] & 0xffff_ffffL) == i + 1L,
                    "Android native registry initially allocates monotonic slots");
        }
        require(((Integer)size.invoke(registry)).intValue() == handles.length,
                "Original registry reports physical high-water slots");
        for (int index : new int[] {73, 7, 35}) {
            require(remove.invoke(registry, handles[index]) != null,
                    "Original native handle removal returns its occupant");
        }
        long first = ((Long)add.invoke(registry, new Object())).longValue();
        long second = ((Long)add.invoke(registry, new Object())).longValue();
        long third = ((Long)add.invoke(registry, new Object())).longValue();
        require((first & 0xffff_ffffL) == 8L && (second & 0xffff_ffffL) == 36L
                        && (third & 0xffff_ffffL) == 74L,
                "Android native registry preserves lowest-free-slot reuse");
        require(first != handles[7] && second != handles[35] && third != handles[73],
                "Recycling must advance each native generation");
        for (long expired : new long[] {handles[7], handles[35], handles[73]}) {
            boolean rejected = false;
            try {
                get.invoke(registry, expired);
            } catch (java.lang.reflect.InvocationTargetException failure) {
                rejected = failure.getCause() instanceof IllegalStateException;
            }
            require(rejected, "Previously disposed Android native handle must remain invalid");
        }
        Object newOwner = new Object();
        replace.invoke(registry, first, newOwner);
        require(get.invoke(registry, first) == newOwner,
                "Android Activity replacement preserves its current generation and native handle");
        require(((Integer)size.invoke(registry)).intValue() == 128,
                "Native recycling cannot increase control/image slot high-water");
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        sendCaseStatus(1, null);
        try {
            // Gradle retrieves this directory before uninstalling the APKs.
            // Direct adb invocation can retain the ordinary app-specific path.
            screenshotDirectory = requestedScreenshotDirectory == null || requestedScreenshotDirectory.isBlank()
                    ? getTargetContext().getExternalFilesDir(null) : new File(requestedScreenshotDirectory);
            require(screenshotDirectory != null
                    && (screenshotDirectory.isDirectory() || screenshotDirectory.mkdirs()),
                    "Native screenshot directory is writable");
            verifyNativeHandleMinHeap();
            activity = (NebulaMobileActivity) startActivitySync(new Intent(getTargetContext(), NebulaMobileActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            NebulaMobileActivity current = activity;
            await(() -> NebulaMobileMain.state != null && windowReady(current), "Nebula source construction");
            int[] checkboxTap = location(surface(current), 42, 86);
            int[] veto = location(surface(current), 42, 158);
            int[] disabled = location(surface(current), 42, 230);
            int[] fill = location(surface(current), 28, 86);
            await(() -> pixelMatches(veto, 0x007aff), "native grayed-checkbox pixels");
            saveScreen("nebula-android-before.png");
            tap(checkboxTap);
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
            // A native hardware DPAD key must flow through the original
            // SWT Combo keyboard owner, not a synthetic Java-only selection.
            runOnMainSync(() -> {
                View comboPeer = findCombo(surface(current));
                require(comboPeer != null && comboPeer.requestFocus(),
                        "Native Android Combo peer receives hardware keyboard focus");
                require(comboPeer.dispatchKeyEvent(
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)),
                        "Android DPAD_UP delivered to SWT Combo native peer");
            });
            await(() -> NebulaMobileMain.state.comboSelection == 0
                    && "First".equals(NebulaMobileMain.state.comboText)
                    && NebulaMobileMain.state.comboEvents == 2,
                    "Original Combo DPAD_UP sends one SWT Selection and updated text");
            runOnMainSync(() -> {
                View comboPeer = findCombo(surface(current));
                require(comboPeer.dispatchKeyEvent(
                        new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)),
                        "Android DPAD_DOWN delivered to SWT Combo native peer");
            });
            await(() -> NebulaMobileMain.state.comboSelection == 1
                    && "Second".equals(NebulaMobileMain.state.comboText)
                    && NebulaMobileMain.state.comboEvents == 3,
                    "Original Combo DPAD_DOWN sends one SWT Selection");
            runOnMainSync(() -> require(
                    findCombo(surface(current)).performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLICK, null),
                    "Android Combo popup reopens for Escape-key test"));
            await(() -> {
                runOnMainSync(() -> popupList.set(findPopupList(
                        surface(current), comboBottom[1])));
                return popupList.get() != null;
            }, "Original Combo popup reopened");
            // A device delivers a hardware key to the focused view, and the open popup's List holds focus.
            sendKeyDownUpSync(KeyEvent.KEYCODE_ESCAPE);
            await(() -> {
                runOnMainSync(() -> popupList.set(findPopupList(
                        surface(current), comboBottom[1])));
                return popupList.get() == null;
            }, "Original Combo Escape closes native popup without selecting");
            require(NebulaMobileMain.state.comboSelection == 1
                    && NebulaMobileMain.state.comboEvents == 3,
                    "Escape does not fabricate SWT Combo Selection");
            // The original SWT Table has one billion virtual logical rows;
            // Android exposes the actual SWT Canvas peer as a GridView for
            // accessibility, not a separate Android Table data model.
            Table table = NebulaMobileMain.state.table;
            require(table != null, "Original SWT Table model is constructed");
            await(() -> NebulaMobileMain.state.tableDataLoads >= 2
                    && NebulaMobileMain.state.tableDataLoads <= 16,
                    "Native original SWT Table materializes only visible SetData rows");
            // Use the same Table model, expanded into the actual screen.
            // This preserves the existing checkbox/Combo/Text acceptance scene.
            int[] expandTap = new int[2];
            runOnMainSync(() -> {
                View toggle = findVisibleDescription(surface(current), "Expand Table");
                require(toggle != null, "SWT Table has an accessible Expand Table button");
                toggle.getLocationOnScreen(expandTap);
                expandTap[0] += toggle.getWidth() / 2;
                expandTap[1] += toggle.getHeight() / 2;
            });
            tap(expandTap);
            await(() -> NebulaMobileMain.state.tableExpanded,
                    "Native Expand Table activates the original Java-owned viewport");
            AtomicInteger tableTop = new AtomicInteger();
            AtomicInteger tableHeader = new AtomicInteger();
            AtomicInteger tableRowHeight = new AtomicInteger();
            table.getDisplay().syncExec(() -> {
                tableTop.set(table.getTopIndex());
                tableHeader.set(table.getHeaderHeight());
                tableRowHeight.set(table.getItemHeight());
                require(table.getItemCount() == 1_000_000_000
                        && table.getColumnCount() == 2,
                        "SWT Table has billion-row model and two original columns");
            });
            runOnMainSync(() -> {
                View view = findNativeTable(surface(current));
                require(view != null, "Original SWT Table has native Android peer");
                AccessibilityNodeInfo info = view.createAccessibilityNodeInfo();
                require("android.widget.GridView".contentEquals(info.getClassName()),
                        "SWT Table native peer publishes GridView accessibility");
                require(view.performAccessibilityAction(
                                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, null),
                        "Native Table accessibility sends original viewport paging event");
            });
            int initialTop = tableTop.get();
            await(() -> {
                table.getDisplay().syncExec(() -> tableTop.set(table.getTopIndex()));
                return tableTop.get() < initialTop;
            }, "Native Table accessibility moves sparse viewport");
            require(NebulaMobileMain.state.tableSelections == 0,
                    "Table accessibility paging does not fabricate SWT.Selection");
            int[] tableTap = new int[2];
            runOnMainSync(() -> {
                View view = findNativeTable(surface(current));
                require(view != null, "Native Table retained after page scroll");
                view.getLocationOnScreen(tableTap);
                float density = view.getResources().getDisplayMetrics().density;
                tableTap[0] += Math.round(40 * density);
                tableTap[1] += Math.round((tableHeader.get()
                        + tableRowHeight.get() / 2f) * density);
            });
            tap(tableTap);
            await(() -> NebulaMobileMain.state.tableSelections == 1
                    && NebulaMobileMain.state.tableSelection == tableTop.get(),
                    "Native Table row tap maps the correct billion-row logical index");
            int[] selectionPixel = new int[2];
            runOnMainSync(() -> {
                View tableView = findNativeTable(surface(current));
                require(tableView != null && tableView.isShown(),
                        "Expanded original SWT Table remains visible on Android screen");
                tableView.getLocationOnScreen(selectionPixel);
                selectionPixel[0] += tableView.getWidth() - 9;
                selectionPixel[1] = tableTap[1];
            });
            await(() -> pixelMatches(selectionPixel, 0x007aff),
                    "Original Table selected row paints actual Android blue pixels");
            int[] headerTap = new int[2];
            runOnMainSync(() -> {
                View tableView = findNativeTable(surface(current));
                tableView.getLocationOnScreen(headerTap);
                float density = tableView.getResources().getDisplayMetrics().density;
                headerTap[0] += Math.round(24 * density);
                headerTap[1] += Math.round(9 * density);
            });
            tap(headerTap);
            await(() -> NebulaMobileMain.state.tableHeaderSelections == 1
                    && NebulaMobileMain.state.tableSelections == 1,
                    "Real TableColumn header click dispatches Selection without row selection");
            require(NebulaMobileMain.state.tableDataLoads <= 80,
                    "Expanded million-scale viewport remains bounded despite billion-row model");
            int[] restoreTap = new int[2];
            runOnMainSync(() -> {
                View toggle = findVisibleDescription(surface(current), "Restore");
                require(toggle != null, "Expanded Table has a native Restore control");
                toggle.getLocationOnScreen(restoreTap);
                restoreTap[0] += toggle.getWidth() / 2;
                restoreTap[1] += toggle.getHeight() / 2;
            });
            tap(restoreTap);
            await(() -> !NebulaMobileMain.state.tableExpanded,
                    "Restore returns the same original Table to its compact viewport");
            SwtMobileRecoveryContracts.verify(this, current, NebulaMobileMain.state.editor.getDisplay());
            require(current.applicationFailure() == null, "SWT application failure: " + current.applicationFailure());
            await(() -> pixelMatches(fill, 0x007aff),
                    "Original checkbox repaints after restoring the Table viewport");
            saveScreen("nebula-android-after.png");
            verifyCheckboxLifecycle();
            verifyTransportCleanup();
            result.putString("stream", "NEBULA_ANDROID_NATIVE_PASS: Table, List, Combo, Text IME, original checkbox input, Activity lifecycle and Android transport cleanup\n");
            result.putString("screenshotDirectory", screenshotDirectory.getAbsolutePath());
            sendCaseStatus(0, null);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            if (activity != null) { NebulaMobileActivity current = activity; runOnMainSync(current::finish); }
            result.putString("stream", "NEBULA_ANDROID_NATIVE_FAIL: " + failure + "\n");
            sendCaseStatus(-2, failure);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    /** Uses Android's original Activity as an isolated host after the SWT app exits. */
    private void verifyTransportCleanup() throws Exception {
        Activity host = startActivitySync(new Intent()
                .setClassName(getTargetContext(), Activity.class.getName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        AtomicReference<AndroidPlatform> reference = new AtomicReference<>();
        AtomicInteger terminations = new AtomicInteger();
        MobilePlatform.EventSink receiver = new MobilePlatform.EventSink() {
            @Override public void input(long peer, int type, int x, int y, int button,
                    int keyCode, char character, int stateMask, int count, int time) { }
            @Override public void resize(long peer, int width, int height) { }
            @Override public void paint(long peer) { }
            @Override public void close(long peer) { }
            @Override public void lifecycle(boolean active) { }
            @Override public void terminate() { terminations.incrementAndGet(); }
            @Override public void accessibility(long peer, int action) { }
        };
        AndroidPlatform platform = null;
        boolean connected = false;
        try {
            runOnMainSync(() -> reference.set(new AndroidPlatform(host)));
            platform = reference.get();
            platform.connect(receiver);
            connected = true;
            long first = platform.createControl(MobilePlatform.SHELL, 0, SWT.NONE);
            long parent = platform.createControl(MobilePlatform.SHELL, 0, SWT.NONE);
            platform.disposeControl(first);
            long child = platform.createControl(MobilePlatform.CANVAS, parent, SWT.NONE);
            long image = platform.createImage(4, 4, null);
            require((child & 0xffffffffL) < (parent & 0xffffffffL),
                    "Child reuses the lower native registry slot before disconnect");
            platform.disconnect();
            connected = false;
            runOnMainSync(() -> {
                ViewGroup content = host.findViewById(android.R.id.content);
                require(content.getChildCount() == 1
                        && ((ViewGroup) content.getChildAt(0)).getChildCount() == 0,
                        "Disconnect releases the reused child before its native parent");
            });

            platform.connect(receiver);
            connected = true;
            long replacement = platform.createControl(MobilePlatform.SHELL, 0, SWT.NONE);
            long replacementImage = platform.createImage(5, 3, null);
            boolean staleControl = false;
            boolean staleImage = false;
            try { platform.setVisible(child, true); }
            catch (IllegalStateException expected) { staleControl = true; }
            try { platform.imagePixels(image); }
            catch (IllegalStateException expected) { staleImage = true; }
            require(staleControl && staleImage,
                    "Disconnected control and image handles remain invalid after reconnect and slot reuse");
            platform.setVisible(replacement, true);
            require(platform.imagePixels(replacementImage).length == 15,
                    "Replacement control and image handles remain usable after rejecting stale handles");
            platform.disconnect();
            connected = false;

            runOnMainSync(host::finish);
            await(host::isDestroyed, "Isolated transport Activity is destroyed before reconnect");
            // SwtActivity forwards the same callback from its original onDestroy.
            AndroidPlatform terminated = platform;
            runOnMainSync(terminated::closeActivity);
            require(terminations.get() == 0, "No disconnected receiver observes host termination");
            platform.connect(receiver);
            connected = true;
            require(terminations.get() == 1,
                    "Host destruction before Display connection retains one termination callback");
            platform.checkFailure();
        } finally {
            try {
                if (connected) platform.disconnect();
            } finally {
                runOnMainSync(host::finish);
                waitForIdleSync();
            }
        }
    }

    private void verifyCheckboxLifecycle() throws Exception {
        Display display = Display.findDisplay(activity.applicationThread());
        require(display != null, "Original SWT Display is owned by the application thread");
        RoundedCheckbox checkbox = findCheckbox(display);
        require(checkbox != null, "Original RoundedCheckbox remains in the shared application");
        NebulaMobileContents.State originalState = NebulaMobileMain.state;
        AtomicInteger selections = new AtomicInteger();
        AtomicBoolean veto = new AtomicBoolean();
        AtomicReference<Boolean> beforeSelection = new AtomicReference<>();
        display.syncExec(() -> {
            checkbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
                beforeSelection.set(checkbox.getSelection());
                selections.incrementAndGet();
                if (veto.get()) event.doit = false;
            }));
            checkbox.setSelection(true);
            checkbox.setSelection(false);
        });
        require(selections.get() == 0 && !selected(checkbox),
                "Programmatic RoundedCheckbox selection emits no Selection");
        tap(screenLocation(checkbox));
        await(() -> selections.get() == 1, "Native checkbox Selection");
        require(Boolean.FALSE.equals(beforeSelection.get()) && selected(checkbox),
                "Original Selection listener observes the pre-toggle value");
        veto.set(true);
        tap(screenLocation(checkbox));
        await(() -> selections.get() == 2, "Vetoed native checkbox Selection");
        require(Boolean.TRUE.equals(beforeSelection.get()) && selected(checkbox),
                "SelectionEvent.doit=false preserves the original checkbox value");
        veto.set(false);

        sendKeyDownUpSync(KeyEvent.KEYCODE_SPACE);
        await(() -> !selected(checkbox), "Physical Space toggles through original KeyUp");
        require(selections.get() == 2,
                "Original keyboard toggle does not acquire an extra Selection event");
        cancelPointer(screenLocation(checkbox));
        waitForIdleSync();
        display.syncExec(() -> { });
        require(selections.get() == 2 && !selected(checkbox),
                "Cancelled pointer does not activate the original widget");
        tap(screenLocation(checkbox));
        await(() -> selections.get() == 3, "Fresh pointer after cancellation");
        require(selected(checkbox), "Pointer tracking recovers after cancellation");

        AtomicReference<View> oldPeer = new AtomicReference<>();
        runOnMainSync(() -> {
            View focus = activity.getWindow().getDecorView().findFocus();
            require(focus != null && focus.performAccessibilityAction(
                    AccessibilityNodeInfo.ACTION_CLICK, null),
                    "Native checkbox accessibility activation is accepted");
            oldPeer.set(focus);
        });
        await(() -> selections.get() == 4, "Native checkbox accessibility Selection");
        require(!selected(checkbox), "Accessibility uses the original cancellable widget action");

        AtomicInteger closeRequests = new AtomicInteger();
        AtomicInteger staleResizes = new AtomicInteger();
        display.syncExec(() -> {
            checkbox.getShell().addListener(SWT.Close, event -> {
                closeRequests.incrementAndGet();
                event.doit = false;
            });
        });
        NebulaMobileActivity previous = activity;
        ActivityMonitor monitor = addMonitor(NebulaMobileActivity.class.getName(), null, false);
        Activity replacement;
        try {
            runOnMainSync(previous::recreate);
            replacement = waitForMonitorWithTimeout(monitor, 15000);
        } finally {
            removeMonitor(monitor);
        }
        require(replacement instanceof NebulaMobileActivity && replacement != previous,
                "Android recreates the existing Activity owner");
        activity = (NebulaMobileActivity) replacement;
        await(() -> Display.findDisplay(activity.applicationThread()) == display
                && windowReady(activity), "Recreated Activity preserves Display and visible layout");
        require(NebulaMobileMain.state == originalState && findCheckbox(display) == checkbox
                && !checkbox.isDisposed() && !selected(checkbox),
                "Activity recreation preserves the original widget, state and selection");
        display.syncExec(() -> checkbox.addListener(SWT.Resize, event -> staleResizes.incrementAndGet()));
        runOnMainSync(() -> {
            oldPeer.get().layout(0, 0, 43, 47);
            oldPeer.get().onKeyUp(KeyEvent.KEYCODE_BACK,
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            require(!oldPeer.get().performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null),
                    "Detached native peer rejects accessibility activation");
        });
        display.syncExec(() -> { });
        require(staleResizes.get() == 0 && closeRequests.get() == 0 && selections.get() == 4,
                "Detached peer cannot resize, close or activate replacement SWT owners");

        tap(screenLocation(checkbox));
        await(() -> selections.get() == 5, "Input after Activity recreation");
        require(selected(checkbox), "Replacement native peer updates the same original widget");
        twoPointers(screenLocation(checkbox));
        waitForIdleSync();
        display.syncExec(() -> { });
        require(selections.get() == 6 && !selected(checkbox),
                "Secondary removal keeps the primary gesture active; primary completion activates once");
        tap(screenLocation(checkbox));
        await(() -> selections.get() == 7, "Fresh pointer after multi-pointer release");
        require(selected(checkbox), "Pointer tracking resets after a multi-pointer sequence");
        int[] recreatedFill = location(surface(activity), 28, 86);
        await(() -> pixelMatches(recreatedFill, 0x007aff),
                "Recreated native peer paints the original selected-checkbox pixels");
        saveScreen("nebula-android-recreated.png");

        // System Back must reach Shell.Close, rather than merely dismissing an
        // IME left open by the preceding original Text acceptance checks.
        runOnMainSync(() -> {
            InputMethodManager keyboard = (InputMethodManager)
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.hideSoftInputFromWindow(surface(activity).getWindowToken(), 0);
        });
        await(() -> {
            AtomicBoolean hidden = new AtomicBoolean();
            runOnMainSync(() -> {
                WindowInsets insets = surface(activity).getRootWindowInsets();
                hidden.set(insets != null && !insets.isVisible(WindowInsets.Type.ime()));
            });
            return hidden.get();
        }, "Native keyboard is hidden before system Back");
        require(getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK),
                "Android system Back action is accepted");
        await(() -> closeRequests.get() == 1, "System Back reaches vetoable SWT Shell.Close");
        require(!display.isDisposed() && !activity.isFinishing() && !originalState.disposed,
                "Shell.Close veto preserves the original application after Back");
        runOnMainSync(activity::finish);
        await(() -> activity.applicationFinished(), "OS destruction finishes the SWT application thread");
        require(display.isDisposed() && checkbox.isDisposed() && originalState.disposed,
                "Final Activity destruction disposes the original Display and widgets despite Close veto");
        require(activity.applicationFailure() == null,
                "Activity lifecycle has no hidden SWT application failure");
    }

    private RoundedCheckbox findCheckbox(Display display) {
        AtomicReference<RoundedCheckbox> result = new AtomicReference<>();
        display.syncExec(() -> {
            for (var shell : display.getShells()) {
                RoundedCheckbox found = findCheckbox(shell);
                if (found != null) { result.set(found); break; }
            }
        });
        return result.get();
    }

    private RoundedCheckbox findCheckbox(Composite parent) {
        for (Control child : parent.getChildren()) {
            if (child instanceof RoundedCheckbox checkbox) return checkbox;
            if (child instanceof Composite composite) {
                RoundedCheckbox found = findCheckbox(composite);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean selected(RoundedCheckbox checkbox) {
        AtomicBoolean value = new AtomicBoolean();
        checkbox.getDisplay().syncExec(() -> value.set(checkbox.getSelection()));
        return value.get();
    }

    private int[] screenLocation(RoundedCheckbox checkbox) {
        AtomicReference<Point> location = new AtomicReference<>();
        checkbox.getDisplay().syncExec(() -> {
            Point origin = checkbox.toDisplay(0, 0);
            Point size = checkbox.getSize();
            location.set(new Point(origin.x + size.x / 2, origin.y + size.y / 2));
        });
        return location(surface(activity), location.get().x, location.get().y);
    }

    private boolean windowReady(Activity host) {
        AtomicBoolean ready = new AtomicBoolean();
        runOnMainSync(() -> {
            ViewGroup content = host.findViewById(android.R.id.content);
            View root = content == null || content.getChildCount() == 0 ? null : content.getChildAt(0);
            ready.set(host.getWindow().getDecorView().hasWindowFocus()
                    && root != null && root.isShown()
                    && root.getWidth() > root.getPaddingLeft() + root.getPaddingRight()
                    && root.getHeight() > root.getPaddingTop() + root.getPaddingBottom());
        });
        return ready.get();
    }

    private void cancelPointer(int[] location) {
        long down = SystemClock.uptimeMillis();
        MotionEvent first = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, location[0], location[1], 0);
        MotionEvent last = MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_CANCEL, location[0], location[1], 0);
        try { sendPointerSync(first); sendPointerSync(last); }
        finally { first.recycle(); last.recycle(); }
    }

    private void twoPointers(int[] location) {
        long down = SystemClock.uptimeMillis();
        MotionEvent.PointerProperties primary = new MotionEvent.PointerProperties();
        MotionEvent.PointerProperties secondary = new MotionEvent.PointerProperties();
        primary.id = 0;
        primary.toolType = MotionEvent.TOOL_TYPE_FINGER;
        secondary.id = 1;
        secondary.toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords first = new MotionEvent.PointerCoords();
        first.x = location[0]; first.y = location[1]; first.pressure = 1; first.size = 1;
        MotionEvent.PointerCoords second = new MotionEvent.PointerCoords();
        second.copyFrom(first);
        second.x += 5;
        MotionEvent.PointerProperties[][] pointers = {{primary}, {primary, secondary}, {primary, secondary}, {primary}};
        MotionEvent.PointerCoords[][] coordinates = {{first}, {first, second}, {first, second}, {first}};
        int[] actions = {MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), MotionEvent.ACTION_UP};
        for (int i = 0; i < actions.length; i++) {
            MotionEvent event = MotionEvent.obtain(down, down + i * 20, actions[i], pointers[i].length,
                    pointers[i], coordinates[i], 0, 0, 1, 1, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
            try { sendPointerSync(event); }
            finally { event.recycle(); }
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

    private View findVisibleDescription(View root, String description) {
        CharSequence label = root.getContentDescription();
        if (root.isShown() && label != null && description.contentEquals(label)) return root;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findVisibleDescription(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private View findNativeTable(View root) {
        CharSequence label = root.getContentDescription();
        if (label != null && "Table".contentEquals(label)) return root;
        if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View candidate = findNativeTable(group.getChildAt(i));
                if (candidate != null) return candidate;
            }
        }
        return null;
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

    /**
     * Taps a screen point of this application's window. A transient system window can cover the point: on Android 13
     * and later, a cut or copy shows the clipboard overlay in the lower start corner for a few seconds, and Android
     * refuses a targeted injection that would reach another application's window. Such a refusal is retried until
     * {@link #OCCLUDED_TAP_WAIT_MS}; a point that stays covered fails with Android's original exception.
     */
    private void tap(int[] location) {
        long deadline = SystemClock.uptimeMillis() + OCCLUDED_TAP_WAIT_MS;
        while (true) {
            try {
                tapOnce(location);
                return;
            } catch (IllegalArgumentException refused) {
                if (!String.valueOf(refused.getMessage()).contains(NOT_OWN_WINDOW)
                        || SystemClock.uptimeMillis() > deadline) throw refused;
                SystemClock.sleep(OCCLUDED_TAP_RETRY_MS);
            }
        }
    }

    private void tapOnce(int[] location) {
        long down = SystemClock.uptimeMillis();
        MotionEvent first = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, location[0], location[1], 0);
        MotionEvent last = MotionEvent.obtain(down, down + 20, MotionEvent.ACTION_UP, location[0], location[1], 0);
        try { sendPointerSync(first); sendPointerSync(last); }
        finally { first.recycle(); last.recycle(); }
    }

    private void saveScreen(String filename) throws Exception {
        Display display = Display.findDisplay(activity.applicationThread());
        require(display != null, "SWT Display is available for visible-screen capture");
        display.syncExec(() -> { });
        AtomicBoolean drawn = new AtomicBoolean();
        runOnMainSync(() -> {
            View root = surface(activity);
            ViewTreeObserver observer = root.getViewTreeObserver();
            observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                @Override public void onDraw() {
                    root.post(() -> {
                        if (observer.isAlive()) observer.removeOnDrawListener(this);
                        drawn.set(true);
                    });
                }
            });
            root.postInvalidateOnAnimation();
        });
        await(drawn::get, "A fresh native window frame before SWT screen capture");
        AtomicReference<int[]> pixels = new AtomicReference<>();
        int[] size = new int[2];
        display.syncExec(() -> {
            Rectangle area = display.getClientArea();
            size[0] = area.width;
            size[1] = area.height;
            Image snapshot = new Image(display, area.width, area.height);
            try {
                GC screen = new GC(display);
                try {
                    screen.copyArea(snapshot, area.x, area.y);
                } finally {
                    screen.dispose();
                }
                ImageData data = snapshot.getImageData();
                int[] argb = new int[Math.multiplyExact(data.width, data.height)];
                for (int y = 0; y < data.height; y++) {
                    for (int x = 0; x < data.width; x++) {
                        RGB color = data.palette.getRGB(data.getPixel(x, y));
                        argb[y * data.width + x] = data.getAlpha(x, y) << 24
                                | color.red << 16 | color.green << 8 | color.blue;
                    }
                }
                pixels.set(argb);
            } finally {
                snapshot.dispose();
            }
        });
        int[] argb = pixels.get();
        require(argb != null && argb.length > 0, "Missing SWT visible-screen pixels");
        boolean varied = false;
        for (int pixel : argb) {
            if (pixel != argb[0]) { varied = true; break; }
        }
        require(varied, "SWT visible-screen capture is blank");
        File appDirectory = getTargetContext().getExternalFilesDir(null);
        require(appDirectory != null && (appDirectory.isDirectory() || appDirectory.mkdirs()),
                "App-specific native screenshot directory is writable");
        File[] directories = screenshotDirectory.getCanonicalFile().equals(appDirectory.getCanonicalFile())
                ? new File[] {screenshotDirectory} : new File[] {screenshotDirectory, appDirectory};
        Bitmap image = Bitmap.createBitmap(argb, size[0], size[1], Bitmap.Config.ARGB_8888);
        try {
            // Keep the original adb evidence path even when Gradle requests a
            // separate additional-output directory before uninstalling APKs.
            for (File directory : directories) {
                try (FileOutputStream out = new FileOutputStream(new File(directory, filename))) {
                    require(image.compress(Bitmap.CompressFormat.PNG, 100, out), "PNG encoding failed");
                }
            }
        } finally { image.recycle(); }
    }

    private void await(BooleanSupplier condition, String description) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        for (;;) {
            Throwable failure = activity == null ? null : activity.applicationFailure();
            if (failure != null) throw new AssertionError(description, failure);
            if (condition.getAsBoolean()) return;
            require(SystemClock.uptimeMillis() < deadline, "Timed out: " + description);
            SystemClock.sleep(50);
        }
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
