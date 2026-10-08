#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
set -euo pipefail

ROOT="${1:-$(pwd)}"
DEST="${2:-${RUNNER_TEMP:-/tmp}/com.synexia-pure-int}"
BINDING="$ROOT/m3/catalogue/pure-int-recipe-binding.tsv"

[[ -f "$BINDING" ]] || { echo "missing pure-int recipe binding: $BINDING" >&2; exit 1; }
[[ -n "${GH_TOKEN:-}" ]] || { echo "M3_SYNEXIA_TOKEN/GH_TOKEN is required for bound recipe proof" >&2; exit 1; }

mapfile -t rows < <(tail -n +2 "$BINDING" | sed '/^[[:space:]]*$/d')
[[ "${#rows[@]}" -eq 1 ]] || {
  echo "expected exactly one pure-int recipe binding row" >&2
  exit 1
}

IFS=$'\t' read -r   schema repository upstream_branch upstream_commit upstream_pr   artifact entrypoint license target_profile local_owner local_owner_state   source_mutation promotion hosted_proof <<< "${rows[0]}"

[[ "$schema" == "M3_NEBULA_CANONICAL_RECIPE_BINDING_V1" ]] || { echo "binding schema drift" >&2; exit 1; }
[[ "$repository" == "hsoliwal/com.synexia" ]] || { echo "binding repository drift" >&2; exit 1; }
[[ "$upstream_branch" == "develop" ]] || { echo "binding branch drift" >&2; exit 1; }
[[ "$upstream_commit" =~ ^[0-9a-f]{40}$ ]] || { echo "binding commit invalid" >&2; exit 1; }
[[ "$upstream_pr" == "9497" ]] || { echo "binding PR drift" >&2; exit 1; }
[[ "$artifact" == "com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT" ]] || {
  echo "binding artifact drift" >&2
  exit 1
}
[[ "$entrypoint" == "com.synexia.rewrite.M3PureIntConvergenceRecipe" ]] || {
  echo "binding entrypoint drift" >&2
  exit 1
}
[[ "$license" == "Apache-2.0" ]] || { echo "binding license drift" >&2; exit 1; }
[[ "$target_profile" == "m3-local-file-convergence" ]] || { echo "binding profile drift" >&2; exit 1; }
[[ "$source_mutation" == "CANDIDATE_ONLY" ]] || { echo "binding mutation authority drift" >&2; exit 1; }
[[ "$promotion" == "false" ]] || { echo "binding promotion authority drift" >&2; exit 1; }

rm -rf "$DEST"
gh repo clone "$repository" "$DEST"
git -C "$DEST" checkout --detach "$upstream_commit"
[[ "$(git -C "$DEST" rev-parse HEAD)" == "$upstream_commit" ]] || {
  echo "checked-out Synexia commit does not match binding" >&2
  exit 1
}

(
  cd "$DEST"
  ./mvnw -B -ntp -pl synexia-openrewrite-recipes -am -DskipTests install
)

LOCAL_JAR="$HOME/.m2/repository/com/synexia/synexia-openrewrite-recipes/1.0.0-SNAPSHOT/synexia-openrewrite-recipes-1.0.0-SNAPSHOT.jar"
[[ -s "$LOCAL_JAR" ]] || { echo "bound Synexia recipe artifact was not installed" >&2; exit 1; }

printf 'BOUND_RECIPE_INSTALLED\t%s\t%s\t%s\t%s\n'   "$repository" "$upstream_commit" "$artifact" "$entrypoint"
