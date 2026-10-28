#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# JaCoCo line and branch coverage of the M3 widget classes over the M3 test suite, both startup modes, on the hidden
# desktop (call through ui_lock.sh). Report: $M3_OUT/coverage (csv + html) and a one-line summary per package.
# Usage: coverage_m3.sh <test classes...>
OUT=${M3_OUT:-S:/m3-scratch/custom-widgets/out/m3}
WT=S:/m3-scratch/worktrees/nebula-widgets
M2=C:/Users/hsoliwal/.m2/repository
CLI=$M2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
HERE=$(cd "$(dirname "$0")" && pwd)
COV=$OUT/coverage; rm -rf "$COV"; mkdir -p "$COV"
for mode in viewport original; do
  bash "$HERE/run_m3.sh" 900 "$COV/run-$mode.log" $mode "$COV/m3.exec" "$@"
  grep -E "^Tests run|^OK" "$COV/run-$mode.log" | sed "s/^/$mode: /"
done
"C:/Program Files/Java/jdk-21/bin/java" -jar "$CLI" report "$COV/m3.exec" --classfiles "$OUT/m3-classes" \
  --sourcefiles "$WT/widgets/m3/org.eclipse.nebula.widgets.m3/src" --csv "$COV/coverage.csv" --html "$COV/html" > "$COV/report.log" 2>&1
awk -F, 'NR>1 {ml+=$8; cl+=$9; mb+=$6; cb+=$7; printf "%s.%s line %.1f%% (%d/%d) branch %.1f%% (%d/%d)\n", $2, $3, 100*$9/($8+$9), $9, $8+$9, ($6+$7)?100*$7/($6+$7):100, $7, $6+$7}
  END {printf "TOTAL line %.2f%% (%d/%d) branch %.2f%% (%d/%d)\n", 100*cl/(ml+cl), cl, ml+cl, 100*cb/(mb+cb), cb, mb+cb}' "$COV/coverage.csv"
