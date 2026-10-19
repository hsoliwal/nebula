#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Run CompositeTable JUnit 4 classes (or a snippet main class) on the hidden test desktop; call through
# C:/m3-superset/tools/ui_lock.sh.
# Usage: run_compositetable.sh <budget-seconds> <log> <classes-dir: compositetable-classes|stock-classes> <JVM options...> -- <test classes...>
# A class given as main:<class> runs that main class with the remaining arguments instead of JUnit.
OUT=${COMPOSITETABLE_OUT:-S:/m3-scratch/custom-widgets/out/compositetable}
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
M2=C:/Users/hsoliwal/.m2/repository
BUDGET=$1; LOG=$2; CLS=$3; shift 3
CP="$OUT/test-classes;$OUT/snippet-classes;$OUT/$CLS;$SWT;$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar;$D/org.eclipse.core.databinding-1.13.700.jar;$D/org.eclipse.core.databinding.observable-1.13.500.jar;$D/org.eclipse.core.databinding.property-1.10.500.jar;$D/org.eclipse.jface.databinding-1.16.0.jar;$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
OPTS=(); while [ "$1" != "--" ]; do OPTS+=("$1"); shift; done; shift
MAIN=org.junit.runner.JUnitCore; TESTS=()
for t in "$@"; do case "$t" in main:*) MAIN=${t#main:} ;; *.*) TESTS+=("$t") ;; *) if [ "$MAIN" = org.junit.runner.JUnitCore ]; then TESTS+=("org.eclipse.nebula.widgets.compositetable.$t"); else TESTS+=("$t"); fi ;; esac; done
bash C:/m3-superset/tools/hidden_java.sh "$BUDGET" "$LOG" -Xmx1500m "${OPTS[@]}" -cp "$CP" "$MAIN" "${TESTS[@]}"
