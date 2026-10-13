#!/bin/sh
# usage: showcase.sh [regex]   Re-runs showcase matrix cells (Synexia #10916 runner) against this branch's jars.
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
RX=${1:-CompositeTable}
NEB=$B/showcase-jars; rm -rf $NEB; mkdir -p $NEB
cp /c/m3-work-c/showcase-build/nebula-jars/*.jar $NEB/ && cp -r /c/m3-work-c/showcase-build/nebula-jars/deps $NEB/deps
for w in compositetable xviewer ctree ctreecombo tablecombo calendarcombo; do
  sh $B/scripts/compile.sh $w showcase $N || exit 2
done
"$J/jar" cf $NEB/org.eclipse.nebula.widgets.compositetable.jar -C "$(cygpath -w $B/compositetable/showcase/classes)" .
"$J/jar" cf $NEB/org.eclipse.nebula.widgets.xviewer.jar -C "$(cygpath -w $B/xviewer/showcase/classes)" .
"$J/jar" cf $NEB/org.eclipse.nebula.widgets.ctree.jar -C "$(cygpath -w $B/ctree/showcase/classes)" .
"$J/jar" cf $NEB/org.eclipse.nebula.widgets.ctreecombo.jar -C "$(cygpath -w $B/ctreecombo/showcase/classes)" .
"$J/jar" cf $NEB/org.eclipse.nebula.widgets.tablecombo.jar -C "$(cygpath -w $B/tablecombo/showcase/classes)" .
"$J/jar" cf $NEB/org.eclipse.nebula.widgets.calendarcombo.jar -C "$(cygpath -w $B/calendarcombo/showcase/classes)" .
SWT=$(jar_of org.eclipse.swt.win32.win32.x86_64 3.135.0)
cd $B/showcase
MATRIX_OUT=$B/showcase-matrix JAVA_HOME="/c/Program Files/Java/jdk-21" NEBULA_COMMIT=$(git -C $N rev-parse HEAD) \
  sh examples/org.eclipse.swt.examples/superset-matrix/run-matrix.sh "$SWT" "$NEB" pr150-tree-table "$RX"
echo "showcase rc=$?"
