<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Original SWT virtual Tree: executable recursive-clear regression

This **test-only**, dependency-free JVM harness compiles the **real**
`widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/M3VirtualTreeBinding.java`
as-is, together with the minimum fake `org.eclipse.swt.*` API event shapes
needed to replay `SWT.SetData` and recursive `Tree.clear(root,true)`.

It must never be packaged, imported, or published in the Nebula production
bundle. The in-memory simulated `Tree` is **not Eclipse SWT or RWT**, has
no OS handles, and does not establish native drawing, keyboard,
accessibility, focus, transport or platform equivalence.

Run from the repository checkout, with JDK 11+:

```sh
bash m3/web-widget-reverse/qualification/virtual-tree-recursive/run.sh
```

The shell runner uses `javac --release 11 -Xlint:all -Werror`, compiles
into a temporary directory, executes the regression and deletes
temporary class files. It uses **no external Java libraries or network**.

## Exact red/green scenario

1. Materialize a root, child and grandchild through original
   `SWT.SetData`, populating three distinct parent-keyed pages.
2. Keep stable IDs, modify child and grandchild displayed values,
   and call `invalidateRoots(0,0)`.
3. Assert the updated descendant values are now used, that at least
   three new pages have been fetched, and that the cache stays bounded.
4. Change the source root's stable key; invalidate again and request
   the child **before** requesting root text. The stub deliberately
   retains the old application-owned `getData` map during clear.
   The binding must initialize the root and dispatch the child's
   request using its current stable key.

**Local execution on 2026-10-27:** using Java 21 with Java 11 target,
the fixed binding passed (8 page fetches). The previous behavior,
reconstructed by reversing exactly the two changes from PR #296
on the same source, **failed** because it returned `Child 0` rather
than `UPDATED child 0`. This is a synthetic event-regression result,
not an executed SWT desktop test.

A separate GitHub workflow runs this cheap headless test first;
the **actual GTK/Xvfb SWT Tycho JUnit** and checksum-verified
**RWT 4.8 Java API** compilation run in separate jobs. A successful
simulated event test does not promote the original SWT changes until
those independent platform gates pass.

Source authorities: [SWT Tree.clear API](https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/swt/widgets/Tree.html)
and [Virtual Tree and Table in SWT](https://www.eclipse.org/articles/Article-SWT-Virtual/Virtual-in-SWT.html).
