# Nebula RoundedCheckbox on the merged SWT Android backend

This is an original Nebula `RoundedCheckbox` consumer of the mobile SWT
`Display`/`Shell`/`Canvas` owners merged in
[hsoliwal/eclipse.platform.swt#369](https://github.com/hsoliwal/eclipse.platform.swt/pull/369).
The product widget, SelectionListenerUtil and ReflectionUtils sources are
included unchanged. The shared application now also instantiates the original
SWT mobile Scale and ProgressBar merged in
[hsoliwal/eclipse.platform.swt#376](https://github.com/hsoliwal/eclipse.platform.swt/pull/376).
A native Scale Selection updates ProgressBar through the unchanged SWT event
loop. The same shared Java UI also creates the original SWT
`List` implementation from
[hsoliwal/eclipse.platform.swt#385](https://github.com/hsoliwal/eclipse.platform.swt/pull/385)
with one billion logical VIRTUAL rows. Visible `SWT.SetData` requests,
row selection, selected pixels and Android ListView accessibility paging
are exercised by native instrumentation; this does not instantiate a separate
Android-specific List widget model.
The same shared Nebula application now consumes
[the original SWT Text backend (#386)](https://github.com/hsoliwal/eclipse.platform.swt/pull/386),
using Android EditText and UIKit UITextField/UITextView peers. A native Android
InputConnection inserts text, the Java SWT Display delivers Verify/Modify,
and a rejected edit is rolled back to the actual Android EditText. These
checks are included in the pinned Android API-35 instrumented test workflow.
Full iOS Java runtime/app packaging, marked-text IME composition and device
keyboard parity remain open.

The shared widget application also instantiates the **original** SWT
`Combo` merged in [SWT #388](https://github.com/hsoliwal/eclipse.platform.swt/pull/388).
On Android the instrumentation invokes its native Spinner accessibility action,
locates the SWT popup Shell/List, taps the second row, and checks the original
Combo Selection and selected text without a separate Android-native Combo
model. With [SWT #390](https://github.com/hsoliwal/eclipse.platform.swt/pull/390),
the native acceptance fixture also dispatches hardware DPAD_UP / DPAD_DOWN to
the original Combo peer, checks each original SWT Selection, reopens the
dropdown and verifies that Escape closes it without fabricating Selection.
The same Java Combo source uses UIKit-owned Text/List/Button/Shell
components on iOS, but iOS device runtime, physical keyboard navigation,
and VoiceOver popup behavior are not yet qualified.

The same shared Java application now consumes the original SWT
`Table`/`TableItem`/`TableColumn` merged in
[SWT #393](https://github.com/hsoliwal/eclipse.platform.swt/pull/393).
The billion-row virtual model fills only visible SWT.SetData rows and draws
two native-backed columns. Android instrumentation examines the actual
SWT Table Canvas peer (GridView accessibility role), pages its viewport with
a native accessibility action, and taps a high-index logical row to verify
ordinary SWT.Selection. The original Table can now expand into the
visible screen using an original SWT Button, then restore its compact layout,
without replacing its TableItem identities or billion-row model. Android
instrumentation verifies this native toggle, a genuine selected-row screen
pixel, TableColumn header Selection, and the restored viewport. These are
runnable acceptance tests, not a report of executed device results. UIKit's
corresponding Table accessibility paging is wired, but Apple runtime/device
qualification remains open. Full Nebula
Grid and XViewer APIs are not claimed by this original SWT Table consumer.

The test also invokes the Android SeekBar accessibility increment provided
by [SWT #378](https://github.com/hsoliwal/eclipse.platform.swt/pull/378),
checking that it produces exactly one original SWT Selection. iOS now exposes
the corresponding UIKit adjustable accessibility methods, but its simulator/
VoiceOver device qualification remains open. There is no competing SWT toolkit.

## Compile and package

Prerequisites: JDK 21, Maven 3.9+, Python 3.10+, Android platform 35,
Android build-tools 35.0.0 (the Android Gradle Plugin 8.13 default) and Gradle
8.13+. The mobile source closure currently
requires Android 15 / API 35; compatibility with older Android is not claimed.

1. Select the SWT checkout revision used by the
   [mobile acceptance workflow](../../../../.github/workflows/m3-nebula-mobile-original-list.yml).
   Both Android and iOS jobs pin that dependency explicitly. This application
   requires the original List backend (#385), Text backend (#386), Combo
   integration (#388/#389), and the native-selection synchronization exercised by its current instrumentation;
   a checkout containing only the initial #369 backend will not compile or
   satisfy these tests. When validating a paired SWT change, build that exact
   reviewed SWT commit and update the workflow pin after it exists.

2. Build the SWT Android Java artifact from that checkout:

   ```sh
   mvn -f /path/to/eclipse.platform.swt/mobile/pom.xml \
     -Dmobile.platform=android -Dandroid.sdk=/path/to/android-sdk clean verify
   ```

3. Build both APKs in this Nebula checkout:

   ```sh
   gradle -p widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.mobile/android \
     -PswtMobileJar=/path/to/eclipse.platform.swt/mobile/target/swt-mobile-android.jar \
     :app:assembleDebug :app:assembleDebugAndroidTest
   ```

`NebulaMobileActivity` extends `org.eclipse.swt.internal.android.SwtActivity`.
Its manifest names `NebulaMobileMain`, which constructs the original widget
tree and runs the ordinary SWT event loop on the Display thread.
The application source is compiled against the exact supplied SWT Android JAR.
There is no second local `:swt` Gradle project. The source and build entrypoint is
this `widgets/roundedcheckbox/...mobile/android` directory; the historical
`m3/mobile` paths and a Nebula `mobile/pom.xml` are not the current build.

## Native acceptance

Install the resulting application and instrumentation APKs with `adb`, then
run the `org.eclipse.nebula.examples.mobile.NebulaMobileInstrumentation`
instrumentation runner on an API-35-or-newer emulator or device. The test
uses real Android screenshots and injected touch input to examine paint,
selection, selection veto, disabled state, Scale-to-ProgressBar interaction
and cleanup. Native editor caret/range selection is compared with SWT Text
selection and clipboard operations; selection-only movement must not generate
Verify/Modify events. Run the Gradle-managed instrumentation lane with the same
project and JAR inputs:

```sh
gradle -p widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.mobile/android \
  -PswtMobileJar=/absolute/path/to/eclipse.platform.swt/mobile/target/swt-mobile-android.jar \
  :app:connectedDebugAndroidTest
```

The same runner retains the original checkbox contracts: programmatic selection
does not emit Selection; a pointer listener observes the pre-toggle value and
can veto the toggle; the original Space KeyUp toggles without an added Selection
event. Cancelled and multi-pointer gestures must not produce extra activations,
and native accessibility must invoke the original cancellable action.

Activity recreation must preserve the same Display, widget instances and shared
application state. Detached native peers must reject input, resize and close
callbacks. System Back is tested through the Android global action and must
reach a vetoable SWT Shell.Close; final OS destruction must still terminate the
application thread and dispose the original SWT owners.

After that application exits, the same runner uses an isolated original Android
`Activity`, declared only in the debug manifest, to check the SWT transport.
Disconnect must release a child before its parent even when the child reused a
lower native handle slot. Disconnected control and image handles must remain
invalid after reconnect and slot reuse, while their replacements remain usable.
Host destruction before the next Display connection must retain one termination
callback. These checks use the existing `AndroidPlatform` implementation.

The original recovery suite remains in the same runner. It checks native
geometry, pointer and modifier state, primary-contact cancellation, reentrant
focus, image drawing, antialias settings and ancestor disabling. The checkbox
multi-pointer sequence also checks that removing a secondary contact preserves
the primary gesture and that completing it activates exactly once.

The runner writes four real visible-window images using SWT
`new GC(display).copyArea(...)`: `nebula-android-before.png`,
`nebula-android-recovery.png`, `nebula-android-after.png` and
`nebula-android-recreated.png`. The existing native acceptance workflow records
both source revisions and the receiving SWT JAR hash, retains installed APKs
while its exit trap retrieves all four app-specific screenshots with adb, then
requires and decodes each file. Missing, duplicate, corrupt, blank or repeated
frames fail validation. A failing native test keeps its original exit status,
and available screenshots and execution logs are retained with the test reports.
When Gradle supplies `additionalTestOutputDir`, the before, after and recreated
captures are also written there for Gradle collection. The original recovery
capture stays in the app-specific external directory. These visible-window
captures are separate from offscreen `Control.print` output.

A successful compilation or APK assembly is **not** an executed native test.
Record the actual device/emulator result separately; merely adding these
assertions or declaring the workflow does not establish a pass.

## Native pixel move (NDK)

The SWT Android backend moves retained pixels (a `Canvas.scroll`) through one native call when
`libswtpixels.so` is in the application and through a Java lane when it is not. Build the
library in the SWT checkout with an Android NDK and copy it into this application:

```sh
python /path/to/eclipse.platform.swt/mobile/android/build_native.py \
  --ndk /path/to/android-ndk --api 35 --output /path/to/native
cp -r /path/to/native/jniLibs app/src/main/
```

`app/src/main/jniLibs` is ignored by git: the libraries are build output. The Android Gradle
Plugin packages them into the APK from there.

`PixelsNativeProbe` is a second instrumentation in the test sources. It calls the native move
and the Java lane for 400 random rectangles and distances on opaque bitmaps, compares both
with a plain model, checks that a target outside the bitmap is refused, and times one large
move both ways. Build the test APK with it as the runner and run it:

```sh
gradle -p widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.mobile/android \
  -PswtMobileJar=/absolute/path/to/swt-mobile-android.jar \
  -PtestRunner=org.eclipse.nebula.examples.mobile.PixelsNativeProbe \
  :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w \
  org.eclipse.nebula.examples.mobile.test/org.eclipse.nebula.examples.mobile.PixelsNativeProbe
```

`INSTRUMENTATION_CODE: -1` is a pass. Without `-PtestRunner` the runner is
`NebulaMobileInstrumentation`, as before. The probe reads no screen, so it also runs on an
automated test device image (`aosp_atd`); the screenshot checks of `NebulaMobileInstrumentation`
need an image that renders (`default` or `google_apis`): on `aosp_atd` every capture is black.

Executed on 2026-11-16 on an API 35 x86_64 `aosp_atd` emulator with NDK r30: library loaded,
400 of 400 cases equal for the native move and for the Java lane, a 2048 by 1984 scroll in
5.6 ms natively against 57.9 ms through the Java lane. With the library left out of the APK the
probe reports it as not loaded and returns `INSTRUMENTATION_CODE: 0`.

The iOS target uses the same `NebulaMobileMain` and SWT's existing
`IOSPlatform.launch` bootstrap. It still requires an actual linked iOS
Java runtime, Apple SDK build and simulator/device tests.

This remains one bounded mobile widget slice. The full Nebula Grid, XViewer
and complete SWT control catalogue are not part of this build.
