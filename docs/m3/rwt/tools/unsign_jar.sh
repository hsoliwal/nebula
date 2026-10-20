#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Copy a jar without its signature: signature files removed, manifest reduced to its main section. Needed for jars
# that put classes into a package of the patched (unsigned) RWT jar, e.g. org.eclipse.rap.rwt.testfixture.
# Usage: unsign_jar.sh <in.jar> <out.jar> <work-dir>
set -eu
IN=$1; OUT=$2; WORK=$3
JDK="${M3RWT_JDK:-C:/Program Files/Java/jdk-21}/bin"
rm -rf "$WORK"; mkdir -p "$WORK/x"
( cd "$WORK/x" && "$JDK/jar" xf "$IN" )
rm -f "$WORK"/x/META-INF/*.SF "$WORK"/x/META-INF/*.RSA "$WORK"/x/META-INF/*.DSA "$WORK"/x/META-INF/*.EC
tr -d '\r' < "$WORK/x/META-INF/MANIFEST.MF" | awk 'NF == 0 { exit } { print }' > "$WORK/MANIFEST.MF"
rm "$WORK/x/META-INF/MANIFEST.MF"
rm -f "$OUT"
"$JDK/jar" cfm "$OUT" "$WORK/MANIFEST.MF" -C "$WORK/x" .
