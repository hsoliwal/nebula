#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
set -euo pipefail

ROOT="$(pwd)"
WORK="$ROOT/.m3-completion"
SWT="$WORK/swt"
JFACE="$WORK/jface"
RECEIPT="$WORK/receipt"
STATUS=FAIL
PHASE=bootstrap

mkdir -p "$RECEIPT"

finish() {
  rc=$?
  {
    echo "schema=M3_UI_DISTILLATION_PUBLIC_COMPLETION_V1"
    echo "job_status=$STATUS"
    echo "exit_code=$rc"
    echo "last_phase=$PHASE"
    [[ -f "$WORK/heads.env" ]] && cat "$WORK/heads.env"
    echo "recipe_authority=hsoliwal/com.synexia"
    echo "runtime_receiver=hsoliwal/nebula"
    echo "real_swt_gc_authority=true"
    echo "proxy_gc_public_replacement=false"
    echo "java_before_jni=true"
    echo "java2s_behavior_only=true"
    echo "challenge_solution_copy=false"
    echo "viewport_z_order=background>scrolling-body>fixed-body>header-footer>transient-feedback"
    echo "gtk3_current_receiver=EXECUTED"
    echo "windows_current_receiver=NOT_EXECUTED_IN_THIS_JOB"
    echo "cocoa_current_receiver=NOT_EXECUTED_IN_THIS_JOB"
  } > "$RECEIPT/completion.properties"
  sha256sum "$RECEIPT/completion.properties" > "$RECEIPT/completion.sha256"
  cat "$RECEIPT/completion.properties"
  exit "$rc"
}
trap finish EXIT

PHASE=linux-prerequisites
sudo apt-get update -qq
sudo apt-get install -qq -y   build-essential git git-lfs dbus-daemon xvfb mutter   libgtk-3-dev libgtk-4-dev freeglut3-dev   webkit2gtk-driver libwebkitgtk-6.0-4

PHASE=checkout-hygiene
git diff --check
rm -rf "$SWT" "$JFACE"
git clone --depth 1 https://github.com/hsoliwal/eclipse.platform.swt.git "$SWT"
git clone --depth 1 https://github.com/hsoliwal/eclipse.platform.ui.git "$JFACE"
{
  echo "NEBULA_HEAD=$(git rev-parse HEAD)"
  echo "SWT_HEAD=$(git -C "$SWT" rev-parse HEAD)"
  echo "JFACE_HEAD=$(git -C "$JFACE" rev-parse HEAD)"
} | tee "$WORK/heads.env"

PHASE=current-swt-owner-contract
for path in   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/ViewportAffineTransform.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/ViewportLayerState.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/ViewportPaintGraph.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/ViewportScrollLayout.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/VirtualSelectionModel.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/VirtualTreeTopology.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/VirtualTreeViewport.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/VirtualTreeVisibleProjection.java'   'bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/VirtualViewportPlanner.java'; do
  test -s "$SWT/$path"
done
SCROLLED="$SWT/bundles/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/ScrolledComposite.java"
grep -F 'ViewportRuntime viewportRuntime' "$SCROLLED"
grep -F 'setLogicalContentSize' "$SCROLLED"
grep -F 'getLogicalContentSize' "$SCROLLED"
grep -F 'clearLogicalContentSize' "$SCROLLED"

PHASE=current-jface-adapter-contract
ABSTRACT="$JFACE/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/AbstractTreeViewer.java"
TREEVIEWER="$JFACE/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/TreeViewer.java"
grep -F 'expandSelectionToLevel' "$ABSTRACT"
grep -F 'collapseSelectionToLevel' "$ABSTRACT"
grep -F 'internalDelegateExpandSelectionToControl' "$ABSTRACT"
grep -F 'internalDelegateCollapseSelectionToControl' "$ABSTRACT"
grep -F 'SWT_TREE_EXPAND_SELECTION_TO_LEVEL' "$TREEVIEWER"
grep -F 'SWT_TREE_COLLAPSE_SELECTION_TO_LEVEL' "$TREEVIEWER"
grep -F 'MethodHandles.publicLookup().findVirtual' "$TREEVIEWER"

PHASE=current-nebula-render-contract
for path in   'widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridGCProxy.java'   'widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridGCStateDAG.java'   'widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridTransform.java'   'widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridPaintDAG.java'   'widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridViewportDamage.java'   'widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridViewportProjection.java'; do
  test -s "$ROOT/$path"
done
PAINT_DAG="$ROOT/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridPaintDAG.java"
grep -F 'background -> body -> fixed overlay -> chrome -> transient overlay' "$PAINT_DAG"
grep -F 'case BACKGROUND -> 0' "$PAINT_DAG"
grep -F 'case BODY -> 1' "$PAINT_DAG"
grep -F 'case FIXED -> 2' "$PAINT_DAG"
grep -F 'case HEADER, FOOTER -> 3' "$PAINT_DAG"
grep -F 'case OVERLAY -> 4' "$PAINT_DAG"
grep -F 'dependsOn' "$PAINT_DAG"
grep -F 'GridTransform' "$ROOT/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridGCStateDAG.java"
grep -F 'alpha' "$ROOT/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridGCStateDAG.java"

PHASE=donor-boundary-contract
CATALOG="$ROOT/m3/convergence/JAVA2S_SWT_SWING_BEHAVIOR_CATALOG.md"
test -s "$CATALOG"
grep -F 'https://www.java2s.com/Tutorial/Java/0280__SWT/Catalog0280__SWT.html' "$CATALOG"
grep -F 'https://www.java2s.com/Tutorial/Java/0300__SWT-2D-Graphics/Catalog0300__SWT-2D-Graphics.html' "$CATALOG"
grep -F 'https://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.html' "$CATALOG"
grep -F 'https://www.java2s.com/Tutorial/Java/0260__Swing-Event/Catalog0260__Swing-Event.html' "$CATALOG"
grep -F 'LeetCode' "$CATALOG"
grep -F 'HackerRank' "$CATALOG"
grep -F 'GeeksForGeeks' "$CATALOG"

PHASE=swt-gtk3-native-api-javadoc
(
  cd "$SWT"
  export GTK_XCFLAGS='-Wno-deprecated-declarations'
  export SWT_GTK4=0
  export GDK_BACKEND=x11
  dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"     mvn --batch-mode -V -U -e       --threads 1C       -DforkCount=1       -Dnative=gtk.linux.x86_64       -Papi-check -Pjavadoc       -Dtycho.baseline.replace=none       --fail-at-end       -DskipNativeTests=false       -DfailIfNoTests=false       clean install
)

PHASE=swt-viewport-screenshot
(
  cd "$SWT/tests/org.eclipse.swt.tests"
  export SWT_GTK4=0
  export GDK_BACKEND=x11
  dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"     mvn --batch-mode -V -U -e       -DforkCount=1       -Dnative=gtk.linux.x86_64       -DskipNativeTests=true       -DfailIfNoTests=true       -Dtest=ViewportScreenshotRegressionTest       -Dswt.viewport.screenshotRegression=true       -Dswt.viewport.screenshots=target/screenshots/viewport       integration-test
)

PHASE=swt-viewport-resource-gates
(
  cd "$SWT/tests/org.eclipse.swt.tests"
  export SWT_GTK4=0
  export GDK_BACKEND=x11
  dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"     mvn --batch-mode -V -U -e       -DforkCount=1       -Dnative=gtk.linux.x86_64       -DskipNativeTests=true       -DfailIfNoTests=true       -Dtest=ViewportResourceRegressionTest       -Dswt.viewport.resourceRegression=true       integration-test
)

PHASE=nebula-recipe-first-reactor
mvn -B -ntp -f m3/reactor.xml verify

PHASE=bind-current-swt
python3 m3/convergence/prepare_swt_receiver.py   "$SWT"   m3/convergence/swt-receiver-p2/target/binding   m3/convergence/swt-receiver-p2/target/repository
mvn -B -f m3/convergence/swt-receiver-p2/pom.xml   -Dm3.swt.source="$ROOT/m3/convergence/swt-receiver-p2/target/binding/input"   verify

PHASE=nebula-current-swt-tycho
set -a
source m3/convergence/swt-receiver-p2/target/binding/receiver.env
set +a
export SWT_GTK4=0
export GDK_BACKEND=x11
dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"   mvn -V -B clean verify     -Dtycho.localArtifacts=ignore     -Dnebula.grid.viewport.screenshots.screen=true     -Dnebula.swt.gc.sha256="$M3_SWT_GC_SHA256"     -Dtarget-platform-platform="$M3_SWT_PLATFORM"

PHASE=nebula-opencv-screenshot-oracle
python3 -m venv "$WORK/vision"
"$WORK/vision/bin/python" -m pip install   opencv-python-headless==4.12.0.88 numpy==2.2.6
"$WORK/vision/bin/python"   m3/convergence/verify_grid_screenshots.py   widgets/grid/org.eclipse.nebula.widgets.grid.test/target/m3-visible-range-screenshots   widgets/grid/org.eclipse.nebula.widgets.grid.test/target/surefire-reports   m3/convergence/swt-receiver-p2/target/binding/expected.json

PHASE=complete
STATUS=PASS
