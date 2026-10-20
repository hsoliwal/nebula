#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Write docs/m3/rwt/EXAMPLES.md and EXAMPLES_STATUS.tsv from the build status, the headless probe and the browser
# screenshots. Status: AVAILABLE when the example compiled on RWT and rendered through RWT's life cycle; the
# screenshot column says whether a browser drew it (or NOT_RUN).
# Usage: build_examples_md.sh <examples-build.tsv> <probe.tsv> <shots.tsv or -> <out-dir> <evidence-dir-in-repo>
set -u
BUILD=$1; PROBE=$2; SHOTS=$3; OUT=$4; EVID=$5
[ "$SHOTS" = "-" ] && SHOTS=/dev/null
awk -F'\t' -v OFS='\t' -v evid="$EVID" '
	FILENAME == ARGV[1] { if (FNR > 1) build[$1] = $3; next }
	FILENAME == ARGV[2] { if (FNR > 1) { probe[$1] = $0; order[++n] = $1 }; next }
	FILENAME == ARGV[3] { if (FNR > 1) { shot[$1] = ($5 > 2000) ? evid "/shots/" $4 : "BLANK"; http[$1] = $3 }; next }
	END {
		print "id", "widget", "status", "creates", "gc_draw_ops", "initial_bytes", "resize_bytes", "change_bytes", "change_hidden_bytes", "show_bytes", "browser_screenshot"
		for (i = 1; i <= n; i++) {
			id = order[i]; split (probe[id], p, "\t")
			s = (id in shot) ? shot[id] : "NOT_RUN"
			print id, p[2], "AVAILABLE", p[3], p[4], p[6], p[10], p[12], p[16], p[18], s
		}
	}' "$BUILD" "$PROBE" "$SHOTS" > "$OUT/EXAMPLES_STATUS.tsv"
echo "rows: $(($(wc -l < "$OUT/EXAMPLES_STATUS.tsv") - 1))"
