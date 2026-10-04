# Executable statement, block and loop composition

The reusable source-changing recipe is owned by Synexia PR #8837:
https://github.com/hsoliwal/com.synexia/pull/8837
Pinned recipe revision: 6d5164334ce8d160fbbad63d363dc01e8fadd6df
Crate: synexia-openrewrite-recipes/recipes/statement-atoms
Named composition: com.synexia.m3.StatementBlockComposition

An executable statement is an atom. Blocks and loops are composite atoms. The existing
OpenRewrite LST retains lexical source/state; no class, method or heap wrapper is required per
statement. The atom view records parent/order/pattern/hash and supports explicit, source-pinned
statement or whole-loop replacement. That primitive does not authorize arbitrary semantic changes.

The first source normalization makes implicit control-body sequences explicit, preserving the
original statement objects, labels, comments and empty bodies. It does not migrate APIs, move
statements across lexical/control boundaries, or discard native/UI behavior.

The prior exact-source local run (bd495742f68e57e63ef3e81b400f09f42a96f5ce) covered all 1756
application Java inputs, generated 513 source postimages, and passed exact inverse/reparse/fixed
point checks for every file. This is historical evidence, not a claim about this later checkout.
The guarded workflow reruns the same pinned recipe against this PR's exact head and records its
own complete inventory/counts before materializing generated Java onto this feature branch.

## Guarded materialization

Only the same-repository branch m3/statement-block-atoms-20261004 is a write target. The job
builds/tests the pinned shared recipe and the existing qualified parser, executes the complete
input pass, verifies original/candidate hashes, then makes one append-only generated-source commit.
No master/develop write, force push, rebase, merge, secret access or permission change is performed.
No existing workflow or test gate is weakened. Generated sources are review candidates.

Evidence files are input-sha256.tsv, files.tsv, summary.properties and execution.properties.
The full atom table and patch are uploaded as a workflow artifact. Existing exact-source repair
recipes remain separate; normalizing their source changes their seals and does not automatically
requalify them. Whole-project type attribution, Tycho/API/native/UI/platform acceptance remain
promotion gates. A completed statement pass is not a completed JNI optimization or full build.
