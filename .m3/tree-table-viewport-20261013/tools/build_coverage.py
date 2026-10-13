#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Joins the extracted API surface with the differential verdicts and the laziness points.

Usage: python -I build_coverage.py <run-dir> <build-dir>
  <run-dir>/API_RAW.tsv, <run-dir>/LAZY_POINTS.tsv,
  <build-dir>/<widget>/diff-visible-post.tsv (missing -> NOT_RUN)
Writes <run-dir>/NEBULA_TT_API_COVERAGE.tsv and <run-dir>/NEBULA_TT_API_COVERAGE_SUMMARY.tsv.
"""
import collections
import os
import sys

WIDGETS = ["xviewer", "ctree", "ctreecombo", "tablecombo", "calendarcombo", "compositetable"]


def read_tsv(path):
    with open(path, encoding="utf-8") as fh:
        rows = [line.rstrip("\n").split("\t") for line in fh if line.strip()]
    return rows[0], rows[1:]


def main():
    run, build = sys.argv[1], sys.argv[2]
    head, api = read_tsv(os.path.join(run, "API_RAW.tsv"))
    verdicts = {}
    for w in WIDGETS:
        path = os.path.join(build, w, "diff-visible-post.tsv")
        if os.path.exists(path):
            _, rows = read_tsv(path)
            for r in rows:
                verdicts[r[0]] = (r[2], r[3][:160], r[4][:160] if len(r) > 4 else "")
    out = ["\t".join(head + ["diff_visible", "pre_outcome", "post_outcome"])]
    summary = collections.defaultdict(collections.Counter)
    for r in api:
        v = verdicts.get(r[0], ("NOT_RUN", "", ""))
        summary[r[1]][v[0]] += 1
        out.append("\t".join(r + [v[0], v[1].replace("\t", " "), v[2].replace("\t", " ")]))
    _, lazy = read_tsv(os.path.join(run, "LAZY_POINTS.tsv"))
    for i, r in enumerate(lazy):
        out.append("\t".join(["lazy-%02d" % i, r[0], "", "", r[4], "laziness_point", "", r[1],
                              "", "", "LAZY", r[2], "before: " + r[3]]))
        summary[r[0]]["LAZY_POINTS"] += 1
    with open(os.path.join(run, "NEBULA_TT_API_COVERAGE.tsv"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out) + "\n")
    keys = ["EQUAL", "EQUAL_NONINVOKED", "DIFF", "NONDETERMINISTIC_BASELINE", "MISSING", "NOT_RUN", "LAZY_POINTS"]
    lines = ["widget\tapi_rows\t" + "\t".join(keys)]
    for w in WIDGETS:
        c = summary[w]
        total = sum(c[k] for k in keys if k != "LAZY_POINTS")
        lines.append(w + "\t" + str(total) + "\t" + "\t".join(str(c[k]) for k in keys))
    with open(os.path.join(run, "NEBULA_TT_API_COVERAGE_SUMMARY.tsv"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
