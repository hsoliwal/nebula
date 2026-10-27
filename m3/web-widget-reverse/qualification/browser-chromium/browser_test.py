# SPDX-License-Identifier: Apache-2.0
"""Playwright/Chromium acceptance of actual JS emitted by merged Nebula Java source.
This tests a real browser DOM, NOT SWT Browser Java glue or RAP/RWT server.
"""
from __future__ import annotations

import asyncio
import hashlib
import json
import time
import shutil
import sys
import os
from pathlib import Path
from playwright.async_api import async_playwright

ROOT = Path(__file__).parent
METRICS: dict[str, object] = {"assertions": 0, "scenarios": []}


def check(actual, expected, reason: str) -> None:
    METRICS["assertions"] += 1
    if actual != expected:
        raise AssertionError(f"{reason}: got {actual!r} expected {expected!r}")


def assert_true(observation, reason: str) -> None:
    check(bool(observation), True, reason)


async def main() -> None:
    if len(sys.argv) != 3:
        raise ValueError("usage: browser_test.py generated-js-directory receipt.json")
    scripts = Path(sys.argv[1]).resolve(strict=True)
    receipt = Path(sys.argv[2])
    if not receipt.parent.is_dir():
        raise ValueError("receipt parent does not exist")
    js = {p.stem: p.read_text(encoding="utf-8") for p in sorted(scripts.glob("*.js"))}
    check(set(js), {"install", "text", "values"}, "exact scripts")
    METRICS["script_sha256"] = {k: hashlib.sha256(v.encode()).hexdigest() for k, v in js.items()}
    METRICS["source_git_blob"] = "dadedf428c28efd95e56ff102f153c2d925963aa"
    start = time.monotonic()
    async with async_playwright() as p:
        chromium = (os.environ.get("M3_CHROMIUM") or shutil.which("chromium")
                    or shutil.which("google-chrome") or shutil.which("google-chrome-stable"))
        if not chromium and Path(p.chromium.executable_path).is_file():
            chromium = p.chromium.executable_path
        if not chromium or not Path(chromium).is_file():
            raise RuntimeError("NO_REAL_CHROMIUM_BINARY: refuse mock-browser fallback")
        browser = await p.chromium.launch(executable_path=chromium, headless=True,
                                         args=["--no-sandbox", "--disable-dev-shm-usage"])
        try:
            page = await browser.new_page(viewport={"width": 1160, "height": 750})
            METRICS["browser_version"] = browser.version
            METRICS["browser_engine"] = "actual Chromium via Playwright; NOT SWT/RWT"
            errors: list[str] = []
            page.on("pageerror", lambda err: errors.append(str(err)))
            await page.set_content("""<!doctype html><html><head><meta charset='utf-8'></head>
                <body><button id='action' data-nebula-id='action' type='button'><span id='nested'>Click me</span></button>
                <input id='field' data-nebula-id='field' value='original' />
                <input id='check' data-nebula-id='check' type='checkbox' />
                <div id='normal' data-nebula-id='normal'>old</div>
                <div id='proto' data-nebula-id='__proto__'>old</div>
                <div id='constructor' data-nebula-id='constructor'>old</div>
                <div id='victim' data-nebula-id='victim'>DO NOT CHANGE</div>
                <div id='weird'>old</div>
                <select id='select' data-nebula-id='select'>
                  <option value='option1'>One</option><option value='option2'>Two</option>
                </select>
                <button id='unmarked'>No data ID</button>
                </body></html>""")
            await page.locator("#weird").evaluate("e => e.setAttribute('data-nebula-id', 'a\"] [data-nebula-id=\"victim')")
            check(await page.evaluate(js["install"]), False, "install rejects missing BrowserFunction")
            check(await page.evaluate("Boolean(document.__m3NebulaInstalled)"), False,
                  "unavailable function cannot poison install receipt")
            await page.evaluate("""() => {
                window.__events = []; window.__evil = 0; window.__listeners = {};
                window.evil = () => { window.__evil++; };
                window.m3NebulaEvent = (type,id,value) => { window.__events.push([type,id,value]); return true; };
                const original = document.addEventListener.bind(document);
                document.addEventListener = function(type, listener, ...args) {
                  window.__listeners[type] = (window.__listeners[type] || 0) + 1;
                  return original(type,listener,...args);
                };
            }""")
            check(await page.evaluate(js["install"]), True, "first install succeeds")
            check(await page.evaluate(js["install"]), True, "repeat install fixed-point")
            check(await page.evaluate("window.__listeners"), {"click": 1, "input": 1, "change": 1},
                  "exactly one registered delegate per event type")
            METRICS["scenarios"].append("browser_delegate_install_idempotence")

            await page.locator("#nested").click()
            check(await page.evaluate("window.__events"), [["click", "action", ""]],
                  "click nested target delegates to marked ancestor")
            await page.locator("#unmarked").click()
            check(await page.evaluate("window.__events.length"), 1, "unmarked events remain inert")
            await page.locator("#field").fill("input changed")
            events = await page.evaluate("window.__events")
            assert_true(any(x == ["input", "field", "input changed"] for x in events),
                        "input event yields current native element value")
            await page.locator("#check").check()
            events = await page.evaluate("window.__events")
            for typ in ["click", "input", "change"]:
                assert_true(any(e == [typ, "check", "true"] for e in events),
                            f"checkbox {typ} forwards boolean state")
            METRICS["scenarios"].append("native_dom_capture_click_input_checkbox")

            before_patch_events = await page.evaluate("window.__events.length")
            check(await page.evaluate(js["text"]), 4, "four exact DOM text updates")
            check(await page.locator("#victim").inner_text(), "DO NOT CHANGE", "selector-key injection refuses victim")
            weird_text = "<img src=x onerror=evil()> & 🙂"
            check(await page.locator("#weird").inner_text(), weird_text, "unsafe HTML remains textContent")
            check(await page.locator("#weird img").count(), 0, "scripted HTML cannot create img")
            check(await page.evaluate("window.__evil"), 0, "scripted onerror cannot execute")
            check(await page.locator("#proto").inner_text(), "safe proto", "__proto__ key is own property")
            check(await page.locator("#constructor").inner_text(), "safe constructor", "constructor key is own property")
            check(await page.locator("#normal").text_content(), "line 1\nline 2\u2028", "text Unicode exactly round-tripped")
            check(await page.evaluate(js["values"]), 2, "two exact input/select value updates")
            check(await page.locator("#field").input_value(), 'Ada <&"🙂', "special Unicode input.value preserved")
            check(await page.locator("#select").input_value(), "option2", "HTML Select value updated")
            check(await page.evaluate("window.__events.length"), before_patch_events,
                  "programmatic patches do not synthesize DOM input/change events")
            METRICS["scenarios"].append("real_dom_safe_patches_unicode_and_selector_injection")

            check(await page.evaluate(js["text"]), 4, "repeat text patch stable count")
            check(await page.locator("#normal").text_content(), "line 1\nline 2\u2028",
                  "idempotent patch identical result")
            METRICS["scenarios"].append("repeated_patch_fixed_point")

            # Navigate to a NEW document/window global, not merely replacing body nodes.
            await page.goto("about:blank")
            await page.set_content("<html><body><button id='next' data-nebula-id='next'>Next</button></body></html>")
            check(await page.evaluate(js["install"]), False,
                  "fresh document has no BrowserFunction / no stale install authority")
            await page.evaluate("""() => {
                window.__events=[];
                window.m3NebulaEvent=(type,id,value)=>{window.__events.push([type,id,value]);return true;};
            }""")
            check(await page.evaluate(js["install"]), True, "new document installs its own delegates")
            check(await page.evaluate(js["install"]), True, "new document duplicate install inert")
            await page.locator("#next").click()
            check(await page.evaluate("window.__events"), [["click", "next", ""]],
                  "exactly one event on new document")
            METRICS["scenarios"].append("real_document_navigation_lifetime_isolation")

            # A second browser page must not inherit this page's delegate or events.
            other = await browser.new_page()
            try:
                await other.set_content(
                    "<html><body><button id='other' data-nebula-id='other'>Other</button></body></html>")
                await other.evaluate("""() => {
                    window.__events=[];
                    window.m3NebulaEvent=(type,id,value)=>{window.__events.push([type,id,value]);return true;};
                }""")
                check(await other.evaluate(js["install"]), True, "second page independently installs")
                await other.locator("#other").click()
                check(await other.evaluate("window.__events"), [["click", "other", ""]],
                      "second page receives only its own event")
                check(await page.evaluate("window.__events"), [["click", "next", ""]],
                      "first page has no second-page event leakage")
                METRICS["scenarios"].append("two_real_browser_page_event_isolation")
            finally:
                await other.close()

            check(errors, [], "no uncaught Chromium JS errors")
        finally:
            await browser.close()
    METRICS["elapsed_s"] = round(time.monotonic()-start, 3)
    METRICS["result"] = "REAL_CHROMIUM_DOM_GATE_PASS"
    receipt.write_text(json.dumps(METRICS, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(METRICS, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    asyncio.run(main())
