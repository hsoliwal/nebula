#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
testdir="$root/../tests/org/eclipse/nebula/widgets/chips"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" "$testdir/ChipsCloseCenterJniProof.java"
java_home="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror -fPIC -shared \
    -I"$java_home/include" -I"$java_home/include/linux" \
    "$testdir/ChipsCloseCenterJniProof.c" -o "$work/libm3_chips_close_center_test.so"
java -Xmx64m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
    org.eclipse.nebula.widgets.chips.ChipsCloseCenterJniProof
java -Xint -Xmx64m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
    org.eclipse.nebula.widgets.chips.ChipsCloseCenterJniProof --quick
gcc -std=c17 -O2 -Wall -Wextra -Wconversion -Wpedantic -Werror -fPIC -shared \
    -fsanitize=undefined -fno-sanitize-recover=undefined \
    -I"$java_home/include" -I"$java_home/include/linux" \
    "$testdir/ChipsCloseCenterJniProof.c" -o "$work/libm3_chips_close_center_test.so"
UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 \
java -Xmx64m -Xcheck:jni -Djava.library.path="$work" -cp "$work" \
    org.eclipse.nebula.widgets.chips.ChipsCloseCenterJniProof
