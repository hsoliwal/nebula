# Nebula A3 file materializer

## Purpose

Nebula already has:

- whole-checkout Java inventory;
- a Java 21 OpenRewrite convergence recipe;
- an ordered FILE-local recipe DAG;
- fixed-point recipe tests;
- fast-search donor/category review;
- Java-before-JNI admission policy;
- the original Tycho reactor as behavioral oracle.

The missing execution atom is an explicit serial per-file materializer.

The materializer does not author another refactoring algorithm. It reuses:

`NebulaM3FileConvergenceRecipeDag.fixedPointRecipe()`

for every explicitly selected Java file.

## Execution

For each requested source path:

1. resolve it beneath the pinned repository root;
2. require a regular Java file outside `.git/`, `m3/` and build `target/` trees;
3. read the exact UTF-8 preimage;
4. parse Java 21 through OpenRewrite;
5. require lossless parse/print;
6. run the existing convergence DAG;
7. reparse the candidate;
8. run the same DAG again;
9. require zero second-pass changes;
10. write only a candidate copy below `m3/recipe-first/target/`;
11. write before/after SHA-256 and fixed-point receipt.

The original source file is never the output target.

## No-root-write invariant

Output is accepted only beneath:

`m3/recipe-first/target/`

A caller cannot redirect candidate output into a widget module, source package, repository root or an
external path.

The candidate retains the original repository-relative path beneath the output's `candidate/`
directory.

## Contract

This stage grants no semantic promotion authority.

A changed candidate still needs:

```text
candidate diff
 -> contract review
 -> original Nebula/Tycho compile + tests
 -> UI/visual gates where applicable
 -> Java/JNI parity if native work is later admitted
 -> second-pass fixed point evidence
 -> serial promotion
```

A no-change candidate is reported honestly with equal before/after SHA-256.

## Why file-by-file

Contract-preserving FILE transformations are mechanically independent. The same recipe can therefore
be improved once and replayed across arbitrarily many files without granting wider PACKAGE/MODULE/API
authority.

When a transformation crosses a boundary, the existing M3 scope/promotion rules take over; this tool
does not infer equivalence across that boundary.

## JNI boundary

Nebula currently has no retained native C/C++ owner. The materializer therefore remains Java-only.

A future JNI candidate must first satisfy `NebulaM3JavaBeforeJniPolicy`:

```text
Java oracle
 -> differential corpus
 -> lifecycle/fallback proof
 -> setup-inclusive benchmark
 -> bounded native candidate
 -> parity
```

JNI is not introduced by the file materializer itself.

## Recipe-first ownership

The materializer and its tests are themselves installed by a source-sealed OpenRewrite recipe before
their reviewed postimages are materialized onto the branch.

This keeps the M3 law recursive: the mechanism used to fan out recipe work is itself recipe-owned and
fixed-point proven.
