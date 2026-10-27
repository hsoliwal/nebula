<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Reproducible virtual-page proof receipt — 2026-10-26

**Product PR:** [hsoliwal/nebula#274](https://github.com/hsoliwal/nebula/pull/274)
(draft, not merged).

## Exact source inspected

The proof is the real
`m3/proof/OriginalVirtualPageProof.java` Java source (Git blob
`29231e2837b9028adefb2e4973119eab37e9a409`;
SHA-256 `cc3df2cc7c7f682b3bb5b57435d96e792a8ed0bfb3f0fff61a45519df0d2cb3b`).
It references the public nested `Page` types in the
existing `M3VirtualTableBinding` and `M3VirtualTreeBinding`, never a
shadow data model in the product.

The page bodies were extracted **verbatim** from the connected GitHub
product files, along with their public size constants, into temporary
JRE-only holders. The exact source revisions originally inspected were
`e0c498146818a4c5ea633649759a4dcc2883037a` (Table file Git blob)
and `09c004aebcd23def8b53442b59be2627d6757423` (Tree file Git
blob). Subsequent edits affect event selection handling outside
these nested Page types.

## Executed local test — PASS

Environment: JDK 21.0.12.1, target `--release 11`.
Compilation: `javac --release 11 -Xlint:all -Werror`.
Runtime: `java OriginalVirtualPageProof`.

The original proof and the new 2,048 deterministic page permutations
completed with the message:

```text
PASS original SWT/RWT virtual Table+Tree page contracts
```

Covered: flat row-major tables, parent-keyed tree sibling pages,
copy isolation of incoming key, child-count and cell arrays, first/last
row/column offsets, variable widths (1–8), variable page lengths
(1–64), invalid duplicate/self-parent keys, invalid counts, null
cells and bounded aggregate string budget.

This is an **extracted-source, no-display Page-only proof**.
It does **not** mean the complete Table/Tree binding classes
were compiled against an SWT or RWT JAR locally, or that a real
`SWT.SetData` event, UI thread, RAP browser transport, native pixels,
selection or resource disposal ran. No such claims are warranted.

## Product-source correctness correction

The table selection listener now derives its key from the
authoritative indexed `Rows.fetch` page instead of trusting
`TableItem.getData("m3.web.id")`. Clearing a virtual SWT item
may not clear arbitrary application metadata on its `Widget`;
using an obsolete key after an invalidated row could select the
wrong source identity. The tree selection listener likewise forces
`TreeItem.getText(0)` to request current virtual item contents after
`Tree.clear` before reading the stable ID. Both fixes include real
SWT JUnit **test source** with stale metadata and invalidation cases;
these display-backed tests remain unrun.

## Independent hosted gates

The existing Nebula workflow
`.github/workflows/m3-web-native-rwt-api-20261025.yml` is updated to:

1. Fetch the pinned real `org.eclipse.rap.rwt:4.8.0` JAR and verify its
   SHA-256 from the original Nebula RWT census.
2. Compile the static SWT factory, `M3VirtualTableBinding` and
   `M3VirtualTreeBinding` against the real RWT Java API with
   JDK 21 and **`--release 11 -Xlint:all -Werror`**, preserving the
   bundle's JavaSE-11 minimum bytecode level.
3. Compile and execute the no-display page proof against the entire
   compiled product classes (not extracted holders).
4. Compile the optional `M3BrowserWidget`/`M3BrowserScripts`
   independently so an unsupported Browser method cannot conceal
   native Table/Tree qualification results.

**GitHub-hosted status:** no successful run confirmed at the latest
inspection. Synexia's separate Actions checks report failures, but
individual job log/step details could not be retrieved; classify the
cause as **UNKNOWN**, not a source compile failure.

Native SWT Win32/GTK/Cocoa screen, OSGi Tycho, real RAP session,
accessibility, IME/RTL, client memory and viewport latency
remain unqualified.

## RAP-only rendering tuning

For RAP applications (not in SWT's shared product source), its
documented `RWT.PRELOADED_ITEMS` key can configure client-visible
item overscan, such as:

```java
// RAP host code only; do not require this import in SWT product classes.
table.setData(org.eclipse.rap.rwt.RWT.PRELOADED_ITEMS, Integer.valueOf(20));
tree.setData(org.eclipse.rap.rwt.RWT.PRELOADED_ITEMS, Integer.valueOf(20));
```

This configures RAP browser-client preloading and is distinct from
the product binding's bounded in-process 4 × 64 page cache. The value
20 is an illustrative tuning candidate, **not** a measured optimal
overscan or a validated throughput/latency result. Benchmark it
against scroll behavior and round-trip counts before selecting a
default.

Official source documentation:
- https://eclipse.dev/rap/developers-guide/tree-table.html
- https://eclipse.dev/rap/developers-guide/rwt.html
- https://www.eclipse.org/articles/Article-SWT-Virtual/Virtual-in-SWT.html
