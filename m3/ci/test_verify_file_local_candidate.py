#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Adversarial unit tests for the read-only Nebula OpenRewrite candidate-scope gate."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("verify-file-local-candidate.py")
SPEC = importlib.util.spec_from_file_location("m3_candidate_scope", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
gate = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(gate)


class CandidateScopeTest(unittest.TestCase):
    def test_only_modified_tracked_java_is_file_local(self) -> None:
        self.assertEqual(
            ("widgets/a/A.java", "m3/recipe-first/src/main/java/A.java"),
            gate.candidate_paths(
                b"M\0widgets/a/A.java\0M\0m3/recipe-first/src/main/java/A.java\0"
            ),
        )
        self.assertEqual((), gate.candidate_paths(b""))

    def test_add_remove_copy_rename_and_type_change_are_not_file_local(self) -> None:
        for status in ("A", "D", "C100", "R100", "T", "U"):
            with self.subTest(status=status):
                with self.assertRaises(ValueError):
                    gate.candidate_paths(
                        status.encode("ascii") + b"\0widgets/a/A.java\0"
                    )

    def test_non_java_paths_are_not_file_local(self) -> None:
        for path in ("pom.xml", "m3/recipe-first/rewrite.yml", "README.md"):
            with self.subTest(path=path):
                with self.assertRaises(ValueError):
                    gate.candidate_paths(b"M\0" + path.encode() + b"\0")

    def test_noncanonical_or_hostile_path_is_rejected(self) -> None:
        for path in (
            "../outside.java",
            "/absolute/A.java",
            "./relative/A.java",
            "folder//A.java",
            ".git/config.java",
            "a\\b.java",
            "a:b.java",
            "has\nnewline.java",
        ):
            with self.subTest(path=path):
                with self.assertRaises(ValueError):
                    gate.candidate_paths(b"M\0" + path.encode() + b"\0")

    def test_duplicate_incomplete_and_invalid_utf8_fail_closed(self) -> None:
        for raw in (
            b"M\0A.java\0M\0A.java\0",
            b"M\0A.java",
            b"M\0A.java\0extra\0",
            b"M\0bad\xff.java\0",
        ):
            with self.subTest(raw=raw):
                with self.assertRaises(ValueError):
                    gate.candidate_paths(raw)

    def test_git_worktree_approves_modified_source_only(self) -> None:
        with tempfile.TemporaryDirectory() as scratch:
            repo = Path(scratch)
            self.git(repo, "init", "-q")
            self.git(repo, "config", "user.email", "m3@example.invalid")
            self.git(repo, "config", "user.name", "M3 Fixture")
            file = repo / "A.java"
            file.write_text("class A {}\n", encoding="utf-8")
            self.git(repo, "add", "A.java")
            self.git(repo, "commit", "-qm", "baseline")

            self.assertEqual((), gate.review(repo))
            file.write_text("class A { int n; }\n", encoding="utf-8")
            self.assertEqual(("A.java",), gate.review(repo))

            (repo / "untracked.java").write_text("class B {}\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "untracked"):
                gate.review(repo)

    def test_git_worktree_rejects_non_java_and_deletion(self) -> None:
        with tempfile.TemporaryDirectory() as scratch:
            repo = Path(scratch)
            self.git(repo, "init", "-q")
            self.git(repo, "config", "user.email", "m3@example.invalid")
            self.git(repo, "config", "user.name", "M3 Fixture")
            (repo / "A.java").write_text("class A {}\n", encoding="utf-8")
            (repo / "pom.xml").write_text("<project/>\n", encoding="utf-8")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-qm", "baseline")

            (repo / "pom.xml").write_text("<project><a/></project>\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "non-Java"):
                gate.review(repo)
            self.git(repo, "checkout", "--", "pom.xml")

            (repo / "A.java").unlink()
            with self.assertRaisesRegex(ValueError, "status"):
                gate.review(repo)

    @staticmethod
    def git(repo: Path, *args: str) -> None:
        subprocess.run(
            ["git", "-C", str(repo), *args],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )


if __name__ == "__main__":
    unittest.main()
