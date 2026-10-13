#!/bin/sh
# usage: compile.sh <widget> <tag> <nebula-root> [extra-src-dir...]
# Compiles the widget's main bundle sources (tests/examples/snippets excluded) to $B/<widget>/<tag>/classes.
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
w=$1; tag=$2; root=$3; shift 3
out=$B/$w/$tag/classes; rm -rf $out; mkdir -p $out
srcs=$(find $root/widgets/$w -type d -name src -path '*/org.eclipse.nebula.widgets.*' | grep -vE '\.(tests?|example|example\.e4|snippets|feature|integration|css)/' )
for s in $srcs "$@"; do (cd $s && find . -type f ! -name '*.java' -exec sh -c 'mkdir -p "$0/$(dirname "$1")" && cp "$1" "$0/$1"' $out {} \;); done
find $srcs "$@" -name "*.java" | while read f; do cygpath -m "$f"; done > $B/$w/$tag/sources.lst
sh $B/scripts/gate.sh > /dev/null || { echo LOWMEM; exit 3; }
"$J/javac" -J-Xmx1g -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP" -d "$(cygpath -w $out)" @"$(cygpath -w $B/$w/$tag/sources.lst)" > $B/$w/$tag/javac.log 2>&1
rc=$?; echo "compile $w $tag rc=$rc files=$(wc -l < $B/$w/$tag/sources.lst) errors=$(grep -c ': error:' $B/$w/$tag/javac.log)"; exit $rc
