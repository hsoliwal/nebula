#!/bin/sh
# usage: diff.sh <widget> <FixtureSimpleName> <visible|hidden> [post-tag]
# One JVM per probe pass (pre, pre-again, post); pre passes are cached by probe-source hash.
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
w=$1; fx=$2; mode=$3; post=${4:-post}
H=$N/.m3/tree-table-viewport-20261013/harness
PD=$B/$w/probe-classes; MD=$B/harness-main
mkdir -p $PD $MD
find $H/probe $H/fixtures/$w -name '*.java' | while read f; do cygpath -m "$f"; done > $B/$w/probe-sources.lst
# always probe the current sources: recompile the post image first (cheap, deterministic)
[ "$post" = post ] && { sh $B/scripts/compile.sh $w post $N || exit 2; }
key=$(cat $(find $H/probe $H/fixtures/$w $H/main -name '*.java' | sort) $N/.m3/tree-table-viewport-20261013/API_RAW.tsv | sha256sum | cut -c1-12)
sh $B/scripts/gate.sh > /dev/null || { echo LOWMEM; exit 3; }
rm -rf $PD/*
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP;$(cygpath -w $B/$w/pre/classes)" -d "$(cygpath -w $PD)" @"$(cygpath -w $B/$w/probe-sources.lst)" > $B/$w/probe-javac.log 2>&1 || { echo "probe javac failed"; head -30 $B/$w/probe-javac.log; exit 2; }
"$J/javac" -J-Xmx512m -nowarn -encoding UTF-8 --release 21 -proc:none -cp "$CP" -d "$(cygpath -w $MD)" $(cygpath -w $H/main/org/eclipse/nebula/m3/tt/TtDiff.java) || exit 2
api=$(cygpath -w $N/.m3/tree-table-viewport-20261013/API_RAW.tsv)
run() { # $1 classes-tag $2 out
  sh $B/scripts/gate.sh > /dev/null || { echo LOWMEM; exit 3; }
  timeout 3000 "$J/java" -Xmx1g -cp "$(cygpath -w $MD);$CP" org.eclipse.nebula.m3.tt.TtDiff probe "$api" $w org.eclipse.nebula.m3.tt.$fx "$(cygpath -w $PD)" "$(cygpath -w $B/$w/$1/classes)" "$(cygpath -w $2)" $mode
  echo "pass $1 rc=$? -> $2"
}
P1=$B/$w/pass-pre-$mode-$key.tsv; P2=$B/$w/pass-pre2-$mode-$key.tsv; P3=$B/$w/pass-$post-$mode-$key.tsv
[ -s $P1 ] || run pre $P1
[ -s $P2 ] || run pre $P2
run $post $P3
out=$B/$w/diff-$mode-$post.tsv
"$J/java" -Xmx512m -cp "$(cygpath -w $MD);$CP" org.eclipse.nebula.m3.tt.TtDiff compare "$api" $w "$(cygpath -w $P1)" "$(cygpath -w $P2)" "$(cygpath -w $P3)" "$(cygpath -w $out)"
echo "diff rc=$? out=$out key=$key"
