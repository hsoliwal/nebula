<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula Browser callback lifecycle — original-owner refinement

## Inventory and unchanged contracts

- Product: `hsoliwal/nebula` original
  `widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/M3BrowserWidget.java`.
- Baseline master: `4508f6e5e47313459be8a039b34aad3231ace8cb`.
- Original Git blob: `9fb5819627fef23406b8794e29c039ee971633a5`.
- Canonical recipe owner: `hsoliwal/com.synexia`, using existing
  `M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe`.
- Related independent UTF-16 source leaf: Synexia #13580 / Nebula #284.
  Do not modify, merge, or silently stack its unqualified source.
- Preserve all public/protected method and constructor descriptors,
  ordinary callback ordering, original SWT Browser ownership, BrowserFunction
  identity, UI-thread confinement, and all valid same-document event behavior.

## Identified defect

BrowserFunction iterates a `CopyOnWriteArrayList` of application listeners.
A listener can synchronously dispose the widget or replace its Browser document.
The iterator still exposes listeners from the former snapshot, so subsequent
callbacks may act on disposed state or a different document.

## Required refinement

The original Browser owner must cancel only *remaining* callbacks for an event
when the widget has been disposed or the originating document has changed.
Use one private monotonic document revision, recorded at event entry.
Revision advances before every non-null setTrustedHtml load attempt, including rejected or failed loads; a null argument is refused without mutation.
Recheck the revision, readiness, and disposal status before each callback and
before returning a positive bridge acknowledgement.

No user-visible event is invented, no listener control is reassigned,
no script parsing/JSX conversion is added, and no new runtime state owner exists.
Listener additions/removals within an event retain existing COW snapshot
behavior unless the lifecycle invalidates the event.

## Qualification order

1. Verify source, licenses, head, original signatures, and absent paths.
2. Documentation and canonical recipe source before product receiver.
3. Exact source-sealed OpenRewrite receipt and fixed-point/drift refusals.
4. Diff against original only, then format/lint, Java 11 API compilation,
   tests, and deterministic event runtime with test-only SWT shapes.
5. Native SWT Win32/GTK/Cocoa, RAP/RWT session, browser engine, screenshots,
   accessibility/IME and full Tycho remain NOT_RUN until authentic evidence.

## Canonical offline proof receiver

The immutable Synexia test-only fixtures are pinned at commit
`d07adac502f5d283541893af680a55cc81b88150`, and the product CI
compiles original and candidate Java with identical signature stand-ins.
The source checksum and `javap` API checks are independent of that test.
A successful test-only event replay is not real SWT/RWT runtime proof.
