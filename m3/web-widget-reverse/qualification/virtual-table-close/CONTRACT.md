<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula virtual Table: provider-triggered close during SetData

## Owner and source identity

- hsoliwal/nebula `master@4508f6e5e47313459be8a039b34aad3231ace8cb`
- Original widget: `widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/M3VirtualTableBinding.java`
- Original blob: `e0c498146818a4c5ea633649759a4dcc2883037a`
- Original test: `widgets/m3/org.eclipse.nebula.widgets.m3.tests/src/org/eclipse/nebula/widgets/m3/tests/M3VirtualTableBindingTest.java`
- No source changes to SWT, Nebula Grid/XViewer, JNI or existing web framework frontends.

## Invariant

A synchronous `Rows.fetch(offset,length)` callback may close the binding before returning
its `Page`. After `close()`, the binding has no authority to repopulate a cached page,
modify a virtual TableItem, retain provider data, or respond to the still-unwinding
`SWT.SetData` event. A deterministic `IllegalStateException` shall refuse further
publication at this point. The original `Table`, `TableItem`, `Display`, and scroll/
selection model retain their SWT ownership; `close()` never disposes the Table.

The preexisting page validation, cache residency, event/listener lifetime,
public signatures, style checks, and behavior on *ordinary nonclosing* fetches
are locked and retained byte-for-byte. The correction requires exactly one
private post-fetch closed guard and targeted regression coverage.

## Source-workflow acceptance

1. Inventory Git head, original full source/text blobs, API signatures, licenses,
   existing PRs and absence of new documentation/test paths.
2. Publish this contract in canonical Synexia workspace before recipe code.
3. Reuse `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`
   with exact original SHA-256 preimages and reviewed postimages.
4. Verify first-pass source, second-pass fixed point, foreign drift refusal,
   and exact receiving Git blobs.
5. Run patch -> lint -> strict Java 11 compile -> test -> runtime, recording logs.
6. Execute original Nebula Tycho and SWT Win32/GTK/Cocoa and RWT session tests
   before promotion. Test-only SWT API stand-ins are explicitly NOT native proof.

A resulting PR is review-only DRAFT/HOLD until original product tests and
Maven/OpenRewrite SDK execution have authentic passing receipts.
