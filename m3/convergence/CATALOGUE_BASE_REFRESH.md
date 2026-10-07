# Nebula catalogue base refresh

## Problem

`m3/convergence/catalogue-plan.json` is intentionally content-addressed to a reviewed Git base.

The current plan names:

```text
62ef3a8135e5d8b57fdd5f5c3c6c5f6c95871e07
```

while the current reviewed `master` baseline for this repair is:

```text
85f3f66e4c9b8baa7f6fb4781b96056c4d805964
```

The catalogue launcher correctly refuses a candidate when changes between the reviewed base and
candidate HEAD extend outside its explicit convergence-wiring allowlist. Therefore changing or
ignoring that guard would weaken source custody.

## Recipe-first repair

The repair is split:

1. land a reusable exact PlainText source-snapshot recipe and a source-sealed
   `NebulaM3CatalogueBaseRefreshRecipe`;
2. prove exact old-plan -> new-plan transformation and second-pass fixed point;
3. only after the recipe is independently admitted, materialize the one JSON preimage update in a
   stacked branch whose reviewed base includes the recipe owner.

The recipe-only branch does not change `catalogue-plan.json`.

## Exact semantic delta

Only `base_revision` changes.

The following remain byte-identical:

- schema version;
- `widgets/**` namespace restriction;
- local convergence recipe identity;
- local recipe artifact coordinate;
- `org.openrewrite.java.RemoveUnusedImports` identity;
- recipe order.

## Authority

This refresh grants no recipe execution or promotion authority. It merely makes a future catalogue
application candidate refer to a reviewed current base while preserving the existing
`prior <= WIRING` admission law.

The original Tycho reactor and fixed-point/candidate proof remain the repository oracle.
