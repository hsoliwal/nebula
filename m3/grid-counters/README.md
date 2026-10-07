# Grid occurrence counters: applied Synexia recipe

This branch applies the exact candidate produced by Synexia's
`com.synexia.m3.NebulaGridCountersV1` (com.synexia PR #9519).
Recipe source, templates, manifests, full regression corpus, memory agent and execution evidence
remain in the canonical Synexia crate. This is a thin receiver receipt, not another recipe engine.
See APPLICATION.json for exact source and recipe identities.

## Product change

The existing invocation-local GridIdentityOccurrenceTable now uses two arrays instead of three:
Object references plus a long containing independent remaining/matched 32-bit counts. Neither
array is allocated until the first non-null previous identity. Null-only bookkeeping uses the
existing scalar fields. Probing, distinct-key growth, reference equality, multiplicity and ordered
occurrence consumption are preserved. Ten other complete component owners remain unchanged.

The Java21 compiler/runtime proof passed the complete 11-file difference component in original,
eager-packed and lazy-packed stages. It is not a full Grid/SWT test. It covered 116,281 range pairs,
300,000 state transitions, all 24 cancellation positions, asymmetric/throwing equals and three
compiled wrong-counter variants. The exact emitted final source was recompiled and tested.

The test-only agent measured table-owned storage (excluding caller-owned keys) as 280 -> 32 bytes
for empty/null-only use and 280 -> 256 bytes for a populated 16-slot single-identity table. These are
JVM-specific observations. CPU medians are mixed: the final 64-identity diagnostic regressed, so
there is no general speedup claim. Broader performance admission is required before promotion.

No SWT item, native resource, paint geometry, event dispatch or GUI thread rule changes. No new JNI
is added to reference-identity bookkeeping without an amortized-cost/lifecycle justification.

## Required acceptance

Use a Synexia checkout pinned to evidence commit 8bc1958d2bf306682e1b487c307e455ac13c0754:

```sh
python3 synexia-openrewrite-recipes/recipe-crates/nebula-grid-counters-20261006/verify.py /absolute/new/proof --target /absolute/nebula
mvn -o -B -ntp -f synexia-openrewrite-recipes/recipe-crates/nebula-grid-counters-20261006/pom.xml clean verify
```

The first mechanism ran on the exact source closure. Maven was unavailable (exit 127), so actual
OpenRewrite scheduler and Jupiter execution remain open. Then run Nebula's existing Tycho/native
UI gates, including the existing Grid visible-range/GC/viewport tests, on the applied branch.
The root Maven/Tycho configuration, earlier source snapshots and test assertions are unchanged.

Keep this branch/PR draft until actual SDK/JUnit, target-native UI/Tycho, other platform and
performance gates are qualified. No master mutation, merge, rebase, force-push or completed-SDK
claim is implied by saving this candidate. The Eclipse frontend/backend/OpenJDK programme remains
work in progress, with Synexia as reusable recipe owner and target repositories as product owners.
