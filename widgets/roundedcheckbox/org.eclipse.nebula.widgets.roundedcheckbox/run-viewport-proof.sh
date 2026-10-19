#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
proof="$root/tests/org/eclipse/nebula/widgets/roundedcheckbox"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
javac --release 21 -Xlint:all -Werror -d "$work" \
  "$proof/RoundedCheckboxPaintScopeProof.java" \
  "$proof/RoundedCheckboxGeometryJniProof.java"
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror -fPIC -shared \
  -I"$java_home/include" -I"$java_home/include/linux" \
  "$proof/RoundedCheckboxGeometryJniProof.c" \
  -o "$work/libm3_rounded_checkbox_coordinates.so"
java -Xmx128m -cp "$work" org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckboxPaintScopeProof
java -Xint -Xmx128m -cp "$work" org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckboxPaintScopeProof
java -Xmx128m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckboxGeometryJniProof
java -Xint -Xmx128m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckboxGeometryJniProof
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror -fPIC -shared \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$java_home/include" -I"$java_home/include/linux" \
  "$proof/RoundedCheckboxGeometryJniProof.c" \
  -o "$work/libm3_rounded_checkbox_coordinates.so"
UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 java -Xmx128m -Xcheck:jni \
  -Djava.library.path="$work" -cp "$work" \
  org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckboxGeometryJniProof
