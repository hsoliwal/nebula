# M3 Nebula native CI integration receipt — 2026-11-16

## Scope and immutability
Purpose: run **existing** independent draft test fixes together, without modifying production code, replacing either draft, changing public APIs, or merging to `master`.

- Repository: `hsoliwal/nebula`, protected base `master` pinned at `cbc0a8c09eb863fb4cf28cb4d646dd0add9b8008`.
- Grid donor: PR [#495](https://github.com/hsoliwal/nebula/pull/495) exact head `95acdad4f179f1874a7769a74995faf164c95e60`; this branch **starts** at that commit and preserves its three test-file changes.
- Hyperlink donor: PR [#496](https://github.com/hsoliwal/nebula/pull/496) exact head `1b1a65a9b37a8e40dc457124551c8be2effed87f`. Only two changed JUnit test-ownership blobs are overlaid.
- No runtime Grid, Hyperlink, SWT, JNI, production, manifest, or widget source is changed by the integration overlay.
- Related canonical recipes and detailed original proof: [com.synexia #17897](https://github.com/hsoliwal/com.synexia/pull/17897) and [com.synexia #17901](https://github.com/hsoliwal/com.synexia/pull/17901).

## Preimage / donor provenance
Exact Git blob IDs independently retrieved via `fetch_file` for donor and protected-base Hyperlink paths. GitHub's native object IDs are retained instead of text reserialization.

| Path | master original Git blob | PR #496 donor Git blob |
|---|---|---|
| `widgets/hyperlink/org.eclipse.nebula.widgets.hyperlink.tests/src/org/eclipse/nebula/widgets/hyperlink/HyperlinkTest.java` | `b185c3404d82291b2a3d4f5a5d188531ebd15ff6` | `e807a5352108c0674c9d710c8955909f99c01d56` |
| `widgets/hyperlink/org.eclipse.nebula.widgets.hyperlink.tests/src/org/eclipse/nebula/widgets/hyperlink/TestSupport.java` | `df3d21b8a0cd009397e4ea96ce2807be43034c5a` | `06aff05fc17372cb948cb6d0c4f46b811dbf1bc4` |

Grid test sources from PR #495:
- `GridBlitScroll_Test.java`: `da33f527ab4ad3626dd20867bc422026f76b460b`.
- `GridPinnedRows_Test.java`: `5d97203094b9e2e5a8929200718424d28dc46346`.
- `GridSwtScreenshotCapture.java`: `4992e75f047e002060efc7f80d5ecb94d753f3f9`.

PR #495 real GitHub GTK3/Tycho Maven [run #43823082911](https://github.com/hsoliwal/nebula/actions/runs/43823082911) passed GridBlitScroll 1/1, GridPinnedRows 6/6 and MobileGridCanvasReceiver 2/2, then failed two Hyperlink assertions. Independent PR #496 [run #43825151043](https://github.com/hsoliwal/nebula/actions/runs/43825151043) failed on Grid's old pixel assertions before Hyperlink could execute. **No combined pass has been observed.**

## Contract and risk gates
- Only the two exact donor Hyperlink test blobs may change relative to PR #495 baseline, besides this receipt. Any other Git tree entry difference is a hard failure.
- Grid native on-screen comparisons must still reject off-scrollbar pixel deviations. Do not change widget painting behavior or weaken geometry bounds to make CI pass.
- Hyperlink tests must observe actual `SWT.Paint` callbacks, never synthesize a Paint event, and preserve hidden-state zero-paint and single-reveal contracts.
- Keep this PR in draft. It is an integration probe, **not a request to merge duplicate test fixes into master** while upstream PRs #495 and #496 are under review.
- Check hash/diff, then lint, compile, native JUnit, complete Tycho reactor, independent platform qualification, API/ABI and upstream provenance. Report each stage by actual logs. Never infer full-suite success from Maven compilation alone.
- A separate GTK workflow failed before tests because Apache Maven binary fetch returned HTTP 429; do not conflate provisioning with test failures.
- If either donor PR changes, require fresh SHA/diff reconciliation. No force push, branch deletion or automatic merge.

## Rollback
Close this draft if superseded; no default branch was mutated by this integration step.
