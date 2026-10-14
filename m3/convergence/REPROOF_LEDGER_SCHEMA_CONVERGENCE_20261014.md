<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 Nebula reproof ledger: exact schema-header convergence

Date: 2026-10-14. State: **CANDIDATE_ONLY / VERIFICATION_PENDING**.

## Inventory

The 2026-10-13 current-master ledger has a literal header field
`nebula_master_preimage`, but
`NebulaM3CurrentMasterReproofTest.proofLedgerBindsCurrentMasterAndByteEquivalentCanonicalRecipes`
checks `master_preimage`. These are not interchangeable source bytes. The October 13
consolidation PR reported this same JUnit failure on the then-current master; it was
not a widget behavior defect.

## Scope and change owner

Only the test assertion's expected TSV header spelling changes.
The data ledger, its source/commit SHA columns, fixture rows, canonical Synexia
recipe hashes, OpenRewrite DAG, and Eclipse Nebula widget source remain unchanged.
This is FILE-local test-schema alignment and grants no semantic equivalence,
source-copy, product-source mutation or promotion authority.

The reusable mechanism is the existing exact Java LST snapshot recipe.
`NebulaM3CurrentMasterReproofHeaderRecipe` is one bounded, EPL-2.0 Nebula
proving fixture, **not** a competing canonical Synexia recipe implementation.
The generalized M3 recipe still belongs in `hsoliwal/com.synexia`.

## Exact file identities

- input: `m3/recipe-first/src/test/java/org/eclipse/nebula/m3/NebulaM3CurrentMasterReproofTest.java`;
- source SHA-256: `975600aaa0dc984ac9c73e1d4fc446a0872180047701598bcc475cb9978ee7d0`;
- expected postimage SHA-256: `b424349855198f0dab5f0ba393b2195bd0e603f2af6ca82d8cebf6129dad3a8b`;
- exact semantic delta: `schema\\tmaster_preimage` becomes
  `schema\\tnebula_master_preimage` in the JUnit expected header only;
- recipe: `org.eclipse.nebula.m3.CurrentMasterReproofHeader`;
- Maven proving profile: `m3/recipe-first/pom.xml -Preproof-header`.

Both Java source images are stored in the Nebula EPL-2.0 proving crate. The original
test file stays untouched in the PR; CI applies the recipe only to its transient
checkout after source-bound recipe tests pass.

## Recipe before target

1. Read the current Java test preimage and TSV fixture verbatim.
2. Pin the exact original and expected source SHA-256.
3. Put the reviewed postimage in the recipe's classpath resources.
4. Run OpenRewrite/JUnit exact-file replay, negative source/path drift tests,
   and unchanged second-pass fixed point.
5. Only if this succeeds, apply the recipe to the test in a candidate checkout.
6. Run the real `NebulaM3CurrentMasterReproofTest` through Maven/Java 21,
   then the complete `m3/reactor.xml verify` and the native Tycho reactor.
7. Keep any cross-repository handoff in HOLD until the hosted proof and
   checksum/readback receipts exist.

## Full programme boundary

Nebula is already Maven/Tycho-based and has OpenRewrite small-atom
INVENTORY -> ATOMIZATION -> PATTERNIZATION/IOP -> DOCUMENTATION -> FIXED_POINT
DAG support, including Camel/Airflow/Drools **non-mutating** projections.
Do not invent another Nebula atomizer, adapter stack, or scheduler.

After Nebula gates pass, reuse the **proven** canonical Synexia Maven/OpenRewrite
recipe/DAG with target-specific inventories for `hsoliwal/M3jdk21` and
`hsoliwal/com.synexia`; perform no automatic cross-repo copy, merge, or
source promotion. M3JDK21's JEP/PR/tooling backport ledger remains a separate
compatibility task, not a consequence of this test repair.

## Evidence / limitations

GitHub's current historical workflow evidence and this candidate commit alone
cannot establish green Maven, JUnit, coverage, SWT desktop behavior, JNI/native
parity, or Java 21 JEP backport completion. When a gate cannot run, mark it
`NOT_RUN` or `HOLD`, never `PASS`.
