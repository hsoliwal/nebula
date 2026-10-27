#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
set -eu
if [ "$#" -ne 1 ]; then echo 'usage: browser-dom-replay.sh generated-script-folder' >&2; exit 2; fi
node --input-type=module - "$1" <<'M3_NODE_EOF'
// SPDX-License-Identifier: Apache-2.0
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { strict as assert } from 'node:assert';

const folder = process.argv[2];
if (!folder) throw new Error('usage: node browser-dom-replay.mjs output-folder');
let patches = 0;
let dispatched = 0;
const nodes = [
 { id: 'a"] [data-nebula-id="victim', type:'label', textContent: '',
   getAttribute(k){return k==='data-nebula-id'?this.id:null;} },
 { id: 'normal', type:'text', textContent: '', value:'',
   getAttribute(k){return k==='data-nebula-id'?this.id:null;} },
 { id: 'victim', type:'text', textContent: 'untouched', value:'',
   getAttribute(k){return k==='data-nebula-id'?this.id:null;} }
];
const handlers = new Map();
const document = {
 querySelectorAll(sel){assert.equal(sel,'[data-nebula-id]');return nodes;},
 addEventListener(type, callback, capture){assert.equal(capture,true);assert(!handlers.has(type));handlers.set(type,callback);},
 __m3NebulaInstalled:false
};
const window={m3NebulaEvent(type,id,value){dispatched++;assert.equal(typeof id,'string');assert.equal(typeof value,'string');}};
const context={document,window};
const evalFile = path => runInNewContext(readFileSync(`${folder}/${path}`,'utf8'),context,{timeout:1000});
patches += evalFile('text.js');
assert.equal(patches,2);
assert.equal(nodes[0].textContent, '<img onerror=evil()> & 🙂');
assert.equal(nodes[1].textContent, 'line 1\nline 2\u2028');
assert.equal(nodes[2].textContent, 'untouched');
patches += evalFile('values.js');
assert.equal(patches,3);
assert.equal(nodes[1].value,'a<&"');
assert.equal(nodes[0].value,undefined);
assert.equal(evalFile('install.js'),true);
assert.equal(evalFile('install.js'),true);
assert.equal(handlers.size,3);
const target={nodeType:1,hasAttribute(){return false;},parentElement:{
 nodeType:1,hasAttribute(x){return x==='data-nebula-id';},getAttribute(){return 'normal';},type:'checkbox',checked:true
}};
handlers.get('click')({type:'click',target});
assert.equal(dispatched,1);
console.log(`NODE_DOM_REPLAY_PASS updated=${patches} delegatedEvents=${dispatched} handlers=${handlers.size}`);
M3_NODE_EOF
