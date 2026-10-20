#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Platform proof of the custom widgets: compile the M3 widgets bundle (and, when given, more source roots) against the
# SWT classes of each platform source set, win32, gtk and cocoa, as compiled by C:/m3-superset/tools/compile_platform.sh
# (C:/m3-superset/build-check-<platform>-wt). The widgets are Java drawn on Canvas/Composite with public SWT API only,
# so 0 errors on all three means one implementation serves the three desktop platforms.
# Usage: compile_platforms.sh [extra source roots...]   Output: S:/m3-scratch/custom-widgets/out/platform-<p>
set -u
export MSYS_NO_PATHCONV=1
WT=S:/m3-scratch/worktrees/nebula-widgets
JDK="C:/Program Files/Java/jdk-21"
OUT=S:/m3-scratch/custom-widgets/out
ROOTS=("$WT/widgets/m3/org.eclipse.nebula.widgets.m3/src" "$@")
status=0
for p in win32 gtk cocoa; do
  swt=C:/m3-superset/build-check-$p-wt
  o=$OUT/platform-$p
  snap=$OUT/swt-$p
  # the shared class directory is rebuilt by other tracks: keep the last complete copy (0 errors) and use it
  if [ -f "$swt/org/eclipse/swt/widgets/Canvas.class" ] && [ "$(grep -c ': error:' "$swt.log" 2>/dev/null)" = 0 ] && [ "$swt.log" -nt "$snap.stamp" ]; then
    rm -rf "$snap" && cp -r "$swt" "$snap" && cp "$swt.log" "$snap.stamp"
  fi
  if [ ! -f "$snap/org/eclipse/swt/widgets/Canvas.class" ]; then echo "$p: no SWT classes at $swt"; status=1; continue; fi
  echo "$p: SWT classes of $swt as of $(date -r "$snap.stamp" '+%F %T')"
  swt=$snap
  rm -rf "$o"; mkdir -p "$o"
  find "${ROOTS[@]}" -name '*.java' > "$o.src"
  "$JDK/bin/javac" -J-Xmx768m --release 21 -encoding UTF-8 -nowarn -proc:none -Xmaxerrs 50 -cp "$swt" -d "$o" @"$o.src" > "$o.log" 2>&1
  n=$(grep -c ': error:' "$o.log")
  echo "$p: sources=$(wc -l < "$o.src") errors=$n swt=$swt ($(date -r "$swt" +%F))"
  grep -A2 ': error:' "$o.log" | head -12
  [ "$n" = 0 ] || status=1
done
exit $status
