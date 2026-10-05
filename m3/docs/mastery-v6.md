# Nebula M3 V6 mastery gate

## Purpose

Nebula already owns a local recipe-first convergence DAG, a compiler/runtime A3 mastery laboratory,
and a no-root-write explicit-file materializer.

This gate does not duplicate those owners. It adds cross-repository custody for the canonical
Synexia M3 recipe-mastery V6 receipt before a stronger mastered materialization lane may call the
existing `NebulaM3A3Apply`.

The ordinary Eclipse Nebula Tycho/Maven reactor remains the product build/test authority.

## Portable V6 evidence

The gate consumes strict `M3_RECIPE_MASTERY_FANIN_V6` key/value TSV.

V6 binds:

- Atomize/Patternize compiler and behavior convergence;
- fixed-point recipe schedules;
- permanent minimized counterexample replay;
- LeetCode/HackerRank/GeeksforGeeks donor-review and serial pass-ledger roots;
- the deterministic 10,000-case regex/string matrix;
- the JDK regex oracle root;
- optional Java/JNI static-signal parity.

The caller must also provide the reviewed expected V6 root.

Nebula independently recomputes the framed SHA-256 root and requires equality with both the receipt
and the caller-pinned root.

## Mastered file materialization

The existing `NebulaM3A3Apply.run(...)` remains unchanged.

The additive gate path is:

```text
V6 receipt + pinned root
    -> NebulaM3MasteryV6Gate
    -> existing NebulaM3A3Apply
    -> candidate + FILE receipt under m3/recipe-first/target
    -> normalized mastery-v6.tsv evidence
```

No widget/product source is written.

## Fail closed

The gate refuses:

- unknown/missing/duplicate/out-of-order TSV keys;
- non-V6 schema;
- SHA-256/root drift;
- caller-pinned root drift;
- incomplete receipts;
- any mutation/semantic/donor-copy/replacement/merge/promotion authority;
- regex case count other than 10,000;
- missing tri-platform donor evidence;
- malformed or inconsistent JNI evidence.

## Recipe-first delivery

The gate and JUnit proof are themselves delivered by a create-only source-sealed OpenRewrite recipe:

`NebulaM3MasteryV6GateRecipe`

The recipe is anchored to the existing Nebula Java-21 convergence owner, targets only M3 tooling/test
paths, refuses foreign occupied bytes, and grants no product-source or promotion authority.

The product Tycho/Maven build remains a later independent gate; V6 admission never implies a Nebula
release or merge is safe.
