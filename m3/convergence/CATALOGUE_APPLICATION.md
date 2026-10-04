# Explicit OpenRewrite catalogue application

This extends `m3/convergence/verify.sh`; its existing no-argument analysis path stays unchanged. This is execution wiring, not a new atomizer, patternizer, catalogue, or transformation engine.

## Required operation

`bash m3/convergence/verify.sh --apply-catalogue PLAN_JSON OUTPUT_DIRECTORY`

The explicit mode must use the actual Maven OpenRewrite `runNoFork` goal on the original Nebula reactor, never a dry-run substitute, template-copy installer, or mock SDK. Select recipes by their real fully qualified names and pinned artifact coordinates from https://docs.openrewrite.org/recipes or existing first-party Synexia recipe owners. Parameterized recipes are supplied through an optional checked-in YAML configuration. Recipes execute in separate Maven invocations so downstream scans reparse materialized predecessors.

A plan contains schema 1, an exact reviewed base revision, explicit allowed non-root path globs, and ordered recipe rows containing `name`, `artifacts`, and optionally `config`. No dynamic LATEST/RELEASE/version ranges, arbitrary shell commands, automatic upgrades, root-file changes, deletions, or automatic promotion. The current Nebula-local artifact remains built through `m3/reactor.xml`; no duplicate recipe implementation or Maven reparenting is introduced. First-party artifacts from com.synexia and its M3Scale/tooling repositories must be installed and version-compatible before selection; repository ownership alone is not proof of classpath compatibility.

Admission requires a clean named candidate branch, an unchanged Git index/HEAD during execution, base ancestry with only this convergence wiring changed since the reviewed base, and fresh external evidence output. Save input hashes before any build or transformation. Stop on the first failed command. Preserve failed candidate files for inspection rather than resetting or deleting them.

Order: recipe-tool build -> original-repository baseline gates -> each selected recipe application -> diff hygiene and original Maven validate -> original compile -> original tests -> original verify/runtime -> next recipe. Baseline gates and non-mutating verification must not change source bytes. After every selected recipe is verified, replay each in a fresh Maven process and require zero file/mode changes, including newly generated non-ignored files. An entirely unchanged first pass is not evidence of a transformation.

Capture exact command vectors, exit codes, stdout/stderr, source pre/post hashes, changed-path inventory, tracked and new-file patches, and terminal status. Snapshot coverage is Git-tracked plus non-ignored untracked files; it is not a filesystem sandbox. Existing project validation, tests and runtime remain authoritative. Maven validate and diff hygiene alone must not be advertised as comprehensive Java static analysis or API-equivalence proof. No source is merged or automatically committed by this entry point.

## Verification boundaries

Launcher tests may use a process double to test routing, failure order and source-custody checks. They must be labelled as such and never counted as real OpenRewrite, Java, Tycho, SWT, JNI or whole-repository proof. The existing narrow Nebula convergence recipe is not relabelled as general cross-file method extraction. Full structural atomization, API/ABI invariance and platform/runtime promotion remain separate required evidence.

Pinned integration baseline: `hsoliwal/nebula@887cf8256ab471425bc631334d9e23d24726d34e`.
Original launcher Git blob: `9b811f683de78b318d9968e63b211134e7f8ff06`.
Original root POM Git blob: `756ee5408447cc2143be47099b1a1443dbe2e360`.
Plugin version already selected by Nebula: `6.46.1`; no plugin version upgrade is authorized here.
