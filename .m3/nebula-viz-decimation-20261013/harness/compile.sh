#!/bin/sh
# usage: compile.sh <tag> <root>   compiles xygraph(src+examples), widgets, oscilloscope main sources -> $B/<tag>/classes
. /c/m3-work-c/viz-build/scripts/env.sh
tag=$1; root=$2
out=$B/$tag/classes; rm -rf $out; mkdir -p $out
V=$root/widgets/visualization; O=$root/widgets/oscilloscope
srcs="$V/org.eclipse.nebula.visualization.xygraph/src $V/org.eclipse.nebula.visualization.xygraph/examples $V/org.eclipse.nebula.visualization.widgets/src $O/org.eclipse.nebula.widgets.oscilloscope/src"
for s in $srcs; do (cd $s && find . -type f ! -name '*.java' -exec sh -c 'mkdir -p "$0/$(dirname "$1")" && cp "$1" "$0/$1"' $out {} \;); done
find $srcs -name "*.java" | while read f; do cygpath -m "$f"; done > $B/$tag/sources.lst
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
"$J/javac" -J-Xmx1g -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP" -d "$(cygpath -w $out)" @"$(cygpath -w $B/$tag/sources.lst)" > $B/$tag/javac.log 2>&1
rc=$?; echo "compile $tag rc=$rc files=$(wc -l < $B/$tag/sources.lst) errors=$(grep -c ': error:' $B/$tag/javac.log)"; exit $rc
