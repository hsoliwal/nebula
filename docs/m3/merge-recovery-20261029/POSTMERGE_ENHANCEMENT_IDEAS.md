<!-- SPDX-License-Identifier: EPL-2.0 -->
# Postmerge lost-code and enhancement decision map — 29 October 2026

## Exact scope

This is an **additive follow-up to merged Nebula #298**, not a second
PR-census implementation. The authoritative 297-PR, 26 non-master
merge-head, 82 source-path and 17 prior candidate inventories are
already present under this directory in Nebula `master`. This pass
cross-references 12 selected source-contract/enhancement categories in
[`POSTMERGE_ENHANCEMENT_IDEAS.tsv`](POSTMERGE_ENHANCEMENT_IDEAS.tsv)
with newer merged PR results and the original Java owners.

Pinned GitHub readback at audit start:
- Nebula `master` `8c5e0f4259524efde12262c6fe0b63dcfa29931e`.
- The merged audit #298 captured original `master`
  `82b7a52c756e26d6075293043766df23291c00a5`.
- The historical Grid branch `dcd1a229bc9cbfac570e2c6e0de8091ffebb7ad2`
  and RWT branch `4821d7dacbab64af2c4023d9de8b2eb7d948c1eb`
  remain first-party source candidates, not approved repairs.
- Synexia merged #13837 is the canonical convergence/donor review
  for the Nebula Grid/RWT code and JSX admission overlaps. Reuse its
  catalogue instead of starting a competing recipient pipeline.

The present master was inspected directly for `M3VirtualTableBinding`
(blob `b47f2397d1919cd037b1e331ae7ada9e0fdfbcd8`),
`M3VirtualTreeBinding` (`74d04135c19f7c4067b4f16ca6016289456f3c62`),
and the generated `OrdersNativeSwt`, `ContactFormNativeSwt`,
`PreferencesNativeSwt` source blobs
(`7e36705ad13432e54a5179003429a94861eb93ca`,
`0bce8e60ffa6959aeaf0f276c6772afaec118028`,
`073313df1b6361b3a0709093a82929867a8aac6e`).
These are **retained**, not lost.

## Ranked recovery decisions

**P0 — dependency closure before any restoration.**
The historical Grid `M3GridRowBinding` and `M3GridSortHeader`
are absent on the original master paths, while their required
`org.eclipse.swt.m3` row and column model API belongs to a
different SWT feature lineage. Recovering these files in isolation
would not establish a working Grid. First compare signatures
against actual current SWT, preserve original `Grid` contracts,
and receive only bounded, independently tested sorting, stable-key
selection or row-delta atoms via canonical Synexia recipes.

The historical RWT `GC.textExtent(String,int)` calls
`M3GC.textForExtent`, a method **absent from the inspected historic
M3GC Git blob**. This is an unresolved source dependency, not a
ready-made optimization. Test tab, delimiter, mnemonic, Unicode and
RWT renderer semantics before adapting any historical code.
The deferred `M3CanvasPaint` and protocol-byte tests also need
a real RAP browser/session, phase and disposal proof.

**P1 — source compiler and framework admission.**
Original GWT/UiBinder parsing, typed/static XHTML and direct native
SWT form receivers already exist in Nebula after #274/#275/#279,
#280/#281/#282 and #283. Source-byte custody/JAR preflight
have subsequently landed through #288/#289/#291/#293/#294.
The additive improvement is one source-sealed, original Java/SWT
differential that preserves UTF-16, named/numeric entities, mnemonic
escapes, selected option *values* rather than display labels,
read-only state, event ordering and Java 11 bytecode acceptance.
The canonical TypeScript/OpenRewrite recipe stays with Synexia
[#13402](https://github.com/hsoliwal/com.synexia/pull/13402).
The existence of a public generated Java file does not prove the
original private recipe passed all current tests.

**P2 — reuse current owners, no rollback.**
The virtual Table and Tree source files are present on master;
#286 and #296 separately repaired publication-after-close and
recursive descendant refresh issues. The correct next
enhancement is to prove stable expansion identity and bounded
window invalidation on the *current* Tree owner, not to restore
the older unqualified source.

#297's variable-height Grid blit and hidden animation gating
were merged into master along with a 327-card widget inventory.
Follow-up viewport optimization must be per widget (selection,
clipping, disposal, reveal) rather than a new generic web canvas.
Browser lifetime, UTF-16/DOM and revision fencing work in
#284/#285/#287/#290/#292/#295 similarly remains current-source
authority.

**P3 — false-positive controls.**
The historic Gallery difference under #297 is only CRLF vs LF
normalization. #267's retained null-only row count, #268's
original Grid GC transform test, and #274's exact generated
Orders SWT Java source all survive. Do not create duplicate
recovery PRs for these.

## Existing history map and relationship

- [Merged Nebula #298](https://github.com/hsoliwal/nebula/pull/298):
  exhaustive Nebula PR-census evidence and the complete
  `PR_HISTORY.tsv`, `STACK_MERGES.tsv`,
  `BRANCH_SOURCE_RECEIPTS.tsv`, `RECOVERY_MAP.tsv` ledgers.
- [Merged Synexia #13837](https://github.com/hsoliwal/com.synexia/pull/13837):
  the narrower, canonical Nebula/RWT recipe and donor catalogue,
  including security/JSX collisions.
- [Open Synexia #13402](https://github.com/hsoliwal/com.synexia/pull/13402):
  unmerged native SWT form emitters and checked-in OpenRewrite
  source proof. Do not paste recipe sources into product Nebula.
- [Merged Nebula #297](https://github.com/hsoliwal/nebula/pull/297):
  qualified viewport source and the complete widget ledger.
- [Merged Nebula #296](https://github.com/hsoliwal/nebula/pull/296):
  recursive virtual Tree page invalidation and regression harness.

## Qualification and no-loss rules

1. An old PR being marked **merged** is evidence of Git history,
   not proof that every class or behavior survives under the original
   source path. Conversely, a branch being ahead or a historical path
   being absent is not evidence that behavior was lost.
2. Always compare exact Git blobs, current receiver signatures,
   dependencies and previously committed original tests.
   Never blindly replace current production Java.
3. Inventory each candidate file, classify the algorithm category,
   select donor and licensing authority, then develop a
   canonical Synexia JDT/OpenRewrite recipe.
4. Execute per-file serial passes and prove deterministic fixed point,
   source hash, original interface/behavior and test matrix.
   Require actual SWT/RWT/GWT compilation and display/browser
   testing before receiving original Java results into product.
5. Keep current `master`, original PR ancestry and all source
   attribution. No force-update, no synthetic passing CI claims.

`tools/M3PostmergeEnhancementAudit.java` verifies the 12-row
catalogue and fails closed on unqualified restores or false-positive
promotion. Run:

```sh
cd docs/m3/merge-recovery-20261029
javac --release 21 -Xlint:all -Werror tools/M3PostmergeEnhancementAudit.java
java -cp tools M3PostmergeEnhancementAudit --self-test
java -cp tools M3PostmergeEnhancementAudit POSTMERGE_ENHANCEMENT_IDEAS.tsv
```

The newly committed workflow runs these source-only checks.
It is **not** an SWT UI/JNI performance or OpenRewrite
migration test; those remain explicit HOLD gates.
