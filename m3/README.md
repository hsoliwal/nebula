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

The standalone crate at `m3/recipe-first` is intentionally outside the Nebula Tycho reactor.
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
mvn -f m3/recipe-first/pom.xml verify
mvn -f m3/recipe-first/pom.xml -Pinventory \
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
