#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
# Real Nebula owner source; only SWT graphics Point is replaced by a test stub.
set -euo pipefail
cd "$(dirname "$0")/../.."
out=target/m3-grid-cell-selection-hash
mkdir -p "$out/classes"
java -version > "$out/java-version.txt" 2>&1
javac -version > "$out/javac-version.txt" 2>&1
sha256sum \
 "widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridCellSelectionList.java" \
 "tools/headless-grid-selection/src/org/eclipse/swt/graphics/Point.java" \
 "tools/headless-grid-selection/src/org/eclipse/nebula/widgets/grid/GridCellSelectionHashContract.java" > "$out/SOURCE_SHA256SUMS"
javac --release 21 -Xlint:all -Werror -d "$out/classes" \
 "tools/headless-grid-selection/src/org/eclipse/swt/graphics/Point.java" \
 "widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridCellSelectionList.java" \
 "tools/headless-grid-selection/src/org/eclipse/nebula/widgets/grid/GridCellSelectionHashContract.java" \
 > "$out/compile.log" 2>&1
java -ea -cp "$out/classes" org.eclipse.nebula.widgets.grid.GridCellSelectionHashContract | tee "$out/run.log"
sha256sum "$out/SOURCE_SHA256SUMS" "$out/compile.log" "$out/run.log" > "$out/EVIDENCE_SHA256SUMS"
