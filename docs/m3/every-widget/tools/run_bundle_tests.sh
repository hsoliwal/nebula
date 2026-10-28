#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Run JUnit 4 or 5 test classes of a build_bundle.sh --tests output on the hidden test desktop (one ui_lock slot), with the
# official SWT 3.135.0 jar of the build's class path or a given SWT jar first (SWT_JAR, for example the M3 fork jar with
# SWT.PartVisible), through the JUnit console launcher (Jupiter and Vintage engines). Prints a heartbeat every 30 s while
# it waits for a desktop slot or runs, then the tests found, successful, failed and aborted, and the failures.
# Usage: [OUT=<dir>] [SWT_JAR=<jar>] [MODE=viewport|original] [BUDGET=<s>] [JVM_OPTS=<opts>] run_bundle_tests.sh <label> <test class>...
set -uo pipefail; export LC_ALL=C MSYS_NO_PATHCONV=1
LABEL="${1:?label of a build_bundle.sh --tests output}"; shift; [ $# -gt 0 ] || { echo "no test classes"; exit 1; }
O=${OUT:-S:/m3-scratch/every-widget/out}/$LABEL; [ -d "$O/classes" ] || { echo "no build $O"; exit 1; }
M2=$(cygpath -m ~/.m2/repository); CS=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
cp="$O/classes;$(cat "$O/classpath.txt")"
[ -n "${SWT_JAR:-}" ] && cp="$SWT_JAR;$cp"
SEL=""; for c in "$@"; do SEL="$SEL --select-class $c"; done
LOG="$O/tests-$(date -u +%Y%m%dT%H%M%S).log"
( while sleep 30; do echo "[heartbeat $(date -u +%H:%M:%S)] waiting or running: $LABEL"; done ) & hb=$!
# JVM_OPTS: extra JVM options, for example --add-opens java.base/java.lang=ALL-UNNAMED for the cglib of Mockito 1.8.4
bash C:/m3-superset/tools/ui_lock.sh nebula-every-widget bash C:/m3-superset/tools/hidden_java.sh "${BUDGET:-900}" "$LOG" -Xmx1g ${JVM_OPTS:-} \
	-Dswt.widget.mode="${MODE:-viewport}" -jar "$CS" execute -cp "$cp" $SEL --disable-banner --details=tree > /dev/null 2>&1
kill $hb 2>/dev/null; wait $hb 2>/dev/null
sed 's/\x1b\[[0-9;]*m//g' "$LOG" | grep -E "tests (found|successful|failed|aborted)" | sed 's/^[[ ]*//; s/ *\]$//'
sed 's/\x1b\[[0-9;]*m//g' "$LOG" | grep -E "✘|■" | sed 's/^[│ ├└─]*//' | cut -c1-200 | head -n 20
echo "log: $LOG"
! sed 's/\x1b\[[0-9;]*m//g' "$LOG" | grep -qE "[1-9][0-9]* tests failed"
