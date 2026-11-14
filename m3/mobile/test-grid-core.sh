#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac --release 17 -Xlint:all -d "$OUT" \
  "$ROOT/widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/internal/mobile/MobileGridViewport.java" \
  "$ROOT/m3/mobile/grid-tests/MobileGridViewportTest.java"
java -ea -cp "$OUT" MobileGridViewportTest
