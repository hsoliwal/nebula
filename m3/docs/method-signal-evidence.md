# M3 method-signal evidence

## Purpose

Nebula's existing M3 inventory and review DAG is file-local. That is useful for repository
triage, but it is too coarse for algorithm replacement or JNI admission: one file can contain a
single search loop, unrelated UI methods, and a native declaration with completely different
contracts.

This pass adds a method-level evidence projection without changing product source.

## Reuse-first architecture

The existing owners remain authoritative:

- `NebulaM3InventoryRecipe` remains the parser/inventory owner;
- `NebulaM3FastSearchReviewPolicy` remains the challenge/GitHub review owner;
- `NebulaM3JavaBeforeJniPolicy` remains the native admission owner;
- `NebulaM3RepositoryReviewRecipe` remains the composed review entry point.

The pass adds no second parser, search engine, JNI runtime, donor catalogue, or promotion path.

Synexia's canonical recipe estate already models Java methods as first-class atoms and attributed
method edges. Nebula therefore adds only the missing target-specific projection: structural/search
facts for each existing OpenRewrite method LST.

## Method evidence

For each Java method/constructor the inventory records:

- source path;
- owner type;
- stable method key;
- simple method name;
- contract surface;
- native declaration flag;
- statement count;
- loop count;
- branch count;
- exception-boundary count;
- synchronization count;
- lambda count;
- method invocation count;
- indexOf/lastIndexOf calls;
- contains/containsKey/containsValue calls;
- sort calls;
- binarySearch calls;
- structural pattern set;
- fast-search signal;
- deterministic next review action.

Local/anonymous-class methods remain independent method rows; their bodies are not charged to the
enclosing method's search/loop counts.

## Method-level donor review

The existing fast-search policy is reused unchanged in meaning.

Method rows are mapped to the same categories and same deterministic pass order:

1. LeetCode;
2. HackerRank;
3. GeeksforGeeks;
4. pinned GitHub donor.

Challenge sites remain reference-only. No solution/editorial source is copied.

File-level review rows are retained for compatibility. New method-level rows narrow the candidate
coordinate for later recipe work.

## Method-level Java-before-JNI review

The existing Java-before-JNI policy is reused at method scope.

A native method receives the existing `REVIEW_EXISTING_NATIVE_DECLARATION` decision.

A non-native method with a loop plus a fast-search signal receives the existing
`PROFILE_JAVA_HOT_PATH_BEFORE_JNI` decision.

Everything else remains `NO_NATIVE_ACTION`.

No method-level row grants native execution or promotion authority.

## Recipe-first delivery

The M3 control-plane change is itself installed by a source-sealed OpenRewrite `ScanningRecipe`.

The recipe owns exact current M3 preimages and reviewed postimages, can add only the reviewed
method-evidence classes/tests, refuses drift, reparses every Java postimage, and reaches a
second-pass fixed point.

The intended history is:

```text
contract/docs
 -> sealed reusable recipe + postimages
 -> materialize recipe-owned M3 control-plane sources
 -> focused Java 21 tests
 -> repository evidence execution
```

## Product boundary

This pass does not change any Nebula widget, SWT behavior, Tycho product source, public API, native
library or event ordering.

A later method optimization still requires:

```text
method evidence
 -> donor/category review
 -> exact contract decision
 -> dedicated recipe
 -> differential proof
 -> apply
 -> compile/test/Tycho/native/UI gates
 -> benchmark when performance is claimed
 -> serial promotion
```
