# Actual compilation bootstrap before Nebula atomization

Pinned input: `hsoliwal/nebula@e4907bf9c19c414d78504c69665fae337b284d40`.

The actual GitHub runner, run 37186576874 attempt 1, job 111389652745,
compiled the recipe crate and rejected four errors: the missing review DAG
package/type and an `ordinal()` call on `ReviewPass`, which declares
`passOrder()`. The source-changing atom-pattern steps did not execute.

This crate reuses `NebulaM3ExactJavaSnapshotRecipe` for precisely two repairs:

- compose the three existing read-only review recipes in their documented order,
  rather than depend on a missing duplicate composition owner;
- use the declared 1-based challenge/donor pass order.

Public recipe/table interfaces and all widget source stay unchanged by this
bootstrap. Preimages/postimages, exact paths and original Git blob identities
are recorded in the resource manifest and fixtures. No new parser, rewrite
engine, native layer or promotion authority is introduced.

`pom-compile-bootstrap.xml` compiles only the existing snapshot engine and its
new wrapper/materializer so the broken parent crate does not prevent its own
recipe repair. Six tests execute real OpenRewrite replay, fixed point, mixed
postimages, source drift, missing source and cancellation. The workflow runs
those tests, applies the recipe in its isolated checkout, repeats, checks
nonempty successful Surefire reports, and then runs all the original full-crate,
whole-checkout and Tycho gates without weakening or replacing them.

The separate replay-input archive contains `git archive HEAD`, its exact Git
file inventory, the actual bootstrap patch, tool versions and the Maven
executable/distribution plus only the Maven repository cache. Git credentials,
Maven settings and token-bearing environment dumps are excluded. This is
execution-tooling custody, not an approved production dependency mirror and not
a claim that Nebula is atomized. The remaining first-stage work must consume
and verify these actual bytes, apply the source-changing recipes, and validate
the transformed full project before any completion claim.
