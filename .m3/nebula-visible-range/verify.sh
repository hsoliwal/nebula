#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
set -euo pipefail
lane=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
root=$(cd -- "$lane/../.." && pwd)
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
source="$root/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridVisibleRangeDiff.java"
test="$lane/src/test/java/org/eclipse/nebula/widgets/grid/GridVisibleRangeDiffContract.java"
javac --release 21 -Xlint:all -Werror -d "$build/classes" "$source" "$test"
java -cp "$build/classes" org.eclipse.nebula.widgets.grid.GridVisibleRangeDiffContract
# Java 21 remains warning-strict; the Java 8 check omits only obsolete-option warnings.
javac --release 8 -Xlint:all,-options -Werror -d "$build/java8" "$source"
