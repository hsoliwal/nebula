#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
src="$root/tests/org/eclipse/nebula/widgets/tiles"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" \
  "$src/TilesHitGridProof.java" "$src/TilesHitGridJniProof.java"
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror \
  -fPIC -shared -I"$java_home/include" -I"$java_home/include/linux" \
  "$src/TilesHitGridJniProof.c" -o "$work/libm3_tiles_hit_grid_oracle.so"
java -Xmx256m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.tiles.TilesHitGridJniProof
java -Xint -Xmx256m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.tiles.TilesHitGridJniProof --quick
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror \
  -fsanitize=undefined -fno-sanitize-recover=undefined -fPIC -shared \
  -I"$java_home/include" -I"$java_home/include/linux" \
  "$src/TilesHitGridJniProof.c" -o "$work/libm3_tiles_hit_grid_oracle.so"
UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 java -Xmx256m -Xcheck:jni \
  -Djava.library.path="$work" -cp "$work" org.eclipse.nebula.widgets.tiles.TilesHitGridJniProof
