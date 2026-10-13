#!/bin/sh
# usage: bench.sh <tag> <main-class> <harness-src-dir> <xmx> "<jvm props>" <args...>
. /c/m3-work-c/viz-build/scripts/env.sh
tag=$1; main=$2; hs=$3; xmx=$4; props=$5; shift 5
HD=$B/$tag/harness; mkdir -p $HD
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$tag/classes)" -d "$(cygpath -w $HD)" "$(cygpath -w $hs/$main.java)" || exit 2
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
timeout 1800 "$J/java" -Xmx$xmx $props -cp "$(cygpath -w $HD);$(cygpath -w $B/$tag/classes);$CP" $main "$@"
echo "bench rc=$?"
