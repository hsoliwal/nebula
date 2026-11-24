<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Original Nebula native Surefire failure inventory (Java 21)

This is a **diagnostics-only extension** to the existing Nebula native test build.
`NativeFailureIndex.java` enumerates original `widgets/**/target/surefire-reports/TEST-*.xml`
files after `Build All Widgets`, records their SHA-256 byte identities, and emits
`reports.tsv`, `cases.tsv`, and a short `summary.md`. Each method retains its
actual failure/error/skip status and original test-source identity. `NativeFailureIndexTest`
checks admission, exact counts, deterministic replays, stale/missing evidence,
symlink refusal, and XXE/DOCTYPE refusal under Java 21 with warnings as errors.

This does **not** replace or duplicate the existing 
`releng/swt-receiver/verify_gtk_host_tests.py` (which verifies source-pinned
**SWT** GTK AllTests) or the original `verify_grid_screenshots.py` (which compares
**Grid** native pixels with OpenCV). The new reader indexes all **Nebula** widget
failure reports after Tycho has already made its pass/fail decision.

## Verified baseline and current failure families

On 2026-11-24, the [exact PR #655 native Java CI run](https://github.com/hsoliwal/nebula/actions/runs/45356730882)
passed the independently run Java 21/JUnit4 `RemoteFrameDecode` and
`RemoteFrameWindow` tests, source-built/bound SWT and the dedicated native
`DrawnBrowser` reactor. The full `Build All Widgets` Tycho reactor completed
with failures in **10 modules**, but its original XML reports were uploaded
(308 `TEST-*.xml` files). The job rightly remained FAILED.

The Java CI log explicitly identifies 20 failing individual test methods, including:

| Owner | Representative original failure | Symptom, not proven root cause |
| --- | --- | --- |
| `opal/colorbutton` | `ColorButtonTest.colorChangeRepaintsTheSwatchOnly` | Native color pixel mismatch |
| `oscilloscope` | `OscilloscopeEnvelopeTest.envelopeColumnsCoverTheSamePixelsAsAllSamples` | Aliased/envelope geometry |
| `pgroup` | `CollapsiblePaneTest.collapsedContentIsAHiddenViewport` | Expected one paint, observed none |
| `progresscircle` | `BusyLabelTest.framePixelsEqualADirectPaint` | Direct-paint pixel mismatch |
| `pshelf` | `PShelfViewportTest.setTextDamagesOneHeaderAndCullsTheOthers` | Expected one header paint, observed none |
| `tablecombo` | `TableComboTests.testClosePopupAfterSelectionTrueClosesPopup` | Popup not visible after opening |
| `visualization` | `XYGraphSetterDamageTest.theErrorBarSettersShowAtOnce` | Error-bar pixels differ |
| `web` | `WebCanvasExactClip_Test.originallyUnclippedStateRemainsUnclipped` | Caller GC clipping ownership |
| `xviewer` | `XViewerViewport_Test.stateSurvivesScrollingAwayAndBack` | Viewport state hash differs |
| `vnc` | `VncViewerNativeTest.capturesDecodedRfbPixelsFromTheVisibleScreen` | Real screen pixels differ from RFB frame |

**Do not assert that all ten failures have one cause.** Some may be native
compositor timing, while others appear to be genuine paint/geometry behavior.
Importantly, [Nebula #662](https://github.com/hsoliwal/nebula/pull/662) and
[Synexia #19183](https://github.com/hsoliwal/com.synexia/pull/19183) already
contain a **draft** original VNC visible-screen fixture correction. Do not
introduce another VNC fixture or silently relax its strict RGB assertions.

## Design and security

* Java 21 only: standard JDK XML and SHA-256, no Maven/Gradle/Python dependency.
* Scan only regular (not symlinked) original Surefire XML files under `widgets`.
* Reject empty report sets, malformed XML, DTD/XXE, oversized reports and
  contradictory testcase status; do not synthesize tests, passes or skips.
* Bound individual XML files (32 MiB), total XML bytes (512 MiB), and reports
  (20,000). Omit raw huge stack traces from TSV; keep the raw Surefire artifacts
  in the existing original artifact upload, pinned by their SHA-256.
* Symptom labels are **heuristics for triage only**. The source XML remains the
  evidence for every test verdict. The Maven/OSGibooter result remains the
  authority for native test success or failure.
* Source files, native SWT controls, GC ownership and JNI are never changed by
  this helper. Its only write destination is outside the scanned checkout.

## Reproduce standalone

```sh
javac --release 21 -Xlint:all -Werror -d /tmp/nebula-native-index-classes \
  releng/swt-receiver/NativeFailureIndex.java \
  releng/swt-receiver/NativeFailureIndexTest.java
java -ea -cp /tmp/nebula-native-index-classes NativeFailureIndexTest
java -cp /tmp/nebula-native-index-classes NativeFailureIndex \
  "$PWD" /tmp/nebula-native-failure-index
```

The GitHub workflow compiles and runs the self-test, scans only already-present
original reports **after `Build All Widgets`**, then uploads the three generated
files even when the reactor has failed. It also appends `summary.md` to the
GitHub job summary. This **does not change the original native build command,
compiler, SWT source pin, Maven version, Tycho plugin, test selection, or fatal
result**. A missing report or unsafe XML makes the diagnostic step fail rather
than falsely declaring native acceptance.

## Admission status

**PASS in isolation:** exact Java 21 compilation (`-Xlint:all -Werror`),
22 assertions from synthetic representative JUnit XML, repeatable output across
11 runs, strict XML entity rejection, and source-package integrity.

**NOT RUN/HOLD:** the new diagnostic step against the complete 308 original
GitHub XML reports, the new hosted workflow, native GTK/Windows/Cocoa geometry
acceptance, and Maven/OpenRewrite SDK source replay. Keep the receiving PR
in draft until hosted artifacts and individual original widget fixes qualify.
