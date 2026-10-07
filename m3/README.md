# Synexia M3 for Eclipse Nebula

This directory applies the Synexia M3 engineering invariant to the `hsoliwal/nebula` fork without
changing Eclipse Nebula public widget contracts by default.

## Hard invariants

1. **Inventory before change.** Repository/module/package/file/method facts are collected before a
   source-changing recipe is admitted.
2. **Recipe is the work product.** Repeated/refactoring work is expressed as a reusable Maven +
   OpenRewrite recipe with tests; target files are not repaired one-by-one by an LLM.
3. **Contract lock.** Existing SWT/JFace/Nebula public APIs and externally observable behavior stay
   unchanged unless a later, explicitly reviewed API-scope recipe authorizes a change.
4. **Additive superset.** Existing behavior remains available while shared machinery is consolidated
   underneath it.
5. **Precompute/index first.** Repeated derivation is a candidate for immutable sidecars, indexes,
   rank tables, lookup geometry or other bounded prepared representations before runtime recompute.
6. **File-local parallel candidates, serial promotion.** Independent files may be inventoried and
   mechanically transformed in parallel, but canonical promotion remains serial and proof-gated.
7. **Java oracle before JNI.** This repository currently contains no native C/C++ owner. JNI is not
   introduced merely for novelty. A native path requires a proven hot primitive, a Java semantic
   oracle, lifecycle/fallback semantics, parity tests and a benchmark that includes setup cost.
8. **No donor-body shortcuts.** Donor repositories and challenge platforms provide taxonomy,
   mechanics and evidence. Source bodies are copied only when licensing/provenance and exact
   compatibility have been reviewed; otherwise they remain reference-only.
9. **Second-pass fixed point.** A recipe that still changes its own output is not ready for
   promotion.
10. **Whole-repository completion is mechanical.** Format/static analysis/compiler/unit/integration
    and the existing Tycho `mvn verify` reactor remain the completion authority.

## First pass

The source-first reactor at `m3/reactor.xml` builds the Nebula-owned qualified parser module before `m3/recipe-first` and remains outside the Nebula Tycho reactor.
It compiles on Java 21 and inventories Java sources with OpenRewrite without modifying them.

The inventory records, per compilation unit:

- owning module coordinate;
- class and method counts;
- public/protected surface count;
- native declaration count;
- loop count;
- linear search signals (`indexOf` / `lastIndexOf`);
- membership signals (`contains*`);
- sort and binary-search signals;
- TODO/FIXME markers;
- the next fast-search review action.

The CLI can inventory the whole checkout:

```bash
mvn -f m3/reactor.xml verify
mvn -f m3/reactor.xml -Pinventory \
  -Dm3.nebula.root="$(pwd)" \
  -Dm3.nebula.out="$(pwd)/m3/recipe-first/target/nebula-inventory" verify
```

The normal Nebula build remains:

```bash
mvn -V -B clean verify -Dtycho.localArtifacts=ignore
```

## Serial fast-search review

`catalogue/fast-search.tsv` is the first donor/problem taxonomy. It is deliberately a catalogue,
not implementation authority. Each source candidate emitted by inventory is reviewed against the
catalogue before a source-changing recipe is authored.

Challenge sites are used only for category/problem-shape comparison. Their problem statements,
editorials and submitted solutions are not copied into this repository.

## JNI boundary

There is no C/C++ source in the pinned Nebula baseline. The first pass therefore records native Java
declarations but does not create JNI. If later profiling identifies a stable primitive hot path,
the sequence is:

```text
Java oracle
  -> differential corpus
  -> bounded JNI/C++ candidate
  -> Java/JNI parity + lifecycle/fallback
  -> setup-inclusive benchmark
  -> recipe-owned integration
```


## Official OpenRewrite declarative composition

The framework/composition contract is documented in
[`docs/openrewrite-declarative-convergence.md`](docs/openrewrite-declarative-convergence.md).
Semantic LST changes remain imperative leaf recipes; the admitted FILE DAG is distributed as a
declarative `META-INF/rewrite` recipe and activated explicitly through OpenRewrite's managed
Environment.

## Local atomize/patternize convergence recipe

Nebula now carries a self-contained Java 21 FILE-scope recipe DAG in `m3/recipe-first`:

```text
NebulaM3InventoryPureIntAtomCandidates
 -> NebulaM3AtomizePureIntReturnRecipe
 -> NebulaM3PatternizePureIntAtomRecipe
 -> NebulaM3DocumentPureIntAtomRecipe
 -> second-run fixed point
```

The first admitted mutation grammar is intentionally small: private static `int` methods with one
return expression composed only from `int` parameters/literals, parentheses, unary
`+/-/~`, and non-throwing primitive arithmetic/bit operators. Division, modulo, field reads,
calls, instance methods, wider signatures and broader scopes remain residue.

The local crate is compiled against OpenRewrite 8.90.4, matching the dedicated
`rewrite-maven-plugin 6.46.1` execution lane. The older Synexia read-only distillation profile is
kept separate rather than mixing OpenRewrite generations in one classloader.

Run:

```bash
mvn -B -ntp -f m3/reactor.xml install
mvn -B -ntp -Pm3-local-file-convergence rewrite:dryRunNoFork
```

CI uses `rewrite:runNoFork` in an ephemeral checkout, saves the exact candidate patch, reruns the
same DAG, requires the patch to be byte-identical, and then runs the original Tycho reactor on the
transformed tree. The recipe remains candidate-only; the saved patch has no automatic promotion
authority.

### Recipe DAG orchestration

OpenRewrite recipes are the atomic mutation operators. Their phase/contract metadata forms the DAG.
Camel, Airflow or Drools/KIE may later schedule or select these same recipe atoms, but orchestration
does not grant edit or promotion authority. The compiler/tests/fixed-point/Tycho gates remain the
oracle.

Once this Nebula branch is green, the recipe/DAG shape can be replayed mechanically into
`hsoliwal/M3jdk21` and `hsoliwal/com.synexia`; repository-specific eligibility recipes are added
as new atoms rather than hand-editing target files.


## Synexia Apache-2.0 custody binding

`m3/catalogue/apache-handoff-binding.tsv` and
`NebulaM3ApacheHandoffBinding` pin the canonical Synexia Apache handoff manifest by exact
repository, branch, commit, PR and SHA-256. This is qualification evidence only while the pinned
Synexia custody PR remains unmerged.

The binding grants no source mutation, automatic application, target relicensing or promotion
authority. Synexia-original Apache-2.0 recipe/proof assets retain their own license and copyright;
Nebula product/source remains under its applicable Eclipse license. The original Tycho reactor and
Nebula behavior tests remain the product acceptance authority.

## Recipe DAG orchestration manifest

The Nebula proving crate now exports the same recipe graph as a content-addressed scheduler-neutral
manifest:

```bash
mvn -f m3/reactor.xml -Pdag-manifest verify
```

Outputs:

```text
m3/recipe-first/target/nebula-m3-dag/
â”œâ”€â”€ recipe-dag.tsv
â”œâ”€â”€ recipe-dag.sha256
â””â”€â”€ orchestrators.tsv
```

The graph is derived from `NebulaM3FileConvergenceRecipeDag`; it is not a second hand-maintained
workflow. OpenRewrite recipe leaves remain the source-changing atoms and Maven remains the build/proof
root. Apache Camel, Airflow, and Drools/KIE are admitted as scheduler/rule-selection targets only.
Their manifest receipts explicitly set mutation and promotion authority to `false`.

This lets larger systems compose small, already-proven recipe atoms into DAGs without changing the
semantic authority model. A scheduler can order/shard work; only the recipe and the compiler/test
proof can establish a candidate, and only serial review can promote it.

## Second-pass signal-chain reuse

Nebula does not duplicate the Synexia second-pass detector. The existing root profile
`m3-atomize-patternize` consumes an exact merged Synexia recipe artifact. CI overrides the
profile's broad compatibility entry point with the canonical named repository second-pass:

`com.synexia.rewrite.M3RepositoryAtomPatternSecondPass`

Exact reviewed binding:

```text
repository  hsoliwal/com.synexia
branch      develop
commit      daaab09a1b91fe8344c1ea97359342b52739bf38
PR          8925 (integrates 8891)
recipe      com.synexia.rewrite.M3RepositoryAtomPatternSecondPass
signal      com.synexia.rewrite.M3AtomPatternSignalChainRecipe
DAG         com.synexia.m3.recipe.OpenRewriteRecipeDagPlan
state       SHARED_JVM_COMPOSITE; external leaf fan-out=false
budget      4
authority   read-only; no mutation/replacement/promotion
```

Install that exact Synexia commit first, then run:

```bash
mvn -B -ntp -Pm3-atomize-patternize \
  -Drewrite.activeRecipes=com.synexia.rewrite.M3RepositoryAtomPatternSecondPass \
  rewrite:dryRunNoFork
```

The machine binding is `catalogue/second-pass-recipe-binding.tsv`; the Java owner is
`NebulaM3SecondPassBinding`. The convergence plan records the signal chain as a read-only
pre-mutation gate. Lexically masked regex may nominate a candidate but never certify a transform;
AST/LST structural/control/contract facts remain authoritative. The merged signal recipe uses three
internal fixed-point passes and the saved application budget is 4; budget 2 is intentionally
insufficient.

Hosted Nebula execution remains evidence, not promotion authority. The workflow checks out the exact
Synexia merge commit, installs the recipe artifact, runs the named repository second-pass twice, and
requires the Nebula source tree to remain unchanged.


### Orchestration locality

The current Synexia second-pass recognizers share one JVM-local signal store. They are logical DAG
leaves inside the OpenRewrite composite, but they are not yet independent process-level tasks.

Camel, Airflow, or another external scheduler may schedule the **second-pass composite as one
recipe atom**. Independent cross-process fan-out of structural/control/contract/regex/repetition
leaves is forbidden until a content-addressed signal artifact handoff and reducer-input contract
are implemented. Drools/KIE remains admission evidence only and gains no mutation or promotion
authority.

