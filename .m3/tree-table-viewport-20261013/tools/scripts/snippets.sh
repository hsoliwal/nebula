#!/bin/sh
# usage: snippets.sh <widget> <tag> <src-dir-or-empty> <MainClass...>
# Smoke-runs each example/snippet main for 10 s (UI runs are wrapped in timeout); one JVM at a time.
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
w=$1; tag=$2; src=$3; shift 3
SD=$B/$w/snippets-$tag; rm -rf $SD; mkdir -p $SD
if [ -n "$src" ]; then
  find $src -name '*.java' | while read f; do cygpath -m "$f"; done > $B/$w/snippets-$tag.lst
  (cd $src && find . -type f ! -name '*.java' -exec sh -c 'mkdir -p "$0/$(dirname "$1")" && cp "$1" "$0/$1"' $SD {} \;)
  "$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$w/$tag/classes)" -d "$(cygpath -w $SD)" @"$(cygpath -w $B/$w/snippets-$tag.lst)" > $B/$w/snippets-$tag-javac.log 2>&1 || { echo "snippet javac failed"; grep -A2 ': error:' $B/$w/snippets-$tag-javac.log | head -20; }
fi
for m in "$@"; do
  sh $B/scripts/gate.sh > /dev/null || { echo "LOWMEM $m"; continue; }
  log=$B/$w/snippet-$tag-$(echo $m | sed 's/.*\.//').log
  timeout 10 "$J/java" -Xmx512m -cp "$(cygpath -w $SD);$(cygpath -w $B/$w/$tag/classes);$CP" $m > $log 2>&1
  rc=$?
  ex=$(grep -c -E 'Exception|Error' $log)
  if [ $rc -eq 124 ] && [ $ex -eq 0 ]; then v=ALIVE_AT_TIMEOUT; elif [ $rc -eq 0 ] && [ $ex -eq 0 ]; then v=EXITED_OK; else v="FAILED(rc=$rc,exceptions=$ex)"; fi
  echo -e "$w\t$tag\t$m\t$v"
done
