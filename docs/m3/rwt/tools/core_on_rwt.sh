#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# One model: compile the SWT fork's shared viewport core (pinned commit, exported with git archive) against the
# patched RWT jar, unchanged, and run the SWT fork's own GWT gate on the same export (WRAP).
# Usage: core_on_rwt.sh <swt-export-root> <rwt-m3.jar> <gwt-lib> <out-dir>
set -u
SWT=$1; RWT=$2; GWT=$3; OUT=$4
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}"
C="$SWT/bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt"
rm -rf "$OUT"; mkdir -p "$OUT/classes"
for f in widgets/VirtualViewportPlanner widgets/VirtualSelectionModel widgets/VirtualScrollMetrics widgets/VirtualListModel \
	widgets/VirtualListWidth widgets/VirtualRowExtents widgets/VirtualBitLanes widgets/VirtualGrowth internal/ViewportRuntime \
	internal/PackedHeapGrowth; do echo "$C/$f.java"; done > "$OUT/sources.txt"
"$JDK/bin/javac" --release 21 -encoding UTF-8 -proc:none -d "$OUT/classes" -cp "$RWT" @"$OUT/sources.txt" > "$OUT/javac.log" 2>&1
echo "javac against RWT: exit $? ($(grep -c ': error:' "$OUT/javac.log") errors, $(find "$OUT/classes" -name '*.class' | wc -l) classes)"
python "$SWT/tests/org.eclipse.swt.tests/gwt-gate/run_gate.py" --swt-root "$SWT" --gwt-lib "$GWT" --work "$OUT/gwt" \
	--java-home "$JDK" --node "C:/Program Files/nodejs/node.exe" > "$OUT/gwt-gate.log" 2>&1
echo "GWT gate: exit $?"; tail -5 "$OUT/gwt-gate.log"
