#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
source_root="$root/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid"
fixture_root="$root/releng/verification/grid-row-neighbors"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac --release 17 -Xlint:all -Werror -d "$out" \
  "$source_root/GridRowTree.java" \
  "$source_root/GridRowIndex.java" \
  "$source_root/model/HeapGrowth.java" \
  "$source_root/model/HeapSampler.java" \
  "$fixture_root/GridItem.java" \
  "$fixture_root/NextVisibleDifferential.java"
java -ea -cp "$out" org.eclipse.nebula.widgets.grid.NextVisibleDifferential
