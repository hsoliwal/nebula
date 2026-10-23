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

Both the root aggregator `pom.xml` and the actual widget parent
`releng/org.eclipse.nebula.nebula-parent/pom.xml` expose the opt-in profile.
The widget parent does not inherit from the root aggregator, so both entry points
need the configuration. Widget modules inherit it from the actual parent.

```bash
mvn -Pm3-atomize-patternize rewrite:dryRunNoFork

# The parent POM is also a supported entry point:
mvn -f releng/org.eclipse.nebula.nebula-parent/pom.xml \
  -Pm3-atomize-patternize rewrite:dryRunNoFork
```

The profile loads the canonical `com.synexia.rewrite.M3NebulaAtomizePatternizeRecipe`
from `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`.
Install the real canonical recipe artifact first; the profile does not manufacture
a local replacement implementation or silently fall back to another inventory.

The executable recipe source is
[`M3NebulaAtomizePatternizeRecipe.java`](https://github.com/hsoliwal/com.synexia/blob/develop/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3NebulaAtomizePatternizeRecipe.java).
The exact parent-POM wiring repair is the canonical declarative recipe
`com.synexia.rewrite.M3NebulaParentRewriteProfile`, backed by the existing
`M3HashPinnedPomSnapshotRecipe`. Its source/postimage manifest and verification
project remain in `com.synexia`.

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


## Nebula-local source-changing seed grammar

The broad hierarchy and Synexia source-model passes remain read-only evidence lanes. A separate local
recipe crate now proves the first source-changing FILE grammar inside Nebula itself.

The mutation order is strictly:

```text
inventory
 -> atomization
 -> patternization / IOP
 -> semantic documentation
 -> second-pass fixed point
 -> transformed Tycho verify
```

Each mutating leaf is one-cycle and shares one eligibility predicate with the inventory pass.
The current grammar admits only private static pure-`int` expression leaves. Non-admitted shapes
are explicit residue and are not generalized by intuition.

GitHub Actions stores `candidate.patch`, `first.patch`, `second.patch` and the changed-file
ledger as evidence. No workflow commits those changes or promotes them automatically.

## Current canonical recipe source custody (2026-10-18)

The reusable recipe source closure from this fork is held in `hsoliwal/com.synexia` at `215546d1ca0e724eb2c8e2e8ef6b4adce724733a`. The [dated custody binding](../../.m3/SYNEXIA_RECIPE_SOURCE_CUSTODY_20261018.json) records the exact public source revisions, canonical snapshot paths, source seals and the separate execution limits. The current source-custody ledger is `synexia-openrewrite-recipes/recipe-crates/public-fork-home-20261018/SOURCE_CUSTODY.tsv` (SHA-256 `ebcfbd42a49eec806f0a4572af731828deda41f22c17250d34d49af985d78f1d`).

Existing public source and SDK/proof mirrors retain their recorded provider revisions because their build and proof callers have not yet migrated to a qualified canonical handoff. Keep improvements in the canonical recipe source and return qualified postimages to this target. Existing native builds, product APIs, runtime acceptance and promotion remain target-owned.

The retained-mirror source revision is the frozen custody origin. Separate current packet applications may update target paths under those roots; their exact inputs, outputs and qualification limits are recorded independently below.

The binding's original-packet verification covers only its listed packets. The [current recipe census](https://github.com/hsoliwal/com.synexia/blob/215546d1ca0e724eb2c8e2e8ef6b4adce724733a/synexia-openrewrite-recipes/recipe-crates/public-fork-home-20261018/evidence/CURRENT_RECIPE_CENSUS.json) (SHA-256 `cfa9edb42a99cb2e54a988b28de7078a3233fdc667c5e4d8f979675e0f767ee0`) records the separate admission, application and gate evidence for current packets. Canonical kernel checks do not establish SDK or product runtime qualification: the canonical SDK attempt was blocked before compilation with zero tests. The binding-only application does not run native product qualification; gate attempts for separate product packet applications are recorded in the current census.

The sealed binding and execution-receipt packets under `.m3/recipe-source-custody-20261018/` are pinned proof mirrors. They grant no competing recipe ownership or product promotion authority.
