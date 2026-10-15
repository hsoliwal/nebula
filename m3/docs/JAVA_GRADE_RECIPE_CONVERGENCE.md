# Nebula M3 Java-grade recipe convergence

## Definition

A Nebula source change is **Java-grade** only when the canonical Maven/OpenRewrite recipe DAG has
been proven on Java 21 and the complete proof stack is bound into
`NebulaM3JavaGradeQualification`.

The source-changing unit remains a small OpenRewrite recipe atom. The canonical FILE DAG remains:

```text
INVENTORY
 -> ATOMIZATION
 -> PATTERNIZATION / IOP
 -> DOCUMENTATION
 -> PROVEN FILE CONVERGENCE
```

Independent FILE candidates may fan out. Authority widens only when the semantic boundary widens:

```text
FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API
```

Repository size alone never widens authority.

## Orchestration

Maven/OpenRewrite is the canonical execution/proof root.

Apache Camel, Apache Airflow, and Drools/KIE are deterministic projections of the same content-
addressed DAG. They may schedule or select recipe atoms but have no direct source-mutation or
promotion authority.

The scheduler projections are content-addressed with the DAG root and must agree across Linux,
Windows, and macOS in the portable proof workflow.

## Qualification

The Java-grade receipt binds:

- Java release 21;
- the exact OpenRewrite version;
- the canonical recipe DAG root;
- the Camel/Airflow/Drools projection root;
- the complete scope ladder;
- the 99% semantic coverage threshold;
- all proof-stage fingerprints;
- the transfer contract root;
- the two transfer targets: `hsoliwal/com.synexia` and `hsoliwal/M3jdk21`.

The hosted proof stages are:

```text
SOURCE_INVENTORY
RECIPE_JUNIT
ATOM_BEHAVIOR
PATTERN_IOP
DOCUMENTATION
JAVA21_COMPILE
FIXED_POINT
SEMANTIC_COVERAGE_99
M3_REACTOR
TYCHO_REACTOR
SWT_PLATFORM_RUNTIME
DIFF_AND_PROVENANCE
```

Any missing/failed stage leaves the Java-grade receipt at `HOLD`.

When all stages pass, disposition becomes
`MANUAL_TRANSFER_REVIEW_REQUIRED`. It still grants neither direct mutation nor automatic
promotion/merge authority.

## Backporting

Backporting is part of the same programme, not a later cleanup step.

The Nebula transfer contract targets:

- `hsoliwal/com.synexia` for reusable recipe/module ownership;
- `hsoliwal/M3jdk21` for Java-21 compatibility and backport lanes.

A proven Nebula recipe/DAG is therefore consumed by the target repository through its own
inventory, compatibility, scope and proof gates. The transfer bundle itself never edits the target.

This keeps the workflow mechanical:

```text
prove recipe atom
 -> prove FILE DAG
 -> prove product behavior
 -> bind Java-grade receipt
 -> target inventory/compatibility review
 -> target recipe adaptation
 -> compile/test/runtime proof
 -> fixed point
 -> manual promotion review
```
