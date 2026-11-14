# Android native execution gate

The existing `.github/workflows/maven.yaml` control plane includes a separate
Android job for the mobile source set. Its original desktop job is unchanged.
Same-repository pull requests run the emulator only when `mobile/`, the original
RoundedCheckbox subtree, its two selected Opal support classes, or the shared
workflow changes. An explicit workflow dispatch also selects this gate. There
is one emulator job, no architecture matrix, and a 45-minute job limit.

`mobile/swt-revision.txt` must contain exactly the published SWT commit: 40
lowercase hexadecimal characters, with an optional final newline. Branch names,
tags, missing pins and placeholders fail before source checkout. Update that
file when deliberately qualifying a new SWT implementation. The job checks out
that exact commit and verifies the resulting HEAD.

The job selects Temurin `21.0.13+9`, Maven `3.9.11`, Android platform `35`,
build-tools `36.0.0`, and `system-images;android-35;default;x86_64`. It uses the
hosted SDK's already accepted licenses; it does not run a license-acceptance
command or pipe approval responses to SDK Manager. Missing packages or license
acceptance stop the job explicitly. SDK/emulator package revisions, build input
hashes and emulator fingerprints are retained with the result.

After the real Maven instrumentation-APK build, `run_instrumentation.py` creates
one isolated AVD using SDK `avdmanager`. It requires a usable `/dev/kvm`, confirms
KVM with `emulator -accel-check`, and launches with `-accel on`. A software VM
fallback is rejected. The GPU backend is SwiftShader; this gate does not qualify
physical GPU performance. The script gives boot and instrumentation five minutes
each, and always terminates its own emulator process group. It does not terminate
other devices or reuse a pre-existing emulator serial.
Its adb server uses a private port and temporary credentials, leaving an existing
host adb server alone.

Run the same product driver on a Linux x86_64 KVM host with the SDK/image already
prepared and a freshly built instrumentation APK:

```sh
python3 mobile/android/run_instrumentation.py \
  --sdk /absolute/path/to/android-sdk \
  --apk mobile/target/swt-nebula-mobile-instrumentation.apk \
  --output mobile/target/native-proof \
  --swt-revision ACTUAL_40_CHARACTER_SWT_COMMIT \
  --nebula-revision ACTUAL_40_CHARACTER_NEBULA_COMMIT
```

Replace both revision placeholders with actual commits. The output directory
must not already exist. Each invocation starts from a fresh AVD and cleared app
data, preventing previous screenshots or PASS records from becoming current
evidence. The script installs only the supplied debug test APK on the emulator
it created.

## Acceptance and retained evidence

A successful adb process is insufficient. The driver requires exactly one
`status=PASS`, the expected original-widget scope, at least 30 executed checks,
and exactly one terminal `INSTRUMENTATION_CODE: -1` from the actual custom Android
Instrumentation harness. Missing, duplicate, failed or aborted results are
rejected. The harness owns native input, original SWT/Nebula event and lifecycle
assertions, and the actual `GC(Display).copyArea` pixel comparisons.

The driver retrieves the three widget screenshots from that fresh app's data,
validates their PNG checksums, image streams and matching dimensions, and rejects
unchanged selected/unselected evidence. These files came from the harness's
visible-window PixelCopy path. The separately labelled
`emulator-at-completion.png` is diagnostic: the fixture may already have closed
when that full-screen capture is taken.

The artifact contains the instrumentation result, stage-by-stage
`NOT_RUN`/`RUNNING`/`FAILED`/`PASSED` report, APK/source identities, SDK package
versions, emulator output, logcat, device properties, widget captures, build logs
and debug APK. Retention is seven days. A boot/infrastructure failure leaves the
instrumentation stage `NOT_RUN`. Parser checks, a compiled APK, a queued workflow,
or a job that never started are not native execution passes.

This remains the initial original RoundedCheckbox/Shell/Composite/Canvas path.
It does not qualify the remaining SWT/Nebula widget catalogue, iOS, general
accessibility/IME behavior, or independent CPU/heap/native-memory budgets.

SDK tool contracts are documented in the Android Developers references for
[AVD Manager](https://developer.android.com/tools/avdmanager),
[emulator acceleration](https://developer.android.com/studio/run/emulator-acceleration),
and [instrumentation command-line execution](https://developer.android.com/studio/test/command-line).
