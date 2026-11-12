#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compile and run the package-local EyeButton image-mode hover acceptance test on the hidden SWT desktop.
# First build the widget bundle with docs/m3/every-widget/tools/build_bundle.sh passwordrevealer.
# Usage: [OUT=<dir>] [BUILD_LABEL=passwordrevealer] [SWT_JAR=<jar>] [M3_JDK=<jdk>] run_eye_button_viewport_test.sh
set -euo pipefail
export LC_ALL=C MSYS_NO_PATHCONV=1

WT=${NEBULA_WT:-S:/m3-scratch/worktrees/nebula-grid-g5}
LABEL=${BUILD_LABEL:-passwordrevealer}
O=${OUT:-S:/m3-scratch/every-widget/out}/$LABEL
JDK=${M3_JDK:-C:/Program Files/Java/jdk-21}
TEST_SOURCE="$WT/docs/m3/every-widget/leaves/hover-repaint/EyeButtonViewportTest.java"
LOCK_SCRIPT=${UI_LOCK_SCRIPT:-C:/m3-superset/tools/ui_lock.sh}
HIDDEN_JAVA=${HIDDEN_JAVA_SCRIPT:-C:/m3-superset/tools/hidden_java.sh}
BUDGET=${BUDGET:-180}
[ -d "$O/classes" ] || { echo "missing bundle build: $O (run build_bundle.sh passwordrevealer first)" >&2; exit 2; }
[ -f "$TEST_SOURCE" ] || { echo "missing test source: $TEST_SOURCE" >&2; exit 2; }
[ -f "$O/classpath.txt" ] || { echo "missing classpath receipt: $O/classpath.txt" >&2; exit 2; }

cp="$O/classes;$(cat "$O/classpath.txt")"
[ -z "${SWT_JAR:-}" ] || cp="$SWT_JAR;$cp"
"$JDK/bin/javac" --release 17 -encoding UTF-8 -proc:none -Xlint:all -Werror \
  -cp "$cp" -d "$O/classes" "$TEST_SOURCE"
LOG="$O/eyebutton-hover-$(date -u +%Y%m%dT%H%M%S).log"
bash "$LOCK_SCRIPT" nebula-every-widget bash "$HIDDEN_JAVA" "$BUDGET" "$LOG" \
  -Xmx512m -Dswt.widget.mode="${MODE:-viewport}" -cp "$cp" \
  org.eclipse.nebula.widgets.passwordrevealer.EyeButtonViewportTest
grep -Fx 'EYE_BUTTON_IMAGE_HOVER_NO_REDRAW=PASS' "$LOG"
echo "log: $LOG"
