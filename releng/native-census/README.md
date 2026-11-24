# Full Nebula native Surefire failure census — Java 21

This is an additive diagnostic, NOT a product renderer or a release-test bypass.
Original native GTK3/X11 SWT/Tycho Maven failures remain fatal. This tool records
the exact JUnit XML suite and failing/skipped cases under their original widget
module so subsequent PRs can target their own source without guessing from logs.

## Reproduce without SWT or Maven

```sh
javac --release 21 -Xlint:all -Werror -d /tmp/native-census \
  releng/native-census/NativeSurefireCensus.java \
  releng/native-census/NativeSurefireCensusProof.java
java -cp /tmp/native-census NativeSurefireCensusProof
java -cp /tmp/native-census NativeSurefireCensus . /tmp/native-census-results
```

The JDK-only proof exercises deterministic sorting, error/failure/skip classification,
malformed/absent report rejection and DTD/external entity denial. Runtime collection
writes native-suites.tsv and native-cases.tsv through atomic replacement. It scans
only original **/target/surefire-reports/TEST-*.xml: max 16 MiB per report, 20,000
reports and 1,000,000 declared tests. No GUI objects, JNI calls, network or browser
connections are used.

## Pinned native baseline — November 24, 2026

Source: https://github.com/hsoliwal/nebula/actions/runs/45356730882
CPU Java 21/JUnit4: 10/10 PASS. Native DrawnBrowser: 409/409 PASS.
Full 372-module reactor: FAIL; 20 failing test methods across ten owner modules.
See FULL_REACTOR_20261124.tsv for the exact observed method identities.
Symptom-family labels are triage aids only, never established root causes.
The known VNC native-screen regression already has original-owner candidate
https://github.com/hsoliwal/nebula/pull/662 and source-sealed Synexia #19183.
Do not duplicate that candidate or weaken the exact screen-pixel assertion.

## Release gates

The CI census step runs after Build All Widgets under if: always(), then uploads
its two TSVs as a separate diagnostic artifact. The unchanged original native
Maven step, independent real JUnit4 CPU gate, required DrawnBrowser 409 cases,
Grid pixel/OpenCV checks, report retention, SWT source pins, and CodeQL remain
the authoritative release gates. A successful census means only that diagnostic
data was collected; it does NOT mark the full reactor green.

Missing or malformed reports make the collector fail closed. Skipped tests are
recorded as SKIPPED, never converted to PASS. Source-only JDK21/local tests
have passed; hosted CI collector execution, GTK4, Win32, Cocoa and repaired
native widget pixels remain NOT_RUN/HOLD until independently verified.
