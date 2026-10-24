#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compile Nebula widget folders with javac, for the "every widget must be a viewport" waves: the plugins found under the
# requested folders (widgets/<name>/**/<plugin>/src, nested folders such as opal included), plus every in-repository plugin
# they need, transitively, through Require-Bundle (Bundle-SymbolicName) and Import-Package (Export-Package) of all
# manifests, in one javac run against the 2026-09 platform jars of the p2 pool. --tests adds the test plugins of the
# requested folders (JUnit 4.13.2, hamcrest 1.3, the JUnit console launcher and every jar inside the test plugins).
# Output: $OUT/<label>/classes (+ resources), javac.log, sources.txt, classpath.txt, plugins.txt, index.tsv.
# Usage: [NEBULA_WT=<worktree>] [OUT=<dir>] [LABEL=<name>] build_bundle.sh [--tests] <widget folder>...
# Example: build_bundle.sh --tests led opal
set -uo pipefail; export LC_ALL=C MSYS_NO_PATHCONV=1
TESTS=""; [ "${1:-}" = --tests ] && { TESTS=1; shift; }
[ $# -gt 0 ] || { echo "usage: build_bundle.sh [--tests] <widget folder>..."; exit 1; }
WT=${NEBULA_WT:-S:/m3-scratch/worktrees/nebula-grid-g5}; W=$WT/widgets
LABEL=${LABEL:-$(printf '%s-' "$@" | sed 's/-$//')}; [ -n "$TESTS" ] && LABEL="$LABEL-tests"
O=${OUT:-S:/m3-scratch/every-widget/out}/$LABEL; rm -rf "$O"; mkdir -p "$O/classes"
JDK="${M3_JDK:-C:/Program Files/Java/jdk-21}"; POOL=$(cygpath -m ~/.p2/pool/plugins); M2=$(cygpath -m ~/.m2/repository)
cp=""
for b in org.eclipse.swt.win32.win32.x86_64 org.eclipse.jface org.eclipse.jface.databinding org.eclipse.core.databinding \
	org.eclipse.core.databinding.property org.eclipse.core.databinding.observable org.eclipse.core.runtime org.eclipse.equinox.common \
	org.eclipse.core.commands org.eclipse.osgi org.eclipse.e4.ui.services org.eclipse.e4.ui.css.swt org.eclipse.e4.ui.css.core \
	org.eclipse.draw2d org.eclipse.ui.forms org.eclipse.ui.workbench org.eclipse.e4.core.contexts org.eclipse.e4.core.di \
	org.w3c.css.sac org.apache.batik.css org.eclipse.equinox.registry org.eclipse.core.jobs org.eclipse.e4.ui.workbench \
	org.eclipse.e4.core.services; do
	f=$(ls "$POOL" | grep "^${b}_[0-9]" | sort | tail -n 1); [ -n "$f" ] && cp="$cp;$POOL/$f"
done
# one pass over every manifest: dir, symbolic name, exported packages, required bundles, imported packages (plugins with sources)
find "$W" -maxdepth 5 -path '*/META-INF/MANIFEST.MF' -print0 | xargs -0 awk '
	function flush(   d, n) {
		if (file == "") return
		d = file; sub(/\/META-INF\/MANIFEST\.MF$/, "", d)
		n = h["Bundle-SymbolicName"]; sub(/;.*/, "", n)
		if (n != "") printf "%s\t%s\t%s\t%s\t%s\n", d, n, ids(h["Export-Package"]), ids(h["Require-Bundle"]), ids(h["Import-Package"])
		delete h; key = ""
	}
	function ids(v,   out, n, i, parts, p, depth, c, cur) {
		# split on commas outside quotes (version ranges contain commas)
		out = ""; cur = ""; q = 0
		for (i = 1; i <= length(v); i++) {
			c = substr(v, i, 1)
			if (c == "\"") q = !q
			if (c == "," && !q) { p = cur; sub(/;.*/, "", p); gsub(/ /, "", p); if (p != "") out = out (out == "" ? "" : ",") p; cur = "" } else cur = cur c
		}
		p = cur; sub(/;.*/, "", p); gsub(/ /, "", p); if (p != "") out = out (out == "" ? "" : ",") p
		return out
	}
	FNR == 1 { flush(); file = FILENAME }
	{ sub(/\r$/, "") }
	/^ / { if (key != "") h[key] = h[key] substr($0, 2); next }
	/^[A-Za-z][A-Za-z0-9-]*:/ { key = $0; sub(/:.*/, "", key); v = $0; sub(/^[^:]*: */, "", v); h[key] = v; next }
	END { flush() }' > "$O/index.tsv"
is_test() { case "$1" in *.test|*.tests) return 0 ;; *) return 1 ;; esac; }
is_skip() { case "$1" in *example*|*snippet*|*.feature|*.doc|*.documentation|*.demo|*.examples) return 0 ;; *) return 1 ;; esac; }
queue=""
for name in "$@"; do
	[ -d "$W/$name" ] || { echo "no widget folder $W/$name"; exit 1; }
	for d in $(awk -F'\t' -v p="$W/$name/" 'index($1, p) == 1 { print $1 }' "$O/index.tsv"); do
		[ -d "$d/src" ] || continue; b=$(basename "$d")
		is_skip "$b" && continue
		if is_test "$b"; then [ -n "$TESTS" ] && queue="$queue $d"; else queue="$queue $d"; fi
	done
done
plugins=""
while [ -n "${queue// /}" ]; do
	set -- $queue; d=$1; shift; queue="$*"
	case " $plugins " in *" $d "*) continue ;; esac
	plugins="$plugins $d"
	for dep in $(awk -F'\t' -v d="$d" '
		NR == FNR { if ($1 == d) { nr = split($4, r, ","); ni = split($5, im, ",") } next }
		FNR == 1 { for (i = 1; i <= nr; i++) need_b[r[i]] = 1; for (i = 1; i <= ni; i++) need_p[im[i]] = 1 }
		($2 in need_b) { print $1; next }
		{ ne = split($3, ex, ","); for (i = 1; i <= ne; i++) if (ex[i] in need_p) { print $1; next } }' "$O/index.tsv" "$O/index.tsv" | sort -u); do
		[ -d "$dep/src" ] || continue
		case " $plugins $queue " in *" $dep "*) ;; *) queue="$queue $dep" ;; esac
	done
done
if [ -n "$TESTS" ]; then
	cp="$cp;$M2/junit/junit/4.13.2/junit-4.13.2.jar;$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"
	cp="$cp;$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
	for d in $plugins; do for j in $(find "$d" -maxdepth 2 -name '*.jar' 2>/dev/null); do cp="$cp;$j"; done; done
fi
cp=${cp#;}
: > "$O/sources.txt"
for d in $plugins; do find "$d/src" -name '*.java' | sed 's#.*#"&"#' >> "$O/sources.txt"; done
printf '%s\n' "$cp" > "$O/classpath.txt"; printf '%s\n' $plugins > "$O/plugins.txt"
"$JDK/bin/javac" -J-Xmx1g --release 17 -encoding UTF-8 -nowarn -proc:none -Xmaxerrs 100 -cp "$cp" -d "$O/classes" @"$O/sources.txt" > "$O/javac.log" 2>&1
rc=$?
for d in $plugins; do (cd "$d/src" && find . -type f ! -name '*.java' | while IFS= read -r f; do mkdir -p "$O/classes/$(dirname "$f")"; cp "$f" "$O/classes/$f"; done); done
echo "compile $LABEL: plugins=$(printf '%s\n' $plugins | grep -c .) sources=$(wc -l < "$O/sources.txt") errors=$(grep -c ': error:' "$O/javac.log") rc=$rc ($O)"
grep -A2 ': error:' "$O/javac.log" | head -n 30
[ "$(grep -c ': error:' "$O/javac.log")" -eq 0 ]
