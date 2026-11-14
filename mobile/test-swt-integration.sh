#!/usr/bin/env bash
set -euo pipefail
NEBULA="$(cd "$(dirname "$0")/.." && pwd)"
SWT="${1:?pass SWT checkout path as first argument}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac --release 21 -Xlint:all -d "$OUT" \
  "$SWT/bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/internal/mobile/MobileWidgetRuntime.java" \
  "$NEBULA/mobile/src/org/eclipse/nebula/widgets/grid/internal/mobile/MobileGridViewport.java" \
  "$NEBULA/mobile/src/org/eclipse/nebula/widgets/grid/internal/mobile/MobileGridViewBinding.java" \
  "$NEBULA/mobile/tests/MobileGridViewBindingTest.java"
java -ea -cp "$OUT" MobileGridViewBindingTest
