# Nebula custody binding to the merged M3 second-pass review DAG

Nebula master already owns the merged runtime second-pass binding through
`NebulaM3SecondPassBinding`. This document does **not** replace that binding.

The candidate-only custody owner `NebulaM3SecondPassReviewDagBinding` records the exact merged
Synexia review-DAG source commit:

- repository: `hsoliwal/com.synexia`;
- branch: `feat/m3-second-pass-review-dag-20261005`;
- commit: `5369fdc8c076b998b0dd39c7c67c85d11a4b2d8f`;
- PR: `#8973`;
- plan: `com.synexia.m3.recipe.M3SecondPassReviewDagPlan`.

The saved review DAG is:

```text
structural signals
  -> bounded atom/pattern signal chain
  -> LeetCode/HackerRank/GeeksforGeeks problem/donor planner
  -> JNI contract inventory
```

Every step is FILE-scoped and DRY_RUN-only in the Synexia plan. This Nebula binding grants no
source-copy, mutation, replacement, native-execution, merge, or promotion authority.

Synexia PR #8973 is merged. Its dedicated hosted second-pass workflow still fails before creating
any jobs, so `HOSTED_PROOF=UPSTREAM_MERGED_WORKFLOW_STARTUP_BLOCKED_NO_JOBS`. This binding therefore
remains candidate-only and must not replace the merged Nebula runtime recipe until an
execution-capable runner proves the focused DAG tests.
