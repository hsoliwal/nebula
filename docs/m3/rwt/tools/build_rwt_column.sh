#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Join the custom-widgets WIDGET_MATRIX.tsv (rank, concept), the route decisions and the differential compile census
# into docs/m3/rwt/RWT_COLUMN.tsv. Mechanical: every number comes from the census files.
# Usage: build_rwt_column.sh <WIDGET_MATRIX.tsv> <census rwt_only.tsv> <out RWT_COLUMN.tsv> [evidence label]
set -u
MATRIX=$1; CENSUS=$2; OUT=$3; LABEL=${4:-census/rwt-4.8.0-v0}
HERE=$(cd "$(dirname "$0")" && pwd)
awk -F'\t' -v OFS='\t' -v label="$LABEL" '
	FILENAME == ARGV[1] { if (FNR > 1) { rank[$3] = $1; name[$3] = $4 }; next }
	FILENAME == ARGV[2] { if (FNR > 1) file[$1] = $2; next }
	FILENAME == ARGV[3] {
		ferr[$1]++
		b = $1; sub(/\/src\/.*/, "", b); berr[b]++
		s = $6; if ($4 == "OTHER") s = $5; sub(/\(.*/, "", s); sub(/^(class|method|variable) +/, "", s)
		if (!(($1 FS s) in seen)) { seen[$1 FS s] = 1; syms[$1] = syms[$1] (syms[$1] == "" ? "" : "|") s }
		next
	}
	FNR == 1 {
		print "rank", "concept_id", "concept", "rwt_route", "rwt_class", "rwt_status", "resize_layout_delta", \
			"hidden_viewport", "nebula_primary", "primary_file_rwt_errors", "primary_bundle_rwt_errors", "primary_missing_symbols", \
			"other_candidates", "evidence", "note"
		next
	}
	{
		c = $1; f = ($4 in file) ? file[$4] : ""; if (f == "-") f = ""; fe = "-"; be = "-"; sy = "-"
		if (f != "") {
			b = f; sub(/\/src\/.*/, "", b)
			fe = ferr[f] + 0; be = berr[b] + 0; sy = (f in syms) ? substr(syms[f], 1, 200) : "-"
		}
		ev = "docs/m3/rwt/" label "/rwt_only.tsv"
		# hidden viewport behaviour of RWT 4.8.0 by resize/layout class (README section 3, rule 24 "Hidden viewports")
		hv = ($7 == "CANVAS_DAMAGE_RECT") ? "PAINTS_WHILE_HIDDEN" : ($7 == "ROWS_ON_DEMAND") ? "ROW_DELTAS_WHILE_HIDDEN" : ($7 == "-") ? "-" : "DELTAS_WHILE_HIDDEN"
		print rank[c], c, name[c], $2, $3, $6, $7, hv, $4, fe, be, sy, $5, ev, $8
	}' "$MATRIX" "$HERE/PRIMARY_FILES.tsv" "$CENSUS" "$HERE/route_decisions.tsv" \
	| { IFS= read -r head; echo "$head"; sort -t$'\t' -k1,1n; } > "$OUT"
echo "rows: $(($(wc -l < "$OUT") - 1))"
awk -F'\t' 'NR > 1 { r[$4]++ } END { for (k in r) printf "%s\t%d\n", k, r[k] }' "$OUT" | sort
