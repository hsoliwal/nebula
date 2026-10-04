# Nebula M3 second-pass signal-chain binding

Nebula reuses the Synexia second-pass OpenRewrite recipe; it does not fork or copy its implementation.

Pinned upstream evidence:

- repository: `hsoliwal/com.synexia`
- branch: `feat/m3-second-pass-signal-chain-20261004`
- commit: `9d33ce5229bee70872110b7a03e4ee99bf0e0878`
- PR: `#8891`
- recipe: `com.synexia.rewrite.M3SecondPassAtomPatternRecipe`
- pass budget: `2`

The existing Nebula root profile `m3-atomize-patternize` activates
`com.synexia.rewrite.M3NebulaAtomizePatternizeRecipe`. On the pinned Synexia branch that recipe
delegates to `M3RepositoryAtomizePatternizeRecipe`, whose evidence chain includes the second-pass
signal DAG.

The second pass is read-only:

```text
AST/LST inventory
  -> structural signals
  -> control-flow signals
  -> effect/contract signals
  -> lexically masked regex cues
  -> repeated-body evidence
  -> deterministic fan-in
  -> candidate risk + typed residue + two-pass fixed-point evidence
```

Regex is nomination-only after Java literal/comment masking. It never certifies equivalence or a
transform. Contract-risk, regex-only, or exhausted-budget cases remain blocking residue.

This binding grants no source-mutation, replacement, JNI execution, absorption, or promotion
authority. Nebula's existing local FILE convergence recipe remains the only candidate mutation lane.

## Execution prerequisite

Before running the Nebula `m3-atomize-patternize` profile, install the exact pinned Synexia recipe
artifact from the commit above as `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`.

Hosted proof is not claimed yet. The dedicated Synexia workflow currently fails before GitHub
creates a job, in the same repository-wide startup-failure pattern as unrelated workflows. That is
not a Maven/JUnit pass or failure.

The binding is machine-readable in `m3/catalogue/second-pass-recipe-binding.tsv`.

### Orchestration locality

The current Synexia second-pass recognizers share one JVM-local signal store. They are logical DAG
leaves inside the OpenRewrite composite, but they are not yet independent process-level tasks.

Camel, Airflow, or another external scheduler may schedule the **second-pass composite as one
recipe atom**. Independent cross-process fan-out of structural/control/contract/regex/repetition
leaves is forbidden until a content-addressed signal artifact handoff and reducer-input contract
are implemented. Drools/KIE remains admission evidence only and gains no mutation or promotion
authority.

