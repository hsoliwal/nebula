#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Group RWT-only compile errors (census_diff.sh rwt_only.tsv) into drift categories by symbol and message.
# Usage: census_categories.sh <rwt_only.tsv>   (prints category <TAB> errors, most first)
awk -F'\t' '{ d = $5; s = $6; c = "OTHER"
	if (d ~ /cannot be converted to Device|RGB cannot be converted to int|constructor found for Color|constructor Color/) c = "COLOR_DRIFT"
	else if (s ~ /(set|get)(Background|Foreground)Pattern|(set|get)Interpolation|(set|get)FillRule|(set|get)XORMode|copyArea|class Pattern$/) c = "GC_GAP_AND_PATTERN"
	else if (s ~ /Mouse(Move|Track|Wheel)|setCapture|traverse|TRAVERSE_|keyLocation|MouseHover|setCursor|[dD]ragDetect|MouseEnter|MouseExit/) c = "INPUT"
	else if (s ~ /StyledText|StyleRange|ST$|Bidi|Segment|Caret|getCaret|LineBackground|MovementListener/) c = "TEXT_STYLEDTEXT"
	else if (s ~ /Region|TextLayout|addArc|Tracker/ || d ~ /textExtent/) c = "GRAPHICS_CLASSES"
	else if (s ~ /Printer|printing|PrintDialog/) c = "PRINTING"
	else if (s ~ /Increment/) c = "SCROLLBAR_INCREMENTS"
	else if (s ~ /getZoom|isRescaling/) c = "DPI_ZOOM"
	else if (s ~ /HIGH|ICON$|PaintItem|MeasureItem|EMBEDDED|TRANSPARENT|COLOR_/) c = "SWT_CONSTANTS"
	else if (d ~ /constructor found for Image/) c = "IMAGE_CTOR"
	n[c]++ } END { for (k in n) printf "%s\t%d\n", k, n[k] }' "$1" | sort -t$'\t' -k2,2nr
