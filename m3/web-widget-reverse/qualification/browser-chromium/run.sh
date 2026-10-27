#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Test-only: real Chromium DOM qualification of original compiled Nebula JS.
set -euo pipefail
if [[ $# -ne 1 ]]; then echo 'usage: run.sh output-directory' >&2; exit 2; fi
out="$1"
repo="$(git rev-parse --show-toplevel)"
here="$(cd -- "$(dirname -- "$0")" && pwd)"
original="$repo/widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/internal/M3BrowserScripts.java"
emitter="$here/src/main/java/org/eclipse/nebula/widgets/m3/qualification/M3BrowserChromiumFixture.java"
expected='dadedf428c28efd95e56ff102f153c2d925963aa'
observed="$(git -C "$repo" hash-object "$original")"
if [[ "$observed" != "$expected" ]]; then
  echo "BROWSER_SOURCE_DRIFT expected=$expected observed=$observed" >&2
  exit 3
fi
mkdir -p "$out/classes" "$out/js"
if [[ -e "$out/CHROMIUM_RECEIPT.json" ]]; then
  echo 'Refusing to overwrite a prior proof receipt' >&2
  exit 4
fi
javac --release 11 -proc:none -Xlint:all -Werror -d "$out/classes" "$original" "$emitter"
java -cp "$out/classes" org.eclipse.nebula.widgets.m3.qualification.M3BrowserChromiumFixture "$out/js"
python3 "$here/browser_test.py" "$out/js" "$out/CHROMIUM_RECEIPT.json"
# A test execution never mutates source or Git metadata.
if [[ -n "$(git -C "$repo" status --porcelain --untracked-files=normal)" ]]; then
  echo 'Git checkout changed during Browser qualification' >&2
  exit 5
fi
