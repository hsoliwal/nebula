# Nebula M3 convergence branch

This directory binds the two existing Nebula M3 lanes into one executable convergence path:

1. `m3/recipe-first` — whole-checkout Java/OpenRewrite inventory from Nebula PR #3;
2. `.m3/atom-pattern` — saved typed hierarchy overlay from Nebula PR #4;
3. canonical implementation — `hsoliwal/com.synexia` PR #8692, commit
   `78d1c67fbc97bb4831f0176f2a8b034ecc994eee`.

No Nebula widget source is hand-edited by this convergence layer.

## Reusable callable rule

Existing reusable functions/lambdas/functional atoms remain the executable leaves. The recipe DAG
adds identity, ordering, scope, pattern/IOP roles, documentation roots and proof gates; it does not
duplicate a callable implementation merely to make it schedulable.

The canonical recipe DAG/projections already live in Synexia. Camel, Airflow and Drools/KIE consume
the same content-addressed DAG root and never receive promotion authority.

## Run

With both repositories checked out:

```bash
SYNEXIA_ROOT=/absolute/path/to/com.synexia \
  bash m3/convergence/verify.sh
```

The script:

- proves the shared Java/JNI hierarchy recipe;
- installs its Maven artifact locally;
- proves the Nebula-local inventory crate;
- inventories the complete Java checkout and requires zero parse failures;
- runs hierarchy analysis twice outside the repository and requires byte-identical output;
- requires no tracked source changes;
- runs Nebula's original Tycho `mvn clean verify`.

## Promotion rule

A successful analysis/build is still not semantic permission to replace source. File-local
atomization/patternization is candidate-only. Any source-changing pass remains contract-locked,
recipe-owned, JUnit-proven, fixed-point, and serially promoted through the required scope.

JNI is not introduced into Nebula merely because the shared recipe has a JNI acceleration helper.
Nebula currently has no native implementation owner in the pinned baseline; Java remains the oracle
until a specific hot primitive has parity, lifecycle/fallback and setup-inclusive benchmark proof.


## Live Maven/OpenRewrite source-model pass

The root reactor now exposes an opt-in profile:

```bash
mvn -Pm3-atomize-patternize rewrite:dryRunNoFork
```

The profile loads the canonical `com.synexia.rewrite.M3NebulaAtomizePatternizeRecipe`
from `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`.

The recipe composes the existing canonical SWT distillation inventory, repository
atom/pattern catalogue and Mavenized absorption inventory. It is read-only:
mutation, source-copy, absorption and promotion authority are all false.

This is deliberately a stage in the existing Nebula/Tycho build, not a replacement
reactor. A bounded source-changing recipe may be admitted only after the catalogue
selects a concrete atom/pattern cohort and the original Nebula build remains the
behavioral oracle.

## Donor and screenshot evidence

See `m3/convergence/VIEWPORT_VISUAL_GATES.md`.

The evidence set includes:

- SWT snippets/examples and the retained viewport screenshot suite;
- Java2s SWT/SWT Graphics/Swing behavioral example categories;
- Nebula Grid/CompositeTable/CWT/XViewer mechanics;
- the supplied historical JFace viewport/deferred viewer sources;
- the supplied Virtual TreeView 8.4.1 demos/help images;
- external viewport/immediate-mode libraries as architecture/mechanics evidence only.

These inputs grant no source-copy authority. They are used to compare behavior after
both donor and target shapes have been atomized/patternized.
