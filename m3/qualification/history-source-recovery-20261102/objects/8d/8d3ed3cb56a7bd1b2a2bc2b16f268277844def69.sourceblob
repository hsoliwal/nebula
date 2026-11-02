# Original-runtime acceptance, not helper-only success

The existing Grid test bundle now includes GridVisibleRangeSupport_Test alongside
GridFixedColumn_Test. No original test, parent, dependency or widget source is removed.
Eight new JUnit4 tests instantiate the actual SWT Grid, test visible row/column deltas,
shared event identity, listener order, failure propagation, last-listener removal and
real Paint callbacks. Reflection is used only to invoke the existing private calculation
and inspect its publication order deterministically. The paint test separately exercises
normal SWT registration. No fake SWT classes or replacement Grid implementation are used.

The tests intentionally preserve the historical removedColumns payload. They must not
be used to admit the separate opt-in correction without corresponding contract review.
They do not add accessibility, all-widget, JavaFX or full platform-parity proof.

Run the original repository reactor with its full Grid test profile:

```bash
mvn -B -ntp -Pfull-grid-contracts clean verify
```

On a headless Linux runner, use an available Xvfb display. Do not add skipTests,
maven.test.skip, test exclusions or ignore-failure flags to turn a failing build green.
The full-grid-contracts profile runs all existing *_Test.java Grid classes, including
legacy classes previously excluded by the bundle's default POM. Any existing failure
remains an acceptance blocker, not permission to suppress that test.

After a successful clean build, the canonical Synexia M3NebulaRuntimeEvidence verifier
checks actual TEST-*.xml files in this bundle's target/surefire-reports directory.
The runtime-tests.tsv requires every one of the eight new method names, plus nonempty
reports from each of the five existing Grid test classes. This manifest is not a claim
of method-by-method coverage of all legacy tests. The full profile, actual build and
broader existing repository gates remain necessary.

The verifier rejects missing, stale, empty, duplicate, skipped, failed or malformed
reports, inconsistent counters, unknown not-run statuses, external entities, symlinks
and bounded-size violations. It accepts a build-start epoch in milliseconds; a fresh
successful clean build must precede admission. The result is a scoped report receipt,
not cryptographic attestation or a whole-repository completion flag.

The new UI tests have been syntax-parsed but not compiled against SWT/JUnit or executed
in this constrained authoring environment. The real Java report verifier is compiled
and tested against explicitly synthetic XML fixtures. Maven and GUI integration remain
separate, unpassed gates. The previous Java/JNI production checks are not relabelled as
SWT runtime success. Keep the PR draft until actual original-runtime checks pass.


## Screenshot evidence

The runtime class also captures four diagnostic PNG scenes under
`target/m3-visible-range-screenshots`:

- `01-top.png`;
- `02-middle.png`;
- `03-horizontal.png`;
- `04-resized.png`.

These are not pixel-golden assertions. The test asserts logical viewport behavior and
the screenshots make header, clipping, scrollbar and repaint regressions inspectable.
The paired Synexia acceptance job requires all four files from the same fresh reactor
run, validates PNG signature/IHDR/dimensions and records canonical SHA-256 evidence.
Missing, stale, symlinked, malformed, oversized or extra PNG evidence is rejected.
