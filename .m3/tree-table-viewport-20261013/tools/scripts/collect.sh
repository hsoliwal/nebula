#!/bin/sh
# Collects evidence into the Nebula run dir and the Synexia crate evidence dir (no JVM).
. /c/m3-work-c/nebula-tt-build/scripts/env.sh
RUN=$N/.m3/tree-table-viewport-20261013
EV=$RUN/evidence; mkdir -p $EV
SYN=/c/m3-work-c/wt/syn-tree-table/synexia-openrewrite-recipes/recipe-crates/nebula-tree-table-viewport-20261013
python -I $RUN/tools/build_coverage.py $RUN $B
for w in xviewer ctree ctreecombo tablecombo calendarcombo compositetable; do
  [ -s $B/$w/diff-visible-post.tsv ] && cp $B/$w/diff-visible-post.tsv $EV/diff-$w-visible.tsv
  for tag in pre post; do [ -s $B/$w/bench-$tag.tsv ] && cp $B/$w/bench-$tag.tsv $EV/bench-$w-$tag.tsv; done
done
for f in $B/logs/junit-*.log $B/logs/snippets.tsv; do [ -s $f ] && cp $f $EV/; done
python -I - "$EV" <<'PY'
import glob, os, sys
ev = sys.argv[1]
rows = {}
for path in sorted(glob.glob(os.path.join(ev, "bench-*-*.tsv"))):
    for line in open(path, encoding="utf-8").read().splitlines()[1:]:
        image, metric, value, unit = line.split("\t")
        widget = os.path.basename(path).split("-")[1]
        rows.setdefault((widget, metric, unit), {})[image] = value
out = ["widget\tmetric\tunit\tpre\tpost"]
for (w, m, u), v in sorted(rows.items()):
    out.append("\t".join([w, m, u, v.get("pre", "NOT_RUN"), v.get("post", "NOT_RUN")]))
open(os.path.join(ev, "BENCH_BEFORE_AFTER.tsv"), "w", encoding="utf-8", newline="\n").write("\n".join(out) + "\n")
print(len(out) - 1, "bench rows")
PY
mkdir -p $SYN/evidence
cp $RUN/NEBULA_TT_API_COVERAGE.tsv $RUN/NEBULA_TT_API_COVERAGE_SUMMARY.tsv $RUN/LAZY_POINTS.tsv $RUN/FOSS_REUSE_DECISION.tsv $RUN/THIRD_PARTY_NOTICES.md $SYN/
cp $RUN/DESIGN.md $SYN/DESIGN.md
cp $EV/* $SYN/evidence/ 2>/dev/null
echo collected
