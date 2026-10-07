<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Grid occurrence counters: applied Synexia v1 -> v2 recipe chain

This branch applies the serial canonical Synexia recipe chain to the existing private
`GridIdentityOccurrenceTable` owner. Synexia remains the reusable recipe/evidence owner;
Nebula contains only the applied product source and this receiver receipt.

## Recipe lineage

1. **v1 memory-layout convergence**
   - recovery/authority PR: `hsoliwal/com.synexia#9540`
   - merge: `1958345b06700cecc1ef767de2aa63e54f3b482e`
   - original SHA-256: `72e66147572991b0fe135d67c406505ca4a6f3fd2831a630e79385f9290d082d`
   - v1 SHA-256: `2ad8c923b978f07069744973dbaa2c7a3d2169282c106d2266e1f76116811d34`

2. **v2 hot-path refinement**
   - PR: `hsoliwal/com.synexia#9552`
   - merge: `f487ed322744b8160ac49712eb462ff087eb8fe7`
   - exact input: v1 SHA above
   - final SHA-256: `1e5849a1661e9f76115b8a17c58e07f9b6200aca766fc8b0851fd313fdef79eb`

Git blob lineage in this target is:
`061faa01d2ff41ec954d1068cd4ebe666338a5cf -> 35fa69c9309b6f55e8a9e37bee3da8aa15bcdf15 -> 92113e37af2db1ff64752154b9728f8a35bd90da`.

## Product change

v1 replaces three eager arrays with two lazily allocated arrays: identity references plus a packed
`long` containing independent remaining/matched 32-bit counters. v2 preserves that representation
and removes repeated hot-path field/array loads by holding the table and packed counter in local
values and writing each packed mutation once. Rehashing similarly snapshots the old key/count lanes.

Reference identity, multiplicity, null handling, probing, load factor, ordered occurrence phases,
cancellation behavior and package/public interfaces are unchanged. No challenge/editorial source is
copied. v2 introduces no new donor algorithm; it inherits the reviewed v1 donor catalogue.

## Executed proof

The exact final v2 source was compiled with Java 21
`--release 21 -proc:none -Xlint:all -Werror` and executed against:

- **116,281** exhaustive previous/current identity-range pairs;
- **300,000** seeded packed-counter transitions, including rehash/state interaction;
- **1,000,000** repeated-identity add/match/skip operations.

All passed.

The v1 memory proof remains authoritative: table-owned storage (excluding caller-owned keys)
improved from **280 -> 32 bytes** for empty/null-only use and **280 -> 256 bytes** for a populated
16-slot single-identity table. v2 retains the same allocation footprint in repeated diagnostics.

CPU diagnostics are intentionally not promoted to a general speedup claim. Repeated v1/v2 runs were
scenario-sensitive; v2 exists as a deterministic source-level hot-path simplification while
preserving v1's proven memory objective.

## JNI decision

No JNI is added for this invocation-local identity table. There is still no demonstrated
setup-inclusive native advantage sufficient to justify native allocation/lifecycle complexity.

## Donor/test catalogue

The canonical v1 donor review remains in Synexia and includes the Nebula owner, LeetCode multiplicity
shape, HackerRank sparse-frequency shape, rejected GeeksForGeeks duplicate-removal mismatch and
OpenJDK IdentityHashMap architecture. Java2s SWT/SWT-2D/Swing/Swing-Event material remains a
behavioral-test catalogue for the broader viewport/graphics distillation; none is copied into this
counter owner.

## Remaining repository-level qualification

GitHub hosted workflows currently report failure while creating **zero jobs**, so that is an
infrastructure/startup condition rather than a compiler/JUnit/Tycho verdict. Full Nebula
Maven/Tycho/native UI and OpenRewrite scheduler/Jupiter runs are therefore not claimed here.

The target branch is retained as the exact applied product branch. No recipe implementation is copied
into Nebula, no rebase/squash/force-push is used, and `swt-classic` is unaffected.
