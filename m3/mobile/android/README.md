# Nebula on the SWT Android host

This app builds the **existing RoundedCheckbox source unchanged** against the
SWT mobile backend. The source closure also includes the unchanged
`SelectionListenerUtil` and `ReflectionUtils` from Opal commons. It does not
create an Android-specific checkbox implementation or copy those widget sources
into a competing package.

`m3/mobile/common/src/main/java/org/eclipse/nebula/examples/mobile/NebulaMobileContents.java`
creates selectable, selection-vetoed/grayed and disabled controls through their
ordinary SWT/Nebula APIs. Android hosts that shared demo body in `SWTActivity`.

## Build

Use the SWT fork containing the mobile sources, JDK 21, Python 3.10+, Android
platform 35 and build-tools 35.0.0:

```sh
python3 /path/to/eclipse.platform.swt/m3/mobile/android/build-apk.py \
  --swt-checkout /path/to/eclipse.platform.swt \
  --nebula-checkout /path/to/nebula \
  --android-sdk /path/to/android-sdk \
  --jdk /path/to/jdk-21 \
  --output /path/to/nebula-android-build
```

The output contains a debug-signed application, a native instrumentation APK,
source/tool/artifact hashes and explicit build results. Native execution stays
`NOT_RUN` until the separate instrumentation succeeds.

The alternative AGP 8.13.0/Gradle 8.13 project uses:

```sh
gradle -p m3/mobile/android -PswtCheckout=/path/to/eclipse.platform.swt \
  :app:assembleDebug :app:assembleDebugAndroidTest
```

## Actual Android verification

```sh
python3 /path/to/eclipse.platform.swt/m3/mobile/android/run-instrumentation.py \
  --build-output /path/to/nebula-android-build \
  --adb /path/to/android-sdk/platform-tools/adb \
  --serial emulator-5554
```

The native runner requires actual device-screen pixels, injected Android touch,
selection changes, selection veto, disabled-state behavior, and disposal after
Activity destruction. It captures before/after PNGs. The test executes the
original widget and the real Android renderer, with no Android/SWT API mocks.

This proves only the exercised mobile slice when its native receipt passes.
Other Nebula widget families and their SWT dependencies still need a mobile
implementation and their own native acceptance. This app does not qualify Grid,
XViewer, browser widgets, accessibility, IME, or the complete Nebula Tycho reactor.
See the SWT host's `m3/mobile/android/README.md` for platform/API limits.
