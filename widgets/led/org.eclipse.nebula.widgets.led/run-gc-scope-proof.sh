#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
test_source="$root/tests/org/eclipse/nebula/widgets/led/LEDNestedGcScopeProof.java"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" "$test_source"
java -Xmx128m -cp "$work" org.eclipse.nebula.widgets.led.LEDNestedGcScopeProof
java -Xint -Xmx128m -cp "$work" org.eclipse.nebula.widgets.led.LEDNestedGcScopeProof --quick
