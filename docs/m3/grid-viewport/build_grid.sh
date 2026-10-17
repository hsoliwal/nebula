#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compile the Nebula Grid bundle (worktree), the upstream Nebula Grid (5d0bcbb9, oracle) and the Grid tests with javac.
# Run under build_lock.sh. Usage: build_grid.sh [upstream]
set -u
export MSYS_NO_PATHCONV=1
WT=K:/m3-work/agents/nebula-grid/wt
OUT=K:/m3-work/agents/nebula-grid/out
JDK="C:/Program Files/Java/jdk-21"
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
DEPS="$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar"
TESTDEPS="$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar;$WT/widgets/grid/org.eclipse.nebula.widgets.grid.test/mockito-core-1.8.4.jar"
G=$WT/widgets/grid/org.eclipse.nebula.widgets.grid/src
T=$WT/widgets/grid/org.eclipse.nebula.widgets.grid.test/src
compile() { # <out> <classpath> <srcdir>
  rm -rf "$1"; mkdir -p "$1"
  find "$3" -name '*.java' > "$1.src"
  "$JDK/bin/javac" -J-Xmx768m --release 17 -encoding UTF-8 -nowarn -proc:none -Xmaxerrs 50 -cp "$2" -d "$1" @"$1.src" > "$1.log" 2>&1
  # resources (images and other non-Java files) next to the classes, as the bundle build does
  (cd "$3" && find . -type f ! -name '*.java' | while read -r f; do mkdir -p "$1/$(dirname "$f")"; cp "$f" "$1/$f"; done)
  echo "$(basename $1): sources=$(wc -l < "$1.src") resources=$(cd "$3" && find . -type f ! -name '*.java' | wc -l) errors=$(grep -c ': error:' "$1.log")"
  grep -A2 ': error:' "$1.log" | head -30
}
compile $OUT/grid-classes "$SWT;$DEPS" $G
if [ "${1:-}" = upstream ]; then
  rm -rf $OUT/upstream-src; mkdir -p $OUT/upstream-src
  git -C $WT archive 5d0bcbb9c6e3a118685a5ec91c6f5d8eb7833893 widgets/grid/org.eclipse.nebula.widgets.grid/src | tar -x -C $OUT/upstream-src
  compile $OUT/upstream-classes "$SWT;$DEPS" $OUT/upstream-src/widgets/grid/org.eclipse.nebula.widgets.grid/src
fi
compile $OUT/test-classes "$OUT/grid-classes;$SWT;$DEPS;$TESTDEPS" $T
