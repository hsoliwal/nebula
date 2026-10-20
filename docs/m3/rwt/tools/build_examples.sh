#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile the RWT examples against org.eclipse.rap.rwt-4.8.0-m3.jar. Examples whose widget sources are listed in
# examples_sources.tsv are compiled with those sources ("-" = RAP only, "@m3" = the pinned M3 widget sources, else
# comma-separated Nebula source folders); an example whose sources do not compile on RWT is left out and reported.
# Usage: build_examples.sh <nebula-root> <m3-widget-src> <lib-dir> <build-dir>
set -u
ROOT=$1; M3SRC=$2; LIB=$3; OUT=$4
HERE=$(cd "$(dirname "$0")" && pwd)
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}/bin"
EX="$ROOT/widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.examples/src/org/eclipse/nebula/widgets/m3/rwt/examples"
R=$LIB/rap-4.8.0; P=$LIB/deps; J=$LIB/jetty
CP="$OUT/org.eclipse.rap.rwt-4.8.0-m3.jar;$R/org.eclipse.rap.jface-4.8.0.jar;$R/org.eclipse.rap.nebula.widgets.grid-4.8.0.jar;$R/org.eclipse.rap.nebula.widgets.richtext-4.8.0.jar;$P/org.eclipse.core.commands-3.13.0.jar;$P/org.eclipse.equinox.common-3.21.0.jar;$P/jakarta.servlet-api-6.1.0.jar"
for j in "$J"/*.jar; do CP="$CP;$j"; done
echo "$CP" > "$OUT/examples-classpath.txt"
rm -rf "$OUT/examples-classes"; mkdir -p "$OUT/examples-classes"
STATUS="$OUT/examples-build.tsv"; printf 'example\tsources\tbuild\n' > "$STATUS"
# layer 1: the framework, RAP-only examples and the M3 widgets
{ ls "$EX"/Rwt*.java "$EX"/VirtualTables.java
  awk -F'\t' -v ex="$EX" 'NR > 1 && ($2 == "-" || $2 == "@m3") { print ex "/" $1 ".java" }' "$HERE/examples_sources.tsv"
  find "$M3SRC" -name '*.java'; } > "$OUT/examples-layer1.txt"
if "$JDK/javac" --release 21 -encoding UTF-8 -proc:none -nowarn -d "$OUT/examples-classes" -cp "$CP" @"$OUT/examples-layer1.txt" > "$OUT/examples-layer1.log" 2>&1; then
	awk -F'\t' 'NR > 1 && ($2 == "-" || $2 == "@m3") { print $1 "\t" $2 "\tOK" }' "$HERE/examples_sources.tsv" >> "$STATUS"
else
	echo "layer 1 failed:"; grep -E "error:" "$OUT/examples-layer1.log" | head -20; exit 1
fi
# layer 2: one Nebula widget at a time
awk -F'\t' 'NR > 1 && $2 != "-" && $2 != "@m3"' "$HERE/examples_sources.tsv" | while IFS=$'\t' read -r example sources; do
	{ echo "$EX/$example.java"; for dir in ${sources//,/ }; do find "$ROOT/$dir" -name '*.java'; done; } > "$OUT/examples-$example.txt"
	mkdir -p "$OUT/examples-$example"
	if "$JDK/javac" --release 21 -encoding UTF-8 -proc:none -nowarn -d "$OUT/examples-$example" -cp "$OUT/examples-classes;$CP" @"$OUT/examples-$example.txt" > "$OUT/examples-$example.log" 2>&1; then
		cp -r "$OUT/examples-$example/." "$OUT/examples-classes/"; printf '%s\t%s\tOK\n' "$example" "$sources" >> "$STATUS"
	else
		printf '%s\t%s\tFAILED %s errors\n' "$example" "$sources" "$(grep -c ': error:' "$OUT/examples-$example.log")" >> "$STATUS"
	fi
done
column -t -s$'\t' "$STATUS"
