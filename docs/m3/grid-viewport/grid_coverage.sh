#!/usr/bin/env bash
# JaCoCo line+branch coverage of the Grid viewport classes over the Grid suite (hidden desktop).
# Pattern: K:/m3-work/logs/tree_coverage.sh. Usage: grid_coverage.sh <mode>
MODE=${1:-viewport}
OUT=K:/m3-work/agents/nebula-grid/out
M=C:/Users/hsoliwal/.m2/repository/org/jacoco
AGENT=$M/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
CLI=$M/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
COV=K:/m3-work/runs/coverage/grid; mkdir -p $COV
INC='org.eclipse.nebula.widgets.grid.GridViewport*:org.eclipse.nebula.widgets.grid.GridRow*:org.eclipse.nebula.widgets.grid.GridSelectionList*:org.eclipse.nebula.widgets.grid.GridCellSelectionList*:org.eclipse.nebula.widgets.grid.GridNative*:org.eclipse.nebula.widgets.grid.GridGC*:org.eclipse.nebula.widgets.grid.GridPaintDAG*:org.eclipse.nebula.widgets.grid.GridVirtualItemList*:org.eclipse.nebula.widgets.grid.GridModelJobs*:org.eclipse.nebula.widgets.grid.GridLazyIntPriorityFrontier*:org.eclipse.nebula.widgets.grid.internal.UpstreamFixes*:org.eclipse.nebula.widgets.grid.internal.WidgetMode:org.eclipse.nebula.widgets.grid.model.*'
SUITE=$(grep '^SUITE=' $OUT/ui_batch53.sh | sed 's/^SUITE="//; s/"$//')
bash $OUT/run_hidden.sh 1800 $COV/run-$MODE.log "-javaagent:$AGENT=destfile=$COV/grid-$MODE.exec,includes=$INC" -Xmx2g -Dgrid.acceptance.roots=1000 -Dswt.widget.mode.Grid=$MODE -- $SUITE GridArchivedBugs_Test
"C:/Program Files/Java/jdk-21/bin/java" -jar "$CLI" report "$COV/grid-$MODE.exec" --classfiles "$OUT/grid-classes" --sourcefiles "K:/m3-work/agents/nebula-grid/wt/widgets/grid/org.eclipse.nebula.widgets.grid/src" --csv "$COV/coverage-$MODE.csv" --html "$COV/html-$MODE" > $COV/report-$MODE.log 2>&1
grep -E "^Tests run|^OK" $COV/run-$MODE.log
awk -F, 'NR>1 && ($2 ~ /grid$/ || $2 ~ /grid.internal$/ || $2 ~ /grid.model$/) && ($3 ~ /^GridViewport|^GridRow|^GridSelectionList|^GridCellSelectionList|^GridNative|^GridGC|^GridPaintDAG$|^GridVirtualItemList$|^GridModelJobs$|^GridLazyIntPriorityFrontier$|^UpstreamFixes$|^WidgetMode/ || $2 ~ /grid.model$/) {ml+=$8; cl+=$9; mb+=$6; cb+=$7; n++} END {printf "viewport classes: %d, line %.2f%% (%d/%d), branch %.2f%% (%d/%d)\n", n, 100*cl/(ml+cl), cl, ml+cl, 100*cb/(mb+cb), cb, mb+cb}' $COV/coverage-$MODE.csv
