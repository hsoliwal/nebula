<!-- SPDX-License-Identifier: Apache-2.0 -->
# PShelf: original widget private-count atom, subclass compatibility

The existing PShelf remains the native viewport. This leaf is a **serial follow-up**
stacked on [Nebula #190](https://github.com/hsoliwal/nebula/pull/190)
and canonical [Synexia #11502](https://github.com/hsoliwal/com.synexia/pull/11502).
It reuses the original PShelf/PShelfItem classes, the existing renderer,
one shared parent item list, and **no new Control, Java item proxy, native JNI
symbol or public/protected API**.

The baseline PShelfItem explicit-index constructor calls
`parent.getItems().length` for valid nonnegative indices. This allocates a
full defensive PShelfItem array on each indexed insertion solely for the count.
The original parent PShelf's `isFirstItem` from Nebula #190 is O(1) for
built-in widget renderers, but would bypass a subclass overriding the public
`getItems()` method and changed empty-state exception messages. Both are
contract concerns, not permission to forbid subclassing silently.

New source atoms:
- `PShelf.itemCountForInsertion()`: package-private native owner lookup
  backed by the original `items.size()` and original `checkWidget()`.
- `PShelfItem` constructor: existing `index < 0` short circuit preserved.
  Only exact runtime `PShelf.class` uses fast count. A subclass continues to
  invoke the original **virtual `getItems()`** and observes its results,
  callbacks, errors and exception timing.
- `PShelf.isFirstItem`: exact-instance nonempty case uses original private
  list O(1). Subclasses and the empty case still call their public/virtual
  `getItems()[0]`, restoring the expected original behavior in those cases.
- The public getter remains unchanged, defensive, overridable; no API/ABI
  change or additional retained data.

This reduces **one constructor count check from O(n) copying to O(1) time
and zero array allocation for an exact PShelf**. It does not claim total
shelf insertion O(1): native peers, item insertion shifts, layout and
paint still have other costs.

Execute the Java21 headless differential, including 60,000 randomized
states and all tested negative index/zero row/exception/override conditions:

    bash widgets/pshelf/org.eclipse.nebula.widgets.pshelf/run-item-owner-proof.sh

This oracle models the exact original and generated branch predicates with
explicit Java array copies and an overriding user subclass; it is not native
SWT. Exact generated source Git blobs and Maven/OpenRewrite SDK replay are
separate required proofs. Complete platform SWT/Tycho/GTK/Cocoa/Win32/UI
tests remain HOLD. The existing #190 Java/C17 JNI layout parity is neither
changed nor claimed for this new insertion atom.

Donor: original Nebula PShelf, 2006 Chris Gross, EPL-2.0; canonical
first-party Synexia Maven/OpenRewrite recipe Apache-2.0. Initiative31
SetData/viewport work informs parent-owned lookup only, with no copied
production code. LeetCode, HackerRank and GeeksforGeeks are taxonomy-only.
