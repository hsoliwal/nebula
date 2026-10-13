#!/bin/sh
# usage: bench.sh <widget> <BenchClass> <image-tag> <xmx> <args...>  (one JVM per image; memory-gated)
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
w=$1; cls=$2; tag=$3; xmx=$4; shift 4
H=$N/.m3/tree-table-viewport-20261013/harness
BD=$B/$w/bench-classes; mkdir -p $BD
find $H/bench/common $H/bench/$w -name '*.java' | while read f; do cygpath -m "$f"; done > $B/$w/bench-sources.lst
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$w/pre/classes)" -d "$(cygpath -w $BD)" @"$(cygpath -w $B/$w/bench-sources.lst)" > $B/$w/bench-javac.log 2>&1 || { echo "bench javac failed"; head -20 $B/$w/bench-javac.log; exit 2; }
sh $B/scripts/gate.sh || { echo LOWMEM; exit 3; }
out=$B/$w/bench-$tag.tsv
timeout 1800 "$J/java" -Xmx$xmx -cp "$(cygpath -w $BD);$(cygpath -w $B/$w/$tag/classes);$CP" org.eclipse.nebula.m3.tt.$cls $tag "$@" "$(cygpath -w $out)"
echo "bench rc=$? out=$out"
