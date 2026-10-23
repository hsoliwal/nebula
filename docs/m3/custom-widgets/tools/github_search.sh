#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# GitHub repository search for UI widget libraries (read-only listing; one TSV row per hit).
# Usage: search.sh <out.tsv>
set -u
OUT=$1; : > "$OUT"
Q=(
 "topic:swt" "topic:swt-widgets" "topic:eclipse-swt" "topic:nebula" "topic:swing" "topic:java-swing"
 "topic:swing-components" "topic:swing-library" "topic:look-and-feel" "topic:javafx-controls"
 "swt widget in:name,description" "swing component library in:name,description" "swing table in:name,description"
 "swing tree table in:name,description" "virtual list swing in:name,description"
 "topic:swing-gui" "topic:javafx-library" "topic:lookandfeel" "topic:swt-ui" "swt custom widgets in:name,description"
)
for q in "${Q[@]}"; do
  for page in 1 2; do
    gh api -X GET search/repositories -f q="$q language:Java" -f sort=stars -f order=desc -f per_page=100 -f page=$page \
      --jq '.items[] | [.full_name, (.license.spdx_id // "NONE"), .stargazers_count, .pushed_at[0:10], (.language // "-"), .archived, ((.description // "-")|gsub("[\t\n\r]";" ")), (.topics|join(","))] | @tsv' \
      2>>search.err | sed "s|\$|\t$q|" >> "$OUT"
    sleep 2
  done
done
wc -l "$OUT"
