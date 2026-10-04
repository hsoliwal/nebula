# Original-runtime acceptance, not helper-only success

The existing Grid test bundle includes GridVisibleRangeSupport_Test alongside
GridFixedColumn_Test. No original test, parent, dependency or widget source is removed.
Ten JUnit4 tests now execute the actual SWT Grid. Eight retain the reviewed runtime
visible-range/event/screenshot contracts; two current-master tests retain atomized
viewport-projection, selection-pattern and rendered-viewport behavior.

The runtime tests cover visible row/column deltas, shared event identity, listener
order, failure propagation, last-listener removal, real Paint callbacks, atomized
projection/selection patterns, and diagnostic viewport scenes. Reflection is used only
to invoke the existing private range calculation and inspect publication order
deterministically. No fake SWT classes or replacement Grid implementation are used.

The tests intentionally preserve the historical removedColumns payload. They must not
be used to admit the separate opt-in correction without corresponding contract review.

Run the original repository reactor with its full Grid test profile:

```bash
mvn -B -ntp -Pfull-grid-contracts clean verify
```

On headless Linux use an available Xvfb display. Do not add skipTests,
maven.test.skip, exclusions or ignore-failure flags to turn a failing build green.
The full-grid-contracts profile runs every existing *_Test.java Grid class.

After a successful clean build, Synexia M3NebulaRuntimeEvidence checks the actual fresh
Surefire reports. runtime-tests.tsv requires all ten exact visible-range method names,
plus nonempty reports from the five existing Grid test classes. Missing, stale, empty,
duplicate, skipped, failed, malformed, flaky or unsafe XML evidence is rejected.

## Screenshot evidence

The runtime suite emits four diagnostic PNG scenes under
`target/m3-visible-range-screenshots`:

- `01-top.png`
- `02-middle.png`
- `03-horizontal.png`
- `04-resized.png`

They are diagnostic evidence, not pixel-golden assertions. The paired admission gate
requires exactly those four files from the same fresh reactor run, validates PNG
signature/IHDR/dimensions/size/freshness/regular-file constraints and records SHA-256.
