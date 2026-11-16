# M3 CI composition receipt — Nebula Grid PR #495 + Hyperlink PR #496

## Purpose

Compose the exact, nonoverlapping **test-only** source postimages from two open draft PRs on one isolated integration branch, without modifying those PRs, any production file or the protected `master` branch. This composition is for hosted CI qualification only; it is not a production merge recommendation.

### Baselines and source identity

- Protected fork branch: `master`; pinned SHA: `cbc0a8c09eb863fb4cf28cb4d646dd0add9b8008`.
- Grid test repair donor: [Nebula PR #495](https://github.com/hsoliwal/nebula/pull/495), head `95acdad4f179f1874a7769a74995faf164c95e60`. Existing Java/Maven run #43823082911: Grid test module **213 run, 0 failures, 0 errors, 0 skipped**, but reactor failed later in Hyperlink.
- Hyperlink test timing donor: [Nebula PR #496](https://github.com/hsoliwal/nebula/pull/496), head `1b1a65a9b37a8e40dc457124551c8be2effed87f`. Its Java/Maven run #43825151043 failed at *old Grid* because PR #496 lacks the separate #495 patch; the Hyperlink tests were never reached.
- Both PRs share the identical pinned base commit; donor changes touch disjoint paths.

## Exact Git blobs: base to donor

| Donor | Original file | Base blob | Donor blob |
|---|---|---|---|
| #495 | `widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridBlitScroll_Test.java` | `6b5228e0a0c67a2950a4c163a76f7c70358cf2c0` | `da33f527ab4ad3626dd20867bc422026f76b460b` |
| #495 | `widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridPinnedRows_Test.java` | `04b7fa0cd2ff29cedbac1791a244df8f4751f07f` | `5d97203094b9e2e5a8929200718424d28dc46346` |
| #495 | `widgets/grid/org.eclipse.nebula.widgets.grid.test/src/org/eclipse/nebula/widgets/grid/GridSwtScreenshotCapture.java` | `603d95bd45fcc13622808663c4ddc01f2130303b` | `4992e75f047e002060efc7f80d5ecb94d753f3f9` |
| #496 | `widgets/hyperlink/org.eclipse.nebula.widgets.hyperlink.tests/src/org/eclipse/nebula/widgets/hyperlink/HyperlinkTest.java` | `b185c3404d82291b2a3d4f5a5d188531ebd15ff6` | `e807a5352108c0674c9d710c8955909f99c01d56` |
| #496 | `widgets/hyperlink/org.eclipse.nebula.widgets.hyperlink.tests/src/org/eclipse/nebula/widgets/hyperlink/TestSupport.java` | `df3d21b8a0cd009397e4ea96ce2807be43034c5a` | `06aff05fc17372cb948cb6d0c4f46b811dbf1bc4` |

Each **base and donor blob SHA was independently re-read from GitHub** on the pinned commit before branch creation. All five matched exactly. No public signatures, JNI, Java production code or native widgets are changed by this composition.

## Patch and verification conditions

1. Create receipt commit **before code**.
2. Overlay exactly five donor blob IDs on the receipt tree; verify a full-tree diff contains exactly five changes and that all postimages equal the donor's blob SHA.
3. Preserve base `master` history and no external branch mutation.
4. Publish a **draft** integration PR; do not merge. Do not close or force-update PR #495 or PR #496.
5. Hosted CI must compile and execute both Grid and Hyperlink tests together. No passing claim until the combined PR-head run actually reports success.
6. If Hyperlink still fails despite bounded real `SWT.Paint` awaiting, inspect `HiddenGate`'s SWT.Show ordering, especially GTK's pre-native Show event, and create a separate source fix only after a repro. Do not mask it with sleeps, skipped tests or weakened counts.
7. Review GTK3, GTK4, Win32/Cocoa and native Android/iOS separately. No broad portability claims from one Linux JVM run.

## Previous failures preserved as evidence

- PR #491/492 Grid failures: native GTK overlay pixels affected exact screenshot hashes (two failures). PR #495 isolated native scrollbar overlay and reached 213/0 Grid tests before the unrelated Hyperlink failure.
- PR #496 still hit old Grid tests; no Hyperlink conclusion is supported.
- GTK3 viewport workflow original mode encountered third-party Maven installer HTTP 429. Rate limiting is not a compiler or behavioral failure.

**Gate state at receipt creation:** inventory, hashes and documentation pass; composition, code build, lint, tests, runtime and PR review pending.
