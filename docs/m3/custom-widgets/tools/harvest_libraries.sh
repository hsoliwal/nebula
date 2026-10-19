#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# CPU-first listing of widget libraries: GitHub metadata plus the Java file tree of each library
# (gh api, no clones). Usage: harvest_libraries.sh <libs.tsv: slug, subpaths, toolkit, source> <cache-dir>
# Writes <cache-dir>/meta.tsv (slug, spdx, stars, pushed, branch, archived, truncated, java_files, toolkit, source)
# and <cache-dir>/classes/<owner>__<repo>.txt (one .java path per line, tests/examples/demos removed).
set -u
LIBS=$1; CACHE=$2; mkdir -p "$CACHE/classes"
META=$CACHE/meta.tsv; : > "$META"
SKIP='(^|/)(test|tests|testing|junit|example|examples|demo|demos|sample|samples|snippet|snippets|benchmark|benchmarks|jmh|it)(/|$)|Test[A-Za-z0-9_]*\.java$|Tests?\.java$|Demo[A-Za-z0-9_]*\.java$|Example[A-Za-z0-9_]*\.java$|Snippet[0-9]*\.java$'
tail -n +2 "$LIBS" | while IFS=$'\t' read -r slug subpaths toolkit source; do
  [ -z "$slug" ] && continue
  out=$CACHE/classes/${slug/\//__}.txt
  m=$(gh api "repos/$slug" --jq '[.full_name, (.license.spdx_id // "NONE"), .stargazers_count, .pushed_at[0:10], .default_branch, .archived] | @tsv' 2>/dev/null)
  if [ -z "$m" ]; then printf '%s\tNOT_FOUND\t0\t-\t-\t-\t-\t0\t%s\t%s\n' "$slug" "$toolkit" "$source" >> "$META"; continue; fi
  branch=$(printf '%s' "$m" | cut -f5); trunc=false; : > "$out"
  IFS=',' read -ra parts <<< "$subpaths"
  for p in "${parts[@]}"; do
    if [ "$p" = . ]; then ref=$branch; pre=; else ref="$branch:${p// /%20}"; pre="$p/"; fi
    gh api "repos/$slug/git/trees/$ref?recursive=1" --jq '"#TRUNC " + (.truncated|tostring), (.tree[] | select(.type=="blob") | .path | select(endswith(".java")))' 2>/dev/null \
      | while read -r line; do case "$line" in "#TRUNC true") echo "#TRUNC" ;; "#TRUNC false") ;; *) echo "$pre$line" ;; esac; done >> "$out"
  done
  grep -q '^#TRUNC' "$out" && trunc=true
  grep -v '^#TRUNC' "$out" | grep -Ev "$SKIP" > "$out.tmp"; mv "$out.tmp" "$out"
  printf '%s\t%s\t%s\t%s\t%s\n' "$m" "$trunc" "$(wc -l < "$out")" "$toolkit" "$source" >> "$META"
done
wc -l "$META"
