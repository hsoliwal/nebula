#!/usr/bin/env bash
# SPDX-License-Identifier: EPL-2.0
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
src="$root/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal/mobile/MobileGridViewport.java"
proof="$root/releng/verification/mobile-grid-viewport/WindowReuseProof.java"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac --release 17 -Xlint:all -Werror -d "$out" "$src" "$proof"
java -ea -cp "$out" org.eclipse.nebula.widgets.grid.internal.mobile.WindowReuseProof
