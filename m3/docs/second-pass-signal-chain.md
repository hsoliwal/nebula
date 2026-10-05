# Nebula M3 second-pass signal-chain binding

Nebula reuses the Synexia second-pass OpenRewrite recipe; it does not fork or copy its implementation.

Pinned upstream evidence:

- repository: `hsoliwal/com.synexia`
- branch: `develop`
- canonical merge commit: `daaab09a1b91fe8344c1ea97359342b52739bf38`
- integration PR: `#8925`
- integration PR head: `5f63a7a6a4541d055df5071de22d5edf9ee23c7a`
- integrated component PRs: `#8891`, `#8897`, `#8915`
- recipe: `com.synexia.rewrite.M3SecondPassAtomPatternRecipe`
- canonical scheduler catalog: `com.synexia.m3.recipe.M3SecondPassRecipeDagCatalog`
- canonical catalog file: `synexia-m3-recipe/recipes/second-pass-atom-pattern.yaml`
- state mode: `SHARED_JVM_COMPOSITE`
- external leaf fan-out: `false`
- pass budget: `2`

The existing Nebula root profile `m3-atomize-patternize` activates
`com.synexia.rewrite.M3NebulaAtomizePatternizeRecipe`. At the pinned merged Synexia integration that recipe
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

Hosted green proof is not claimed yet. The integration is merged, and PR-head workflow runs were
observed queued/pending for the exact integration head during this review. Until focused Maven/JUnit
jobs complete successfully, this binding remains candidate/read-only evidence and cannot authorize
source mutation or promotion.

The binding is machine-readable in `m3/catalogue/second-pass-recipe-binding.tsv`.

### Orchestration locality

The current Synexia second-pass recognizers share one JVM-local signal store. They are logical DAG
leaves inside the OpenRewrite composite, but they are not yet independent process-level tasks.

Camel, Airflow, or another external scheduler may schedule the **second-pass composite as one
recipe atom**. Independent cross-process fan-out of structural/control/contract/regex/repetition
leaves is forbidden until a content-addressed signal artifact handoff and reducer-input contract
are implemented. Drools/KIE remains admission evidence only and gains no mutation or promotion
authority.

