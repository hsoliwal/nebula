#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
test="$root/tests/org/eclipse/nebula/widgets/pgroup/PGroupSelectionParity.java"
javac --release 21 -Xlint:all -Werror -d "$work" "$test"
java -Xmx192m -cp "$work" org.eclipse.nebula.widgets.pgroup.PGroupSelectionParity
java -Xint -Xmx192m -cp "$work" org.eclipse.nebula.widgets.pgroup.PGroupSelectionParity --quick
