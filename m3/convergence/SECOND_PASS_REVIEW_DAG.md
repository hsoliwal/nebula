# Nebula candidate binding to the saved M3 second-pass review DAG

Nebula master already owns the merged runtime second-pass binding through
`NebulaM3SecondPassBinding`. This document does **not** replace that binding.

The candidate custody owner `NebulaM3SecondPassReviewDagBinding` records the exact unmerged
Synexia review-DAG candidate:

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

The current Synexia hosted workflows fail before creating any jobs, so
`HOSTED_PROOF=WORKFLOW_STARTUP_BLOCKED_NO_JOBS`. This binding must not replace the merged Nebula
runtime recipe until the upstream saved DAG is merged and an execution-capable runner proves its
focused tests.
