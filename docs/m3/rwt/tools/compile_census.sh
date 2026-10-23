#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Compile every Nebula main source directory (tests, examples, snippets and demos excluded) in ONE javac run
# against a given UI toolkit class path, and write the diagnostics as TSV.
# Usage: compile_census.sh <nebula-root> <out-dir> <label> <class-path> [extra source dir ...]
# Output: <out-dir>/<label>/{sources.txt,javac.log,errors.tsv}
#   errors.tsv columns: file, line, family, kind, detail, symbol, location
#   kind = PKG (package does not exist), SYM (cannot find symbol), OTHER (any other error)
set -u
ROOT=$1; OUT=$2; LABEL=$3; CP=$4; shift 4
JAVAC="${JAVA_HOME:-C:/Program Files/Java/jdk-21}/bin/javac"
D="$OUT/$LABEL"
rm -rf "$D"; mkdir -p "$D/classes"
cd "$ROOT" || exit 2
find widgets -type d -name src -not -path '*/target/*' \
	| grep -viE '\.tests?/|\.examples?/|\.snippets?/|\.demos?/|example|snippet|/test' | sort > "$D/src_dirs.txt"
for extra in "$@"; do echo "$extra" >> "$D/src_dirs.txt"; done
while read -r dir; do find "$dir" -name '*.java'; done < "$D/src_dirs.txt" | sort > "$D/sources.txt"
"$JAVAC" --release 21 -proc:none -nowarn -encoding UTF-8 -Xmaxerrs 1000000 -XDshould-stop.ifError=FLOW \
	-implicit:none -d "$D/classes" -cp "$CP" @"$D/sources.txt" > "$D/javac.log" 2>&1
echo "javac exit $? for $(wc -l < "$D/sources.txt") files"
tr -d '\r' < "$D/javac.log" | awk -v OFS='\t' '
	function flush() { if (file != "") print file, line, fam, kind, detail, (sym == "" ? "-" : sym), (loc == "" ? "-" : loc); file = "" }
	/^[^ ].*\.java:[0-9]+: error: / {
		flush()
		p = index($0, ": error: "); head = substr($0, 1, p - 1); detail = substr($0, p + 9)
		n = split(head, a, ":"); line = a[n]; file = substr(head, 1, length(head) - length(line) - 1)
		gsub(/\\/, "/", file); split(file, f, "/"); fam = f[2]; sym = ""; loc = ""
		kind = "OTHER"
		if (detail ~ /^package .* does not exist$/) { kind = "PKG"; sym = detail; sub(/^package /, "", sym); sub(/ does not exist$/, "", sym) }
		else if (detail ~ /^cannot find symbol/) kind = "SYM"
		next
	}
	/^  symbol: / { s = $0; sub(/^  symbol: +/, "", s); sym = s; next }
	/^  location: / { s = $0; sub(/^  location: +/, "", s); loc = s; next }
	END { flush() }' > "$D/errors.tsv"
echo "errors: $(wc -l < "$D/errors.tsv")"
