#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
testdir="$root/tests"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac --release 21 -Xlint:all -Werror -d "$work" "$testdir/BadgedLabelGcOwnershipProof.java"
java -Xmx128m -cp "$work" BadgedLabelGcOwnershipProof
java -Xint -Xmx128m -cp "$work" BadgedLabelGcOwnershipProof
