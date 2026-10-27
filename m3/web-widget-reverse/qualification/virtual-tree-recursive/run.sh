#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
REPO=$(cd "$HERE/../../../.." && pwd)
REAL="$REPO/widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/M3VirtualTreeBinding.java"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
find "$HERE/src" -type f -name '*.java' | sort > "$TMP/sources.txt"
echo "$REAL" >> "$TMP/sources.txt"
mkdir -p "$TMP/classes"
javac --release 11 -Xlint:all -Werror -encoding UTF-8 -d "$TMP/classes" @"$TMP/sources.txt"
java -cp "$TMP/classes" TreeRecursiveClearSmoke
