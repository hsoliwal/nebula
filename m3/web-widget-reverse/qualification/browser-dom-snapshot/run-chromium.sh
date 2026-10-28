#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Test-only. Compile actual original Nebula Java and run its JS in real Chromium.
set -euo pipefail
if [[ $# != 1 ]]; then echo 'usage: run-chromium.sh output-directory' >&2; exit 2; fi
here="$(cd -- "$(dirname -- "$0")" && pwd)"
repo="$(git -C "$here" rev-parse --show-toplevel)"
original="$repo/widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/internal/M3BrowserScripts.java"
emitter="$here/src/main/java/org/eclipse/nebula/widgets/m3/qualification/BrowserNestedPatchEmit.java"
expected='33e62975550a2b2c6ca7e8958b535ffca27efe25'
actual="$(git -C "$repo" hash-object "$original")"
if [[ "$actual" != "$expected" ]]; then
  echo "BROWSER_DOM_CANDIDATE_SOURCE_DRIFT expected=$expected got=$actual" >&2
  exit 3
fi
out="$1"
if [[ -e "$out/DOM_RECEIPT.json" ]]; then
  echo 'refusing duplicate browser proof receipt' >&2
  exit 4
fi
mkdir -p "$out/classes" "$out/js"
javac --release 11 -proc:none -Xlint:all -Werror -encoding UTF-8 \
  -d "$out/classes" "$original" "$emitter"
java -cp "$out/classes" org.eclipse.nebula.widgets.m3.qualification.BrowserNestedPatchEmit "$out/js"
python3 "$here/real_chromium.py" "$out/js" candidate "$out/DOM_RECEIPT.json"
python3 - "$out/DOM_RECEIPT.json" <<'PY'
from pathlib import Path
import json,sys
r=json.loads(Path(sys.argv[1]).read_text())
assert r.get('result')=='PASS_ACTUAL_CHROMIUM_NOT_SWT_RWT'
assert r.get('assertions')==43 and len(r.get('scenarios',[]))==10
print('M3_BROWSER_ACTUAL_CHROMIUM_GATE_PASS assertions=43 scenarios=10')
PY
if [[ -n "$(git -C "$repo" status --porcelain --untracked-files=normal)" ]]; then
  echo 'source checkout changed during Chromium proof' >&2
  exit 5
fi
