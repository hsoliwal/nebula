#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Deterministic CPU projection: WIDGET_PLATFORM_MATRIX.tsv, one row per widget (SwingX JX components, Nebula widgets,
# WIDGET_MATRIX concepts, Eclipse stacks) and one column per platform: SWT win32, SWT gtk, SWT cocoa, Swing (M3JDK m3/),
# Initiative 31 (SWT on Swing), RWT/RAP. Cells: AVAILABLE <class> | ADAPTER_NEEDED <basis> | MISSING [plan].
# Usage: build_platform_matrix.sh <nebula worktree> <harvest cache dir> <out dir>
# Inputs next to this script: platform_map.tsv (curated SwingX, stacks, Nebula entry points), m3_names.tsv,
# rwt_rows.tsv (rows sent by the RWT column owner: widget_id, rwt_route, rwt_status, evidence); ../WIDGET_MATRIX.tsv.
set -u
export LC_ALL=C
HERE=$(cd "$(dirname "$0")" && pwd); WT=$1; CACHE=$2; OUT=$3; mkdir -p "$OUT"
M3SRC=$WT/widgets/m3/org.eclipse.nebula.widgets.m3/src/org/eclipse/nebula/widgets/m3
# 1. Nebula widget classes: public, non-abstract subclasses (transitively) of SWT controls, per bundle
find "$WT/widgets" -path '*/src/*' -name '*.java' | grep -vE '/(test|tests|example|examples|snippets?|internal|demo)/|\.tests?/|\.example/|\.snippets/|/widgets/m3/' \
  | xargs grep -HE '^public (abstract )?class [A-Za-z0-9_]+(<[^>]*>)? extends [A-Za-z0-9_.]+' 2>/dev/null \
  | sed -E 's#^.*/widgets/([^/]+)/([^/]+)/[^:]*/([A-Za-z0-9_]+)\.java:public (abstract )?class [A-Za-z0-9_]+(<[^>]*>)? extends ([A-Za-z0-9_.]+).*#\1\t\3\t\6\t\4\t\2#' \
  | awk -F'\t' -v OFS='\t' 'BEGIN { split("Composite Canvas Control Scrollable Shell Dialog SashForm ScrolledComposite Text Button Label Combo StyledText", b, " "); for (i in b) w[b[i]] = 1
      split("SComposite ToolbarComposite GanttControlParent CustomButton CDatePanel NavigationPageGraphics PrintPieceCanvas ReadOnlyStyledText ChoiceWidget CustomCombo AlphaLabel", x, " "); for (i in x) skip[x[i]] = 1 }
    { bundle[NR] = ($1 == "opal") ? "opal/" $5 : $1; cls[NR] = $2; sup[NR] = $3; abs[NR] = ($4 != ""); n = NR }
    END { for (it = 0; it < 6; it++) for (i = 1; i <= n; i++) if (sup[i] in w) w[cls[i]] = 1
      for (i = 1; i <= n; i++) if ((sup[i] in w) && !abs[i] && !(cls[i] in skip) && cls[i] !~ /^(Abstract|Base|Default|Internal|Dummy|Test|Example|Demo)/) print bundle[i], cls[i] }' \
  | sort -u > "$OUT/.nebula.tsv"
# 2. stock class lists: SWT (win32 widgets + custom), RWT (RAP), Initiative 31 prototype-swing
# RAP also ships the e4 renderers (bundles/org.eclipse.e4.ui.workbench.renderers.swt in eclipse-rap/org.eclipse.rap)
{ grep -oE '[^/]+\.java$' "$CACHE/classes/eclipse-rap__org.eclipse.rap.txt" | sed 's/\.java$//'; echo StackRenderer; } | sort -u > "$OUT/.rwt"
grep -oE '[^/]+\.java$' "$CACHE/classes/swt-initiative31__prototype-swing.txt" | sed 's/\.java$//' | sort -u > "$OUT/.i31"
ls "$M3SRC" 2>/dev/null | sed -n 's/\.java$//p' | sort -u > "$OUT/.m3done"
touch "$HERE/rwt_rows.tsv"
awk -F'\t' -v OFS='\t' '
  function simple(c) { sub(/;.*/, "", c); sub(/[+ \/|].*/, "", c); sub(/.*\./, "", c); return c }
  function custom(c) { return c ~ /^(StyledText|CTabFolder|CLabel|CCombo|SashForm|StackLayout|ScrolledComposite|ViewForm|CBanner|TableEditor|TreeEditor)$/ }
  function swtcells(kind, cls, plan,   s) {
    s = simple(cls)
    if (kind == "NA") { sw = "MISSING (not applicable)"; i3 = sw; rw = sw; return }
    if (kind == "STOCK") { sw = "AVAILABLE " cls; i3 = ((s in i31) || custom(s)) ? "AVAILABLE " s " (I31 prototype-swing)" : "ADAPTER_NEEDED I31 has no " s
      rw = (s in rwt) ? "AVAILABLE " s " (RWT)" : "ADAPTER_NEEDED RWT has no " s; return }
    if (kind == "NEBULA" || (kind == "M3" && (s in m3done))) { sw = "AVAILABLE " cls; i3 = "AVAILABLE " s " (SWT API on I31 Canvas/Composite; not run)"
      rw = (s == "Grid") ? "AVAILABLE org.eclipse.rap.nebula.widgets.grid.Grid (RAP port)" : (s ~ /^RichText/) ? "AVAILABLE org.eclipse.rap.nebula.widgets.richtext (RAP port)" : "ADAPTER_NEEDED Canvas/GC replay on RWT (RWT column owner)"; return }
    if (kind == "M3") { sw = "MISSING planned " cls; i3 = sw; rw = sw; return }
    sw = "MISSING" (plan == "" ? "" : " planned " plan); i3 = sw; rw = sw }
  function swingcell(stock, name, basis) {
    if (stock != "" && stock != "-" && stock !~ /^-/) return "AVAILABLE " stock " (JDK)"
    return "ADAPTER_NEEDED javax.swing." name " over the shared model (" basis ")" }
  function emit(id, fam, w, concept, kind, cls, swing, plan, owner, evidence,   rr) {
    swtcells(kind, cls, plan)
    rr = (id in rroute) ? rroute[id] : "-"
    if (id in rstatus) rw = rstatus[id]
    if (id in revid) evidence = evidence "; RWT: " revid[id]
    print id, fam, w, concept, sw, sw, sw, swing, i3, rw, (cls == "" ? "-" : cls), "-", rr, plan, owner, evidence }
  FILENAME ~ /\.rwt$/ { rwt[$1] = 1; next }
  FILENAME ~ /\.i31$/ { i31[$1] = 1; next }
  FILENAME ~ /\.m3done$/ { m3done[$1] = 1; next }
  FILENAME ~ /rwt_rows/ { if (FNR > 1 && $1 != "") { rroute[$1] = $2; rstatus[$1] = $3; revid[$1] = $4 } next }
  FILENAME ~ /m3_names/ { if (FNR > 1) m3n[$1] = $2; next }
  FILENAME ~ /platform_map/ { if (FNR == 1) next; seen[$1] = 1; if ($2 == "nebula") nebkey[$5] = 1
    split($5, parts, ";"); bas = ($2 == "swingx") ? "re-derived from the SwingX API shape, LGPL pattern only" : "Swing track"
    nm = (substr($3, 1, 2) == "JX") ? "M3" substr($3, 3) : "M3" simple($5)
    sg = ($7 != "-") ? "AVAILABLE " $7 " (JDK)" : "ADAPTER_NEEDED javax.swing." nm " over the shared model (" bas ")"
    if ($6 == "NA") sg = "MISSING (not applicable)"
    ev = ($6 == "NA") ? "-" : ($6 == "M3") ? (( simple($5) in m3done) ? "implemented in widgets/m3; docs/m3/custom-widgets/compile_platforms.sh (win32/gtk/cocoa 0 errors)" : "planned") : ($6 == "STOCK" ? "SWT stock" : "Nebula source")
    row[++rows] = $1; emitargs[rows] = $1 SUBSEP $2 SUBSEP $3 SUBSEP $4 SUBSEP $6 SUBSEP $5 SUBSEP sg SUBSEP $8 SUBSEP $9 SUBSEP ev; next }
  FILENAME ~ /\.nebula\.tsv$/ { id = "NEBULA-" $1 "-" $2; if (id in seen) next; seen[id] = 1
    row[++rows] = id; emitargs[rows] = id SUBSEP "nebula" SUBSEP $2 SUBSEP (($2 in nebconcept) ? nebconcept[$2] : "-") SUBSEP "NEBULA" SUBSEP $2 SUBSEP "ADAPTER_NEEDED javax.swing.M3" $2 " over the shared model (Swing track)" SUBSEP "Swing twin" SUBSEP "Swing track" SUBSEP "Nebula source (widgets/" $1 ")"; next }
  FILENAME ~ /WIDGET_MATRIX/ { if (FNR == 1) next; c = $3; split($6, nb, ";"); for (k in nb) if (nb[k] != "-") nebconcept[nb[k]] = c
    nm = (c in m3n) ? m3n[c] : "M3" c
    if ($8 !~ /^-/) { kind = "STOCK"; cls = $8 } else if ($6 != "-") { kind = "NEBULA"; cls = nb[1] } else { kind = "M3"; cls = nm }
    basis = ($5 != "-") ? "SwingX " $5 " API shape" : "donors in WIDGET_MATRIX"
    sg = ($10 !~ /^-/) ? "AVAILABLE " $10 " (JDK)" : (($14 + 0) > 0 ? "ADAPTER_NEEDED javax.swing." nm " over the shared model (" basis ")" : "MISSING")
    crow[c] = "CONCEPT-" c SUBSEP "concept" SUBSEP $4 SUBSEP c SUBSEP kind SUBSEP cls SUBSEP sg SUBSEP $16 SUBSEP $19 SUBSEP "WIDGET_MATRIX rank " $1; corder[++cn] = c; next }
  END {
    print "widget_id", "family", "widget", "concept_id", "swt_win32", "swt_gtk", "swt_cocoa", "swing_m3jdk", "initiative31", "rwt_rap", "swt_class", "swing_class", "rwt_route", "gap_plan", "owner", "evidence"
    for (i = 1; i <= rows; i++) { split(emitargs[i], a, SUBSEP); emit(a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10]) }
    for (i = 1; i <= cn; i++) { split(crow[corder[i]], a, SUBSEP); emit(a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10]) } }' \
  "$OUT/.rwt" "$OUT/.i31" "$OUT/.m3done" "$HERE/rwt_rows.tsv" "$HERE/m3_names.tsv" "$HERE/../WIDGET_MATRIX.tsv" "$HERE/platform_map.tsv" "$OUT/.nebula.tsv" \
  | awk -F'\t' -v OFS='\t' 'NR == 1 { print; next } { split($8, s, " "); $12 = (s[1] == "AVAILABLE" || s[1] == "ADAPTER_NEEDED") ? s[2] : "-"; print }' > "$OUT/WIDGET_PLATFORM_MATRIX.tsv"
rm -f "$OUT/.rwt" "$OUT/.i31" "$OUT/.m3done"
awk -F'\t' 'NR > 1 { n++; for (c = 5; c <= 10; c++) { split($c, s, " "); k[c, s[1]]++ } }
  END { printf "rows=%d\n", n; split("swt_win32 swt_gtk swt_cocoa swing_m3jdk initiative31 rwt_rap", h, " ");
    for (c = 5; c <= 10; c++) printf "%-13s AVAILABLE %3d  ADAPTER_NEEDED %3d  MISSING %3d\n", h[c - 4], k[c, "AVAILABLE"], k[c, "ADAPTER_NEEDED"], k[c, "MISSING"] }' "$OUT/WIDGET_PLATFORM_MATRIX.tsv"
