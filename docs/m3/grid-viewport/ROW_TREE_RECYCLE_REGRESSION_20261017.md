# GridRowTree slot-recycling and failure-atomicity follow-up (2026-10-17)

**Status: DRAFT / receiver test candidate only; no product GridRowTree edits.**

## Lineage and source identity

- SWT intrinsic viewport integration: `hsoliwal/eclipse.platform.swt#201` and `#203`.
- Nebula Grid: `hsoliwal/nebula#234` (mediated viewport) and `#235`
  (AVL `GridRowTree`, later legacy regression tests).
- Working Grid source snapshot: Nebula `#237` (already merged).
- Attribution: Nebula `#238`.
- Original reviewed Nebula `master@91c474935eb4a06e46716a0188b9082f0af0eda9`.
- `GridRowTree.java` existing Git blob SHA-1:
  `6b3dac2acb24e6953cf1bdae417c59b47ddbaa73`.
- Canonical Synexia recipe-first candidate:
  [Synexia #12076](https://github.com/hsoliwal/com.synexia/pull/12076),
  stacked on the existing viewport catalogue recipe branch, reusing
  `M3InsertStatements` rather than a second rewrite system.

## Failure boundary in original owner

The source currently invokes `removeAt(root, index)` before calling
`release(slot)`. `release` may expand `free[]` with
`Arrays.copyOf`, which can fail after detaching a row but before
`item.leftRowTree(index)` and `modCount++`.

The recipe inserts the already-existing free-array growth operation before
`root = removeAt(root, index)` in exactly `remove(int)`. A failed allocation
can then leave the AVL tree unmodified. The existing `release` logic is not
replaced; the reserved capacity makes a second allocation unnecessary.

This is a **candidate**, not a proven product fix. It does not rewrite
other contributors' or agents' files, change public interfaces, alter default
viewport/original modes, introduce a wrapper, or replace rendering backends.

## Reviewable receiver test

`GridRowTreeRecycle_Test` is a new JUnit4 source file next to
`GridRowTree_Test`. It creates a real SWT Grid and exercises 128 serial
remove/reinsert steps, covering first/middle/end positions, root/child
topology, variable heights, expansion, ranks and pixel-prefix answers.
It compares the original `GridRowTree` against a fresh linear oracle after
every mutation. Original mode skips the tree-specific test, like existing
`GridRowTree_Test`.

The test checks **normal-operation parity**, not OOME. OOME proof requires
a source-verified allocation-failure seam or equivalent deterministic
preflight-failure injection; process-level OOME alone is too unreliable as
a required CI assertion.

## Proof and promotion

1. Require exact source identity and the canonical Synexia recipe test to pass.
2. Execute the named OpenRewrite recipe in a disposable Nebula checkout.
3. Confirm pass 1 changes only `GridRowTree.remove(int)`; pass 2 is
   `UNCHANGED`. Refuse source drift instead of overwriting another change.
4. Run `GridRowTreeRecycle_Test` and `GridRowTree_Test` through the
   original Grid/Tycho test runner (or the existing `full-grid-contracts`
   profile); test viewport and original modes.
5. Run 604-case legacy Grid regression, million-row distant scrolling,
   native events, screenshots, SWT Windows/macOS/Linux and GPU/Skija opt-ins
   where supported. Record actual head CI checks rather than asserting them.
6. Only after proof, publish the recipe-generated **one-method production
   candidate as a separate PR**. Keep all prior branches and PRs intact.

No Maven, native SWT/OS rendering, screenshot or fault-injection proof
has been executed by this PR authoring turn. Never rebase Synexia develop,
force-push branches or auto-merge this draft.
