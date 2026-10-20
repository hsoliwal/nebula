#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Every hidden-desktop run of the custom-widgets track in one ui_lock ticket:
#   bash C:/m3-superset/tools/ui_lock.sh custom-widgets bash ui_batch.sh [steps...]
# Steps (default: all): m3 shots gallery tablecombo compositetable oracle. Logs and evidence under S:/m3-scratch/custom-widgets.
HERE=$(cd "$(dirname "$0")" && pwd)
S=S:/m3-scratch/custom-widgets
L=$S/logs; R=$S/recordings; SHOT=$S/screenshots
mkdir -p "$L" "$R" "$SHOT"
STEPS=${*:-m3 bench shots gallery tablecombo compositetable oracle}
summary() { grep -hE '^(OK|Tests run|FAILURES)|^gallery-oracle' "$1" 2>/dev/null | sed "s#^#$(basename "$1" .log): #"; }
differential() { # <widget dir> <runner> <candidate classes> <property prefix> <test>
  local w=$1 run=$2 cls=$3 p=$4 t=$5
  bash "$HERE/$w/$run" 900 "$L/$w-record.log" stock-classes "-D$p.recording=$R/$w" -- "$t"; summary "$L/$w-record.log"
  for mode in viewport original; do
    bash "$HERE/$w/$run" 900 "$L/$w-$mode.log" "$cls" "-D$p.recording=$R/$w" "-Dswt.widget.mode=$mode" -- "$t"; summary "$L/$w-$mode.log"
  done
}
for step in $STEPS; do
  case $step in
  m3)
    bash "$HERE/coverage_m3.sh" M3BusyLabelTest M3CollapsiblePaneTest M3ColorButtonTest M3DatePickerTest M3ErrorPaneTest M3FindBarTest M3GradientChooserTest M3HeatMapTest M3HyperlinkTest M3ImageViewTest M3InfoPopupTest M3JFaceBindingsTest M3MonthViewTest M3MultiSplitPaneTest M3ResidentPagesTest M3StackLayoutDifferentialTest M3StackLayoutTest M3StatusBarTest M3TaskPaneTest M3ToastsTest ;;
  shots)
    for c in M3BusyLabelSnippet M3CollapsiblePaneSnippet M3ColorButtonSnippet M3DatePickerSnippet M3ErrorPaneSnippet M3FindBarSnippet M3GradientChooserSnippet M3HeatMapSnippet M3HyperlinkSnippet M3ImageViewSnippet M3InfoPopupSnippet M3MonthViewSnippet M3MultiSplitPaneSnippet M3StackLayoutSnippet M3StatusBarSnippet M3TaskPaneSnippet M3ToastsSnippet; do
      bash "$HERE/run_m3.sh" 180 "$L/shot-$c.log" viewport - "-Dm3.snippet.screenshot=$SHOT/$c.png" "main:org.eclipse.nebula.widgets.m3.snippets.$c"
    done
    bash "$HERE/gallery/run_gallery.sh" 300 "$L/shot-gallery.log" gallery-classes "-Dm3.snippet.screenshot=$SHOT/GalleryMillionSnippet.png" -- main:org.eclipse.nebula.snippets.gallery.GalleryMillionSnippet
    bash "$HERE/tablecombo/run_tablecombo.sh" 300 "$L/shot-tablecombo.log" tablecombo-classes "-Dm3.snippet.screenshot=$SHOT/TableComboMillionSnippet.png" -- main:org.eclipse.nebula.snippets.tablecombo.TableComboMillionSnippet
    bash "$HERE/compositetable/run_compositetable.sh" 300 "$L/shot-compositetable.log" compositetable-classes "-Dm3.snippet.screenshot=$SHOT/CompositeTableMillionSnippet.png" -- main:org.eclipse.nebula.snippets.compositetable.CompositeTableMillionSnippet
    ls -la "$SHOT" ;;
  gallery)
    differential gallery run_gallery.sh gallery-classes nebula.gallery GalleryViewportDifferentialTest ;;
  tablecombo)
    differential tablecombo run_tablecombo.sh tablecombo-classes nebula.tablecombo TableComboViewportDifferentialTest ;;
  compositetable)
    differential compositetable run_compositetable.sh compositetable-classes nebula.compositetable CompositeTableViewportDifferentialTest ;;
  bench)
    bash "$HERE/run_m3.sh" 900 "$L/bench-m3.log" viewport - -Dm3.bench=true M3OptimizationBench; summary "$L/bench-m3.log"; grep -h "^bench" "$L/bench-m3.log"
    for cls in stock-classes gallery-classes; do
      bash "$HERE/gallery/run_gallery.sh" 900 "$L/bench-gallery-$cls.log" "$cls" -Dnebula.gallery.bench=true -Dnebula.gallery.bench.counts=10000,100000 -- GallerySelectionBench
      grep -h "^bench" "$L/bench-gallery-$cls.log"
    done ;;
  oracle)
    for cls in stock-classes gallery-classes; do
      bash "$HERE/gallery/run_gallery.sh" 900 "$L/gallery-oracle-$cls.log" "$cls" -Dnebula.gallery.oracle=true -Dnebula.gallery.oracle.counts=10000,100000 -- GalleryScaleOracleTest
      summary "$L/gallery-oracle-$cls.log"
    done ;;
  esac
done
