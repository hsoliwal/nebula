# Nebula M3 full second-pass review DAG binding

Nebula now carries an additive, read-only binding to the saved Synexia full second-pass review DAG
candidate from PR #8973. It does not copy the Synexia implementation and it does not change Nebula
widget source.

Pinned upstream candidate:

- repository: `hsoliwal/com.synexia`
- branch: `feat/m3-second-pass-review-dag-20261005`
- commit: `5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f`
- PR: `#8973`
- plan: `com.synexia.m3.recipe.M3SecondPassReviewDagPlan`
- DAG id: `m3_second_pass_review`
- Nebula declarative wrapper: `org.eclipse.nebula.m3.SecondPassReviewDag`

The four external review atoms are serial and FILE-scoped:

```text
M3CodeSignalTriggerRecipe
  -> M3AtomPatternSignalChain
  -> M3ProblemRecipePlanner
  -> M3JniContractInventoryRecipe
```

Their canonical Synexia Maven profiles are:

1. `m3-code-signal-review`
2. `m3-atom-pattern-signal-chain`
3. `m3-problem-recipe-planner`
4. `m3-jni-contract-inventory`

Every atom remains `DRY_RUN` and requires `DIFF/LINT/COMPILE/TEST/FIXED_POINT` evidence.
Camel and Airflow are execution/scheduling projections of the same canonical upstream DAG; Drools/KIE
is admission evidence only. None receives mutation or promotion authority.

## Signal semantics

The signal stage remains:

```text
AST/LST inventory
  -> structural signals
  -> control-flow signals
  -> effect/contract signals
  -> masked-regex nomination cues
  -> pattern candidates
  -> contract-risk classification
  -> typed residue / fixed point
```

Regex is nomination-only after Java literal/comment/text-block masking. It cannot certify semantic
equivalence or authorize a transform.

## Donor review

The problem/donor atom retains deterministic challenge review order:

`LeetCode -> HackerRank -> GeeksforGeeks -> pinned GitHub donor/license evidence`.

Challenge/editorial/solution bodies remain reference evidence only.

## JNI review

The final atom inventories Java native declarations, JVM descriptors, JNI short/long linkage
candidates and ownership evidence. Java remains the oracle. Native acceleration still requires a
differential corpus, lifecycle/fallback proof, Java/JNI parity and a setup-inclusive benchmark.

## Execution

Build/install the local Nebula recipe artifact, then install the exact pinned Synexia recipe artifact.
The explicit Nebula profile is:

```bash
mvn -B -ntp -f m3/reactor.xml install
mvn -B -ntp -Pm3-second-pass-review-dag rewrite:dryRunNoFork
```

This profile is not lifecycle-bound and is read-only. The current upstream PR is still draft, so the
binding remains `PENDING_HOSTED_PROOF`. No merge/promotion authority is implied by this branch.
