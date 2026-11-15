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

The application includes an additional **Table** button that opens an
owned native SWT Shell containing the original `Table`, `TableColumn` and
`TableItem` mobile implementations. It starts at logical row
999,999,990 of a one-billion-row virtual model, materializes rows via
`SWT.SetData`, and handles column-header selection, row selection and native
accessibility paging without enumerating the entire model. The original
RoundedCheckbox, Text, List and Combo remain on their existing screen.
Android instrumentation checks the native GridView role, bounded visible
data loading, exact row/header events and selection-neutral accessibility
scrolling. iOS uses the same shared Java Table consumer, but still requires
Apple SDK/device and embedded-Java runtime qualification.

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

The test also invokes the Android SeekBar accessibility increment provided
by [SWT #378](https://github.com/hsoliwal/eclipse.platform.swt/pull/378),
checking that it produces exactly one original SWT Selection. iOS now exposes
the corresponding UIKit adjustable accessibility methods, but its simulator/
VoiceOver device qualification remains open. There is no competing SWT toolkit.

## Compile and package

Prerequisites: JDK 21, Maven 3.9+, Python 3.10+, Android platform 35,
Android build-tools and Gradle 8.13+. The mobile source closure currently
requires Android 15 / API 35; compatibility with older Android is not claimed.

1. Build the SWT Android Java artifact from a checkout that includes #369:

   ```sh
   mvn -f /path/to/eclipse.platform.swt/mobile/pom.xml \
     -Dmobile.platform=android -Dandroid.sdk=/path/to/android-sdk clean verify
   ```

2. Build both APKs in this Nebula checkout:

   ```sh
   gradle -p widgets/roundedcheckbox/org.eclipse.nebula.widgets.roundedcheckbox.mobile/android \
     -PswtMobileJar=/path/to/eclipse.platform.swt/mobile/target/swt-mobile-android.jar \
     :app:assembleDebug :app:assembleDebugAndroidTest
   ```

`NebulaMobileActivity` extends `org.eclipse.swt.internal.android.SwtActivity`.
Its manifest names `NebulaMobileMain`, which constructs the original widget
tree and runs the ordinary SWT event loop on the Display thread.
The application source is compiled against the exact supplied SWT Android JAR,
not a second local `:swt` Gradle project.

## Native acceptance

Install the resulting application and instrumentation APKs with `adb`, then
run the `org.eclipse.nebula.examples.mobile.NebulaMobileInstrumentation`
instrumentation runner on an API-35-or-newer emulator or device. The test
uses real Android screenshots and injected touch input to examine paint,
selection, selection veto, disabled state, Scale-to-ProgressBar interaction
and cleanup. A successful compilation or APK assembly is **not** an executed
native test.

The iOS target uses the same `NebulaMobileMain` and SWT's existing
`IOSPlatform.launch` bootstrap. It still requires an actual linked iOS
Java runtime, Apple SDK build and simulator/device tests.

This remains one bounded mobile widget slice. The full Nebula Grid, XViewer
and complete SWT control catalogue are not part of this build.
