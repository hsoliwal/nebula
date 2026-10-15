<!-- SPDX-License-Identifier: Apache-2.0 -->
# Tiles: preserve original ordered mouse hits after mutable-frame edits

**Canonical source-sealed second FILE pass** stacked on the historical Tiles
recipe [Synexia #11540](https://github.com/hsoliwal/com.synexia/pull/11540)
and its merged Nebula receiver
[#199](https://github.com/hsoliwal/nebula/pull/199).

## Concrete regression on the merged original widget

`FrameStatic.getTiles()` returns the actual, mutable `rendered` ArrayList.
The private indexed-grid guard in merged `FrameStatic` only checked
`rendered.size()`, allowing a same-size `List.set` or `Collections.swap`
to reorder actual `Tile<T>` references after the grid had been admitted.
`Tiles.getTileAt` would then use a stale slot, returning null or a
different Tile while the original ordered scan still found the first hit.

This affects the **original existing Tiles Canvas**, not a replacement
widget. Original Java `int` inclusive edge/overflow behavior, callbacks,
selected/hovered object identity, public/protected source declarations,
native Paint/GC lifecycle, and all prior fallback paths remain unchanged.

## Chosen bounded private atom

`FrameStatic` still retains one and only one ArrayList backing. A private,
static, no-outer-owner-reference subclass of ArrayList records:
- the existing `modCount` of structural mutations (add, remove, clear,
  iterator.remove, removeIf, replaceAll and sort in Java21);
- a `long` content/view epoch for nonstructural `set` and for mutable
  `subList` / `ListIterator` views whose later `set` would not increment
  ArrayList.modCount.

After exact complete static-grid validation, the original
`hitGridColumns` is published last with both captured epochs. Fast
`hasIndexedHitGrid()` additionally checks both epochs in O(1).
Callback-bearing bulk operations (`replaceAll`, `sort`,
`removeIf`, `removeAll`, `retainAll`) invalidate the content epoch
**before** user comparators, predicates or membership callbacks can
re-enter the widget while an ArrayList is partially changed.
This closes a second failure path that post-operation modCount checking
alone cannot catch. The original List method return values and exceptions
are not otherwise intercepted.

After a mutation the **unchanged original ordered first-hit scan** is used.
An original `FrameStatic.update()` can requalify the fast path.
There is no new per-tile array/object, native widget, data broker, scrollbar,
style, production JNI call, or public collection API. A conservative
read-only ListIterator/subList escape may disable the fast path until the next
update, which costs performance but never changes semantics.

## Executed reference and mutation tests

The source-exact headless Java21 model `TilesMutationEpochProof.java`
(Git blob `c532e00e6dc124b1e1eae23e7a83dbd6ec2378b8`; SHA-256
`0ff0d7cd14e073f80c7793026343e9c1fa7cb1ef86fa8688d08f0503b0e3995a`)
compares the original scan to the new guarded index, and **also shows a
real divergent same-size swap against the previous size-only algorithm**.

Run:
```bash
bash widgets/tiles/org.eclipse.nebula.widgets.tiles/run-mutation-epoch.sh
```
OpenJDK 21.0.12.1 local: `javac --release 21 -Xlint:all -Werror`
PASS; standard JVM **61,533 assertions PASS** (40,000 warm indexed
queries; 11,507 fallback queries); `-Xint --quick` **10,933 PASS**.
Cases include `Collections.swap`, `List.set`, `subList.set`,
`ListIterator.set`, `removeIf`, `replaceAll`, `sort`, iterator
removal, a pre-update retained alias, and reentrant sort/replaceAll/removeIf callbacks. No claim that the actual
native SWT/OSGi package compiled or ran under those headless tests.

Earlier [Synexia #11540](https://github.com/hsoliwal/com.synexia/pull/11540)
retains the C17/JNI test-only geometry oracle (**76,503** finite
comparisons). JNI does not model Java List mutation and is not a substitute
for this regression. No production JNI accelerator was added.

## Recipe, provenance and HOLD

Canonical Maven/OpenRewrite leaf
`com.synexia.m3.NebulaTilesMutationEpoch20261015` reuses unchanged
`M3GpuToolJoinRecipe`/`M3GpuToolJoinManifest`. One exact original
`FrameStatic.java` preimage and postimage, three ABSENT-before model/
runner/category artifacts plus workflow and README, SDK JUnit replay/
refusal/fixed-point, and SHA-256 manifest are shipped together.
Product original SPDX/EPL-2.0 headers and contributors remain unchanged;
Synexia-owned recipe/docs are Apache-2.0 and example oracle EPL-2.0.
Donor pattern: JDK21 ArrayList.modCount source and existing Synexia
SWT/Nebula primitive-epoch PRs; no challenge solution text copied.

**Not yet qualified:** actual Maven/OpenRewrite SDK execution, full
Nebula Tiles source/SWT GTK/Win32/Cocoa Tycho, matched javap binary API,
native item/UI disposal callbacks and pixel proof, JFR heap/performance,
GWT/RAP and 2^32 structural-modification rollover. Keep DRAFT/HOLD,
fail closed on unknown source postimage, never rebase Synexia develop.
