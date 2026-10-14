# M3 one-build convergence trial — Nebula

Status: proving ground before mechanical transfer to M3JDK21 and Synexia.

## Objective

Use the reusable OpenRewrite recipe as the unit of work, not hand-edited target files.

Nebula is already Maven/Tycho based and already carries the M3 recipe-first module. The trial therefore
does not add another build system. It binds the existing canonical FILE-local recipe DAG to the root
Maven reactor and proves it in one disposable CI build.

## Canonical FILE DAG

```text
INVENTORY
  -> ATOMIZATION
  -> PATTERNIZATION / IOP
  -> DOCUMENTATION
  -> PROVEN_FILE_CONVERGENCE
  -> SECOND-PASS FIXED POINT
```

Every source-changing leaf remains a tested OpenRewrite recipe. Independent files may fan out, but
the recipe order inside one file remains deterministic and serial.

## Orchestration

OpenRewrite owns source mutation. Maven owns build/proof.

The existing content-addressed DAG can also be projected to:

- Apache Camel YAML;
- Apache Airflow Python DAG;
- Drools/KIE DRL.

Those projections schedule/select recipe atoms only. They have no direct source mutation or
promotion authority.

## One-build proof

The root Maven profile is:

`m3-nebula-file-convergence`

The CI sequence is:

1. diff hygiene;
2. Java 21 compile/JUnit/99% semantic coverage of the recipe module;
3. DAG + Camel/Airflow/Drools manifest generation;
4. root OpenRewrite dry run;
5. candidate apply only in the disposable checkout;
6. candidate diff capture;
7. second OpenRewrite dry run with no remaining patch;
8. recipe/JUnit/coverage re-proof;
9. candidate/DAG evidence upload.

The workflow never commits its disposable candidate automatically.

## Transfer / backport order

Backporting remains part of the programme; it is deliberately downstream of this proof.

```text
Nebula proven recipe/DAG
  -> bind exact recipe + DAG root + proof receipt
  -> hsoliwal/com.synexia canonical reusable recipe
  -> hsoliwal/M3jdk21 mechanical application
  -> JEP / PR / tooling backport lanes use the same atomized/patternized substrate
```

No M3JDK21 source is changed merely because the Nebula candidate exists. Transfer requires the exact
recipe identity, Java 21 proof, fixed point and contract evidence first.

## Invariant

Repository size never widens edit authority. A contract-preserving FILE transformation stays FILE
even when applied to thousands of files. Scope promotes only when visibility, package, module,
multi-module or library/API boundaries are actually crossed.
