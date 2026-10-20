#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Run RwtExamplesProbeTest (every example through RWT's render phase on RAP's test fixture, no browser) and write
# the per-example evidence TSV. Usage: probe_examples.sh <nebula-root> <lib-dir> <build-dir> <evidence.tsv>
# (build_compat.sh, test_compat.sh and build_examples.sh first)
set -u
ROOT=$1; LIB=$2; OUT=$3; EVIDENCE=$4
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}/bin"
M2=C:/Users/hsoliwal/.m2/repository
JUNIT=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
CP="$OUT/examples-classes;$(cat "$OUT/examples-classpath.txt");$OUT/org.eclipse.rap.rwt.testfixture-4.8.0-unsigned.jar"
rm -rf "$OUT/probe-classes"; mkdir -p "$OUT/probe-classes" "$OUT/tmp"
find "$ROOT/widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.examples.tests/src" -name '*.java' > "$OUT/probe-sources.txt"
"$JDK/javac" --release 21 -encoding UTF-8 -proc:none -d "$OUT/probe-classes" -cp "$CP;$JUNIT" @"$OUT/probe-sources.txt" || exit 1
"$JDK/java" -Xmx2g -Dm3.rwt.evidence="$EVIDENCE" -Djava.io.tmpdir="$OUT/tmp" -jar "$JUNIT" execute \
	--class-path "$OUT/probe-classes;$CP" --select-class org.eclipse.nebula.widgets.m3.rwt.examples.RwtExamplesProbeTest \
	--details=tree --disable-banner 2>&1 | sed 's/\x1b\[[0-9;]*m//g' > "$OUT/probe.log"
grep -E "tests (successful|failed)" "$OUT/probe.log"
