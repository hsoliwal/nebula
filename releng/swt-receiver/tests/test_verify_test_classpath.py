#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""The SWT AllTests classpath guard, run against the classpath recorded by run 45433289163."""
import contextlib
import importlib.util
import io
import tempfile
import unittest
from pathlib import Path
from unittest import mock

HERE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("guard", HERE / "verify_test_classpath.py")
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)

# surefire.test.class.path of the forked JVM in run 45433289163 (148 entries), verbatim.
RECORDED = Path(__file__).resolve().parent / "data/run-45433289163-surefire-test-classpath.txt"
WORK = "/home/runner/work/nebula/nebula/.m3-swt/binaries/"
NATIVE = WORK + "org.eclipse.swt.gtk.linux.x86_64"
X86_64 = NATIVE + "/target/org.eclipse.swt.gtk.linux.x86_64-3.138.0-SNAPSHOT.jar"
AARCH64 = WORK + "org.eclipse.swt.gtk.linux.aarch64/target/org.eclipse.swt.gtk.linux.aarch64-3.138.0-SNAPSHOT.jar"
P2 = "/home/runner/.m2/repository/p2/osgi/bundle/"
UPSTREAM_AARCH64 = (P2 + "org.eclipse.swt.gtk.linux.aarch64/3.137.0.v20261112-2300/"
                    "org.eclipse.swt.gtk.linux.aarch64-3.137.0.v20261112-2300.jar")
DOWNLOADED_X86_64 = (P2 + "org.eclipse.swt.gtk.linux.x86_64/3.137.0.v20261112-2300/"
                     "org.eclipse.swt.gtk.linux.x86_64-3.137.0.v20261112-2300.jar")


class TestClasspathGuardTest(unittest.TestCase):
    def setUp(self):
        self.entries = RECORDED.read_text(encoding="utf-8").strip().split(":")

    def classpath(self, replace=None, drop=()):
        replace = replace or {}
        return ":".join(replace.get(entry, entry) for entry in self.entries if entry not in drop)

    def test_the_recorded_run_has_aarch64_ahead_of_x86_64(self):
        self.assertEqual(len(self.entries), 148)
        self.assertEqual((self.entries.index(AARCH64), self.entries.index(X86_64)), (7, 138))

    def test_the_recorded_run_classpath_is_refused(self):
        with self.assertRaisesRegex(ValueError, "exactly one SWT platform fragment .* found 2"):
            guard.check(self.classpath(), NATIVE)

    def test_the_host_only_classpath_passes(self):
        self.assertEqual(guard.check(self.classpath(drop={AARCH64}), NATIVE), X86_64)

    def test_a_classpath_without_a_platform_fragment_is_refused(self):
        with self.assertRaisesRegex(ValueError, "found 0"):
            guard.check(self.classpath(drop={AARCH64, X86_64}), NATIVE)

    def test_an_upstream_p2_aarch64_fragment_is_refused(self):
        with self.assertRaisesRegex(ValueError, "found 2"):
            guard.check(self.classpath(replace={AARCH64: UPSTREAM_AARCH64}), NATIVE)
        with self.assertRaisesRegex(ValueError, "not org.eclipse.swt.gtk.linux.x86_64"):
            guard.check(self.classpath(replace={AARCH64: UPSTREAM_AARCH64}, drop={X86_64}), NATIVE)

    def test_a_downloaded_x86_64_fragment_is_refused(self):
        with self.assertRaisesRegex(ValueError, "not the source-built one"):
            guard.check(self.classpath(replace={X86_64: DOWNLOADED_X86_64}, drop={AARCH64}), NATIVE)

    def test_a_parent_directory_escape_is_refused(self):
        escape = NATIVE + "/../../../../../tmp/org.eclipse.swt.gtk.linux.x86_64-3.138.0-SNAPSHOT.jar"
        with self.assertRaisesRegex(ValueError, "not the source-built one"):
            guard.check(self.classpath(replace={X86_64: escape}, drop={AARCH64}), NATIVE)

    def test_an_empty_classpath_is_refused(self):
        for classpath in ("", "\n", X86_64):
            with self.subTest(classpath=classpath), self.assertRaisesRegex(ValueError, "Empty or truncated"):
                guard.check(classpath, NATIVE)

    def test_command_line_prints_the_marker_only_on_success(self):
        with tempfile.TemporaryDirectory() as root:
            for classpath, passes in ((self.classpath(drop={AARCH64}), True), (self.classpath(), False)):
                path = Path(root) / "m3-test-classpath.txt"
                path.write_text(classpath + "\n", encoding="utf-8")
                output = io.StringIO()
                with self.subTest(passes=passes), contextlib.redirect_stdout(output), \
                        mock.patch.object(guard.sys, "argv", ["verify_test_classpath.py", str(path), NATIVE]):
                    if passes:
                        guard.main()
                        self.assertIn("SWT_TEST_CLASSPATH_HOST_FRAGMENT=PASS", output.getvalue())
                    else:
                        with self.assertRaises(ValueError):
                            guard.main()
                        self.assertNotIn("PASS", output.getvalue())


if __name__ == "__main__":
    unittest.main()
