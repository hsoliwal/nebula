# Typed, bottom-up M3 atom and pattern recipes

This additive overlay covers every tracked file and discovered module in this repository without
reparenting its original Maven/Tycho/Gradle/native build. It is not a claim that the whole repository
has already passed analysis, compilation, tests or absorption gates.

Install `com.synexia.verification:m3-hierarchy-recipe:0.1.0-hierarchy1` from the canonical
`synexia-maven-plugin/recipes/hierarchical-atom-pattern` crate in `hsoliwal/com.synexia` first:

```bash
mvn -f /path/to/com.synexia/synexia-maven-plugin/recipes/hierarchical-atom-pattern/pom.xml install
mvn -f .m3/atom-pattern/pom.xml verify \
  -Dm3.repository=OWNER/REPOSITORY -Dm3.revision="$(git rev-parse HEAD)" \
  -Dm3.output=/absolute/new/directory/outside/this/repository
```

The canonical crate also provides `verify.sh --jni` and a dependency-free Java CLI. Compilation of
the new core does not depend on Maven or on a substitute/mock OpenRewrite implementation.

Order: **file → package/namespace → module → project → repository**. Each file is parsed once;
wider scopes consume child receipts and compare candidate buckets. Maven aggregation comes from
local `<modules>` relationships, not from directory names or parent inheritance. Conditional,
missing or external modules remain explicit. Java source roots and test source roots stay distinct.
`MODULE_RECIPES.tsv` binds every discovered module to the shared owners; it is not a claim of
plugin inheritance from an unrelated aggregate POM.

Java delegates to the canonical `M3FileAtomizer`. Markdown has a separate lossless, fence-aware block
partition; it does not claim complete CommonMark/GFM semantics, rewrite prose, alter links or compile
code examples as production Java. XML uses a secure no-external-entity parser. Other code languages,
JSON/YAML, binary and license files retain explicit format-specific residue rather than receive Java
cleanup. No source changes or automatic replacement decisions are made by the default intake.

For a typed OpenRewrite pass, retain the original project build and activate the existing
`com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe` from the production recipe pack. Its existing
`M3MechanicalDonorShapeReviewRecipe` retains the canonical LeetCode/HackerRank/GFG category donors.
No new catalogue or challenge solution source is copied by this overlay. The original Tycho/Maven
verify, resolved classpath, UI parity, contract/effect checks and fixed point remain separate gates.

Provenance, namespace ownership, exact bytes and unresolved residue travel upward with every receipt.
Hash/shape similarity is only a comparison candidate. Never rebase `develop`, force-push, replace an
existing local policy automatically, or treat a receipt as semantic-equivalence proof.

## Canonical second-pass binding

Nebula consumes the merged, read-only Synexia second-pass through an exact source pin:

```text
repository       hsoliwal/com.synexia
branch           develop
commit           daaab09a1b91fe8344c1ea97359342b52739bf38
integration PR   #8925 (includes #8891)
named recipe     com.synexia.rewrite.M3RepositoryAtomPatternSecondPass
signal class     com.synexia.rewrite.M3AtomPatternSignalChainRecipe
DAG compiler     com.synexia.m3.recipe.OpenRewriteRecipeDagPlan
pass budget      4
state mode       SHARED_JVM_COMPOSITE
external fanout  false
authority        read-only; mutation=false; replacement=false; promotion=false
```

The signal chain masks comments/strings/chars/text blocks before regex nomination. Regex is evidence
only; OpenRewrite LST/AST structure remains semantic authority. The merged implementation requires
three internal fixed-point passes, therefore budget 2 is deliberately invalid and is not used by
this repository.

The exact machine binding lives in `m3/catalogue/second-pass-recipe-binding.tsv` and is enforced by
`NebulaM3SecondPassBindingTest`. CI installs exactly the pinned Synexia commit and runs the named
repository second-pass twice with source diffs forbidden.
