# Nebula M3 final proven transfer gate

Status: proving branch for mechanical transfer of the M3 recipe-first convergence shape.

## Canonical recipe

The source-changing work unit is:

`org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe`

Its FILE-local DAG is:

```text
INVENTORY
 -> ATOMIZATION
 -> PATTERNIZATION / IOP
 -> DOCUMENTATION
 -> PROVEN_FILE_CONVERGENCE
 -> second-run fixed point
```

Every leaf remains a tested OpenRewrite recipe. Maven is the build/proof root. The original Nebula
Tycho reactor remains the product authority.

## Refactoring authority ladder

Source-changing authority follows the semantic boundary actually crossed:

```text
FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API
```

Repository size never widens authority. Independent FILE candidates may fan out horizontally.
PROJECT and REPOSITORY are outer proof/fan-in scopes only; they are not source-mutation authority.

The checked-in policy, Java scope oracle, convergence plan and transfer receipt all carry this same
order. A target repository must preserve it when adapting the recipe DAG.

## Orchestration

The same content-addressed DAG may be scheduled by:

- Maven/OpenRewrite;
- Apache Camel;
- Apache Airflow;
- Drools/KIE.

The recipe crate exports deterministic scheduler views from the same canonical DAG:

- `camel-route.yaml`;
- `airflow-dag.py`;
- `drools-agenda.drl`;
- `orchestrator-plans.sha256`.

Schedulers receive **no source-mutation or promotion authority**. They may order/shard admitted
recipe atoms and collect receipts only. The Airflow plan uses non-mutating task nodes; Camel
dispatches only to the declared OpenRewrite adapter endpoint; the Drools agenda only marks
dependency-ready atoms.

## Portable transfer receipt

Run:

```bash
mvn -B -ntp -f m3/reactor.xml -Pdag-manifest -Ptransfer-contract verify
```

The build emits:

```text
m3/recipe-first/target/nebula-m3-transfer/
├── transfer-metadata.tsv
├── transfer-dag.tsv
├── transfer-targets.tsv
├── orchestrators.tsv
├── camel-route.yaml
├── airflow-dag.py
├── drools-agenda.drl
├── orchestrator-plans.sha256
└── transfer.sha256
```

The transfer contract binds Java 21, OpenRewrite 8.90.4, the canonical recipe entrypoint, the DAG
root, scheduler-plan root, scheduler authority, and the intended mechanical targets:

- `hsoliwal/M3jdk21` — Java 21 JDK compatibility/backport lanes;
- `hsoliwal/com.synexia` — reusable recipe-pack/module lanes.

This receipt authorizes no target edits. Each target repository must inventory its own contracts,
supply repository-specific eligibility/source fences, run the same recipe phases, prove fixed point,
and pass its native build/tests before promotion.

## Final acceptance

The final CI gate requires, in order:

1. diff hygiene;
2. Java 21 compile/JUnit for the local recipe reactor;
3. focused convergence/transfer/scheduler JUnit proof;
4. **>=99% JaCoCo line coverage** across the admitted atomization, patternization/IOP,
   documentation, convergence-entrypoint and scheduler-plan kernel;
5. content-addressed DAG, scheduler plans and transfer receipt generation;
6. complete repository Java inventory with zero parse failures;
7. installation of the tested recipe artifact;
8. first FILE-local atomize/patternize/document candidate application;
9. second identical application with byte-identical diff;
10. original Nebula Tycho `clean verify` on the transformed candidate tree;
11. candidate-only authority checks.

A green gate is the condition for mechanically porting the recipe/DAG shape to M3JDK21 and
com.synexia. It is not permission to bulk rename APIs or bypass scope promotion.

## M3JDK21 backport transfer

Backporting is not removed from the programme. It is a named target lane in the transfer receipt:

`hsoliwal/M3jdk21 -> JAVA21_JDK_COMPATIBILITY_AND_BACKPORT_LANES`.

A green Nebula gate transfers the **recipe/DAG mechanics**, not Nebula source or semantic assumptions.
M3JDK21 must still inventory each JEP/PR/tooling candidate, classify Java-21 source/classfile/VM/API
compatibility, bind compatible dependency closure, and run JDK-specific compile/jtreg/runtime proof.
Spec-visible or incompatible features remain isolated/rejected rather than being forced through a
FILE recipe.

`hsoliwal/com.synexia` separately receives the reusable recipe-pack/module mechanics. Both target
repositories must preserve their own scope ladders, contract fences, native/JNI parity and
promotion gates.
