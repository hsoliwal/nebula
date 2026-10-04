# Fast-search donor and challenge policy

This catalogue implements the M3 rule that algorithm reuse starts with a serial category review,
not with copying a solution into a target file.

## Challenge taxonomy

The initial taxonomy uses:

- LeetCode Binary Search study plan / search categories;
- HackerRank Algorithms subdomains, especially Search, Strings and Sorting;
- GeeksforGeeks Searching Algorithms.

These sites are **reference-only**. Problem statements, editorials and user solutions are not
repository source donors.

## GitHub donor custody

Pinned donor revisions are recorded in `fast-search.tsv`.

- **Apache Lucene** — Apache-2.0. Search automata, bounded queues and pruning mechanics may be
  studied and adapted when Nebula contracts match.
- **fastutil** — Apache-2.0. Primitive array/collection search mechanics are candidates when object
  boxing or repeated primitive lookup is demonstrated.
- **Caffeine** — Apache-2.0. Bounded/adaptive cache mechanics are evidence for deterministic derived
  state, not permission to add hidden unbounded caches.
- **OpenJDK** — GPL family. Timsort and related adaptive ordering mechanics are architecture-only in
  this catalogue. No OpenJDK source body is copied by this pass.

## Admission order

For each candidate source file:

```text
inventory signal
  -> problem/category classification
  -> challenge taxonomy comparison
  -> GitHub donor/mechanics comparison
  -> contract/equivalence decision
  -> CREATE/IMPROVE RECIPE or KEEP EXISTING
  -> fixture + differential test
  -> dry run
  -> review
  -> apply
  -> full verification
```

A faster algorithm is not automatically equivalent. Null handling, comparator ordering, stable tie
behavior, mutation, SWT thread affinity, event ordering and identity semantics are part of the
contract.
