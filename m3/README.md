# M3 Nebula recipe-first control plane

## Purpose

Nebula already has an authoritative Maven/Tycho build. M3 does not replace it.

The M3 sidecar Maven reactor inventories and mechanically converges Java source before any broad
refactor or donor absorption:

```text
Nebula Java source
  -> OpenRewrite inventory
  -> FILE-local atomization
  -> FILE-local patternization / IOP role
  -> semantic documentation
  -> second-pass fixed point
  -> candidate postimages + content-addressed receipt
  -> reviewed apply in a disposable branch/worktree
  -> original Nebula/Tycho mvn verify
  -> UI/SWT tests
  -> serial promotion
```

The canonical product build remains:

```text
mvn verify
```

from the repository root.

## Initial atom family

The first admitted atom family is the already-proven M3 private-static pure-`int` expression leaf
from `hsoliwal/M3jdk21`.

The donor is first-party M3 tooling, not an external widget implementation. Its mechanical domain is
intentionally small:

- private static method;
- `int` return type;
- only `int` parameters;
- one return statement;
- expression composed only from primitive int parameters/literals, parentheses, safe unary
  `+ - ~`, and non-throwing primitive binary arithmetic/bitwise/shift operators;
- no division/remainder, allocation, field access, call, I/O, synchronization, exception path,
  visibility change or member-surface change.

The transformation introduces one local named atom and source-only M3 IOP/documentation markers.
It does not add a runtime dependency.

Broader atom families are added only after their own JUnit proof.

## Candidate-only execution

The control plane never writes Nebula product source.

It scans repository Java files while excluding generated/control areas such as:

- `.git/`;
- `target/`;
- `build/`;
- the M3 sidecar itself.

For every source file it writes a deterministic receipt containing:

- repository-relative path;
- SHA-256 preimage;
- SHA-256 postimage;
- atomization/patternization/documentation change flags;
- fixed-point state;
- status/message;
- candidate postimage path when changed.

Candidate files live only below the sidecar Maven `target/`.

## Contract rule

FILE-local atomization is admitted only while externally observable behavior and member/interface
surface remain unchanged.

The M3 sidecar has no authority to:

- change public/protected/package contracts;
- change SWT/OSGi metadata;
- modify MANIFEST.MF or feature/product descriptors;
- change widget behavior;
- copy donor widget source;
- merge or promote product code.

Any transformation crossing FILE scope must be a separate reviewed recipe with explicit scope
promotion.

## Native/JNI rule

This Nebula snapshot is Java/SWT/Tycho oriented. No JNI/native accelerator is invented merely to
satisfy the M3 architecture.

If a future hot path has a measurable pure primitive kernel suitable for JNI, the order is:

```text
Java oracle
  -> donor/mechanical review
  -> JNI candidate
  -> Java/JNI differential parity
  -> lifecycle/fallback proof
  -> benchmark including setup cost
  -> optional publication
```

## Donor programme

Fast-search and algorithm donor review remains catalogue-first.

For each applicable shape, review is ordered across:

1. existing repository/M3 owners;
2. permissively licensed GitHub implementations;
3. LeetCode category evidence;
4. HackerRank category evidence;
5. GeeksforGeeks category evidence.

Challenge problem/editorial bodies are reference evidence, not automatic copy authority.

A donor is used only when it improves a real Nebula atom and its license/provenance/contract fit is
clear.

## Saved recipe and branch

The reusable recipe is stored in this repository under the M3 Maven sidecar and must reach a
second-pass fixed point before any product source application.

This branch is intentionally additive and keeps the original Tycho structure unchanged.
