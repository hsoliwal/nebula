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

Nebula does not duplicate the Synexia second-pass detector. The opt-in root profile
`m3-atomize-patternize` now activates the current stable repository entry point:

`com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe`

The reviewed Synexia snapshot is:

```text
repository       hsoliwal/com.synexia
branch           develop
commit           1cd108647f1eb1d5c288d917acbf7e8e635c8ce9
integration PR   8925
integration head 5f63a7a6a4541d055df5071de22d5edf9ee23c7a
components       8891,8897,8915
recipe           com.synexia.rewrite.M3AtomPatternSignalChainRecipe
named recipe     com.synexia.rewrite.M3AtomPatternSignalChain
repository entry com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe
state            SCANNING_RECIPE_INTERNAL_FANOUT
external fanout  false
budget           4
fixed-point      3 passes required
authority        read-only; no equivalence/mutation/replacement/promotion
```

The signal-chain recipe, named YAML, repository recipe, lexical mask, hostile fixture and proof
workflow are each pinned by Git blob identity in
`catalogue/second-pass-recipe-binding.tsv`. The Java owner is
`NebulaM3SecondPassBinding`; its JUnit proof requires the checked-in TSV, policy, convergence plan
and root Maven profile to agree exactly.

The current Synexia repository recipe includes:

`new M3AtomPatternSignalChainRecipe(sourceFilePattern, 4)`

The second pass is evidence-only:

```text
AST/LST inventory
  -> structural/control/effect recognizers
  -> masked-regex nomination cues
  -> deterministic fan-in
  -> pattern candidates
  -> typed contract/risk residue
  -> content-addressed repository root
  -> fixed-point recheck
```

Regex runs only after length-preserving Java literal/comment/text-block masking. It can nominate a
candidate but cannot certify semantic equivalence or a transformation. The focused Synexia torture
fixture deliberately contains fake code inside strings/comments to prove this fail-closed boundary.

### Orchestration locality

The recognizers are internal stages of one OpenRewrite `ScanningRecipe`. Internal Java fan-out does
not make them independently schedulable cross-process atoms.

Camel or Airflow may schedule the whole reviewed repository/signal-chain recipe as one recipe atom.
Drools/KIE may provide admission policy. None receives source-mutation, semantic-equivalence,
replacement or promotion authority.

A future cross-process recognizer fan-out would require an explicit content-addressed signal
handoff/reducer-input contract and new proof; it is not inferred from the current implementation.

### Donor and native review

The existing read-only review order remains:

```text
LeetCode -> HackerRank -> GeeksforGeeks -> pinned GitHub donor/license evidence
```

Challenge/editorial/solution bodies remain reference-only. Native work still requires the Java
oracle, differential corpus, lifecycle/fallback proof, setup-inclusive benchmark and Java/JNI
parity before any bounded native candidate is considered.

Hosted Maven/JUnit/Tycho success is not inferred from merge state. Exact-head workflows must finish
successfully before this evidence can participate in serial promotion.

