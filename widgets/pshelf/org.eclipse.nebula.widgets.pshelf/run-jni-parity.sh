#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
source_dir="$root/src/org/eclipse/nebula/widgets/pshelf"
tests="$root/tests/org/eclipse/nebula/widgets/pshelf"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

javac --release 21 -Xlint:all -Werror -d "$work" \
  "$source_dir/PShelfViewportGeometry.java" "$tests/PShelfGeometryJniParityTest.java"
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror -fPIC -shared \
  -I"$java_home/include" -I"$java_home/include/linux" \
  "$tests/PShelfGeometryJniParityTest.c" -lm \
  -o "$work/libm3_pshelf_geometry_test.so"

java -Xcheck:jni -Xmx256m -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.pshelf.PShelfGeometryJniParityTest
java -Xint -Xcheck:jni -Xmx256m -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.pshelf.PShelfGeometryJniParityTest
