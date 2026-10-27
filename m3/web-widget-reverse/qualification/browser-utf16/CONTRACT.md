<!-- SPDX-License-Identifier: Apache-2.0 -->
# Nebula Browser bridge: UTF-16 scalar admission (2026-10-27)

Product: `hsoliwal/nebula` existing private helper
`org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts`.
Canonical convergence owner: `hsoliwal/com.synexia` Maven/OpenRewrite.

## Source inventory and boundaries

- Product original Git blob `54916a8b0297f5e18109343d7f88bd1736d831af`.
- Existing product JUnit4 test Git blob `d14f5a61a4a61ec30b3c352c13e736f3ea74caa0`.
- Product base `master@4508f6e5e47313459be8a039b34aad3231ace8cb`.
- Synexia base `develop@5807cf82ea81b3a045eb3fa8b10451008065c7b9` (re-check before any publication).
- `M3BrowserScripts` is an opt-in original SWT/RWT Browser fallback. SWT/RWT owns Browser, navigation, listeners, control disposal, viewport and sessions. No new owner or JavaScript runtime is introduced.

## Single behavior correction

Java Strings may contain invalid Unicode surrogate units. The current
`jsString()` copies them into a JavaScript literal instead of rejecting
malformed Unicode. `validEvent()` accepts similarly malformed input IDs/values.
These input shapes are outside the documented *valid browser event/literal*
contract. The candidate refuses them, before JavaScript evaluation, without
normalizing or replacing characters.

All **well-formed** UTF-16 input units must preserve byte-for-byte JavaScript
literal output, public method descriptors, existing budgets, event-type set,
patch count/ordering, and DOM update semantics. Valid supplementary code points
remain encoded as original Java surrogate pairs. A malformed Java string may not
be silently changed to replacement characters by a browser/UTF-8 transport.

Do not change `install()` semantics, generated widget APIs, data ownership,
SWT/RAP listeners, or browser script execution routing. Web callbacks from
untrusted documents are not a security boundary; caller-trusted HTML remains
required. No GWT/JNI/native equivalence is claimed.

## Serial proof gates

1. Inventory precise source blobs and rights (documented before code).
2. Canonical Synexia hash-pinned OpenRewrite Java receipt: preimage refusal,
   reviewed postimage, fixed-point, duplicate/foreign-source refusal.
3. Exact generated receiver blob comparison and one-hunk mutation audit.
4. Java 21 lint (`-Xlint:all -Werror`) and compile of original helper/tests.
5. Existing JUnit4 tests + new malformed-pair boundary controls; independent
   differential comparison for valid BMP/supplementary Unicode and budgets.
6. Node.js 22 DOM patch replay against the real emitted JavaScript.
7. Original product Tycho tests, native SWT and RWT browser session remain
   `NOT_RUN` until authentic runner logs and artifact receipts are available.

No merge, rebase, force push, upstream source rename, or public ABI change.
