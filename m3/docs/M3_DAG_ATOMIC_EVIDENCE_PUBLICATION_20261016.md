<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 recipe DAG evidence: atomic per-file publication

## Owner and scope

The canonical work unit is the ordered OpenRewrite DAG
`org.eclipse.nebula.m3.rewrite.exact.NebulaM3DagAtomicConvergenceRecipe`:

1. `NebulaM3DagAtomicMavenProfileRecipe`: exact XML-LST opt-in POM admission;
2. `NebulaM3DagEvidenceAtomicRecipe`: exact Java-LST FILE exporter rewrite.

Each atom has pinned pre/post SHA-256, stale-preimage refusal and unchanged second replay.
The Java change is **FILE** scope; the opt-in Maven profile is **MODULE/build** scope.
The DAG's highest authority is consequently MODULE, which requires normal review and product
proof rather than silent FILE promotion. Neither leaf runs mutation in Maven's default lifecycle.

The recipe is a Nebula target-specific proving leaf. Canonical reusable M3 recipes remain
owned by `hsoliwal/com.synexia`. No duplicate compiler, atomizer, patternizer, scheduler
or native/JNI subsystem is introduced.

## Mechanical behavior

The same seven UTF-8 DAG/scheduler output filenames and the same file contents are generated.
The exporter now:

1. computes every artifact in memory before output publication;
2. refuses filesystem-root or symlinked output directories and non-regular/symlinked targets;
3. checks all target types before staging any content;
4. stages changed files in the output directory and atomically replaces each file by rename;
5. removes uncommitted staging files when an error occurs;
6. leaves byte-identical existing files untouched on replay.

**This is atomicity for each individual artifact, not an all-seven-file transaction.** A failure
partway through the commit phase may leave a mixture of complete old/new artifacts. The
consumer must verify `recipe-dag.sha256`, `orchestrator-plans.sha256`, and its associated
receipt roots before accepting a publication. Unsupported atomic moves fail without fallback
to a potentially truncated direct write.

The original DAG root, Camel/Airflow/Drools projections and receipt formats remain unchanged.
External schedulers have no source-mutation, semantic-equivalence or promotion authority.

## Recipe / JUnit verification

The portable Linux/Windows/macOS Java 21 workflow now requires all three:

- `NebulaM3DagAtomicConvergenceRecipeTest`: exact XML + Java LST DAG order, byte pins, scoped
  source application, stale POM refusal, unchanged second replay and opt-in profile proof;
- `NebulaM3DagEvidenceAtomicRecipeTest`: exact Java LST, stale source rejection and fixed point;
- `NebulaM3RecipeDagAtomicPublicationTest`: actual publication output, identical replay,
  special target rejection, symlink/hard-link cases, safe UTF-8 output and interruption.

To run the complete module proof:

```bash
mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am verify
```

The named recipe is registered as `org.eclipse.nebula.m3.DagAtomicEvidence` in
`m3/recipe-first/rewrite.yml` and loaded by the explicit `dag-atomic-evidence` Maven profile.
After installing the module artifact for OpenRewrite's plugin dependency, invoke the dry-run
explicitly:

```bash
mvn -B -ntp -f m3/recipe-first/pom.xml -Pdag-atomic-evidence install
mvn -B -ntp -f m3/recipe-first/pom.xml -Pdag-atomic-evidence rewrite:dryRun
```

No mutating OpenRewrite goal is lifecycle-bound; `rewrite:run` requires separate user
authorization after proof.

## Evidence and limitations

On 2026-10-16, the exact postimage SHA-256
`7bad32921368032cf42ee7297653e425afe96965634aafe9212abff67fce1c2c`
compiled using Java 21 `--release 21 -Xlint:all -Werror`. An independent JDK-only
smoke harness executed 11 behavior checks successfully with minimal stand-ins for the
manifest and scheduler data providers. This verifies the publication atom, **not** the
real OpenRewrite/JUnit integration, full Tycho/SWT build, or cross-platform permission model.

Until hosted Maven/JUnit, full Nebula product verification and all platform-specific
tests succeed, this PR remains **candidate-only / HOLD**. It does not authorize a Synexia
or M3JDK21 backport or a 99% coverage claim.

## Future mechanical transfer

After proving the Nebula exact recipe against real Maven/JUnit/Tycho:

```text
Nebula target proof
  -> Synexia canonical reusable publication/receipt recipe custody
  -> M3JDK21 target-specific backport DAG and receipt writer
  -> target Java 21 compilation/jtreg/runtime tests
  -> unchanged second pass
  -> serial promotion
```

This is a small independently replaceable recipe atom in the wider
INVENTORY -> ATOMIZATION -> PATTERNIZATION/IOP -> DOCUMENTATION -> FIXED_POINT graph.
