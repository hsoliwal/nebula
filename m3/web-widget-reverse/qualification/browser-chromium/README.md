<!-- SPDX-License-Identifier: Apache-2.0 -->
# Real Chromium DOM evidence for the original Nebula Browser scripts

This is a **test-only executable qualification lane**. It builds the original
`org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts` from the current
Nebula source tree (Git blob sealed to `f5b31661f721dbe8908045c5716f6973143c9291`),
then uses that **actual Java owner** to generate its install/text/value JavaScript.
It does not copy a JavaScript implementation, provide a new widget, or depend
on any browser integration library in the shipped M3 widget plugin.

The generated code is executed in **real headless Chromium** through Playwright,
not an invented browser-shaped host. Scenarios cover `BrowserFunction`-style
callback delegation from marked descendants, input/checkbox events, installation
idempotency, distinct document and page lifetimes, exact `data-nebula-id`
lookup resisting CSS selector injection, prototype-key ownership (`__proto__`,
`constructor`), literal textContent instead of HTML interpretation, Unicode
non-BMP values and line separators, no synthetic input/change event on patch,
select/input value updates, and repeat patch stability. The Java fixture refuses
malformed UTF-16 before browser dispatch.

## Run

Required: Java 21 compiler (targeted to Java 11 bytecode), Python 3.12+,
`playwright==1.57.0`, **real Chromium** accessible as `chromium`,
`google-chrome`, `google-chrome-stable`, `M3_CHROMIUM`, or the Playwright bundled
Chromium executable. No mock fallback is permitted.

From Nebula repository root:

`bash m3/web-widget-reverse/qualification/browser-chromium/run.sh /tmp/m3-browser-chromium-proof`

The output directory must be fresh and is kept outside the repository.
`CHROMIUM_RECEIPT.json` is written only if all checks pass. A changed original
Git blob, missing real browser, unsafe source or changed checkout **fails closed**.

The `.github/workflows/m3-browser-chromium-20261027.yml` job executes this
lane separately from native Tycho. Passing this gate means actual browser DOM
behavior of the **emitted scripts**, not SWT Browser Java callbacks, RAP/RWT
server/session protocol, Java/JNI ABI, real native SWT painting, IME,
accessibility, resource lifetimes or cross-platform pixels. Those remain HOLD
until their original owning platform tests run.

## Requalified source revision (29 October 2026)

The original Browser owner was refined by the separately merged short-circuit
recipe, changing only JavaScript guard behavior and preserving its Java API.
This receiver explicitly pins `f5b31661f721dbe8908045c5716f6973143c9291`, rather than silently accepting
arbitrary revisions. The exact original Java emitter and 34-test Chromium
suite were re-executed against that source on Chromium 144 with Java 11
`-Xlint:all -Werror` compilation. This remains browser-side proof only;
SWT BrowserFunction, RAP/RWT sessions and native Tycho remain unqualified.
