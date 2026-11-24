#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Require the source-built GTK x86_64 fragment as the only SWT platform fragment on the AllTests classpath.

In run 45433289163 the -am reactor of the host-test step also built
org.eclipse.swt.gtk.linux.aarch64. Its jar was surefire classpath entry 7, ahead
of the x86_64 fragment (entry 138), so org.eclipse.swt.internal.Library loaded from a
jar with SWT-Arch: aarch64 and Platform.exitIfNotLoadable() ended the test JVM.

The receiver's host-test step writes the test bundle's resolved classpath with
maven-dependency-plugin build-classpath before any test runs, then calls this check.
"""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

HOST_FRAGMENT = "org.eclipse.swt.gtk.linux.x86_64"
FRAGMENT = re.compile(r"org\.eclipse\.swt\.(?:gtk|win32|cocoa)\.[a-z0-9]+\.[a-z0-9_]+")
# The receiver runs on Linux, where Maven joins classpath entries with ':'.
SEPARATOR = ":"


def check(classpath: str, native_dir: str) -> str:
    """Return the single host fragment entry, or raise ValueError."""
    entries = [entry for entry in classpath.strip().split(SEPARATOR) if entry]
    if len(entries) < 2:
        raise ValueError("Empty or truncated SWT test classpath")
    fragments = [entry for entry in entries if FRAGMENT.search(entry)]
    if len(fragments) != 1:
        raise ValueError("Expected exactly one SWT platform fragment on the test classpath, found %d: %s"
                         % (len(fragments), ", ".join(fragments)))
    entry = fragments[0]
    if set(FRAGMENT.findall(entry)) != {HOST_FRAGMENT}:
        raise ValueError("The SWT platform fragment on the test classpath is not " + HOST_FRAGMENT + ": " + entry)
    # Resolve ".." and symbolic links; the fragment must be the source-built one.
    root, resolved = os.path.realpath(native_dir), os.path.realpath(entry)
    if os.path.commonpath([root, resolved]) != root:
        raise ValueError("The " + HOST_FRAGMENT + " fragment on the test classpath is not the source-built one: " + entry)
    return entry


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: verify_test_classpath.py CLASSPATH_FILE SOURCE_BUILT_FRAGMENT_DIR")
    entry = check(Path(sys.argv[1]).read_text(encoding="utf-8"), sys.argv[2])
    print("SWT fragment on test classpath:", HOST_FRAGMENT, entry)
    print("SWT_TEST_CLASSPATH_HOST_FRAGMENT=PASS")


if __name__ == "__main__":
    main()
