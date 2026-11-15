# Nebula Grid mobile viewport projection (candidate)

## Release verification

The [release workflow](../.github/workflows/release-verification.yml) runs on
every `release/**` push and can be started manually for a selected branch. It
calls the existing desktop-widget, mobile-core, SWT-binding, Android-package
and original-widget Android/iOS workflows without their change-path filters.
Those workflows retain their exact SWT source pins; changing the Nebula branch
does not silently select a newer SWT build.

The desktop lane runs the Maven/Tycho widget tests and Grid pixel checks on GTK3
and retains all Surefire reports. Release calls explicitly enable tests and
make failures fatal, including the CDateTime, CWT and CompositeTable test modules
whose POMs skip tests during ordinary builds. Their known failures must be
resolved before a release pass; partial reports cannot stand in for those tests.
The mobile lanes cover portable Grid geometry
and ownership, Android debug APK assembly, original-widget Android
instrumentation, and iOS Java/UIKit/JNI compilation. A built APK is a test
application, not a signed release. iOS device execution and complete original
Grid API parity remain separate requirements. Interpret the actual reports for
the candidate being released; workflow configuration does not establish a pass.

## Portable Grid model

MobileGridViewport is an internal, array-backed row residency atom for a
future Android/iOS backend of the **original** Nebula Grid. It does not replace,
rename, wrap or alter the existing Grid widget public API. It supports long
logical row counts, fixed row geometry, O(viewport + overscan) retained physical
row slots, stable slot reuse, selection/dirty-region updates, native row release
and overflow-safe large-scroll arithmetic.

MobileGridViewBinding now also consumes SWT's MobileWidgetRuntime (exact
source pinned by the cross-fork CI workflow) and creates a bounded pool of
native clickable row views. It wires platform-native selection events, text,
positions, accessibility labels, row rebind, and lifecycle cleanup. This is a
working private native presentation receiver, **not original Grid API parity**.

Run bash mobile/test-core.sh for geometry/oracle regressions. With the SWT fork
checked out beside Nebula, run bash mobile/test-swt-integration.sh ../swt.
These prove portable Java and event ownership only. Native Grid painting,
columns, sorting, editing, accessibility, RTL, IME, touch, built Android APK
or iOS runtime, and full equivalent desktop behavior remain unqualified.
No upstream Grid source or public widget contract was modified.

## Android native-view demonstration

The standalone `mobile/android` Gradle application uses `MobileGridActivity` to
render 1,000,000,000,000 logical rows in three display columns while maintaining
only viewport + overscan native row buttons. It supports touch-motion scrolling,
selection callback and device Activity lifecycle transitions. Its SWT source
is pinned in `.github/workflows/m3-mobile-grid-android-apk.yml` to a known
mobile-host commit; to build locally, place that SWT fork checkout at
`./swt-mobile` relative to the Nebula repository root, install Android SDK 35,
then run `gradle -p mobile/android assembleDebug` with Java 17.

This is a **private demonstration app**, not a replacement implementation of
`org.eclipse.nebula.widgets.grid.Grid`. Android APK assembly, emulator/device
execution and production Grid API compatibility still require passing proof.
