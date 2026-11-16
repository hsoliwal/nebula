# WebCanvas physical clock receiver

This receiver makes automatic animation respect native ancestor visibility, positive widget size, shell minimization and disposal. After the model animation query returns, it checks current eligibility again and coalesces reentrant scheduling before arming the timer. Explicit offscreen painting remains available.

The bundle contains all six recipe documents needed by the root recipe. It applies the original clock mediation, the post-query lifetime repair, the scoped opened-clock tests and the new admission tests in order. No POM or dependency change is required by these text recipes.

## Apply through Maven

Run from the Nebula repository root in an admitted build/output lane. Keep the repository's current temp/cache routing, storage reserves and output limits. The bundle path is:

`tools/m3/webcanvas-physical-clock-20261116/receive-scoped-webcanvas-physical-clock.yml`

Preview the exact recipe:

~~~sh
mvn org.openrewrite.maven:rewrite-maven-plugin:5.23.1:dryRun -Drewrite.configLocation=tools/m3/webcanvas-physical-clock-20261116/receive-scoped-webcanvas-physical-clock.yml -Drewrite.activeRecipes=com.synexia.m3.ReceiveScopedWebCanvasPhysicalClock20261116
~~~

Apply it after reviewing the preview:

~~~sh
mvn org.openrewrite.maven:rewrite-maven-plugin:5.23.1:run -Drewrite.configLocation=tools/m3/webcanvas-physical-clock-20261116/receive-scoped-webcanvas-physical-clock.yml -Drewrite.activeRecipes=com.synexia.m3.ReceiveScopedWebCanvasPhysicalClock20261116
~~~

The single configuration file supplies every child recipe; separate recipe-resource installation is unnecessary. Plugin 5.23.1 is the existing Synexia recipe-owner coordinate. These Maven commands are receiver instructions, not an executed Maven qualification. The filesystem Maven lifecycle and repository-module path interpretation remain a separate verification step.

The transformations use exact full-file preimages and paths. Drifted files remain unchanged. The new admission test uses `overwriteExisting: false`, so an occupied test path is preserved. On the qualified final images, same and fresh recipe replay make zero changes.

## Qualified changes and tests

The final WebCanvas source is Git blob `756141e76c3f7b19804443295bc50316f0c9e19f`. Its public and protected API remains unchanged. The shared damage/blit prefix, existing paint rendering and two ordered size queries are preserved.

The receiving test files are:

- `WebAnimationVisibilityTest.java`: 19 opened-clock tests, blob `feb952dde74d70d8aedcc2a4da7b914515a2edad`.
- `WebClockAdmissionAtomTest.java`: 16 admission, listener identity, rollback, resize/error-order and wake-query reentry tests, blob `bdc3c188f8ab6816f5e2ee7b166f001d62b49882`.

Both fixtures declare their scope before creating a Display: Win32 and `SWT.getVersion() == 4974` (the qualified SWT 3.135.0 build). Other runtimes are skipped and remain unqualified by these fixtures. Strict font-registry ownership, exact timer/listener identities and disposal assertions remain in place; no reflection failure or unknown resource is waived.

The bounded RAM/native run passed the 19 and 16 tests plus six stock-widget cases: 41 fresh cases, zero failures and zero assumption skips on the qualified runtime. It also loaded this complete bundle through one `YamlResourceLoader`, checked all five stock recipe leaves, produced the three exact receiving paths and proved replay, occupied-path, drift and foreign-path preservation. The source comparison retained 938 API entries and three serialization UIDs across 47 production classes.

The earlier repaired-source run separately passed 17 overlap and 18 exact-clip regressions. Its blocked-clock diagnostic measured 101,000 ordinary animation deltas after the first native denial: no additional visibility/shell/size queries, timer calls or paint emission. Ordinary damage/blit reads and delta generation still occur. Allocation readings and timings are bounded observations, not zero-allocation, speedup or UI-latency guarantees.

## Evidence and limits

- Bundle: `ac24c2f0620d7c90b86929fe542d348f165429ea`.
- Final runtime: `6226309656a2b8c25110f5e2ffcbf0662a039ac4`.
- Extracted final result: `bb18ed3dabe7b5c19dc9fda9a9d39ddbff8d1058`.
- Exact bundle bindings: `7b83007ab7bd02892f58fbea3eac8d09861b60d2`.
- Stock observations: `0d5a6c7551d211fefffcda6fb1b38adc4ec62e68`.
- Earlier repaired-source result: `8ea93525c186090646ac51ba0a758c48b5653c59`.

The executed recipe proof used the existing cache coordinate 8.17.1. That cached binary reports package implementation version `8.17.0-SNAPSHOT`; pristine upstream release provenance is not attested. No Maven/JShell filesystem lifecycle, JNI, Skija/GPU, RAP/GWT, SCREEN capture or all-platform qualification is claimed. The broader all-web-widget work remains incomplete.
