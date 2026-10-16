#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
testdir="$root/tests/org/eclipse/nebula/widgets/pshelf"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" \
  "$testdir/PShelfItemCountContractTest.java"
java -Xmx128m -cp "$work" org.eclipse.nebula.widgets.pshelf.PShelfItemCountContractTest
