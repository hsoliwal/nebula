#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# Keep the widget libraries among the GitHub search hits (one row per repository, highest stars first).
# Usage: filter_search.sh <hits.tsv from github_search.sh> > libs_from_search.tsv
sort -t$'\t' -k1,1 -u "$1" | sort -t$'\t' -k3,3nr | awk -F'\t' 'BEGIN{IGNORECASE=1}
{ s=$1" "$7" "$8; isswt=(s ~ /swt|nebula|eclipse/);
  lib=(s ~ /(librar|component|widget|look.?and.?feel|lookandfeel|[^a-z]laf|controls|toolkit|theme|docking|date.?picker|time.?picker|calendar|chart|table|tree|gauge|tiles|layout|ribbon|toast|notification|autocomplete|skin|ui kit|ui-kit|framework for|swingx|jide|material)/);
  app=(s ~ /(management|game|tutorial|student|homework|project for|course|clone of|calculator app|bank|hospital|hotel|library management|inventory|pharmacy|chat app|messenger|client tool|minecraft|snake|tetris|chess|sudoku|demo app|example code|examples|exercise|solution|school|login ui|dashboard build|crud)/);
  if (lib && !app && ($3>=15 || (isswt && $3>=3))) print }'
