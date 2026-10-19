#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Run M3 widget JUnit 4 classes on the hidden test desktop with the official SWT 3.135.0 jar, optionally under JaCoCo.
# Call through C:/m3-superset/tools/ui_lock.sh. Usage: run_m3.sh <budget-seconds> <log> <mode: viewport|original>
#   <jacoco exec file or -> <test classes...>
OUT=${M3_OUT:-S:/m3-scratch/custom-widgets/out/m3}
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
M2=C:/Users/hsoliwal/.m2/repository
AGENT=$M2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
BUDGET=$1; LOG=$2; MODE=$3; EXEC=$4; shift 4
CP="$OUT/test-classes;$OUT/m3-classes;$SWT;$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
OPTS=(-Xmx1g "-Dswt.widget.mode.M3StackLayout=$MODE")
[ "$EXEC" != - ] && OPTS+=("-javaagent:$AGENT=destfile=$EXEC,append=true,includes=org.eclipse.nebula.widgets.m3.*")
TESTS=(); for t in "$@"; do TESTS+=("org.eclipse.nebula.widgets.m3.tests.$t"); done
bash C:/m3-superset/tools/hidden_java.sh "$BUDGET" "$LOG" "${OPTS[@]}" -cp "$CP" org.junit.runner.JUnitCore "${TESTS[@]}"
