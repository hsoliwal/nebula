#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# Explicit application is separate from the unchanged default analysis lane.
if [[ "${1:-}" == "--apply-catalogue" ]]; then
  if [[ "$#" -ne 3 ]]; then
    printf 'usage: %s --apply-catalogue PLAN_JSON OUTPUT_DIRECTORY\n' "$0" >&2
    exit 2
  fi
  exec python3 "$ROOT/m3/convergence/apply_catalogue.py" \
    --root "$ROOT" --plan "$2" --out "$3"
fi
SYNEXIA_ROOT="${SYNEXIA_ROOT:?set SYNEXIA_ROOT to the hsoliwal/com.synexia checkout}"
RECIPE_COMMIT="78d1c67fbc97bb4831f0176f2a8b034ecc994eee"
RECIPE_ROOT="$SYNEXIA_ROOT/synexia-maven-plugin/recipes/hierarchical-atom-pattern"
REPOSITORY="${M3_REPOSITORY:-hsoliwal/nebula}"
REVISION="${M3_REVISION:-$(git -C "$ROOT" rev-parse HEAD)}"

command -v git >/dev/null
command -v java >/dev/null
command -v mvn >/dev/null

test -f "$RECIPE_ROOT/pom.xml"
test -x "$RECIPE_ROOT/verify.sh"

git -C "$SYNEXIA_ROOT" cat-file -e "$RECIPE_COMMIT^{commit}"
git -C "$SYNEXIA_ROOT" merge-base --is-ancestor "$RECIPE_COMMIT" HEAD

git -C "$ROOT" diff --check

bash "$RECIPE_ROOT/verify.sh" --jni
mvn -B -ntp -f "$RECIPE_ROOT/pom.xml" install

# Install the canonical OpenRewrite recipe pack and execute the live Nebula
# Maven/Tycho source-model catalogue. The active recipe is read-only.
mvn -B -ntp -f "$SYNEXIA_ROOT/pom.xml" \
  -pl synexia-openrewrite-recipes -am \
  install -DskipTests
mvn -B -ntp -f "$ROOT/pom.xml" \
  -Pm3-atomize-patternize \
  rewrite:dryRunNoFork

mvn -B -ntp -f "$ROOT/m3/recipe-first/pom.xml" verify
INV_OUT="$(mktemp -d "${TMPDIR:-/tmp}/nebula-m3-inventory.XXXXXX")"
mvn -B -ntp -f "$ROOT/m3/recipe-first/pom.xml" -Pinventory \
  -Dm3.nebula.root="$ROOT" \
  -Dm3.nebula.out="$INV_OUT" \
  verify
grep -F $'failureRows\t0' "$INV_OUT/nebula-m3-summary.tsv"

OUT1="$(mktemp -d "${TMPDIR:-/tmp}/nebula-m3-hierarchy-1.XXXXXX")"
OUT2="$(mktemp -d "${TMPDIR:-/tmp}/nebula-m3-hierarchy-2.XXXXXX")"
run_hierarchy() {
  local out="$1"
  mvn -B -ntp -f "$ROOT/.m3/atom-pattern/pom.xml" verify \
    -Dm3.repository="$REPOSITORY" \
    -Dm3.revision="$REVISION" \
    -Dm3.output="$out"
}
run_hierarchy "$OUT1"
run_hierarchy "$OUT2"
diff -ru "$OUT1" "$OUT2"

git -C "$ROOT" diff --exit-code
git -C "$ROOT" diff --cached --exit-code

mvn -V -B -f "$ROOT/pom.xml" clean verify -Dtycho.localArtifacts=ignore
