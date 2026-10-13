#!/bin/sh
# usage: junit.sh <widget> <tag> <test-src-dir> <TestClass...>   (compiles tests against <tag> classes; runs JUnitCore)
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
w=$1; tag=$2; src=$3; shift 3
TD=$B/$w/test-$tag; rm -rf $TD; mkdir -p $TD
find $src -name '*.java' | while read f; do cygpath -m "$f"; done > $B/$w/test-$tag.lst
sh $B/scripts/gate.sh > /dev/null || { echo LOWMEM; exit 3; }
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$w/$tag/classes)" -d "$(cygpath -w $TD)" @"$(cygpath -w $B/$w/test-$tag.lst)" > $B/$w/test-$tag-javac.log 2>&1 || { echo "test javac failed"; grep -A2 ': error:' $B/$w/test-$tag-javac.log | head -30; exit 2; }
timeout 900 "$J/java" -Xmx2g -cp "$(cygpath -w $TD);$(cygpath -w $B/$w/$tag/classes);$CP" org.junit.runner.JUnitCore "$@"
echo "junit rc=$?"
