#!/bin/sh
# Serial (one JVM at a time) evidence pipeline for the tree/table leaf. Usage: run_all.sh <steps...>
# steps: compile diffs xviewer-post junit snippets bench
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
S=$B/scripts
L=$B/logs; mkdir -p $L
W="xviewer ctree ctreecombo tablecombo calendarcombo compositetable"
PRE=$B/pre-src/widgets
POST=$N/widgets
for step in "$@"; do
case $step in
compile)
  for w in $W; do timeout 1800 sh $S/compile.sh $w post $N; done > $L/compile.log 2>&1 ;;
diffs)
  timeout 7000 sh $S/diff.sh ctree CTreeFixture visible > $L/diff-ctree.log 2>&1
  timeout 7000 sh $S/diff.sh ctreecombo CTreeComboFixture visible > $L/diff-ctreecombo.log 2>&1
  timeout 7000 sh $S/diff.sh tablecombo TableComboFixture visible > $L/diff-tablecombo.log 2>&1
  timeout 7000 sh $S/diff.sh calendarcombo CalendarComboFixture visible > $L/diff-calendarcombo.log 2>&1
  timeout 7000 sh $S/diff.sh compositetable CompositeTableFixture visible > $L/diff-compositetable.log 2>&1 ;;
xviewer-post)
  timeout 7000 sh $S/diff.sh xviewer XViewerFixture visible > $L/diff-xviewer.log 2>&1 ;;
junit)
  T=$B/junit-src; rm -rf $T; mkdir -p $T/xv-pre/org/eclipse/nebula/widgets/xviewer/tests $T/xv-post $T/ct-post $T/cmt-pre $T/cmt-post
  XT=org.eclipse.nebula.widgets.xviewer.tests/src
  cp $PRE/xviewer/$XT/org/eclipse/nebula/widgets/xviewer/tests/DummyTest.java $T/xv-pre/org/eclipse/nebula/widgets/xviewer/tests/
  cp -r $POST/xviewer/$XT/. $T/xv-post/
  cp -r $POST/ctree/org.eclipse.nebula.widgets.ctree.tests/src/. $T/ct-post/
  CTS=org.eclipse.nebula.widgets.compositetable.tests/src
  cp -r $PRE/compositetable/$CTS/. $T/cmt-pre/
  cp -r $POST/compositetable/$CTS/. $T/cmt-post/
  CMT="org.eclipse.nebula.widgets.compositetable.CompositeTableTest org.eclipse.nebula.widgets.compositetable.CompositeTable_267316Test org.eclipse.nebula.widgets.compositetable.NativeHeaderTest org.eclipse.nebula.widgets.compositetable.day.CalendarControlTestSuite"
  sh $S/junit.sh xviewer pre $T/xv-pre org.eclipse.nebula.widgets.xviewer.tests.DummyTest > $L/junit-xviewer-pre.log 2>&1
  sh $S/junit.sh xviewer post $T/xv-post org.eclipse.nebula.widgets.xviewer.tests.DummyTest org.eclipse.nebula.widgets.xviewer.tests.XViewerViewport_Test > $L/junit-xviewer-post.log 2>&1
  sh $S/junit.sh ctree post $T/ct-post org.eclipse.nebula.widgets.ctree.CTreeViewport_Test > $L/junit-ctree-post.log 2>&1
  sh $S/junit.sh compositetable pre $T/cmt-pre $CMT > $L/junit-compositetable-pre.log 2>&1
  sh $S/junit.sh compositetable post $T/cmt-post $CMT org.eclipse.nebula.widgets.compositetable.CompositeTableViewport_Test > $L/junit-compositetable-post.log 2>&1 ;;
snippets)
  for tag in pre post; do
    R=$PRE; [ $tag = post ] && R=$POST
    sh $S/snippets.sh ctreecombo $tag $R/ctreecombo/org.eclipse.nebula.widgets.ctreecombo.snippets/src org.eclipse.nebula.widgets.ctreecombo.snippets.CTreeComboSnippet
    sh $S/snippets.sh tablecombo $tag $R/tablecombo/org.eclipse.nebula.widgets.tablecombo.snippets/src org.eclipse.nebula.widgets.tablecombo.CheckTableComboSnippet org.eclipse.nebula.widgets.tablecombo.TableComboSnippet org.eclipse.nebula.widgets.tablecombo.TableComboViewerSnippet1
    sh $S/snippets.sh xviewer $tag $R/xviewer/org.eclipse.nebula.widgets.xviewer.example/src org.eclipse.nebula.widgets.xviewer.example.MyXViewerTest org.eclipse.nebula.widgets.xviewer.example.styledExample.XViewerStyledStringLableProviderTest
    sh $S/snippets.sh calendarcombo $tag "" org.eclipse.nebula.widgets.calendarcombo.CalendarComboTester org.eclipse.nebula.widgets.calendarcombo.Tester
  done > $L/snippets.tsv 2>&1 ;;
bench)
  for tag in pre post; do
    sh $S/bench.sh xviewer XViewerBench $tag 2g 100000 9
    sh $S/bench.sh ctree CTreeBench $tag 3g 100000 9
    sh $S/bench.sh tablecombo TableComboBench $tag 2g 100000 200
    sh $S/bench.sh ctreecombo CTreeComboBench $tag 2g 10000 9 200
    sh $S/bench.sh calendarcombo CalendarComboBench $tag 1g 100
    sh $S/bench.sh compositetable CompositeTableBench $tag 1g 1000000
  done > $L/bench.log 2>&1 ;;
esac
echo "step $step done $(date +%H:%M:%S)" >> $L/steps.log
done
