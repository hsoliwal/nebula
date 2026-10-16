#!/usr/bin/env bash
# Build a mutant Grid (the 'complete' step-back of GridViewport.rowRange removed) to prove the differential detects it.
export MSYS_NO_PATHCONV=1
OUT=K:/m3-work/agents/nebula-grid/out
WT=K:/m3-work/agents/nebula-grid/wt
rm -rf $OUT/mutant-src && mkdir -p $OUT/mutant-src && cp -r $WT/widgets/grid/org.eclipse.nebula.widgets.grid/src/. $OUT/mutant-src/
F=$OUT/mutant-src/org/eclipse/nebula/widgets/grid/GridViewport.java
sed -i 's/if (complete \&\& taken > 1 \&\& extent > availableHeight + 1L) {/if (false \&\& complete \&\& taken > 1 \&\& extent > availableHeight + 1L) {/' $F
grep -c 'if (false && complete' $F
SWT=C:/m3-work-c/integ-build/official/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
D=C:/m3-work-c/showcase-build/platform-deps
rm -rf $OUT/grid-mutant; mkdir -p $OUT/grid-mutant
find $OUT/mutant-src -name '*.java' > $OUT/grid-mutant.src
"C:/Program Files/Java/jdk-21/bin/javac" -J-Xmx768m --release 17 -encoding UTF-8 -nowarn -proc:none -cp "$SWT;$D/org.eclipse.jface-3.40.0.jar;$D/org.eclipse.core.commands-3.13.0.jar;$D/org.eclipse.equinox.common-3.21.0.jar" -d $OUT/grid-mutant @$OUT/grid-mutant.src 2>&1 | grep -c error
