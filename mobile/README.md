# Nebula Grid mobile viewport projection (candidate)

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
