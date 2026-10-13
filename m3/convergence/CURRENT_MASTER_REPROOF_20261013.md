# Nebula current-master recipe/DAG reproof — 2026-10-13

This branch is a proof trigger and custody refresh only. It changes no Nebula widget/runtime Java,
JNI/native source, public API, renderer behavior, SWT behavior, donor implementation body, or
automatic promotion authority.

## Exact receiver

- repository: `hsoliwal/nebula`
- master preimage: `eab2a65c451c8e66df1004dcbcb8d892356d8862`
- proof state: `REQUESTED_CURRENT_NEBULA_REPROOF_20261013`

## Canonical recipe custody

The reusable recipes remain owned by `hsoliwal/com.synexia`.

Current Synexia `develop` observed for this reproof:
`46e296ad537c6ecbd62b9783874e8f1714e278a9`.

The historically pinned recipe commits are deliberately retained because their exact Git blobs are
byte-identical to the current canonical owners:

- `M3PureIntConvergenceRecipe`: blob
  `c2a7a3c93aa278d234c2ccfe59c0e1eb78161b95`;
- `M3AtomPatternSignalChainRecipe`: blob
  `09df87a872817787fc8607bf11805cfa26c53aed`;
- `M3SecondPassReviewDagPlan`: blob
  `a98b490872fc8adbeb3e509fda31e478a5dc1f37`.

Repinning identical bytes would reduce provenance clarity without changing behavior.

## Canonical recipe DAG

Small OpenRewrite recipes are the semantic mutation atoms. The canonical Nebula FILE convergence
path remains:

```text
INVENTORY
  -> ATOMIZATION
  -> PATTERNIZATION / IOP
  -> DOCUMENTATION
  -> FILE FIXED POINT
```

The larger read-only review DAG also retains challenge/donor review and JNI contract inventory.
Camel, Airflow and Drools/KIE are projections/admission mechanisms over the same content-addressed
DAG. Maven/OpenRewrite remains the mutation/proof execution root. No scheduler receives direct
source mutation or promotion authority.

## Required hosted proof

The proof branch must make GitHub execute, not merely describe:

1. diff/custody gates;
2. Nebula-local recipe JUnit suite;
3. content-addressed recipe DAG export;
4. Camel/Airflow/Drools projection checks;
5. whole Java inventory with zero parse failures;
6. exact bound Synexia recipe installation;
7. FILE-local atomize -> patternize/IOP -> document candidate execution;
8. second-run byte-identical fixed point;
9. original Nebula Tycho reactor on the transformed candidate;
10. Java behavior oracle and Java-before-JNI review;
11. candidate-only/no-automatic-promotion assertions.

Until those hosted jobs pass, this document does not claim GREEN status.

## Backport/export sequence

After this Nebula proof is green, the same verified recipe/DAG—not hand edits—is the transfer unit
for `hsoliwal/M3jdk21` and then canonical Synexia target/application lanes. M3JDK21 JEP/tooling
backport work remains a separate compatibility ledger and is not replaced by this proof.
