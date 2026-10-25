#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Per-test status of a run_bundle_tests.sh log (JUnit console launcher, --details=tree), one line per test:
# <class>#<test><TAB>PASS|FAIL|SKIP, sorted. With two logs, prints the tests whose status differs (before -> after),
# so a leaf's run compares with the baseline run of the stock code test by test, not by counts.
# Usage: test_status.sh <log>            per-test status
#        test_status.sh <before> <after>  changed tests only ("<test><TAB><before> -> <after>"; NEW and GONE for one side)
set -uo pipefail; export LC_ALL=C
status() {
	sed 's/\x1b\[[0-9;]*m//g' "$1" | awk '
		/[├└]─ / {
			i = index($0, "├─ "); if (i == 0) i = index($0, "└─ ")
			prefix = substr($0, 1, i - 1); gsub(/│  /, "x", prefix); gsub(/   /, "x", prefix); depth = length(prefix)
			rest = substr($0, i + length("├─ "))
			st = ""
			if (index(rest, " ✔")) st = "PASS"; else if (index(rest, " ✘")) st = "FAIL"; else if (index(rest, " ↷")) st = "SKIP"
			name = rest; sub(/ (✔|✘|↷).*$/, "", name)
			if (depth == 1) cls = name
			else if (depth == 2 && st != "") printf "%s#%s\t%s\n", cls, name, st
		}' | sort -u
}
if [ $# -eq 1 ]; then status "$1"; exit 0; fi
join -t "$(printf '\t')" -a 1 -a 2 -e GONE -o 0,1.2,2.2 <(status "$1") <(status "$2") \
	| awk -F'\t' '$2 != $3 { b = $2; a = $3; if (b == "GONE") b = "NEW"; print $1 "\t" b " -> " a }'
