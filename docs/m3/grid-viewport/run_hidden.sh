#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Run Grid JUnit 4 test classes on the hidden test desktop.
# Usage: run_hidden.sh <budget-seconds> <log> <extra JVM options...> -- <test classes...>
OUT=K:/m3-work/agents/nebula-grid/out
WT=K:/m3-work/agents/nebula-grid/wt
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
CP="$OUT/test-classes;${GRID_CLASSES:-$OUT/grid-classes};$SWT;$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar;$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar;$WT/widgets/grid/org.eclipse.nebula.widgets.grid.test/mockito-core-1.8.4.jar;$M2/org/objenesis/objenesis/3.3/objenesis-3.3.jar"
BUDGET=$1; LOG=$2; shift 2
OPTS=(); while [ "$1" != "--" ]; do OPTS+=("$1"); shift; done; shift
TESTS=(); for t in "$@"; do TESTS+=("org.eclipse.nebula.widgets.grid.$t"); done
bash C:/m3-superset/tools/hidden_java.sh "$BUDGET" "$LOG" -Xmx1500m --add-opens=java.base/java.lang=ALL-UNNAMED "${OPTS[@]}" -cp "$CP" org.junit.runner.JUnitCore "${TESTS[@]}"
