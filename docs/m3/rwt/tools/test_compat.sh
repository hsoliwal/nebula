#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile and run the compat JUnit tests on RAP's test fixture against org.eclipse.rap.rwt-4.8.0-m3.jar, with JaCoCo.
# Usage: test_compat.sh <nebula-root> <rap-lib-dir> <deps-dir> <build-dir>   (build_compat.sh first)
set -eu
ROOT=$1; RAP=$2; DEPS=$3; OUT=$4
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}/bin"
M2=C:/Users/hsoliwal/.m2/repository
JUNIT=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
JACOCO=$M2/org/jacoco/org.jacoco.agent/0.8.12/org.jacoco.agent-0.8.12-runtime.jar
TESTS="$ROOT/widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.compat.tests/src"
bash "$(dirname "$0")/unsign_jar.sh" "$RAP/org.eclipse.rap.rwt.testfixture-4.8.0.jar" "$OUT/org.eclipse.rap.rwt.testfixture-4.8.0-unsigned.jar" "$OUT/unsign-testfixture"
CP="$OUT/org.eclipse.rap.rwt-4.8.0-m3.jar;$OUT/org.eclipse.rap.rwt.testfixture-4.8.0-unsigned.jar;$DEPS/jakarta.servlet-api-6.1.0.jar"
rm -rf "$OUT/compat-test-classes"; mkdir -p "$OUT/compat-test-classes"
find "$TESTS" -name '*.java' | sort > "$OUT/compat-test-sources.txt"
"$JDK/javac" --release 21 -encoding UTF-8 -proc:none -d "$OUT/compat-test-classes" -cp "$CP;$JUNIT" @"$OUT/compat-test-sources.txt"
rm -f "$OUT/jacoco-compat.exec"
"$JDK/java" -javaagent:"$JACOCO=destfile=$OUT/jacoco-compat.exec,includes=org.eclipse.swt.*M3*:org.eclipse.swt.graphics.Pattern*" \
	-Djava.io.tmpdir="$OUT/tmp" -jar "$JUNIT" execute --class-path "$OUT/compat-test-classes;$CP" \
	--scan-class-path "$OUT/compat-test-classes" --details=tree --disable-banner 2>&1 | tee "$OUT/compat-tests.log" | tail -40
# coverage of the classes this bundle adds (patched RWT files are reported per method in the CSV)
JACOCO_CLI=$M2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
rm -rf "$OUT/coverage-classes" "$OUT/coverage"; mkdir -p "$OUT/coverage-classes" "$OUT/coverage"
( cd "$OUT/compat-classes" && find . -name '*.class' | grep -E '/(M3[A-Za-z]*|Pattern)(\$[A-Za-z0-9]*)?\.class$' | while read -r c; do mkdir -p "$OUT/coverage-classes/$(dirname "$c")"; cp "$c" "$OUT/coverage-classes/$c"; done )
"$JDK/java" -jar "$JACOCO_CLI" report "$OUT/jacoco-compat.exec" --classfiles "$OUT/coverage-classes" \
	--sourcefiles "$ROOT/widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.compat/src" --csv "$OUT/coverage/compat.csv" --html "$OUT/coverage/html" --quiet
awk -F, 'NR > 1 { li += $8; lm += $9; bi += $6; bm += $7 } END { printf "coverage of added classes: lines %d/%d (%.1f%%), branches %d/%d (%.1f%%)\n", lm, li + lm, 100 * lm / (li + lm), bm, bi + bm, 100 * bm / (bi + bm) }' "$OUT/coverage/compat.csv"
