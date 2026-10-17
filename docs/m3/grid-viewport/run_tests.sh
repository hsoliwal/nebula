#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Run Grid JUnit 4 test classes. Usage: run_tests.sh <budget-seconds> <extra JVM options...> -- <test classes...>
export MSYS_NO_PATHCONV=1
OUT=K:/m3-work/agents/nebula-grid/out
WT=K:/m3-work/agents/nebula-grid/wt
JDK="C:/Program Files/Java/jdk-21"
SWT=C:/m3-work-c/integ-build/official/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
CP="$OUT/test-classes;${GRID_CLASSES:-$OUT/grid-classes};$SWT;$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar;$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar;$WT/widgets/grid/org.eclipse.nebula.widgets.grid.test/mockito-core-1.8.4.jar"
BUDGET=$1; shift
OPTS=(); while [ "$1" != "--" ]; do OPTS+=("$1"); shift; done; shift
TESTS=(); for t in "$@"; do TESTS+=("org.eclipse.nebula.widgets.grid.$t"); done
bash C:/m3-superset/tools/run_guarded.sh "$BUDGET" "$JDK/bin/java" -Xmx1500m "${OPTS[@]}" -cp "$CP" org.junit.runner.JUnitCore "${TESTS[@]}"
