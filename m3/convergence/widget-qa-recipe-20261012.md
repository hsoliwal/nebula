<!-- SPDX-License-Identifier: Apache-2.0 -->
# Automated SWT and Nebula QA recipe receipt — 2026-10-12

Canonical recipe/evidence: [Synexia PR #10534](https://github.com/hsoliwal/com.synexia/pull/10534),
commit `e4aff2e7302be68ef1ebe1d3c488a01baa259722`, directory
`synexia-openrewrite-recipes/recipe-crates/widget-qa-completion-20261012/`.

The actual OpenRewrite SDK emitted the existing combined workflow, runner, Grid
validator and AGENTS invariant onto Nebula base
`6a002b68cd56e14d8ce2009b7bb349562b8403fc`. This receipt is publication metadata.
The existing workflow pins SWT source candidate
`1737076eaf15db78a145d8bedaec975d236f4bbd` from
[SWT PR #110](https://github.com/hsoliwal/eclipse.platform.swt/pull/110).
That candidate contains the exact SDK-generated shared validator; its PR head
`0cf4074df05655039e15d160d77896392b33b51b` adds only provenance.

The combined runner now requires six explicit PASSED stages: SWT native tests,
SWT screenshot tests, SWT OpenCV, SWT resources, Nebula native tests and Nebula
OpenCV. Its receipt cannot describe a failed setup as executed. It uses the same
SWT validator as SWT CI and records resolved repository heads. The Grid validator
requires the scene-producing test methods, rejects duplicate reports and mismatched
artifact names, and removes stale PASS output before validation.

Native visible pixels remain captured by SWT's `GC(Display).copyArea`.
`Control.print` remains distinct offscreen evidence. Existing Java/JNI capture,
pixel oracles and resource budgets are unchanged. No manual QA or screenshot
sign-off is required; AGENTS records that invariant.

Canonical offline Maven verification passed on Java 21 / OpenRewrite 8.17.1:
two JUnit recipe tests, 14 Python validator/runner tests, unchanged replay and
six drift refusals. Two regressions reproduce acceptance by the old validators
and rejection by the successors. Synthetic PNGs test the validators only.

Fresh native execution remains blocked in the authoring environment: GTK/Xvfb
are unavailable, package installation failed and environment policy rejected
sandbox escalation. The existing target CI must execute source-built SWT/JNI and
Nebula capture, vision and CPU/heap/RSS gates. Win32/Cocoa and full reactor remain
separate gates. No fresh platform pass, complete widget/donor distillation or
performance improvement is claimed. Existing licenses and attribution remain.

## Current-source successor

[Synexia #10550](https://github.com/hsoliwal/com.synexia/pull/10550), commit
`829067596dff4bb72705ad4ccfc98e57122340da`, owns the executed successor in
`synexia-openrewrite-recipes/recipe-crates/swt-viewport-reconciliation-20261012/`.
The existing OpenRewrite text owner generated four receiver files against Nebula
master `ede83fc10c5b072b6676ed53f955f3ff7a61139e`. This receipt is publication
metadata; both the prior candidate and current master remain in commit ancestry.

The workflow pins generated SWT source `1eb87b687039e8858fb50284d93d7bf12c4e8d58`;
SWT PR #110 head `84ed239cede4791f3a2fb7c6e41ca18754092ace` adds provenance only.
The reconciliation preserves eight Grid scenes, screen and control captures,
million-row bottom/return semantics and the existing control-oracle selftests.
It retains required producing JUnit methods, duplicate/artifact/stale-PASS
refusals and all six explicit completion stages. SWT names are an AGENTS invariant;
no public/protected Java declaration was changed or new Swing facade introduced.

The canonical offline Maven run passes three recipe JUnit tests and eighteen
Python validator/runner tests. All eighteen generated targets pass no-change
replay and complete-input drift refusal. The pinned SWT source compiles under
Java 21 for GTK, Win32 and Cocoa (494/673/634 product files); eleven headless
viewport/selection tests pass per backend build. This is source qualification,
not native Win32/Cocoa execution. Synthetic PNGs exercise validator behavior only.

Fresh native widget capture and CPU/heap/RSS execution remain blocked locally by
missing GTK/Xvfb and rejected installation escalation. The existing combined CI
must produce native JUnit, OpenCV and resource receipts. No manual QA is required;
no complete widget/donor distillation or performance improvement is claimed.
Preserve the earlier receipt above as dated source history.
