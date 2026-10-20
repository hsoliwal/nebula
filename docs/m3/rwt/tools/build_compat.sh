#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Build org.eclipse.rap.rwt-4.8.0-m3.jar: RWT 4.8.0 with the classes of
# widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.compat compiled over it. The Eclipse signature files are removed
# (a class replaced inside a signed package fails to load) and the per-entry digests are dropped from the manifest.
# Usage: build_compat.sh <nebula-root> <rap-lib-dir> <deps-dir> <out-dir>
set -eu
ROOT=$1; RAP=$2; DEPS=$3; OUT=$4
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}/bin"
SRC="$ROOT/widgets/m3rwt/org.eclipse.nebula.widgets.m3.rwt.compat/src"
RWT="$RAP/org.eclipse.rap.rwt-4.8.0.jar"
rm -rf "$OUT/compat-classes" "$OUT/rwt-m3"; mkdir -p "$OUT/compat-classes" "$OUT/rwt-m3"
find "$SRC" -name '*.java' | sort > "$OUT/compat-sources.txt"
"$JDK/javac" --release 21 -encoding UTF-8 -proc:none -Xlint:all,-serial,-rawtypes,-unchecked,-deprecation -d "$OUT/compat-classes" \
	-cp "$RWT;$DEPS/jakarta.servlet-api-6.1.0.jar" @"$OUT/compat-sources.txt"
( cd "$OUT/rwt-m3" && "$JDK/jar" xf "$RWT" )
rm -f "$OUT/rwt-m3"/META-INF/*.SF "$OUT/rwt-m3"/META-INF/*.RSA "$OUT/rwt-m3"/META-INF/*.DSA "$OUT/rwt-m3"/META-INF/*.EC
# keep the main manifest section only (up to the first blank line)
tr -d '\r' < "$OUT/rwt-m3/META-INF/MANIFEST.MF" | awk 'NF == 0 { exit } { print }' > "$OUT/MANIFEST.MF"
rm "$OUT/rwt-m3/META-INF/MANIFEST.MF"
cp -r "$OUT/compat-classes/." "$OUT/rwt-m3/"
rm -f "$OUT/org.eclipse.rap.rwt-4.8.0-m3.jar"
"$JDK/jar" cfm "$OUT/org.eclipse.rap.rwt-4.8.0-m3.jar" "$OUT/MANIFEST.MF" -C "$OUT/rwt-m3" .
echo "compat classes: $(find "$OUT/compat-classes" -name '*.class' | wc -l)"
sha256sum "$OUT/org.eclipse.rap.rwt-4.8.0-m3.jar"
