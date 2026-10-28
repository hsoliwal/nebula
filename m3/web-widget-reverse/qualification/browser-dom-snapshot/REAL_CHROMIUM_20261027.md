<!-- SPDX-License-Identifier: Apache-2.0 -->
# Real Chromium qualification for original Nebula DOM snapshot receiver

Canonical source-sealed Maven/OpenRewrite receipt: `hsoliwal/com.synexia`,
`m3-nebula-browser-dom-chromium-20261027`. Product owner: original
`M3BrowserScripts.java`; this independent test leaf does **not** create another
SWT Control, browser widget, data model, parser, event bridge or JNI owner.

The admitted *candidate* Java source is the exact Git blob
`33e62975550a2b2c6ca7e8958b535ffca27efe25`, introduced by separate
Nebula PR #290. This test is a **stacked receiving candidate** on #290, not a
modification of the current `master` source. Its acceptance is specific to that
exact source image. Any further source mutation refuses until requalified.

Run from an unmodified working copy with Java 21+ installed and Python
Playwright + an actual Chromium binary:

```
./m3/web-widget-reverse/qualification/browser-dom-snapshot/run-chromium.sh /tmp/m3-dom-chromium
```

Actual original Java emits nine JS patch scripts. Python Playwright runs the
*emitted* source with real DOM nodes and the browser event system. Ten cases,
43 assertions include ancestor detachment, deep nesting, synchronous root
replacement, siblings, duplicates, child-only, literal Unicode, safe escaping,
input values and prototype-property keys. The historic source's erroneous
counts 2/3/2 become 1/1/1; unaffected counts stay identical. The historical
34-assertion Chromium suite remains an independent gate, and its original
source pin must be updated in a separate exact reviewed receipt **after** #290
integration; never broaden a pinned test silently.

This suite is **not** Maven/OpenRewrite execution, original JUnit4/Tycho,
actual SWT BrowserFunction in Win32/GTK/Cocoa, RAP/RWT server/browser session,
JNI/GC/pixels, IME/accessibility or resource/performance parity. Those remain
NOT_RUN and block full runtime promotion even if real Chromium passes.
