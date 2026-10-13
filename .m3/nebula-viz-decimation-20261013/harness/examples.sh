#!/bin/sh
# usage: examples.sh <tag> <root>   runs xygraph examples + oscilloscope snippets for 4 s each via ExampleRunner
. /c/m3-work-c/viz-build/scripts/env.sh
tag=$1; root=$2
ED=$B/$tag/examples; rm -rf $ED; mkdir -p $ED
H=/c/m3-work-c/nebula-viz/.m3/nebula-viz-decimation-20261013/harness
SN=$root/widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.snippets/src
( find $SN -name '*.java'; echo $H/ExampleRunner.java ) | while read f; do cygpath -m "$f"; done > $ED.lst
sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$tag/classes)" -d "$(cygpath -w $ED)" @"$(cygpath -w $ED.lst)" > $ED-javac.log 2>&1 || { echo "examples javac failed"; grep -A2 ': error:' $ED-javac.log | head; exit 2; }
mains="$(cd $root/widgets/visualization/org.eclipse.nebula.visualization.xygraph/examples && grep -rl 'static void main' --include=*.java . | sed 's#^\./##;s#\.java$##;s#/#.#g')
$(cd $SN && grep -rl 'static void main' --include=*.java . | sed 's#^\./##;s#\.java$##;s#/#.#g')"
for m in $mains; do
  sh $B/scripts/memwait.sh || { echo LOWMEM; exit 3; }
  (cd /c/m3-work-c/viz-logs && timeout 60 "$J/java" -Xmx1g -XX:ErrorFile=C:/m3-work-c/viz-logs/hs_err_%p.log -cp "$(cygpath -w $ED);$(cygpath -w $B/$tag/classes);$CP" ExampleRunner $tag 4000 $m 2>&1 | grep -E "^$tag	" || echo "$tag	$m	NO_RESULT")
done
