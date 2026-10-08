#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
set -euo pipefail

ROOT="${1:-$(pwd)}"
DEST="${2:-${RUNNER_TEMP:-/tmp}/com.synexia-pure-int}"
BINDING="$ROOT/m3/catalogue/pure-int-recipe-binding.tsv"
RECEIVER="$ROOT/m3/catalogue/pure-int-public-receiver.tsv"

[[ -f "$BINDING" ]] || { echo "missing pure-int recipe binding: $BINDING" >&2; exit 1; }
[[ -f "$RECEIVER" ]] || { echo "missing public recipe receiver binding: $RECEIVER" >&2; exit 1; }

mapfile -t rows < <(tail -n +2 "$BINDING" | sed '/^[[:space:]]*$/d')
[[ "${#rows[@]}" -eq 1 ]] || {
  echo "expected exactly one pure-int recipe binding row" >&2
  exit 1
}

IFS=$'\t' read -r   schema repository upstream_branch upstream_commit upstream_pr   artifact entrypoint license target_profile local_owner local_owner_state   source_mutation promotion hosted_proof <<< "${rows[0]}"

[[ "$schema" == "M3_NEBULA_CANONICAL_RECIPE_BINDING_V1" ]] || { echo "binding schema drift" >&2; exit 1; }
[[ "$repository" == "hsoliwal/com.synexia" ]] || { echo "binding repository drift" >&2; exit 1; }
[[ "$upstream_branch" == "develop" ]] || { echo "binding branch drift" >&2; exit 1; }
[[ "$upstream_commit" == "dcc967a005edb47fd45e1aecc710339b236f28ea" ]] || { echo "binding commit drift" >&2; exit 1; }
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

mapfile -t receiver_rows < <(tail -n +2 "$RECEIVER" | sed '/^[[:space:]]*$/d')
[[ "${#receiver_rows[@]}" -eq 1 ]] || {
  echo "expected exactly one public receiver row" >&2
  exit 1
}

IFS=$'\t' read -r   receiver_schema receiver_repository receiver_pr receiver_commit receiver_module   receiver_artifact receiver_entrypoint receiver_license receiver_authority <<< "${receiver_rows[0]}"

[[ "$receiver_schema" == "M3_NEBULA_PUBLIC_RECIPE_RECEIVER_V1" ]] || { echo "receiver schema drift" >&2; exit 1; }
[[ "$receiver_repository" == "hsoliwal/M3jdk21" ]] || { echo "receiver repository drift" >&2; exit 1; }
[[ "$receiver_pr" == "437" ]] || { echo "receiver PR drift" >&2; exit 1; }
[[ "$receiver_commit" == "9c7d6121d342e35c8b008557812d41498359c3f7" ]] || { echo "receiver commit drift" >&2; exit 1; }
[[ "$receiver_module" == "m3/synexia-import/pure-int-recipe-custody/pom.xml" ]] || { echo "receiver module drift" >&2; exit 1; }
[[ "$receiver_artifact" == "$artifact" ]] || { echo "receiver artifact mismatch" >&2; exit 1; }
[[ "$receiver_entrypoint" == "$entrypoint" ]] || { echo "receiver entrypoint mismatch" >&2; exit 1; }
[[ "$receiver_license" == "$license" ]] || { echo "receiver license mismatch" >&2; exit 1; }
[[ "$receiver_authority" == "CUSTODY_ONLY" ]] || { echo "receiver authority drift" >&2; exit 1; }

rm -rf "$DEST"

TOKEN="${M3_SYNEXIA_TOKEN:-${GH_TOKEN:-}}"
if [[ -n "$TOKEN" ]]; then
  export GH_TOKEN="$TOKEN"
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
  source_mode="CANONICAL_PRIVATE"
else
  git init "$DEST"
  git -C "$DEST" remote add origin "https://github.com/$receiver_repository.git"
  git -C "$DEST" sparse-checkout init --cone
  git -C "$DEST" sparse-checkout set m3/synexia-import/pure-int-recipe-custody
  git -C "$DEST" fetch --depth 1 --filter=blob:none origin "refs/pull/$receiver_pr/head"
  git -C "$DEST" checkout --detach FETCH_HEAD
  [[ "$(git -C "$DEST" rev-parse HEAD)" == "$receiver_commit" ]] || {
    echo "checked-out public receiver commit does not match binding" >&2
    exit 1
  }

  SOURCE="$DEST/m3/synexia-import/pure-int-recipe-custody/SOURCE.tsv"
  [[ -f "$SOURCE" ]] || { echo "public receiver source receipt missing" >&2; exit 1; }
  grep -F $'hsoliwal/com.synexia\t9497\tdcc967a005edb47fd45e1aecc710339b236f28ea\t' "$SOURCE" >/dev/null

  mvn -B -ntp -f "$DEST/$receiver_module" clean verify install
  source_mode="PUBLIC_M3JDK21_CUSTODY"
fi

LOCAL_JAR="$HOME/.m2/repository/com/synexia/synexia-openrewrite-recipes/1.0.0-SNAPSHOT/synexia-openrewrite-recipes-1.0.0-SNAPSHOT.jar"
[[ -s "$LOCAL_JAR" ]] || { echo "bound Synexia recipe artifact was not installed" >&2; exit 1; }

jar tf "$LOCAL_JAR" | grep -Fx 'com/synexia/rewrite/M3PureIntConvergenceRecipe.class' >/dev/null

printf 'BOUND_RECIPE_INSTALLED\t%s\t%s\t%s\t%s\t%s\n'   "$source_mode" "$repository" "$upstream_commit" "$artifact" "$entrypoint"
