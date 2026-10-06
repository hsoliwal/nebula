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

## Orchestration

The same content-addressed DAG may be scheduled by:

- Maven/OpenRewrite;
- Apache Camel;
- Apache Airflow;
- Drools/KIE.

Schedulers receive **no source-mutation or promotion authority**. They may order/shard admitted
recipe atoms and collect receipts only.

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
└── transfer.sha256
```

The transfer contract binds Java 21, OpenRewrite 8.90.4, the canonical recipe entrypoint, the DAG
root, scheduler authority, and the intended mechanical targets:

- `hsoliwal/M3jdk21` — Java 21 JDK compatibility/backport lanes;
- `hsoliwal/com.synexia` — reusable recipe-pack/module lanes.

This receipt authorizes no target edits. Each target repository must inventory its own contracts,
supply repository-specific eligibility/source fences, run the same recipe phases, prove fixed point,
and pass its native build/tests before promotion.

## Final acceptance

The final CI gate requires, in order:

1. diff hygiene;
2. Java 21 compile/JUnit for the local recipe reactor;
3. content-addressed DAG and transfer receipt generation;
4. complete repository Java inventory with zero parse failures;
5. installation of the tested recipe artifact;
6. first FILE-local atomize/patternize/document candidate application;
7. second identical application with byte-identical diff;
8. original Nebula Tycho `clean verify` on the transformed candidate tree;
9. candidate-only authority checks.

A green gate is the condition for mechanically porting the recipe/DAG shape to M3JDK21 and
com.synexia. It is not permission to bulk rename APIs or bypass scope promotion.
