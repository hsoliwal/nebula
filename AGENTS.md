<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula M3 agent instructions

For reusable M3/OpenRewrite source transformations, hsoliwal/com.synexia is the canonical recipe
owner and Nebula is the proving/product target. Do not create a competing Nebula recipe when an
equivalent admitted Synexia recipe exists.

For FILE-local atomization/patternization/IOP/documentation, use the exact Synexia binding recorded
in m3/catalogue/pure-int-recipe-binding.tsv. The compatibility profile
m3-local-file-convergence invokes com.synexia.rewrite.M3PureIntConvergenceRecipe.
Nebula-local pure-int recipe classes remain historical proving fixtures only.

Preserve the canonical sequence:

INVENTORY -> ATOMIZATION -> PATTERNIZATION/IOP -> DOCUMENTATION -> FIXED_POINT.

Small recipes may be composed into the canonical content-addressed DAG. Camel may execute admitted
dependency waves, Airflow may schedule/export that same DAG, and Drools/KIE may perform candidate
admission. None receives semantic-equivalence, direct source-mutation, or promotion authority.

Nebula's original Maven/Tycho build and tests remain product authority. A Synexia recipe handoff is
candidate machinery only; Nebula must still prove compile/tests/runtime behavior and contract
preservation before promotion. Repeatable target defects feed back into the canonical Synexia
recipe/corpus before broad reapplication. Never rebase or rewrite useful proving history.

## Automated native-widget acceptance

Use SWT's visible-screen capture API, `new GC(display).copyArea(image, x, y)`,
after painting settles. Keep Control.print evidence separately labelled. Reuse
the existing GridSwtScreenshotCapture, JUnit scene/ownership tests and OpenCV
validator; do not require manual screenshot sign-off. Reject missing, skipped,
failed or duplicate test evidence, blank/stale/corrupt frames, wrong bounds and
geometry, and stale PASS receipts. Preserve independent CPU/heap/native-memory
budgets and the distinction between default and explicitly enabled models.

The combined SWT/Nebula runner must execute BOTH targets' pixel validators and
SWT's resource gate. Record each stage as NOT_RUN, RUNNING, FAILED or PASSED;
never infer native execution from a source check or an earlier run. Fixture-only
validator tests and unavailable platforms remain explicitly unqualified. Keep
shared improvements and exact recipe/source/test provenance in Synexia.

## SWT naming for distilled capabilities

Keep product APIs and generated runtime code in SWT terminology: Canvas, GC,
Image, Control, Table, Tree, ScrolledComposite, ScrollBar, and their existing
packages/contracts. Adapt donor ideas into these owners; do not introduce Swing
widget/facade names or a second toolkit naming layer. Donor names belong in
provenance. Preserve already-existing interoperability APIs and third-party
notices; this rule does not authorize renaming or removing compatible APIs.
