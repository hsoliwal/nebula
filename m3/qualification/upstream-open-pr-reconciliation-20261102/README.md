# SWT Nebula upstream open-PR additive reconciliation — 2026-11-02

**Status:** draft candidate only; no master or runtime promotion. Repository `hsoliwal/nebula`, source base `m3/github-donors-superset-20261031@1f7a870ae41c63c1c8e3f9dbb36d3813549b25c3`. This change is stacked on the existing ten-donor fork superset, not a replacement for it.

## Source contracts and decisions

### [EclipseNebula PR #707](https://github.com/EclipseNebula/nebula/pull/707), draft, source head `47e5a432a20aac3952a1e0bddc3fa56ec39fdfe4`

- Existing GridItem owner source Git blob `00bdfc996a189666e8bbabe19a923ab982912e12` already avoids fetching the current row from its parent during disposal, preserving a prior issue #700 repair. Do not overwrite that fix.
- **Add only** the missing out-of-domain current-row guard before walking additional spanned rows, returning the receiver's own height for stale negative or excessive item indexes. Width and normal-row behavior remain unchanged.
- Add upstream's editor/scrollbar/disposal regression to existing `GridFixedColumn_Test` (preimage Git blob `fe08d92c713564882c88062aa880b719aee2fcc7`) with package-local owner and native SWT fixtures preserved.
- No new public/protected API or event framework.

### [EclipseNebula PR #708](https://github.com/EclipseNebula/nebula/pull/708), open, source head `23d2baca5ced0d4513b50569cd567ba360cd641b`

- Existing `TableCombo.internalLayout` source Git blob `09cc3ae33b87e93bfdadbd0b4a1d88c7adf3a056` already prevents unconditional popup recreation during selection refresh (issue #705). Do **not** blindly add the donor's second `dropDown(true)` action without an SWT event-order comparison.
- Add the donor's two SWTBot regression cases to existing `TableComboTests` (preimage `970c37265582390aed18bc0af8016403fd1ccb10`) and its standalone `Bug705` reproducer to the snippets package. The new snippet path was absent at inventory.
- Do not modify the TableCombo or CheckTableCombo production source in this stage. If native regressions reproduce a failure, reconcile the least behavior-changing leaf separately.

## Verification order and explicit holds

1. Inventory and pin source identity before edits; documentation precedes application.
2. Apply only the exact target source/test leaves above with preimage SHA and one serial update per path, then verify each Git blob readback, changed-path inventory, and non-additive deletions (must be zero).
3. **NOT RUN:** No full Tycho `mvn verify`, native GTK/Xvfb SWTBot/JUnit, Windows/Cocoa/RAP, or interactive event/runtime tests are available to this branch audit. Hosted checks must run before considering the stack merge.
4. Assert GridItem public/protected descriptors, TableCombo selection/close events and SWT resource lifetimes remain unchanged. A successful Java syntax check or historic donor PR is not substitute for SWT native acceptance.
5. Keep this reconciliation separate from JavaSE-11 to JavaSE-17 baseline changes in #309, which are incompatible by default and still HOLD.

This branch is a **partial two-PR source reconciliation**, not a representation that all PRs across 109 forks have been merged or qualified.
