#!/bin/sh
# usage: junit.sh <tag> <test-src-dir> <name> "<jvm props>" <TestClass...>
# compiles tests against $B/<tag>/classes into $B/<tag>/test-<name>, runs JUnitCore (timeout 1200 s, -Xmx2g)
. /c/m3-work-c/viz-build/scripts/env.sh
tag=$1; src=$2; name=$3; props=$4; shift 4
TD=$B/$tag/test-$name; rm -rf $TD; mkdir -p $TD
find $src -name '*.java' | while read f; do cygpath -m "$f"; done > $B/$tag/test-$name.lst
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$tag/classes)" -d "$(cygpath -w $TD)" @"$(cygpath -w $B/$tag/test-$name.lst)" > $B/$tag/test-$name-javac.log 2>&1 || { echo "test javac failed"; grep -A2 ': error:' $B/$tag/test-$name-javac.log | head -30; exit 2; }
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
timeout 1200 "$J/java" -Xmx2g $props -cp "$(cygpath -w $TD);$(cygpath -w $B/$tag/classes);$CP" org.junit.runner.JUnitCore "$@"
echo "junit rc=$?"
