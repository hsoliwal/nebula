<!-- SPDX-License-Identifier: EPL-2.0 -->
# Native/mobile Grid parent-owned viewport: strict headless proof

Compiles the REAL production MobileGridViewport.java with Java 17 API,
-Xlint:all -Werror on both JDK 17 and 21. No fake SWT Control, no separate
runtime viewport, no JNI and no additional production widget.

Checks 1 trillion logical rows, exact long-index jump, bounded resident
row slots, selection and provider failure contracts, synchronous empty
window reset and cleanup, safe reuse after count regrowth, plus 1,000
consecutive unchanged frames returning the exact same immutable Window
object and no new provider data fetches.

Run:
bash releng/verification/mobile-grid-viewport/verify.sh

This is a strict headless API/model proof, NOT native SWT GTK/Cocoa/Windows
or Android/iOS platform validation. Full Tycho/OSGi, UI-thread, scrolling,
accessibility, rendering and pixel tests are still required. Do not merge
or claim full release readiness solely because this job succeeds.
