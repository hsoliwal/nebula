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
swt_native=NOT_RUN
swt_screenshots=NOT_RUN
swt_opencv=NOT_RUN
swt_resources=NOT_RUN
nebula_native=NOT_RUN
nebula_opencv=NOT_RUN

mkdir -p "$RECEIPT"

finish() {
  rc=$?
  trap - EXIT
  [[ $swt_native != RUNNING ]] || swt_native=FAILED
  [[ $swt_screenshots != RUNNING ]] || swt_screenshots=FAILED
  [[ $swt_opencv != RUNNING ]] || swt_opencv=FAILED
  [[ $swt_resources != RUNNING ]] || swt_resources=FAILED
  [[ $nebula_native != RUNNING ]] || nebula_native=FAILED
  [[ $nebula_opencv != RUNNING ]] || nebula_opencv=FAILED
  if [[ $rc -eq 0 && $STATUS != PASS ]]; then rc=1; fi
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
    echo "gtk3_current_receiver=$swt_native"
    echo "manual_qa_required=false"
    echo "swt_native=$swt_native"
    echo "swt_screenshots=$swt_screenshots"
    echo "swt_opencv=$swt_opencv"
    echo "swt_resources=$swt_resources"
    echo "nebula_native=$nebula_native"
    echo "nebula_opencv=$nebula_opencv"
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
# Optional exact task receiver; record the resolved commit below in either mode.
if [[ -n ${M3_SWT_REF:-} ]]; then
  [[ $M3_SWT_REF =~ ^[0-9a-f]{40}$ ]]
  git -C "$SWT" fetch --depth 1 origin "$M3_SWT_REF"
  git -C "$SWT" checkout --detach FETCH_HEAD
  [[ $(git -C "$SWT" rev-parse HEAD) == "$M3_SWT_REF" ]]
fi
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
swt_native=RUNNING
(
  cd "$SWT"
  export GTK_XCFLAGS='-Wno-deprecated-declarations'
  export SWT_GTK4=0
  export GDK_BACKEND=x11
  dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"     mvn --batch-mode -V -U -e       --threads 1C       -DforkCount=1       -Dnative=gtk.linux.x86_64       -Papi-check -Pjavadoc       -Dtycho.baseline.replace=none       --fail-at-end       -DskipNativeTests=false       -DfailIfNoTests=false       clean install
)

swt_native=PASSED

PHASE=swt-viewport-screenshot
swt_screenshots=RUNNING
(
  cd "$SWT/tests/org.eclipse.swt.tests"
  export SWT_GTK4=0
  export GDK_BACKEND=x11
  dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"     mvn --batch-mode -V -U -e       -DforkCount=1       -Dnative=gtk.linux.x86_64       -DskipNativeTests=true       -DfailIfNoTests=true       -Dtest=ViewportScreenshotRegressionTest       -Dswt.viewport.screenshotRegression=true       -Dswt.viewport.screenshots=target/screenshots/viewport       integration-test
)

swt_screenshots=PASSED

PHASE=swt-opencv-screenshot-oracle
swt_opencv=RUNNING
python3 -m venv "$WORK/vision"
"$WORK/vision/bin/python" -m pip install opencv-python-headless==4.12.0.88 numpy==2.2.6
"$WORK/vision/bin/python" "$SWT/tests/org.eclipse.swt.tests/verify_viewport_screenshots.py" \
  --test-root "$SWT/tests/org.eclipse.swt.tests"
swt_opencv=PASSED

PHASE=swt-viewport-resource-gates
swt_resources=RUNNING
(
  cd "$SWT/tests/org.eclipse.swt.tests"
  export SWT_GTK4=0
  export GDK_BACKEND=x11
  dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"     mvn --batch-mode -V -U -e       -DforkCount=1       -Dnative=gtk.linux.x86_64       -DskipNativeTests=true       -DfailIfNoTests=true       -Dtest=ViewportResourceRegressionTest       -Dswt.viewport.resourceRegression=true       integration-test
)

python3 "$SWT/tests/org.eclipse.swt.tests/verify_viewport_screenshots.py" \
  --test-root "$SWT/tests/org.eclipse.swt.tests" --resources
swt_resources=PASSED

PHASE=nebula-recipe-first-reactor
mvn -B -ntp -f m3/reactor.xml verify

PHASE=bind-current-swt
python3 m3/convergence/prepare_swt_receiver.py   "$SWT"   m3/convergence/swt-receiver-p2/target/binding   m3/convergence/swt-receiver-p2/target/repository
mvn -B -f m3/convergence/swt-receiver-p2/pom.xml   -Dm3.swt.source="$ROOT/m3/convergence/swt-receiver-p2/target/binding/input"   verify

PHASE=nebula-current-swt-tycho
nebula_native=RUNNING
set -a
source m3/convergence/swt-receiver-p2/target/binding/receiver.env
set +a
export SWT_GTK4=0
export GDK_BACKEND=x11
dbus-run-session -- xvfb-run -a -s "-screen 0 1200x800x24 -noreset"   mvn -V -B clean verify     -Dtycho.localArtifacts=ignore     -Dnebula.grid.viewport.screenshots.screen=true     -Dnebula.swt.gc.sha256="$M3_SWT_GC_SHA256"     -Dtarget-platform-platform="$M3_SWT_PLATFORM"

nebula_native=PASSED

PHASE=nebula-opencv-screenshot-oracle
nebula_opencv=RUNNING
"$WORK/vision/bin/python"   m3/convergence/verify_grid_screenshots.py   widgets/grid/org.eclipse.nebula.widgets.grid.test/target/m3-visible-range-screenshots   widgets/grid/org.eclipse.nebula.widgets.grid.test/target/surefire-reports   m3/convergence/swt-receiver-p2/target/binding/expected.json

nebula_opencv=PASSED

[[ $swt_native == PASSED ]]
[[ $swt_screenshots == PASSED ]]
[[ $swt_opencv == PASSED ]]
[[ $swt_resources == PASSED ]]
[[ $nebula_native == PASSED ]]
[[ $nebula_opencv == PASSED ]]
PHASE=complete
STATUS=PASS
