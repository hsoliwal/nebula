#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile the Nebula M3 widgets bundle, its tests, its example tab (with the Nebula examples base classes) and its
# snippets with javac. Run under C:/m3-superset/tools/build_lock.sh. Output: $M3_OUT (default S:/m3-scratch/custom-widgets/out/m3).
set -u
export MSYS_NO_PATHCONV=1
WT=S:/m3-scratch/worktrees/nebula-widgets
OUT=${M3_OUT:-S:/m3-scratch/custom-widgets/out/m3}
JDK="C:/Program Files/Java/jdk-21"
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
JFACE="$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar;$D/org.eclipse.ui.workbench-3.140.0.jar;$D/org.eclipse.ui-3.209.100.jar;$D/org.eclipse.core.runtime-3.35.0.jar;$D/org.eclipse.osgi-3.24.300.jar;$D/org.eclipse.equinox.registry-3.12.600.jar;$D/org.eclipse.core.jobs-3.15.900.jar"
JUNIT="$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
M=$WT/widgets/m3
EX=$WT/examples/org.eclipse.nebula.examples/src/org/eclipse/nebula/examples
mkdir -p "$OUT"
compile() { # <out> <classpath> <sources...>
  local o=$1 cp=$2; shift 2
  rm -rf "$o"; mkdir -p "$o"
  find "$@" -name '*.java' > "$o.src"
  "$JDK/bin/javac" -J-Xmx768m --release 17 -encoding UTF-8 -nowarn -proc:none -Xmaxerrs 50 -cp "$cp" -d "$o" @"$o.src" > "$o.log" 2>&1
  echo "$(basename "$o"): sources=$(wc -l < "$o.src") errors=$(grep -c ': error:' "$o.log")"
  grep -A2 ': error:' "$o.log" | head -30
}
compile "$OUT/m3-classes" "$SWT" "$M/org.eclipse.nebula.widgets.m3/src"
compile "$OUT/test-classes" "$OUT/m3-classes;$SWT;$JUNIT" "$M/org.eclipse.nebula.widgets.m3.tests/src"
compile "$OUT/example-classes" "$OUT/m3-classes;$SWT;$JFACE" "$M/org.eclipse.nebula.widgets.m3.example/src" "$EX/AbstractExampleTab.java" "$EX/ButtonFactory.java" "$EX/ListenersDialog.java" "$EX/ExamplesView.java"
compile "$OUT/snippet-classes" "$OUT/m3-classes;$OUT/example-classes;$SWT;$JFACE" "$M/org.eclipse.nebula.widgets.m3.snippets/src"
