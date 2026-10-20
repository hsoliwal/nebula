#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Run M3 widget JUnit 4 classes (or a snippet main class) on the hidden test desktop with the official SWT 3.135.0
# jar, optionally under JaCoCo. Call through C:/m3-superset/tools/ui_lock.sh.
# Usage: run_m3.sh <budget-seconds> <log> <mode: viewport|original> <jacoco exec file or -> [-D...]... <test classes...>
#        run_m3.sh <budget-seconds> <log> <mode> - [-D...]... main:<class> [args...]
OUT=${M3_OUT:-S:/m3-scratch/custom-widgets/out/m3}
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
AGENT=$M2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
JFACE="$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar;$D/org.eclipse.ui.workbench-3.140.0.jar;$D/org.eclipse.ui-3.209.100.jar;$D/org.eclipse.core.runtime-3.35.0.jar;$D/org.eclipse.osgi-3.24.300.jar;$D/org.eclipse.equinox.registry-3.12.600.jar;$D/org.eclipse.core.jobs-3.15.900.jar"
BUDGET=$1; LOG=$2; MODE=$3; EXEC=$4; shift 4
CP="$OUT/test-classes;$OUT/snippet-classes;$OUT/example-classes;$OUT/m3jface-classes;$OUT/m3-classes;$SWT;$JFACE;$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
OPTS=(-Xmx1g "-Dswt.widget.mode.M3StackLayout=$MODE")
[ "$EXEC" != - ] && OPTS+=("-javaagent:$AGENT=destfile=$EXEC,append=true,includes=org.eclipse.nebula.widgets.m3.*")
while [ "${1:-}" != "${1#-D}" ]; do OPTS+=("$1"); shift; done
MAIN=org.junit.runner.JUnitCore; ARGS=()
for t in "$@"; do case "$t" in main:*) MAIN=${t#main:} ;; *.*) ARGS+=("$t") ;; *) if [ "$MAIN" = org.junit.runner.JUnitCore ]; then ARGS+=("org.eclipse.nebula.widgets.m3.tests.$t"); else ARGS+=("$t"); fi ;; esac; done
bash C:/m3-superset/tools/hidden_java.sh "$BUDGET" "$LOG" "${OPTS[@]}" -cp "$CP" "$MAIN" "${ARGS[@]}"
