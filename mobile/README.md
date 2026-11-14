# Original Nebula widget on SWT mobile

This application executes the original `RoundedCheckbox`, `SelectionListenerUtil`,
and `ReflectionUtils` source files through the SWT mobile platform. It does not
substitute another checkbox implementation. The application entry is
`org.eclipse.nebula.examples.mobile.NebulaMobileApplication`, with ordinary SWT
Shell, GridLayout, Canvas, and SelectionListener use.

This is an initial Canvas/Composite/Shell integration path. It is not the whole
Nebula widget collection, nor the complete SWT desktop API. See the SWT
`mobile/README.md` for the supported source closure and remaining widget families.

## Reproducible SDK build through Maven

Install JDK 21, Maven 3.9 or later, Python 3.10 or later, Android SDK platform 35,
and Android build-tools 36.0.0. Set `JAVA_HOME` to the JDK. The build does not
install or update SDKs. It uses the SDK's actual D8, aapt2, zipalign, and apksigner
through a Maven-controlled product build script; no Gradle or Kotlin is required.

**Android 15 / API 35 is the minimum and target for this initial closure.**
Unchanged common SWT sources use runtime APIs beyond older Android versions.
There is no core-library desugaring layer, and compiling Java 21 syntax does not
prove that every Java 21 library API exists on Android. The builder rejects a
lower `--min-api`.

```sh
mvn -f /path/to/nebula/mobile/pom.xml \
  -Dswt.checkout=/path/to/swt \
  -Dandroid.sdk=/path/to/android-sdk verify
```

The SWT checkout must contain its `mobile/source-manifest.json` and actual
Android platform sources. The builder verifies the pinned unchanged SWT/Nebula
source hashes, compiles the selected owners and application with `--release 21`,
and produces:

- `mobile/target/swt-nebula-mobile-debug.apk`
- `mobile/target/app-classes.jar`
- `mobile/target/nebula-mobile-sources.jar`
- `mobile/target/build-report.json` and `build.log`

The report binds actual source and output hashes and labels native qualification
as not run by the build. A failed rebuild removes previous executable/report
outputs so they cannot be mistaken for fresh evidence. No device installation or
instrumentation run is hidden inside `verify`.

The APK is always debuggable and signed only with a locally generated, publicly
passworded Android debug key. The builder has no release-keystore parameter and
refuses an existing key without its generated-debug identity marker. It never
uploads or publishes an application. Reusing the same output directory retains
that debug key; `mvn clean` removes it and creates a new certificate on the next
build. A new certificate changes the signed APK bytes and may prevent replacing
an already installed application signed by the old key. The build process and
source/class JARs are reproducible; APKs generated with different debug keys are
not expected to have identical bytes.

Use `-Dmobile.output=/path/to/build-directory` to select an output directory.
On Windows, use `-Dpython.executable=python` if necessary. Paths with spaces must
be quoted in the shell. The Python bridge also accepts explicit `--swt`,
`--nebula`, `--output`, `--java-home`, and `--sdk` arguments for diagnostics; Maven
is the normal public build entrypoint.

## Android instrumentation is a separate executed gate

```sh
mvn -f /path/to/nebula/mobile/pom.xml \
  -Dswt.checkout=/path/to/swt \
  -Dandroid.sdk=/path/to/android-sdk \
  -Dmobile.instrumentation=true verify
```

This variant produces `swt-nebula-mobile-instrumentation.apk`. It includes only
the Android instrumentation source explicitly selected in the manifest, along
with its non-exported transport-test Activity. The normal application variant
contains neither component. SWT's headless Java2D test transport is excluded
from both APKs.

After choosing an actual API 35 device or emulator, execute the platform harness
explicitly, using that device's serial:

```sh
adb -s DEVICE_SERIAL install -r /path/to/nebula/mobile/target/swt-nebula-mobile-instrumentation.apk
adb -s DEVICE_SERIAL shell am instrument -w \
  org.eclipse.swt.mobile.probe/org.eclipse.nebula.tests.mobile.AndroidMobileInstrumentation
```

Accept only an actual successful instrumentation result. The harness sends native
input, exercises original SWT/Nebula ownership and lifecycle behavior, and captures
visible pixels through SWT `GC(Display).copyArea(...)` backed by Android PixelCopy.
Its PNGs are diagnostic evidence, not a replacement for executed assertions.
Android Instrumentation is the OS test harness for this lane; the portable owner
contracts separately run as genuine JUnit 5 tests through SWT Maven/Surefire.

The manifest opts into the native Back dispatcher. System Back remains the
vetoable SWT Shell.Close operation; final OS Activity destruction is a separate,
non-vetoable termination of the SWT application thread.

## iOS application bytecode input

First build the SWT Java/JNI source closure:

```sh
mvn -f /path/to/swt/mobile/pom.xml -Dmobile.platform=ios clean verify
mvn -f /path/to/nebula/mobile/pom.xml \
  -Dmobile.platform=ios \
  -Dswt.checkout=/path/to/swt \
  -Dswt.mobile.jar=/path/to/swt/mobile/target/swt-mobile-ios.jar verify
```

This creates `nebula-mobile-application.jar` containing the original Nebula
classes and application entry, and a source JAR. It checks the supplied SWT iOS
JAR for the real shared owners, ACC, IOSPlatform, and OS classes. It does not
supply or select an unverified AOT/JVM provider, produce a signed iOS application,
or claim an Apple simulator/device pass. The separate SWT `mobile/ios` lane owns
UIKit/native compilation and runtime integration.

## Licensing and source selection

The application uses the existing upstream files byte for byte. The selected
original Nebula source hashes and baseline are recorded by SWT's public source
manifest, so the SWT portable contract suite can use a baseline Nebula checkout
without a circular dependency on this example module. The APK/JAR retains SWT
and Nebula licensing notices. These files are product build/test outputs;
private reusable source transformation recipes remain with their canonical
Synexia owner.
