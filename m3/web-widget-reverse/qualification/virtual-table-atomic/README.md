<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Original SWT VIRTUAL Table — complete-SetData reentrancy contract

**Only the original** `M3VirtualTableBinding.java` is product source.
All local classes named `org.eclipse.swt.*` under this qualification
directory are **test-only event/API shapes**, NOT Eclipse SWT or a
replacement widget. Do not add this directory to the OSGi/Tycho
production bundles or publish it as SWT/RWT.

Run from any full Nebula checkout with a JDK 11+ compiler:

```sh
bash m3/web-widget-reverse/qualification/virtual-table-atomic/run.sh
```

It compiles the actual product Java unchanged with Java 11 source
compatibility and `-Xlint:all -Werror`, then executes a deterministic
no-display event-shape regression. No dependency downloads or native
display are required.

## Reproduced correctness defect

The pre-fix `M3VirtualTableBinding.onSetData` guarded
`Rows.fetch()` with `fetching`, but reset the flag **before**
`TableItem.setData` and `TableItem.setText`. A native/client
callback triggered by a cell write could enter a second
`SWT.SetData`, invalidate a page, replace the snapshot or update
selection **inside** the first event's item-population atom.

New `populating` state covers the entire original `SetData`
callback. It uses `try/finally` so exceptions cannot permanently
latch the guard. The same check stops snapshot mutation,
bounded invalidation and selected-key changes during any
in-progress Table item population.

## Local red/green receipt — 7 November 2026

Original `master` source: exact Git blob
`b47f2397d1919cd037b1e331ae7ada9e0fdfbcd8`.

Corrected source: exact Git blob
`7e4667ef3e23fc1c738c9e6c17fd78485aaf7515`.
Both blobs were recovered byte-for-byte from the connected
repository; the previous source was also reconstructed by reversing
only the proposed PR edits, then its exact original blob was verified.

- **Unfixed original**: synthetic TableItem setter dispatches another
  `SetData` after provider `fetch` returned; the old original
  binding wrongly allows it. The identical no-display test
  **fails** with `Expected reentrant refusal`.
- **Fixed original**: nested `SetData`, invalidation, snapshot
  replacement, and selected-key mutation during a native item
  write are all **refused**; subsequent normal row population
  succeeds; resident cache remains bounded.
- Executed with JDK 21.0.12.1, `javac --release 11 -proc:none
  -Xlint:all -Werror`.
- Console: `PASS original virtual Table SetData atomicity +
  4 reentrant mutation refusals; indexed fetches=1`.

**This proves a source-level control-flow contract with test-only
SWT event shapes**, not genuine SWT/GTK/RWT runtime reentrancy.
Native SWT, RWT 4.8.0 Java API compilation, RAP browser,
accessibility/IME, focus, pixels, and full Tycho are independent
gates. Keep the PR draft until those platform checks succeed.

The original Tree may legitimately re-enter a parent
`SetData` while resolving a child through `parent.getText(0)`;
this Table-specific fix is intentionally **not** applied
indiscriminately to Tree.
