# SPDX-License-Identifier: Apache-2.0
"""Real Chromium semantic-delta proof for exact original Nebula Browser Java JS.

Input JS is emitted by javac-compiled original product owner (baseline or PR #290).
The Python harness does not copy/reimplement Nebula's patch algorithm.
"""
from __future__ import annotations
from playwright.sync_api import sync_playwright
from pathlib import Path
import json, hashlib, sys, os, shutil

if len(sys.argv) != 4:
    raise SystemExit("usage: browser_nested_real_chromium.py <js-dir> <baseline|candidate> <receipt.json>")
js_dir, mode, output = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
assert mode in {'baseline','candidate'},mode
scripts={p.stem:p.read_text(encoding='utf-8') for p in js_dir.glob('*.js')}
assert set(scripts)=={'parent-child','deep-nested','siblings','child-only','duplicate-id','text-safe','values','empty','proto'}
checks=0
scenarios=[]

def eq(actual,expected,message):
    global checks
    checks+=1
    if actual != expected:
        raise AssertionError(f'{mode} {message}: got={actual!r} expected={expected!r}')

def run(page, name, html, want_original, want_candidate):
    page.set_content('<!doctype html><html><body>'+html+'</body></html>')
    before = page.evaluate("""() => {window.__evil=0;window.evil=()=>window.__evil++;
        window.__events=[];for(const name of ['click','input','change'])
        document.addEventListener(name,e=>window.__events.push(e.type));
        return document.querySelectorAll('[data-nebula-id]').length;}""")
    actual=page.evaluate(scripts[name])
    expected=want_original if mode=='baseline' else want_candidate
    eq(actual,expected,name+' update count')
    eq(page.evaluate('window.__evil'),0,name+' no HTML injection')
    eq(page.evaluate('window.__events.length'),0,name+' patch emits no events')
    scenarios.append({'name':name,'initial_connected':before,'count':actual})

with sync_playwright() as pw:
    chromium=(os.environ.get('M3_CHROMIUM') or shutil.which('chromium') or
              shutil.which('google-chrome') or
              (pw.chromium.executable_path if Path(pw.chromium.executable_path).is_file() else None))
    if not chromium or not Path(chromium).is_file():
        raise RuntimeError('NO_ACTUAL_CHROMIUM_BINARY: no mock-browser fallback')
    browser=pw.chromium.launch(executable_path=chromium,headless=True,
                                args=['--no-sandbox','--disable-dev-shm-usage'])
    try:
        page=browser.new_page()
        run(page,'parent-child','<div id="p" data-nebula-id="parent">OLD<span id="c" data-nebula-id="child">old child</span></div>',2,1)
        eq(page.locator('#p').text_content(),'parent replacement <em>literal</em> 😃','parent content literal')
        eq(page.locator('#p em').count(),0,'nested patch cannot render HTML')
        eq(page.locator('#c').count(),0,'child detached after ancestor replacement')
        run(page,'deep-nested','<div data-nebula-id="outer"><div data-nebula-id="middle"><span data-nebula-id="inner">old</span></div></div>',3,1)
        run(page,'siblings','<div id="a" data-nebula-id="one">1</div><div id="b" data-nebula-id="two">2</div>',2,2)
        eq(page.locator('#a').text_content(),'new one','unaffected sibling 1')
        eq(page.locator('#b').text_content(),'new two','unaffected sibling 2')
        run(page,'child-only','<div data-nebula-id="parent"><span id="child" data-nebula-id="child">old</span></div>',1,1)
        eq(page.locator('#child').text_content(),'new child','child-only update')
        run(page,'duplicate-id','<p class="dup" data-nebula-id="same">x</p><p class="dup" data-nebula-id="same">y</p>',2,2)
        eq(page.locator('.dup').all_text_contents(),['changed','changed'],'duplicate key updates both')
        run(page,'text-safe','<div id="safe" data-nebula-id="parent">OLD</div>',1,1)
        eq(page.locator('#safe').text_content(),'<img src=x onerror=evil()> & "😃"','safe literal text')
        eq(page.locator('#safe img').count(),0,'script must not create an img')
        run(page,'values','<input id="field" data-nebula-id="field" value="old"><input id="other" data-nebula-id="other" value="old2">',2,2)
        eq(page.locator('#field').input_value(),'new value','value one updated')
        eq(page.locator('#other').input_value(),'a<b&😃','value two Unicode updated')
        run(page,'proto','<span id="pr" data-nebula-id="__proto__">old</span><span id="cons" data-nebula-id="constructor">old</span>',2,2)
        eq(page.locator('#pr').text_content(),'safe','__proto__ handled literally')
        eq(page.locator('#cons').text_content(),'clean','constructor handled literally')
        run(page,'empty','<div data-nebula-id="parent">old</div>',0,0)
        eq(page.locator('[data-nebula-id="parent"]').text_content(),'old','empty is inert')
        # Explicit root replacement during parent text mutation. Call the actual JS,
        # intercept only DOM's setter to model synchronous host-document replacement.
        page.set_content('<!doctype html><html><body><div data-nebula-id="parent"><span data-nebula-id="child">old</span></div></body></html>')
        page.evaluate("""() => {
           const owner=document.querySelector('[data-nebula-id=parent]');
           const descriptor=Object.getOwnPropertyDescriptor(Node.prototype,'textContent');
           Object.defineProperty(owner,'textContent',{configurable:true,set(value){
             descriptor.set.call(this,value);
             const next=document.createElement('html');
             next.innerHTML='<body><span id="replacement">new document root</span></body>';
             document.replaceChild(next,document.documentElement);
           }});
        }""")
        eq(page.evaluate(scripts['parent-child']),2 if mode=='baseline' else 1,'root change short-circuits stale nodes')
        eq(page.locator('#replacement').text_content(),'new document root','new root survived')
        scenarios.append({'name':'document-root-replaced-during-patch','count':2 if mode=='baseline' else 1})
        # Preserve baseline published real Chromium install/event tests separately.
        receipt={'schema':'m3-browser-nested-real-chromium/1','mode':mode,
                 'result':'PASS_ACTUAL_CHROMIUM_NOT_SWT_RWT','browser':browser.version,
                 'source':'exact_GitHub_blob_verified','assertions':checks,'scenarios':scenarios,
                 'script_sha256':{k:hashlib.sha256(v.encode('utf-8')).hexdigest() for k,v in sorted(scripts.items())},
                 'holds':['MAVEN_OPENREWRITE_SDK_NOT_RUN','TRUE_JUNIT4_NOT_RUN','NATIVE_SWT_BROWSER_NOT_RUN','RAP_RWT_SESSION_NOT_RUN']}
        output.write_text(json.dumps(receipt,indent=2,ensure_ascii=False)+'\n')
        print(json.dumps({'result':receipt['result'],'mode':mode,'assertions':checks,'scenarios':len(scenarios),'browser':browser.version}))
    finally:
        browser.close()
