# Nebula mobile SWT consumer (review candidate)

The Android and iOS entrypoints instantiate the **original**, unchanged Nebula
`RoundedCheckbox` using the canonical SWT mobile Display/Control/Canvas source
merged in [SWT #369](https://github.com/hsoliwal/eclipse.platform.swt/pull/369).
No cloned widget source or substitute public SWT API is introduced.

## Entrypoints and platform boundary

- `common/.../NebulaMobileMain.java` implements normal
  `public static void main(String[])` with SWT `Display`, `Shell`, the
  original RoundedCheckbox and the SWT event loop.
- `android/.../NebulaMobileActivity.java` subclasses the merged
  `org.eclipse.swt.internal.android.SwtActivity`; the Activity manifest
  identifies `NebulaMobileMain` in `org.eclipse.swt.mobile.application`.
  The app requires the `swt-mobile-android.jar` compiled from SWT #369 and
  does not embed a separate SWT Gradle project.
- `ios/.../IOSNebulaDemo.java` delegates to the same main. A native iOS
  application must call the merged `org.eclipse.swt.internal.ios.IOSPlatform.launch`
  with this class after supplying its verified Java runtime and JNI registration.
  This PR **does not provide** an iOS Java runtime, signed app or device proof.

See [Android build and instrumented acceptance](android/README.md). Compiling
or packaging an Android APK is not the same as running native instrumentation.
The native acceptance fixture tests painting, pointer activation, selection,
veto, disabled state and disposal of this one original Nebula Canvas widget.

## Release / provenance status

This branch contains follow-up compatibility changes after the
[Maven/OpenRewrite-generated handoff](https://github.com/hsoliwal/com.synexia/pull/17423).
The older `SOURCE-RECEIPT.tsv` attests the **original handoff only**, not the
newer SWT #369 compatibility corrections. Its original path/output hashes
must not be read as verifying the current branch; regenerate the canonical
Synexia recipes/receipts, compile the current cross-fork heads, then execute the
Android native tests before making the PR mergeable as qualified production.

The complete SWT/Nebula widget catalogue is **not** implemented here:
Grid, XViewer, Table/Tree, native Text/IME, accessibility and other widgets
still need both platform backends and their own native integration checks.
No original desktop widget source has been modified by this candidate.
