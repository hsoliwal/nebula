#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
source="$root/tests/org/eclipse/nebula/widgets/tiles/TilesMutationEpochProof.java"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" "$source"
java -Xmx256m -cp "$work" org.eclipse.nebula.widgets.tiles.TilesMutationEpochProof
java -Xint -Xmx256m -cp "$work" org.eclipse.nebula.widgets.tiles.TilesMutationEpochProof --quick
