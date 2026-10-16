#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
src="$root/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/src/org/eclipse/nebula/widgets/pshelf"
tests="$root/widgets/pshelf/org.eclipse.nebula.widgets.pshelf/tests/org/eclipse/nebula/widgets/pshelf"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" \
  "$src/PShelfViewportGeometry.java" "$tests/PShelfViewportGeometryTest.java"
java -Xmx256m -cp "$work" org.eclipse.nebula.widgets.pshelf.PShelfViewportGeometryTest
