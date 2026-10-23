#!/usr/bin/env bash
# Runs the Grid example snippets on the hidden desktop in both modes; screenshots and logs under $GRID_OUT/examples.
OUT=K:/m3-work/agents/nebula-grid/out
E=${GRID_OUT:?set GRID_OUT to the output location}/examples
SWT=C:/Users/hsoliwal/.m2/repository/org/eclipse/platform/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
CP="$GRID_OUT/examples-classes;$OUT/grid-classes;$SWT"
mkdir -p $E
for s in GridSnippetMillionTreeRows GridSnippetCellSelection200k GridSnippetVariableHeightScroll; do
  for m in viewport original; do
    bash C:/m3-superset/tools/hidden_java.sh 900 $E/$s-$m.log -Xmx2g -Dswt.widget.mode.Grid=$m \
      -Dnebula.snippet.screenshot=$E/$s-$m.png -cp "$CP" org.eclipse.nebula.snippets.grid.$s
  done
done
