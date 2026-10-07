# Recipe-first Nebula visible-range atom

Original donor: `hsoliwal/nebula@b7dd5097f62ba5ea2515b98311ea20e900642384`.
This extends GridVisibleRangeSupport in place; it does not introduce another widget,
public collection framework, spelling store or native widget residency mechanism.
The original root and Grid Maven/Tycho POMs are unchanged.

## Applied behavior-preserving transformation

`com.synexia.m3.NebulaVisibleRangeCompatibility` in `hsoliwal/com.synexia`
uses the existing source-sealed OpenRewrite installer. It replaces the two duplicate
remove-first-equal blocks with the package-private GridVisibleRangeDiff atom.

For ordinary identity-equality objects, temporary identity-indexed occurrence counts
replace repeated ArrayList search/removal. Expected work is O(old + new), with O(unique
old identities + emitted differences) temporary storage. Stationary ranges skip index
construction. Hash-table collision worst cases are not claimed constant-time.

Duplicates cancel their first old occurrences; remaining old/new references preserve
encounter order. Nulls are supported. Classes overriding equals keep the exact original
list algorithm, including equals-call direction, exceptions and side effects. User
hashCode is never introduced into that path. Reflection denial selects the fallback.
ClassValue caches only immutable equality classification, not widgets or source arrays.

The default paint path remains non-cancellable. The package-private overload accepting
Runnable adapts an existing monitor::checkCanceled; null still checks interruption.
Callbacks must not mutate input snapshots. No partial diff is published on cancellation.

Listener registration, paint timing, event array construction, listener iteration,
exception propagation and oldRange publication remain verbatim. This is deliberate:
`removedColumns` currently reads the added-column list. The compatibility recipe does
NOT silently change this observable behavior. The separate opt-in recipe
`com.synexia.m3.NebulaRemovedColumnsContractRepair` corrects that one assignment to agree
with its documented meaning. It requires the exact optimized source and is not activated
by this branch. Its template is retained in the canonical Synexia recipe crate.

## Verification

Run `bash .m3/nebula-visible-range/verify.sh` with JDK 21. It compiles/executes the actual
production diff and a deterministic differential suite, then compiles the helper at
Java 8 source/API level. The helper tests need no simulated SWT classes.

`mvn -f .m3/nebula-visible-range/pom.xml verify` is a focused Maven test entry, NOT the
whole original build. Full acceptance still requires the original Nebula Tycho build,
resolved SWT dependencies, actual paint/event tests and platform behavior checks.
The compiled helper alone does not establish UI compatibility.

The canonical Synexia proof lane additionally runs the existing M3FileAtomizer and
repaired M3JavaAtomizer on source pre/postimages, compares existing declarations and untouched
member atoms, tests the existing source-seal manifest, and compares the existing optional
Java/JNI Hamming-count kernel. Similarity results only nominate reuse candidates; they
never authorize replacement. No JNI method or raw object address is added to Nebula.

Problem-category reviews are in CATEGORY_REVIEW.tsv. No challenge solution code was
copied. The Eclipse source/resources keep EPL-2.0; Synexia recipe implementation keeps
its own Apache-2.0 license. Larger donor/UI parity and repository-wide acceptance remain
separate from this bounded source-changing pass. Never rebase develop.
