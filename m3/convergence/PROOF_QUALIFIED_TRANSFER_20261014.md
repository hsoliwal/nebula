<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 Nebula pilot qualification receipts

**State:** candidate-only. This document does not assert green Maven, Tycho, coverage or platform proof.

## Why this exists

`NebulaM3TransferContractCli` already emits the content-addressed recipe DAG and the
non-mutating Camel/Airflow/Drools projections. Those files are useful *planning evidence*,
but neither their existence nor their SHA-256 roots establishes that Nebula's target-specific
Java 21, JUnit, fixed-point, Tycho, SWT and 99% semantic coverage gates ran successfully.

A missing run must not become a fabricated PASS.

## Receipt contract

`NebulaM3ProofGate` is a small Java-21 evidence atom. It records exactly one status in canonical
order for each required proof stage:

```text
SOURCE_INVENTORY
  -> RECIPE_JUNIT
  -> ATOM_BEHAVIOR
  -> PATTERN_IOP
  -> DOCUMENTATION
  -> JAVA21_COMPILE
  -> FIXED_POINT
  -> SEMANTIC_COVERAGE_99
  -> M3_REACTOR
  -> TYCHO_REACTOR
  -> SWT_PLATFORM_RUNTIME
  -> DIFF_AND_PROVENANCE
```

Statuses are `NOT_RUN`, `HOLD`, `FAILED`, `PASSED`. A PASSED row requires a lowercase
64-character SHA-256 evidence fingerprint. Non-PASSED rows must use `-`, never a guessed
fingerprint. Every stage is present exactly once in enum order; duplicates/omissions reject
the receipt. Source and canonical Synexia Git commit identities are required before a receipt
can be complete; `UNBOUND` is allowed *only* for an unproven hold.

Even if all stages are PASSED, the best disposition is `MANUAL_REVIEW_REQUIRED`, never
`PROMOTABLE`, `MERGED`, or permission to edit source. Receipt hashing binds the source
commits, DAG root, ordered proof statuses and the explicit absence of edit/promotion
authority. This class **does not authenticate external logs**; a trusted CI or reviewer
must independently validate each referenced evidence artifact.

## Transfer integration

The existing transfer CLI gains exactly one additive file:
`transfer-qualification.tsv`. When called without externally verified proof receipts
it writes `HOLD`, `UNBOUND` commits and `NOT_RUN` for all stages. Existing transfer
TSVs, `transfer.sha256`, scheduler projections and public widget sources are unchanged.

This prevents a generated transfer bundle from silently appearing fully qualified.

## Canonical recipe and adoption

The original reusable source mutation authority remains in
`hsoliwal/com.synexia`. Target-specific Nebula source files are EPL-2.0; a Synexia
hash-pinned Java LST recipe crate must bind their exact module-relative pre/post images
and tests, including stale preimage refusal and second-run fixed point.

No automatic application to `hsoliwal/M3jdk21` or `hsoliwal/com.synexia` occurs.
Once the complete Nebula proof is externally verified, each receiver performs its own
inventory, Java-21 compatibility, contract/JNI tests, fixed-point proof and serial
promotion. The JDK 22–27 JEP/tooling backport denominator remains independently open.
