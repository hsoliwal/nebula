#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Differential compile census: an error is attributed to RWT when it appears in the RWT compile and NOT in the
# baseline compile against the official desktop SWT (same sources, same non-toolkit class path). Errors present in
# both come from dependencies that neither class path provides (draw2d, workbench, ...).
# Usage: census_diff.sh <baseline errors.tsv> <rwt errors.tsv> <out-dir>
# Output: rwt_only.tsv (errors), by_family.tsv, by_symbol.tsv, summary.txt
set -u
BASE=$1; RWT=$2; OUT=$3
mkdir -p "$OUT"
# key = file, line, kind, detail, symbol (location text differs between toolkits for the same site, so it is left out)
awk -F'\t' -v OFS='\t' 'NR == FNR { seen[$1 FS $2 FS $4 FS $5 FS $6] = 1; next }
	!(($1 FS $2 FS $4 FS $5 FS $6) in seen)' "$BASE" "$RWT" > "$OUT/rwt_only.tsv"
{
	printf 'family\trwt_only_errors\tfiles_with_rwt_errors\tbaseline_errors\n'
	awk -F'\t' 'FILENAME == ARGV[1] { b[$3]++; next } { e[$3]++; if (!(($3 FS $1) in fl)) { fl[$3 FS $1] = 1; f[$3]++ } }
		END { for (k in b) if (!(k in e)) e[k] = 0; for (k in e) printf "%s\t%d\t%d\t%d\n", k, e[k], f[k], b[k] }' \
		"$BASE" "$OUT/rwt_only.tsv" | sort -t$'\t' -k2,2nr -k1,1
} > "$OUT/by_family.tsv"
{
	printf 'kind\tsymbol\tlocation_type\terrors\tfamilies\n'
	awk -F'\t' -v OFS='\t' '{
		s = $6; if ($4 == "OTHER") s = $5; sub(/\(.*/, "", s)
		l = $7; sub(/^variable [^ ]+ of type /, "", l); sub(/^(class|interface|package) /, "", l); sub(/<.*/, "", l)
		k = $4 FS s FS l; n[k]++; if (!((k FS $3) in seen)) { seen[k FS $3] = 1; fam[k] = fam[k] (fam[k] == "" ? "" : ",") $3 } }
		END { for (k in n) print k, n[k], fam[k] }' "$OUT/rwt_only.tsv" | sort -t$'\t' -k4,4nr -k2,2
} > "$OUT/by_symbol.tsv"
{
	echo "baseline errors: $(wc -l < "$BASE")"
	echo "rwt errors: $(wc -l < "$RWT")"
	echo "rwt-only errors: $(wc -l < "$OUT/rwt_only.tsv")"
	echo "files with rwt-only errors: $(cut -f1 "$OUT/rwt_only.tsv" | sort -u | wc -l)"
	echo "families with rwt-only errors: $(cut -f3 "$OUT/rwt_only.tsv" | sort -u | wc -l)"
} > "$OUT/summary.txt"
cat "$OUT/summary.txt"
