<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Original Nebula Browser: connected-node DOM snapshot admission

This is a **product receipt** from the canonical Synexia Maven/OpenRewrite
source recipe `m3-nebula-browser-dom-snapshot-20261027`, never a new SWT widget,
renderer, DOM facade, service, JavaScript interpreter or runtime dependency.

## Defect and exact API boundary

The existing `M3BrowserScripts.patch(Map,boolean)` snapshots all
`[data-nebula-id]` elements with `querySelectorAll`. Replacing an ancestor's
`textContent` detaches its descendants, but an old static `NodeList` still
contains those JavaScript object references. The previous implementation
counted updates to detached descendants even though their text was no longer
in the current document. This is observable through `M3BrowserWidget.setTexts`.

The original Java owner now uses its existing script builder to pin the
initial `document.documentElement` identity and skip nodes that are not
contained by that root **or whose current document root has changed**.
This also refuses stale snapshot nodes when synchronous application code
replaces the entire document root during a text patch. A disconnected node is **not** a successful DOM
update. Sibling patches, child-only patches, duplicate IDs, value patches,
JavaScript string-literal safety, budget checks, event behavior and existing
public Java descriptors are unchanged. The original `Browser`, `BrowserFunction`,
`Composite`, RAP session, listeners and DOM document remain platform-owned.

## Source-locked test scopes

The original Nebula JUnit4 test adds one source method; the focused `pom.xml`
builds the existing original Java owner and real JUnit4 test from their original
PDE source locations, not from duplicate copied Java sources. The companion
canonical Synexia recipe preserves exact pre/post UTF-8 SHA-256 and supplies a
Node.js DOM snapshot oracle that reconstructs the observable detached-node
counterexample. Both cases must produce identical public `javap` descriptors.

`source -> compilation -> test -> runtime` are independent gates. Model DOM
proof is not a real browser engine. Native GTK/Win32/Cocoa SWT Browser/RAP-RWT
session, UI event-loop, accessibility, IME, screenshot, native resource and
full Tycho reactor remain `NOT_RUN` until actual receipts exist. The separate
reentrant Browser.evaluate and UTF-16 changes are not rewritten by this leaf.

No source copied from JWt, Vaadin, Wicket, MyFaces, Tapestry or another donor.
