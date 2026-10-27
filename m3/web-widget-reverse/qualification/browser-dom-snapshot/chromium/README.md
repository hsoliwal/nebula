<!-- SPDX-License-Identifier: Apache-2.0 -->
# Real Chromium regression of original Nebula Browser detached-DOM short circuit

This is a **test-only, original-owner** qualification lane stacked on Nebula
[#290](https://github.com/hsoliwal/nebula/pull/290) and the canonical Synexia
[#13678](https://github.com/hsoliwal/com.synexia/pull/13678) recipe. It reuses

a) the unchanged source-owned `M3BrowserScripts.patch` (current candidate Git
blob `33e62975550a2b2c6ca7e8958b535ffca27efe25`), and
b) its existing `BrowserPatchEmit.java` test emitter (Git blob
`643d777e91dd7a40c43c429810e6cf2d7c4b7bcd`).

**No JS algorithm, runtime widget, native SWT Control, renderer, JNI owner,
public API, source model or second widget framework is introduced.**
The tests first compile the two original Java sources with `--release 11
-Xlint:all -Werror`; the original emitter then produces all six JavaScript
fixtures. Python Playwright runs those *actual emitted scripts* in real
Chromium. It tests a detached child of an updated ancestor, real document-root
replacement between NodeList iterations, ordinary child-only/sibling updates,
complex text, duplicate IDs and value-only input updates. It explicitly refuses
missing Chromium binaries, source drift, dirty worktrees, and result overwrite.

From the Nebula product checkout on PR #290 or its eventual source-identical
successor:

```bash
bash m3/web-widget-reverse/qualification/browser-dom-snapshot/chromium/run.sh \
  /tmp/m3-dom-shortcircuit-real-chromium-fresh
```

Requirements: Java 21 compiler configured for Java 11 output, Python 3.12+,
`playwright==1.57.0`, installed real Chromium. No fake DOM fallback.

This proves browser-side DOM behavior only. Native SWT BrowserFunction callback,
RAP/RWT 4.8 server/client/session protocol, JUnit4/Tycho, multi-platform pixels,
accessibility/IME, performance and Maven/OpenRewrite SDK remain **NOT_RUN** until
authentic stage receipts exist. A GitHub Actions run with zero assigned runners
or no executed steps is not a test pass.
