#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile the CompositeTable bundle (this worktree), the stock CompositeTable (33aaf10f, the reference), its tests and
# its snippets with javac. Run under C:/m3-superset/tools/build_lock.sh. Usage: build_compositetable.sh [stock]
set -u
export MSYS_NO_PATHCONV=1
WT=S:/m3-scratch/worktrees/nebula-widgets
OUT=${COMPOSITETABLE_OUT:-S:/m3-scratch/custom-widgets/out/compositetable}
JDK="C:/Program Files/Java/jdk-21"
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
DEPS="$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar;$D/org.eclipse.core.databinding-1.13.700.jar;$D/org.eclipse.core.databinding.observable-1.13.500.jar;$D/org.eclipse.core.databinding.property-1.10.500.jar;$D/org.eclipse.jface.databinding-1.16.0.jar"
TESTDEPS="$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
B=widgets/compositetable/org.eclipse.nebula.widgets.compositetable/src
T=widgets/compositetable/org.eclipse.nebula.widgets.compositetable.tests/src
SN=examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/compositetable
mkdir -p "$OUT"
compile() { # <out> <classpath> <srcdir...>
  local o=$1 cp=$2; shift 2
  rm -rf "$o"; mkdir -p "$o"
  find "$@" -name '*.java' | xargs grep -L -e 'org.eclipse.swtbot' -e 'core.databinding' > "$o.src"
  "$JDK/bin/javac" -J-Xmx768m --release 17 -encoding UTF-8 -nowarn -proc:none -Xmaxerrs 50 -cp "$cp" -d "$o" @"$o.src" > "$o.log" 2>&1
  for d in "$@"; do (cd "$d" && find . -type f ! -name '*.java' | while read -r f; do mkdir -p "$o/$(dirname "$f")"; cp "$f" "$o/$f"; done); done
  echo "$(basename "$o"): sources=$(wc -l < "$o.src") errors=$(grep -c ': error:' "$o.log")"
  grep -A2 ': error:' "$o.log" | head -30
}
compile "$OUT/compositetable-classes" "$SWT;$DEPS" "$WT/$B"
if [ "${1:-}" = stock ]; then
  rm -rf "$OUT/stock-src"; mkdir -p "$OUT/stock-src"
  git -C "$WT" archive 33aaf10f "$B" | tar -x -C "$OUT/stock-src"
  compile "$OUT/stock-classes" "$SWT;$DEPS" "$OUT/stock-src/$B"
fi
compile "$OUT/test-classes" "$OUT/compositetable-classes;$SWT;$DEPS;$TESTDEPS" "$WT/$T"
[ -d "$WT/$SN" ] && compile "$OUT/snippet-classes" "$OUT/compositetable-classes;$SWT;$DEPS" "$WT/$SN"
