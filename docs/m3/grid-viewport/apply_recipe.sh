#!/usr/bin/env bash
# Apply the Nebula Grid viewport hook recipe twice (second pass must be UNCHANGED).
C=K:/m3-work/syn-recipes/synexia-openrewrite-recipes/crates/m3-viewport-catalogue-recipes
WT=K:/m3-work/agents/nebula-grid/wt
G=$WT/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid
export JAVA_HOME="C:/Program Files/Java/jdk-21"
python -I $WT/docs/m3/grid-viewport/recipe/gen_grid_viewport_yml.py
CPR="$WT/docs/m3/grid-viewport/recipe;$C/target/classes;$(cat $C/target/cp.txt)"
for pass in 1 2; do
  echo "recipe pass $pass:"
  "$JAVA_HOME/bin/java" -Xmx1g -cp "$CPR" com.synexia.openrewrite.viewport.M3PerFileApply com.synexia.openrewrite.viewport.NebulaGridViewport "$G/Grid.java" "$G/GridItem.java" 2>&1 | grep -E '^(CHANGED|UNCHANGED|ERROR)|Exception' | head -5
done
