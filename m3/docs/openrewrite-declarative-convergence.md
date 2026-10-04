# Nebula M3 — official OpenRewrite declarative convergence

Status: M3 tool-plane contract. Nebula widget/public behavior remains locked.

## Authoring rule

Nebula follows the OpenRewrite recipe authoring split:

1. semantic Java/LST transformations are imperative recipe leaves;
2. composition of already-proven leaves is declarative YAML under `META-INF/rewrite`;
3. the named recipe is explicitly activated through OpenRewrite `Environment`;
4. declarative behavior is proven with `RewriteTest.recipeFromResources(...)`;
5. the existing Java composite remains a compatibility oracle, not a second mutation authority.

Canonical named recipe:

```text
org.eclipse.nebula.m3.ConvergeFileAtoms
```

Ordered leaf DAG:

```text
NebulaM3InventoryPureIntAtomCandidates
 -> NebulaM3AtomizePureIntReturnRecipe
 -> NebulaM3PatternizePureIntAtomRecipe
 -> NebulaM3DocumentPureIntAtomRecipe
 -> NebulaM3SvgLoaderLengthConvergenceRecipe
```

The YAML declares `causesAnotherCycle: true`; the existing FILE-local fixed-point gate remains
authoritative.

## Build boundary

The original Nebula/Tycho reactor remains the product oracle:

```text
mvn -V -B clean verify -Dtycho.localArtifacts=ignore
```

The M3 Maven/OpenRewrite module remains a control/proof plane only. It does not replace Tycho.

## Recipe-first delivery

The Java control-plane switch must be source-sealed before materialization:

- exact preimage SHA-256 for each existing Java target;
- explicit `ABSENT` state for additive Java targets;
- reviewed postimage resources;
- Java-21 parse/print proof;
- second-pass fixed point;
- no public widget/API mutation authority.

The declarative YAML is itself the reusable recipe artifact.

## Donor boundary

Official OpenRewrite documentation/API is the framework donor for recipe composition mechanics.
Nebula remains the semantic/product authority.

Challenge platforms and unrelated repositories remain taxonomy/mechanics evidence only. Their source
bodies are not copied into Nebula without explicit license/provenance and compatibility admission.

## Completion boundary

Passing the recipe-module proof is necessary but not sufficient.

Canonical promotion still requires:

```text
inventory
 -> recipe
 -> dry run
 -> review
 -> apply candidate
 -> second-pass fixed point
 -> Tycho compile/tests
 -> SWT/JFace/Nebula behavior proof
 -> JNI parity only if a native hot path is actually introduced
 -> benchmark only when performance is claimed
 -> serial promotion
```
