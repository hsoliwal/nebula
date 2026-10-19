#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Deterministic CPU projection: library class listings x widget concept regexes -> LIBRARIES.tsv + WIDGET_MATRIX.tsv.
# Usage: build_matrix.sh <cache-dir (meta.tsv, classes/)> <out-dir>
# Inputs next to this script: concepts.tsv, owners.tsv, licence_overrides.tsv, excluded.tsv.
# Score = library_count x virt_weight (Y=3, P=2, N=1) x missing_weight (absent from stock SWT and Swing = 3, from one = 2,
# from neither = 1). Priority: owners.tsv override (P0), else rank 1-12 P1, 13-30 P2, rest P3.
set -u
export LC_ALL=C
HERE=$(cd "$(dirname "$0")" && pwd); CACHE=$1; OUT=$2; mkdir -p "$OUT"
EXCL='(UI|Skin|Behavior|Behaviour|Listener|Event|Model|Renderer|Painter|Provider|Factory|Adapter|Util|Utils|Helper|Action|Accessible|Accessibility|Handler|Command|Configuration|Config|Styler|Border|Converter|Exception|Constants|Messages|Impl|BeanInfo|Descriptor|Serializer|Resources?|Cell|CellEditor|Layer|Test|Tests|Demo|Example|Snippet|Interface|Strategy|State|Data|Item|Column|Builder|Settings|Addon|Runner|Engine|Processor|Fixture|Dataset|Series|Position|Memento|Flags|Operations)$|^(Abstract|Basic|Metal|Synth|Default|Windows|Motif|Aqua|Gtk|Mock|Fake|Dummy)|^I[A-Z]'
# 1. slug <TAB> class basename, one line per distinct pair
for f in "$CACHE"/classes/*.txt; do
  s=$(basename "$f" .txt); s=${s/__//}; s=${s,,}
  sed -e 's#.*/##' -e 's#\.java$##' "$f" | grep -Ev "$EXCL" | sort -u | sed "s#^#$s\t#"
done > "$OUT/.pairs.tsv"
# 2. concept matches: concept <TAB> slug <TAB> class
awk -F'\t' 'NR==FNR { if (FNR > 1) { n++; id[n] = $1; re[n] = $3 } next }
  { for (i = 1; i <= n; i++) if ($2 ~ re[i]) printf "%s\t%s\t%05d\t%s\n", id[i], $1, length($2), $2 }' "$HERE/concepts.tsv" "$OUT/.pairs.tsv" \
  | sort -t'	' -k1,1 -k2,2 -k3,3 -k4,4 -u | cut -f1,2,4 > "$OUT/.matches.tsv"
# 3. libraries with licence route and widget-class count
awk -F'\t' -v OFS='\t' '
  function route(l) {
    if (l ~ /^EPL/) return "ADAPT (EPL)";
    if (l ~ /^(Apache-2\.0|BSD|MIT|MIT-0|Unlicense|ISC|ECL-2\.0|0BSD)/) return "ADAPT (permissive, keep notice)";
    if (l ~ /^GPL-2\.0.*Classpath|^GPL-2\.0 WITH/) return "PATTERN_ONLY (SWT/Nebula); PORT inside M3jdk21 src (same licence)";
    if (l ~ /GPL|LGPL|MPL|CPL|EUPL|CDDL/) return "PATTERN_ONLY (copyleft)";
    return "PATTERN_ONLY (licence review: none detected)";
  }
  FILENAME ~ /licence_overrides/ { if (FNR > 1) ov[tolower($1)] = $2; next }
  FILENAME ~ /excluded/ { if (FNR > 1) ex[tolower($1)] = $2; next }
  FILENAME ~ /matches/ { if (!(($2, $3) in seen)) { seen[$2, $3] = 1; lib[$2]++ } next }
  FILENAME ~ /meta/ { slug = $1; k = tolower(slug); done[k] = 1; lic = (k in ov) ? ov[k] : $2; stock = ($9 ~ /-stock$/);
    r = stock ? "STOCK (column reference)" : route(lic);
    if ($2 == "NOT_FOUND") r = "EXCLUDE (repository not found)";
    if (k in ex) r = "EXCLUDE (" ex[k] ")";
    if (k ~ /swingx$/) r = "PATTERN_ONLY (LGPL-2.1: API shape only, never code)";
    print slug, lic, $3, $4, $6, $9, $8, (k in lib) ? lib[k] : 0, $7, $10, r; next }
  { k = tolower($1); if ((k in ex) && !(k in done)) { done[k] = 1;
      print $1, $2, $3, $4, $6, "-", "-", "-", "-", "gh-search", "EXCLUDE (" ex[k] ")" } }' \
  "$HERE/licence_overrides.tsv" "$HERE/excluded.tsv" "$OUT/.matches.tsv" "$CACHE/meta.tsv" "$CACHE/search_hits.tsv" \
  | sort -t'	' -k6,6 -k3,3nr > "$OUT/.libs.tsv"
{ printf 'slug\tlicence\tstars\tlast_push\tarchived\ttoolkit\tjava_files\twidget_classes_matched\ttree_truncated\tsource\tlicence_route\n'
  cat "$OUT/.libs.tsv"; } > "$OUT/LIBRARIES.tsv"
# 4. matrix rows
awk -F'\t' -v OFS='\t' '
  FILENAME ~ /concepts/ { if (FNR > 1) { n++; id[n] = $1; nm[$1] = $2; vt[$1] = $4; sw[$1] = $5; sg[$1] = $6 } next }
  FILENAME ~ /owners/ { if (FNR > 1) { po[$1] = $2; ow[$1] = $3; pn[$1] = $4 } next }
  FILENAME ~ /libs/ { rt[tolower($1)] = $11; tk[tolower($1)] = $6; next }
  { c = $1; s = $2; k = $3; if (rt[s] ~ /^EXCLUDE/) next;
    if (tk[s] ~ /-stock$/) { st[c, tk[s]] = st[c, tk[s]] (st[c, tk[s]] ? ";" : "") k; next }
    fam = (s ~ /swingx$/) ? "SwingX" : s;
    if (!((c, fam) in seen)) { seen[c, fam] = 1; cnt[c]++; libs[c] = libs[c] (libs[c] ? " " : "") fam }
    if (s ~ /swingx$/) { if (index(";" sx[c] ";", ";" k ";") == 0 && nsx[c]++ < 4) sx[c] = sx[c] (sx[c] ? ";" : "") k; next }
    if (s == "hsoliwal/nebula") { if (nnb[c]++ < 4) nb[c] = nb[c] (nb[c] ? ";" : "") k; next }
    if (oc[c, s] < 3) { oc[c, s]++; ol[c] = ol[c] (ol[c] ? "; " : "") s ":" k }
    if (rt[s] ~ /^ADAPT/ && index(ad[c], s) == 0) ad[c] = ad[c] (ad[c] ? "," : "") s }
  END {
    for (i = 1; i <= n; i++) { c = id[i]; v = vt[c]; vw = (v == "Y") ? 3 : (v == "P") ? 2 : 1;
      ms = (sw[c] ~ /^-/) + (sg[c] ~ /^-/); mw = (ms == 2) ? 3 : (ms == 1) ? 2 : 1;
      miss = (ms == 2) ? "both" : (sw[c] ~ /^-/) ? "swt" : (sg[c] ~ /^-/) ? "swing" : "none";
      score = (cnt[c] + 0) * vw * mw;
      swtside = (sw[c] !~ /^-/) ? "SWT stock" : (nb[c] != "") ? "Nebula" : "missing";
      plan = "";
      if (swtside == "missing") plan = "NEW_SWT org.eclipse.nebula.widgets.m3 (M3 name)";
      else if (swtside == "Nebula" && v != "N") plan = "NEBULA_VIEWPORT (" nb[c] ")";
      else if (swtside == "Nebula") plan = "NEBULA_KEEP (" nb[c] "; theme seam only)";
      else if (v == "Y") plan = "SWT_VIEWPORT (stock widget, SWT tracks)";
      else plan = "SWT_STOCK_KEEP";
      if (sg[c] ~ /^-/) plan = plan "; SWING_HANDOFF M3 class in java.desktop (enhanced-API Swing track)";
      if (c in pn) plan = plan "; " pn[c];
      lr = (ad[c] != "") ? "ADAPT possible from " ad[c] : "PATTERN_ONLY (no permissive or EPL implementation found)";
      if (sx[c] != "") lr = lr "; SwingX shape only";
      print score, c, nm[c], (sx[c] == "" ? "-" : sx[c]), (nb[c] == "" ? "-" : nb[c]), (ol[c] == "" ? "-" : ol[c]),
        sw[c], (st[c, "swt-stock"] == "" ? "-" : st[c, "swt-stock"]), sg[c], (st[c, "swing-stock"] == "" ? "-" : st[c, "swing-stock"]),
        (st[c, "javafx-stock"] == "" ? "-" : st[c, "javafx-stock"]), v, cnt[c] + 0, miss, plan, lr, (c in po && po[c] != "-") ? po[c] : "", (c in ow) ? ow[c] : "custom-widgets", libs[c] } }' \
  "$HERE/concepts.tsv" "$HERE/owners.tsv" "$OUT/.libs.tsv" "$OUT/.matches.tsv" \
  | sort -t'	' -k1,1nr -k13,13nr -k2,2 \
  | awk -F'\t' -v OFS='\t' '{ r++; p = $17; if (p == "") p = (r <= 12) ? "P1" : (r <= 30) ? "P2" : "P3"; $17 = p; print r, $0 }' \
  | sort -t'	' -k18,18 -k1,1n > "$OUT/.matrix.tsv"
{ printf 'rank\tscore\tconcept_id\tconcept\tswingx_class\tnebula_class\tother_library_classes\tswt_stock\tswt_stock_listed\tswing_stock\tswing_stock_listed\tjavafx_stock_listed\tvirtualizable\tlibrary_count\tmissing_from_stock\tm3_plan\tlicence_route\tpriority\towner\tlibraries\n'
  cat "$OUT/.matrix.tsv"; } > "$OUT/WIDGET_MATRIX.tsv"
rm -f "$OUT/.pairs.tsv" "$OUT/.libs.tsv" "$OUT/.matrix.tsv"
echo "libraries=$(($(wc -l < "$OUT/LIBRARIES.tsv") - 1)) concepts=$(($(wc -l < "$OUT/WIDGET_MATRIX.tsv") - 1)) matches=$(wc -l < "$OUT/.matches.tsv")"
# 5. Swing hand-off rows in the shared ENHANCED_API_MAP format (enhanced-API Swing track, M3jdk21 java.desktop)
awk -F'\t' -v OFS='\t' '
  FILENAME ~ /m3_names/ { if (FNR > 1) { nm[$1] = $2; dp[$1] = $3 } next }
  FNR == 1 { print "api_id", "family", "source", "swt", "nebula", "rwt", "intellij", "swing_swingx", "awt", "m3jdk_core", "donor_slugs",
      "donor_evidence", "proposed_m3_surface", "doop_data_plan", "contract_tests", "priority", "status"; next }
  $16 ~ /SWING_HANDOFF/ || $18 == "P0" { c = $3; n = (c in nm) ? nm[c] : "M3" c;
    swt = ($8 ~ /^-/) ? "-" : "Y (" $8 ")"; neb = ($6 == "-") ? "-" : "Y (" $6 ")"; ij = ($20 ~ /intellij/) ? "Y" : "-";
    sx = ($5 == "-") ? "-" : "SwingX " $5 " (API shape only)";
    print "CW-" c, "widget/" tolower(c), "custom-widgets WIDGET_MATRIX rank " $1 " (score " $2 ")", swt, neb, "-", ij, sx, "-", "-",
      $20, $7, "SWT: org.eclipse.nebula.widgets.m3." n " | Swing: javax.swing." n " (same members; Swing naming and events)",
      (c in dp) ? dp[c] : "-", "Swing: seeded differential against the stock Swing composition it replaces (--patch-module java.desktop), 99% JaCoCo; SWT: recorded differential and example",
      $18, "HANDOFF to the enhanced-API Swing track (m3/swing-enhanced-api-20261019); SWT side: " (($16 ~ /^NEW_SWT/) ? "custom-widgets new widget" : $16 ~ /^NEBULA/ ? "Nebula widget onto the viewport" : "stock SWT") }' \
  "$HERE/m3_names.tsv" "$OUT/WIDGET_MATRIX.tsv" > "$OUT/ENHANCED_API_MAP.tsv"
echo "swing_handoff_rows=$(($(wc -l < "$OUT/ENHANCED_API_MAP.tsv") - 1))"
