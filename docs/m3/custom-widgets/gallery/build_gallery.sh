#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile the Gallery bundle (this worktree), the stock Gallery (33aaf10f, the reference) and the Gallery tests with
# javac. Run under C:/m3-superset/tools/build_lock.sh. Usage: build_gallery.sh [stock]
set -u
export MSYS_NO_PATHCONV=1
WT=S:/m3-scratch/worktrees/nebula-widgets
OUT=${GALLERY_OUT:-S:/m3-scratch/custom-widgets/out/gallery}
JDK="C:/Program Files/Java/jdk-21"
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
DEPS="$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar"
TESTDEPS="$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
B=widgets/gallery/org.eclipse.nebula.widgets.gallery/src
T=widgets/gallery/org.eclipse.nebula.widgets.gallery.tests/src
E=widgets/gallery/org.eclipse.nebula.widgets.gallery.example/src
mkdir -p "$OUT"
compile() { # <out> <classpath> <srcdir...>
  local o=$1 cp=$2; shift 2
  rm -rf "$o"; mkdir -p "$o"
  find "$@" -name '*.java' > "$o.src"
  "$JDK/bin/javac" -J-Xmx768m --release 17 -encoding UTF-8 -nowarn -proc:none -Xmaxerrs 50 -cp "$cp" -d "$o" @"$o.src" > "$o.log" 2>&1
  for d in "$@"; do (cd "$d" && find . -type f ! -name '*.java' | while read -r f; do mkdir -p "$o/$(dirname "$f")"; cp "$f" "$o/$f"; done); done
  echo "$(basename "$o"): sources=$(wc -l < "$o.src") errors=$(grep -c ': error:' "$o.log")"
  grep -A2 ': error:' "$o.log" | head -30
}
compile "$OUT/gallery-classes" "$SWT;$DEPS" "$WT/$B"
if [ "${1:-}" = stock ]; then
  rm -rf "$OUT/stock-src"; mkdir -p "$OUT/stock-src"
  git -C "$WT" archive 33aaf10f "$B" | tar -x -C "$OUT/stock-src"
  compile "$OUT/stock-classes" "$SWT;$DEPS" "$OUT/stock-src/$B"
fi
compile "$OUT/test-classes" "$OUT/gallery-classes;$SWT;$DEPS;$TESTDEPS" "$WT/$T"
EX=examples/org.eclipse.nebula.examples/src/org/eclipse/nebula/examples
UI="$D/org.eclipse.ui.workbench-3.140.0.jar;$D/org.eclipse.ui-3.209.100.jar;$D/org.eclipse.core.runtime-3.35.0.jar;$D/org.eclipse.osgi-3.24.300.jar;$D/org.eclipse.equinox.registry-3.12.600.jar;$D/org.eclipse.core.jobs-3.15.900.jar"
compile "$OUT/example-classes" "$OUT/gallery-classes;$SWT;$DEPS;$UI" "$WT/$E" "$WT/$EX/AbstractExampleTab.java" "$WT/$EX/ButtonFactory.java" "$WT/$EX/ListenersDialog.java" "$WT/$EX/ExamplesView.java"
SN=examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/gallery
compile "$OUT/snippet-classes" "$OUT/gallery-classes;$SWT;$DEPS" "$WT/$SN"
