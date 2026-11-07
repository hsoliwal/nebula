#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../../.." && pwd)"
ACTUAL="$REPO/widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3/M3VirtualTableBinding.java"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
find "$HERE/src" -name '*.java' -type f | sort > "$TEMP/sources.txt"
echo "$ACTUAL" >> "$TEMP/sources.txt"
mkdir -p "$TEMP/classes"
javac --release 11 -proc:none -Xlint:all -Werror -encoding UTF-8 \
  -d "$TEMP/classes" @"$TEMP/sources.txt"
java -cp "$TEMP/classes" VirtualTablePopulationSmoke
