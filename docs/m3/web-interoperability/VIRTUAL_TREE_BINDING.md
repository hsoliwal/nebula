<!-- SPDX-License-Identifier: Apache-2.0 -->
# Web hierarchical widgets → original SWT/RWT Tree

**Product implementation:** `org.eclipse.nebula.widgets.m3.M3VirtualTreeBinding`
in the existing Nebula M3 SWT plugin. **Convergence recipes and source
compiler ownership:** `hsoliwal/com.synexia`. This is not a parallel
widget toolkit, DOM renderer or replacement for the existing Nebula Grid.

## Source-framework mapping

| Web widget/model | Original SWT/RAP receiver | Semantic obligations |
| --- | --- | --- |
| GWT `CellTree` / `CellBrowser` | `Tree(SWT.VIRTUAL)` | Stable node IDs, parent-relative SetData, selection and expansion |
| Vaadin Flow `TreeGrid<T>` | `Tree(SWT.VIRTUAL)` for tree/list; RAP Nebula Grid for rich tree tables | HierarchicalQuery parent, offset, limit, sorting/filtering, async loading |
| Apache Wicket nested/expandable tree | `Tree(SWT.VIRTUAL)` | Detachable models, request isolation, parent-child identity |
| MyFaces Trinidad tree/treeTable | `Tree(SWT.VIRTUAL)` or RAP Nebula Grid | Row keys, disclosure state, JSF lifecycle and stamping |
| Apache Pivot BXML TreeView | `Tree(SWT.VIRTUAL)` | BXML declarative structure and selection/model contract |
| Spring Data hierarchical repository | `Tree(SWT.VIRTUAL)` with published in-memory snapshot | Parent-key indexed paging, async fetch, security, cancellation |

The above are **typed source/model conversion candidates**, not
implemented framework-specific transpilations. No Wicket, Vaadin, GWT,
JSF, Pivot or Spring runtime dependency is introduced into Nebula.

RAP's official Tree/Table guide confirms `SWT.VIRTUAL` and
on-demand client-visible rows; its separate Nebula Grid port provides
richer but not fully desktop-equivalent behavior. The official SWT
virtual-tree article shows `event.index` relative to the parent
and `TreeItem.setItemCount` to declare children lazily.

## Product API

```java
Tree tree = new Tree(parent, SWT.VIRTUAL | SWT.MULTI | SWT.BORDER);
new TreeColumn(tree, SWT.NONE).setText("Name");
new TreeColumn(tree, SWT.NONE).setText("Category");

M3VirtualTreeBinding binding = M3VirtualTreeBinding.bind(
    tree, new M3VirtualTreeBinding.Nodes() {
        @Override public int rootCount() { return resident.roots(); }
        @Override public int columns() { return 2; }
        @Override public M3VirtualTreeBinding.Page fetch(
                String parentKey, int offset, int length) {
            // Must be a resident snapshot. No blocking HTTP/DB here.
            return resident.flatSiblingPage(parentKey, offset, length);
        }
        @Override public int[] path(String stableKey) {
            return resident.indexedVisiblePath(stableKey);
        }
    });

// After publishing an already-indexed replacement snapshot:
binding.replaceNodes(replacement);
// For a bounded changed root interval:
binding.invalidateRoots(first, last);
binding.setSelectedKeys("stable-node-id");
binding.close(); // does not dispose SWT Tree
```

`resident` is application-owned illustrative code, not a fabricated
framework adapter. `Nodes.fetch` is a synchronous SWT UI-thread
callback and **MUST NOT** perform SQL, JPA, blocking HTTP or other
network calls. Load and index pages asynchronously, with cancellation
and session/authorization handling in the application, then publish
a stable snapshot before the SWT callback.

## Storage, identity and lifecycle invariants

- At most **4 resident × 64 sibling-row pages**, regardless of tree
  depth or logical size; keys, child counts and cell text live in
  flat, immutable, defensively copied arrays.
- A page is identified by exact `(parentKey, firstSiblingIndex)`.
  A root has a null parent key, but node IDs are nonempty and must be
  **globally unique** throughout the logical tree. The page constructor
  rejects duplicate keys within a page, self-parenting, negative
  child counts, malformed layout and text-budget overruns.
- The original `TreeItem` owns child placeholders; `SetData`
  sets the stable ID, indexed column text and child count. Child
  pages materialize on demand. **No per-node Control or Map.Entry**.
- `Nodes.path(key)` provides indexed root-to-leaf *view indices*
  or null for filtered/missing nodes. Selection keys (bounded to
  256) persist across model replacement and can be restored when
  ancestors remain expanded. There is no global traversal to
  discover an ID and no forced expansion of hidden subtrees.
- `replaceNodes` preflights the next root size, column count
  and path geometry before mutating SWT. It invalidates old
  sibling-page cache and resets the root item count through zero,
  recreating the original TreeItem identity space. A mere
  `Tree.clearAll(true)` could retain **stale expansion state
  by index** after a sort, causing the wrong logical node to expand.
  Existing expansion is intentionally reset on a structural snapshot
  change; preserving it by stable key is a separate future contract.
- `invalidateRoots(first,last)` is bounded to at most 256 roots.
  Original `Tree.clear(root,true)` recursively invalidates child and
  grandchild items, so the binding must evict **all four cached pages**,
  not only intersecting root pages. Descendant pages are indexed by
  parent key and sibling offset; preserving them would replay old
  child data after a parent refresh. Four pages are a constant-sized
  cache, not the complete source tree. Structural changes should use
  `replaceNodes`.
- SWT/OS rendering, scrollbars, keyboard/focus events, expansion
  notifications and widget disposal remain owned by the
  **existing Tree**, not a new canvas renderer.
- On widget disposal or explicit binding closure, listeners,
  source references, page references and saved selection keys
  are released. The actual Tree is never disposed by the binding.

## Known boundaries

The bounded page-cache contract does not guarantee that SWT/OS
allocates only 256 internal TreeItem handles; that is toolkit-owned.
The source model must ensure globally unique keys across pages,
version-stable `Nodes.path` lookups and nonblocking `fetch`.
There is no synthesized native drag/drop, custom client renderer,
off-screen graphics, ARIA tree-grid equivalence, Wicket detach,
Spring transaction or Vaadin background dispatcher here.

The existing M3 Grid row binding in a separate integration branch
remains distinct: it owns rich spreadsheet/Grid semantics, including
column spans/frozen columns. The RAP Grid port should be used for
web rich-grid features after separately qualified capability checks,
rather than assuming all desktop Grid methods exist in RWT.

## Tests and source/CI status

- `M3VirtualTreePageTest`: pure Java page geometry, copies,
  sibling keys/child count and aggregate text budgets.
- `M3VirtualTreeBindingTest`: real SWT display-backed test
  *source* for 100,000 roots, three hierarchy levels, 12 disjoint
  pages, selected child/root identities, reversed ordering,
  bounded damage and invalid-path refusal.
- `.github/workflows/m3-web-native-rwt-api-20261025.yml`
  now compiles `M3VirtualTreeBinding` alongside the virtual Table
  and generated static controls using the checksum-pinned
  Eclipse RAP/RWT 4.8.0 JAR.
- The display-free `m3/proof/OriginalVirtualPageProof.java` now compiles
  and exercises actual product Tree and Table page classes (defensive
  copies, row-major layouts and invalid-page refusals) in the
  checksum-pinned RWT API workflow, independently of native displays.
- Source has been committed to draft Nebula #274. **Actual CI,
  native SWT display, RWT browser/session, screenshots, memory/
  latency measurements and full Tycho are NOT QUALIFIED** by
  this documentation or source creation.

Primary references:
- https://www.eclipse.org/articles/Article-SWT-Virtual/Virtual-in-SWT.html
- https://eclipse.dev/rap/developers-guide/tree-table.html
- https://help.eclipse.org/latest/topic/org.eclipse.rap.doc/guide/reference/api/org/eclipse/swt/widgets/Tree.html
- https://vaadin.com/docs/latest/components/tree-grid/data-binding
- https://wicket.apache.org/docs/about-models.html

## 2026-10-27 recursive clear qualification

Source-level correction in the original `M3VirtualTreeBinding`:

1. Clear all four in-process sibling-page cache slots before
   `Tree.clear(rootIndex, true)` rather than only root-keyed pages.
2. Resolve a virtual item's parent content through
   `parent.getText(0)` before consulting its stable
   `parent.getData("m3.web.id")`. SWT does not promise to clear
   application-owned Widget metadata alongside visual text; stale
   parent keys must never address child pages.
3. Regression source in `M3VirtualTreeBindingTest` first loads
   a root, child and grandchild, changes values without changing
   keys, calls `invalidateRoots(0,0)`, and verifies all three
   source pages are fetched anew. The old implementation would
   replay stale descendant content.

SWT's documented `Tree.clear(int,boolean)` contract explicitly
specifies recursive item invalidation when `all` is true and
re-requesting virtual item attributes as needed. This fix
aligns the four-page cache with that contract.

**Qualification status:** product fix and SWT display-backed test
source committed; native SWT/RWT execution and full Tycho/OS
pixel tests remain pending. Do not infer platform pass from source
review alone.

Official API: https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/swt/widgets/Tree.html
