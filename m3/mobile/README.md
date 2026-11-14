# Nebula mobile application

The Android and iOS entrypoints instantiate the same existing Nebula
`RoundedCheckbox` source through the new SWT mobile backend. The existing
`SelectionListenerUtil` and `ReflectionUtils` are included directly from their
original source locations. Product widget sources are unchanged.

The [Android build and native test](android/README.md) use the real Android SDK,
ART, native View/Canvas, screen pixels and input injection. The shared demo body
is `common/src/main/java/org/eclipse/nebula/examples/mobile/NebulaMobileContents.java`.

The iOS entrypoint is
`ios/src/main/java/org/eclipse/nebula/examples/mobile/IOSNebulaDemo.java`. An
embedding application must first supply its JavaVM and UIView to the SWT
`SWTIOSAttach` API, then invoke `IOSNebulaDemo.start()`. SWT runs its normal event
loop on a separate thread while UIKit owns the main thread. This source does not
provide the embedded VM, a signed iOS application or evidence that the complete
Nebula widget catalogue runs on iOS.

The acceptance scope is the exercised Canvas widget: paint, primary pointer,
selection, selection veto, disabled state and lifecycle. Grid, XViewer and other
widgets remain dependent on additional SWT mobile owners. Java compilation,
Android APK construction, native Android execution, iOS renderer tests and a
complete iOS Java application are reported separately.

The passive delivery receipt binds the exact emitted source to its canonical
Maven/OpenRewrite recipe in Synexia. No recipe tooling is needed at runtime.
