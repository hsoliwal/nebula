#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac --release 21 -Xlint:all -d "$OUT" \
  "$ROOT/mobile/src/org/eclipse/nebula/widgets/grid/internal/mobile/MobileGridViewport.java" \
  "$ROOT/mobile/tests/MobileGridViewportTest.java"
java -ea -cp "$OUT" MobileGridViewportTest
