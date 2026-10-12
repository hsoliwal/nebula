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
