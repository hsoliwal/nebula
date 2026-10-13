#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
# build-superset-nebula.sh - compile, jar and headless-test the Nebula superset bundles (2026-10-13.2).
#
# Adapted copy of .m3/integration-20261013/build-integration.sh (build stages only; the original is
# not edited). Differences: worktree C:/m3-superset/nebula, output C:/m3-superset/build-nebula and
# C:/m3-superset/dist, the official SWT 3.135.0 jar on the compile classpath, a BREE-level compile
# check, META-INF/m3/MAPPING.tsv plus M3-* manifest entries in every jar, and headless JUnit 4 runs.
#
# Usage: build-superset-nebula.sh compile|bree|jar|tests|run-tests|all
set -euo pipefail
export MSYS_NO_PATHCONV=1

STAMP=20261013.2                       # jar file stamp
QUALIFIER=m3-20261013-2                # OSGi qualifier (no dots allowed)
BRANCH=m3/superset-consolidation-20261013
WT=${NEBULA_WT:-C:/m3-superset/nebula}
OUT=${OUT:-C:/m3-superset/build-nebula}
DIST=${DIST:-C:/m3-superset/dist}
JDK=${M3_JDK:-C:/Program Files/Java/jdk-21}
M2=${M2:-$HOME/.m2/repository}
SWT_JAR=${SWT_JAR:-C:/m3-work-c/integ-build/official/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar}
DRAW2D_JAR=${DRAW2D_JAR:-C:/m3-work-c/showcase-build/platform-deps/org.eclipse.draw2d_3.23.100.202608071137.jar}
LEDGER_DOC=docs/m3/SUPERSET_CONSOLIDATION_20261013.md

NEBULA_BUNDLES=(grid/org.eclipse.nebula.widgets.grid
  xviewer/org.eclipse.nebula.widgets.xviewer.core xviewer/org.eclipse.nebula.widgets.xviewer
  ctree/org.eclipse.nebula.widgets.ctree ctreecombo/org.eclipse.nebula.widgets.ctreecombo
  tablecombo/org.eclipse.nebula.widgets.tablecombo calendarcombo/org.eclipse.nebula.widgets.calendarcombo
  compositetable/org.eclipse.nebula.widgets.compositetable
  visualization/org.eclipse.nebula.visualization.xygraph visualization/org.eclipse.nebula.visualization.widgets
  oscilloscope/org.eclipse.nebula.widgets.oscilloscope)
TEST_BUNDLES=(grid/org.eclipse.nebula.widgets.grid.test xviewer/org.eclipse.nebula.widgets.xviewer.tests
  ctree/org.eclipse.nebula.widgets.ctree.tests ctreecombo/org.eclipse.nebula.widgets.ctreecombo.tests
  tablecombo/org.eclipse.nebula.widgets.tablecombo.test calendarcombo/org.eclipse.nebula.widgets.calendarcombo.tests
  compositetable/org.eclipse.nebula.widgets.compositetable.tests
  visualization/org.eclipse.nebula.visualization.xygraph.test visualization/org.eclipse.nebula.visualization.widgets.tests
  oscilloscope/org.eclipse.nebula.widgets.oscilloscope.tests)
NEBULA_DEPS=(platform/org.eclipse.jface/3.40.0 platform/org.eclipse.core.commands/3.13.0
  platform/org.eclipse.equinox.common/3.21.0 platform/org.eclipse.ui.workbench/3.140.0
  platform/org.eclipse.ui.forms/3.14.200 platform/org.eclipse.core.runtime/3.35.0
  platform/org.eclipse.core.jobs/3.15.900 platform/org.eclipse.osgi/3.24.300
  platform/org.eclipse.equinox.registry/3.12.600 platform/org.eclipse.core.databinding.observable/1.13.500
  platform/org.eclipse.core.expressions/3.9.700 platform/org.eclipse.equinox.preferences/3.12.100)
TEST_DEPS=("$M2/junit/junit/4.13.2/junit-4.13.2.jar" "$M2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar")

mkdir -p "$OUT" "$DIST"
log() { printf '%s %s\n' "$(date -u +%H:%M:%SZ)" "$*" | tee -a "$OUT/build.log" >&2; }

memory_gate() { # refuse to start a JVM unless at least 4 GB physical memory is available (free + standby)
  local mb
  mb=$(powershell -NoProfile -Command "[int](Get-Counter '\\Memory\\Available MBytes').CounterSamples[0].CookedValue" | tr -d '\r ')
  log "memory gate: available RAM $((mb / 1024)) GB"
  [ "$mb" -ge 4096 ] || { log "memory gate FAILED (< 4 GB available)"; exit 4; }
}

bp_get() { # <build.properties> <key-regex>: comma list of a (continued) property, one item per line
  tr -d '\r' <"$1" | awk -v k="$2" '
    { line = cont ? line $0 : $0; cont = (substr(line, length(line), 1) == "\\")
      if (cont) { line = substr(line, 1, length(line) - 1); next }
      if (line ~ "^" k "[ \t]*=") { sub("^" k "[ \t]*=[ \t]*", "", line); n = split(line, a, ",")
        for (i = 1; i <= n; i++) { gsub(/^[ \t]+|[ \t]+$/, "", a[i]); if (a[i] != "") print a[i] } } }'
}

base_cp() {
  local cp e
  cp=$(cygpath -m "$SWT_JAR")
  for e in "${NEBULA_DEPS[@]}"; do
    local g=${e%%/*} rest=${e#*/}; local a=${rest%/*} v=${rest##*/}
    cp="$cp;$(cygpath -m "$M2/org/eclipse/$g/$a/$v/$a-$v.jar")"
  done
  printf '%s;%s' "$cp" "$(cygpath -m "$DRAW2D_JAR")"
}

bundle_cp() { # every compiled bundle's classes
  local cp="" b
  for b in "${NEBULA_BUNDLES[@]}"; do cp="$cp;$OUT/${b#*/}/classes"; done
  printf '%s' "${cp#;}"
}

sources_of() { # <bundle-dir> <outfile>
  local d=$1 s
  mapfile -t srcs < <(bp_get "$d/build.properties" 'source[.][.]')
  for s in "${srcs[@]}"; do find "$d/${s%/}" -name '*.java' -type f; done | sort | sed 's#.*#"&"#' >"$2"
}

compile_bundle() { # <widget>/<bundle> <release> <classes-dir> <log>
  local d="$WT/widgets/$1" rel=$2 cls=$3 lg=$4 name=${1#*/}
  rm -rf "$cls"; mkdir -p "$cls"
  sources_of "$d" "$OUT/$name/sources.txt"
  "$JDK/bin/javac" -J-Xmx1g --release "$rel" -encoding UTF-8 -proc:none -g -Xmaxwarns 100000 -Xmaxerrs 100000 \
    -cp "$(base_cp);$(bundle_cp)" -d "$cls" @"$OUT/$name/sources.txt" >"$lg" 2>&1
}

do_compile() {
  memory_gate
  local b rc=0 name
  for b in "${NEBULA_BUNDLES[@]}"; do
    name=${b#*/}; mkdir -p "$OUT/$name"
    if compile_bundle "$b" 21 "$OUT/$name/classes" "$OUT/$name/javac.log"; then
      log "compile OK $name ($(wc -l <"$OUT/$name/sources.txt") sources)"
    else
      log "compile FAILED $name"; tail -20 "$OUT/$name/javac.log" >&2; rc=5
    fi
  done
  return $rc
}

do_bree() { # compile again at each bundle's Bundle-RequiredExecutionEnvironment level
  local b rc=0 name bree
  for b in "${NEBULA_BUNDLES[@]}"; do
    name=${b#*/}
    bree=$(tr -d '\r' <"$WT/widgets/$b/META-INF/MANIFEST.MF" | sed -n 's/^Bundle-RequiredExecutionEnvironment: *JavaSE-//p')
    if compile_bundle "$b" "$bree" "$OUT/$name/bree-classes" "$OUT/$name/javac-bree.log"; then
      log "bree OK $name (--release $bree)"
    else
      log "bree FAILED $name (--release $bree)"; rc=5
    fi
    rm -rf "$OUT/$name/bree-classes"
  done
  return $rc
}

file_sources() { # file -> consolidation sources (ledger labels) from the first-parent history origin/master..HEAD
  git -C "$WT" log --first-parent -m --format='@@%s' --name-only origin/master..HEAD | awk '
    /^@@/ { s = substr($0, 3)
            if (match(s, /PR #[0-9]+/)) l = substr(s, RSTART, RLENGTH)
            else if (match(s, /Merge [^ ]+/)) l = substr(s, RSTART + 6, RLENGTH - 6)
            else l = "consolidation"
            next }
    NF { if (!(($0, l) in seen)) { seen[$0, l] = 1; m[$0] = ($0 in m) ? m[$0] "," l : l } }
    END { for (f in m) print f "\t" m[f] }' | sort >"$OUT/file-sources.tsv"
}

write_mapping() { # <widget>/<bundle> <classes-dir> <out-tsv>
  local rel=$1 cls=$2 out=$3 d="widgets/$1"
  local head; head=$(git -C "$WT" rev-parse HEAD)
  git -C "$WT" ls-tree -r HEAD -- "$d" | awk -F'\t' '{ split($1, a, " "); print $2 "\t" a[3] }' >"$OUT/tree.tsv"
  git -C "$WT" log --first-parent -m --format='@@%h' --name-only -- "$d" | awk '/^@@/ { c = substr($0, 3); next }
    NF && !($0 in last) { last[$0] = c } END { for (f in last) print f "\t" last[f] }' >"$OUT/last.tsv"
  mapfile -t srcs < <(bp_get "$WT/$d/build.properties" 'source[.][.]')
  {
    printf '# M3 class-to-source mapping; bundle %s; source commit %s; branch %s\n' "$d" "$head" "$BRANCH"
    printf 'class_entry\tsource_path\tsource_blob\tlast_mainline_commit\tconsolidation_sources\n'
    ( cd "$cls" && find . -name '*.class' -type f | sed 's#^\./##' | sort ) | while read -r c; do
      local outer src s
      outer=${c%%\$*}; outer=${outer%.class}.java; src="-"
      for s in "${srcs[@]}"; do [ -f "$WT/$d/${s%/}/$outer" ] && { src="$d/${s%/}/$outer"; break; }; done
      printf '%s\t%s\n' "$c" "$src"
    done | awk -F'\t' -v T="$OUT/tree.tsv" -v L="$OUT/last.tsv" -v S="$OUT/file-sources.tsv" '
      BEGIN { while ((getline l < T) > 0) { split(l, a, "\t"); blob[a[1]] = a[2] }
              while ((getline l < L) > 0) { split(l, a, "\t"); last[a[1]] = a[2] }
              while ((getline l < S) > 0) { split(l, a, "\t"); src[a[1]] = a[2] } }
      { b = ($2 in blob) ? blob[$2] : "-"; c = ($2 in last) ? last[$2] : "-"; s = ($2 in src) ? src[$2] : "master"
        print $1 "\t" $2 "\t" b "\t" c "\t" s }'
  } >"$out"
}

do_jar() {
  local b d name ver base jarname head w
  head=$(git -C "$WT" rev-parse HEAD)
  [ -z "$(git -C "$WT" status --porcelain -- widgets)" ] || { log "jar: widgets/ has uncommitted changes"; exit 8; }
  file_sources
  for b in "${NEBULA_BUNDLES[@]}"; do
    name=${b#*/}; d="$WT/widgets/$b"; w="$OUT/$name"
    rm -rf "$w/stage"; mkdir -p "$w/stage/META-INF/m3"; cp -r "$w/classes/." "$w/stage/"
    mapfile -t srcs < <(bp_get "$d/build.properties" 'source[.][.]')
    local s; for s in "${srcs[@]}"; do ( cd "$d/${s%/}" && find . -type f ! -name '*.java' ! -name 'package.html' | while read -r f; do
        mkdir -p "$w/stage/$(dirname "$f")" && cp "$f" "$w/stage/$f"; done ); done
    local inc; while read -r inc; do # only plain relative names inside the bundle
      inc=${inc%/}
      case "$inc" in ''|.|META-INF) continue ;; esac
      if [[ ! "$inc" =~ ^[A-Za-z0-9_][A-Za-z0-9_./-]*$ || "$inc" == *..* ]]; then log "skip bin.includes '$inc' in $name"; continue; fi
      [ -e "$d/$inc" ] && cp -r "$d/$inc" "$w/stage/"
    done < <(bp_get "$d/build.properties" 'bin[.]includes')
    write_mapping "$b" "$w/classes" "$w/stage/META-INF/m3/MAPPING.tsv"
    ver=$(tr -d '\r' <"$d/META-INF/MANIFEST.MF" | sed -n 's/^Bundle-Version: *//p'); base=${ver%.qualifier}
    { tr -d '\r' <"$d/META-INF/MANIFEST.MF" | sed "s/^Bundle-Version: .*/Bundle-Version: $base.$QUALIFIER/" | sed '/^$/d'
      printf 'Implementation-Version: %s %s\n' "$BRANCH" "$head"
      printf 'M3-Build: nebula-superset-%s\n' "$STAMP"
      printf 'M3-Source-Repository: https://github.com/hsoliwal/nebula\n'
      printf 'M3-Source-Branch: %s\n' "$BRANCH"
      printf 'M3-Source-Commit: %s\n' "$head"
      printf 'M3-Source-Bundle: widgets/%s\n' "$b"
      printf 'M3-Mapping: META-INF/m3/MAPPING.tsv\n'
      printf 'M3-Mapping-Classes: %s\n' "$(($(wc -l <"$w/stage/META-INF/m3/MAPPING.tsv") - 2))"
      printf 'M3-Consolidation-Ledger: %s\n' "$LEDGER_DOC"
      printf 'M3-Compile-SWT: %s\n' "$(basename "$SWT_JAR")"
    } >"$w/MANIFEST.MF"
    jarname="$name-$base-m3-$STAMP.jar"; rm -f "$DIST/$jarname"
    "$JDK/bin/jar" --create --file "$DIST/$jarname" --manifest "$w/MANIFEST.MF" -C "$w/stage" .
    log "jar $jarname ($(find "$w/classes" -name '*.class' | wc -l) classes)"
  done
  ( cd "$DIST" && sha256sum *-m3-$STAMP.jar | grep -v 'org.eclipse.swt' >SHA256SUMS.txt && cat SHA256SUMS.txt )
  { printf 'jar\tclass_entry\tsource_path\tsource_blob\tlast_mainline_commit\tconsolidation_sources\n'
    for b in "${NEBULA_BUNDLES[@]}"; do
      name=${b#*/}; base=$(tr -d '\r' <"$WT/widgets/$b/META-INF/MANIFEST.MF" | sed -n 's/^Bundle-Version: *//p'); base=${base%.qualifier}
      tail -n +3 "$OUT/$name/stage/META-INF/m3/MAPPING.tsv" | sed "s#^#$name-$base-m3-$STAMP.jar\t#"
    done; } >"$DIST/MAPPING.tsv"
}

do_tests() { # compile the test bundles of the touched modules
  memory_gate
  local t name rc=0 tcp
  tcp="$(base_cp);$(bundle_cp)"
  for t in "${TEST_DEPS[@]}"; do tcp="$tcp;$(cygpath -m "$t")"; done
  printf 'bundle\tsource\treason\n' >"$OUT/tests/NOT_COMPILED.tsv"
  for t in "${TEST_BUNDLES[@]}"; do
    name=${t#*/}; mkdir -p "$OUT/tests/$name"; rm -rf "$OUT/tests/$name/classes"; mkdir -p "$OUT/tests/$name/classes"
    sources_of "$WT/widgets/$t" "$OUT/tests/$name/sources.all.txt"
    # SWTBot is not in the local repository: such sources are recorded, not compiled
    : >"$OUT/tests/$name/sources.txt"
    local s; while read -r s; do
      if grep -q 'org\.eclipse\.swtbot' "${s//\"/}"; then
        printf '%s\t%s\tSWTBot (org.eclipse.swtbot.go) not available locally\n' "$name" "${s//\"/}" >>"$OUT/tests/NOT_COMPILED.tsv"
      else
        printf '%s\n' "$s" >>"$OUT/tests/$name/sources.txt"
      fi
    done <"$OUT/tests/$name/sources.all.txt"
    local bcp="$tcp" j; for j in "$WT/widgets/$t"/*.jar; do [ -f "$j" ] && bcp="$bcp;$j"; done  # Bundle-ClassPath jars
    if "$JDK/bin/javac" -J-Xmx1g --release 21 -encoding UTF-8 -proc:none -g -Xmaxwarns 100000 -Xmaxerrs 100000 \
         -cp "$bcp" -d "$OUT/tests/$name/classes" @"$OUT/tests/$name/sources.txt" >"$OUT/tests/$name/javac.log" 2>&1; then
      log "test-compile OK $name ($(wc -l <"$OUT/tests/$name/sources.txt") sources)"
    else
      log "test-compile FAILED $name"; tail -20 "$OUT/tests/$name/javac.log" >&2; rc=5
    fi
    ( cd "$WT/widgets/$t" && find src -type f ! -name '*.java' 2>/dev/null | while read -r f; do
        mkdir -p "$OUT/tests/$name/classes/$(dirname "${f#src/}")" && cp "$f" "$OUT/tests/$name/classes/${f#src/}"; done ) || true
  done
  return $rc
}

do_run_tests() { # headless JUnit 4: classes with @Test and no Display/Shell/widgets reference; the rest NOT_RUN
  memory_gate
  local t name f cls tcp status
  tcp="$(base_cp);$(bundle_cp)"
  for t in "${TEST_DEPS[@]}"; do tcp="$tcp;$(cygpath -m "$t")"; done
  printf 'bundle\ttest_class\tstatus\ttests\tfailures\tevidence\n' >"$OUT/tests/RESULTS.tsv"
  for t in "${TEST_BUNDLES[@]}"; do
    name=${t#*/}
    while read -r f; do
      cls=$(sed -n 's/^package \([^;]*\);.*/\1/p' "$f" | head -1).$(basename "$f" .java)
      if grep -qE 'org\.eclipse\.swt\.widgets|\bDisplay\b|\bShell\b|swtbot' "$f"; then
        printf '%s\t%s\tNOT_RUN\t-\t-\tneeds a Display (headless run only)\n' "$name" "$cls" >>"$OUT/tests/RESULTS.tsv"; continue
      fi
      local lg="$OUT/tests/$name/$cls.log"
      if "$JDK/bin/java" -Xmx1g -Djava.awt.headless=true -cp "$tcp;$OUT/tests/$name/classes" org.junit.runner.JUnitCore "$cls" >"$lg" 2>&1; then
        status=PASS
      else
        status=FAIL
      fi
      local n fl
      n=$(tr -d '\r' <"$lg" | sed -n 's/^OK (\([0-9]*\) tests\{0,1\})$/\1/p; s/^Tests run: \([0-9]*\),.*/\1/p' | tail -1)
      fl=$(tr -d '\r' <"$lg" | sed -n 's/^Tests run: [0-9]*,  Failures: \([0-9]*\)$/\1/p' | tail -1)
      printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$name" "$cls" "$status" "${n:-0}" "${fl:-0}" "$(basename "$lg")" >>"$OUT/tests/RESULTS.tsv"
      log "test $status $cls (${n:-0} tests, ${fl:-0} failures)"
    done < <(grep -rl '@Test' "$WT/widgets/$t/src" --include='*.java' | sort)
  done
  column -t -s $'\t' "$OUT/tests/RESULTS.tsv" >&2 || true
}

case "${1:-all}" in
  compile) do_compile ;;
  bree) do_bree ;;
  jar) do_jar ;;
  tests) do_tests ;;
  run-tests) do_run_tests ;;
  all) do_compile; do_bree || true; do_tests || true; do_run_tests; do_jar ;;
  *) sed -n '2,12p' "$0"; exit 64 ;;
esac
