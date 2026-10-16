#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
proof="$root/tests/org/eclipse/nebula/widgets/tiles"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" "$proof/TilesHitGridProof.java"
java -Xmx64m -cp "$work" org.eclipse.nebula.widgets.tiles.TilesHitGridProof
