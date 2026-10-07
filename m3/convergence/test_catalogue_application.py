# SPDX-License-Identifier: EPL-2.0
"""Launcher tests with a Maven PROCESS DOUBLE. No Java/OpenRewrite SDK claim."""
import contextlib
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("catalogue_apply", HERE / "apply_catalogue.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

DOUBLE = '''#!/usr/bin/env python3
import json, os, pathlib, subprocess, sys
args = sys.argv[1:]
with open(os.environ["TRACE"], "a") as stream:
    stream.write(json.dumps(args) + "\\n")
root = pathlib.Path.cwd()
source = root / "widgets/A.java"
goal = next((a for a in args if a in ("validate", "compile", "test", "verify", "install") or a.endswith(":runNoFork")), "")
recipe = next((a.split("=", 1)[1] for a in args if a.startswith("-Drewrite.activeRecipes=")), "")
if os.environ.get("FAIL_GOAL") == goal and (not os.environ.get("FAIL_AFTER_APPLY") or "return 2" in source.read_text()):
    print("PROCESS_DOUBLE: intentional failure")
    sys.exit(19)
if os.environ.get("MUTATE_VALIDATE") and goal == "validate":
    source.write_text("verification incorrectly changed source")
if recipe in ("test.Apply", "test.Generate"):
    source.write_text(source.read_text().replace("return 1", "return 2"))
if recipe == "test.Generate":
    (root / "widgets/Atom.java").write_text("final class Atom {}\\n")
if recipe == "test.Drift":
    source.write_text(source.read_text() + "// drift\\n")
if recipe == "test.Escape":
    (root / "pom.xml").write_text("unapproved root write\\n")
if recipe == "test.PlanDrift":
    p = root / "m3/convergence/catalogue-plan.json"
    p.write_text(p.read_text() + " ")
if recipe == "test.IndexDrift":
    source.write_text(source.read_text().replace("return 1", "return 2"))
    subprocess.run(["git", "add", "widgets/A.java"], check=True)
print("PROCESS_DOUBLE ONLY; no OpenRewrite or Java execution")
'''


class CatalogueApplicationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="catalogue-test-")
        self.addCleanup(self.temp.cleanup)
        self.parent = Path(self.temp.name)
        self.root = self.parent / "repo"
        self.root.mkdir()
        self.run_git("init", "-b", "candidate")
        self.run_git("config", "user.name", "Fixture")
        self.run_git("config", "user.email", "fixture@example.invalid")
        self.run_git("remote", "add", "origin", "https://github.com/hsoliwal/nebula.git")
        (self.root / "widgets").mkdir()
        (self.root / "widgets/A.java").write_text("class A { int a() { return 1; } }\n")
        (self.root / "pom.xml").write_text("<project/>\n")
        convergence = self.root / "m3/convergence"
        convergence.mkdir(parents=True)
        shutil.copyfile(HERE / "apply_catalogue.py", convergence / "apply_catalogue.py")
        shutil.copyfile(HERE / "verify.sh", convergence / "verify.sh")
        self.run_git("add", ".")
        self.run_git("commit", "-m", "fixture baseline")
        self.base = self.run_git("rev-parse", "HEAD").strip()
        self.plan_path = convergence / "catalogue-plan.json"
        self.plan = {"schema": 1, "base_revision": self.base,
                     "allowed_paths": ["widgets/**"],
                     "recipes": [{"name": "test.Apply", "artifacts": []}]}
        self.save_plan()
        self.out = self.parent / "evidence"
        self.trace = self.parent / "trace.jsonl"
        double = self.parent / "maven-process-double.py"
        double.write_text(DOUBLE)
        double.chmod(0o755)
        self.environment = patch.dict(os.environ, {"M3_MVN": str(double), "TRACE": str(self.trace), "DISPLAY": "process-double-only"})
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def run_git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args], stderr=subprocess.STDOUT).decode()

    def save_plan(self):
        self.plan_path.write_text(json.dumps(self.plan) + "\n")
        self.run_git("add", "m3/convergence/catalogue-plan.json")
        self.run_git("commit", "--allow-empty", "-m", "fixture plan")

    def execute(self):
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            return module.execute(self.root, self.plan_path, self.out)

    def status(self):
        return json.loads((self.out / "status.json").read_text())

    def test_real_goal_and_exact_gate_order(self):
        self.assertEqual(0, self.execute())
        events = self.status()["events"]
        self.assertEqual(["launcher-syntax", *["baseline-" + s for s in ("diff-hygiene", "validate", "compile", "test", "runtime-verify")],
                          "recipe-000-apply", *["recipe-000-" + s for s in ("diff-hygiene", "validate", "compile", "test", "runtime-verify")],
                          "replay-000"], [e["stage"] for e in events])
        apply = next(e["argv"] for e in events if e["stage"] == "recipe-000-apply")
        self.assertIn(module.PLUGIN, apply)
        self.assertIn("-Drewrite.activeRecipes=test.Apply", apply)
        self.assertFalse(any("dryRun" in a for a in apply))
        self.assertFalse(self.status()["production_promoted"])
        self.assertEqual("NOT_ESTABLISHED_BY_LAUNCHER", self.status()["api_equivalence"])
        self.assertEqual(["widgets/A.java"], self.status()["changed_paths"])
        self.assertIn("return 2", (self.out / "candidate.patch").read_text())

    def test_generated_files_in_patch_and_snapshot(self):
        self.plan["recipes"][0]["name"] = "test.Generate"
        self.save_plan()
        self.assertEqual(0, self.execute())
        self.assertIn("widgets/Atom.java", json.loads((self.out / "after.json").read_text()))
        self.assertIn("new file mode", (self.out / "candidate.patch").read_text())
        self.assertIn("b/widgets/Atom.java", (self.out / "candidate.patch").read_text())

    def test_baseline_compile_failure_never_applies(self):
        with patch.dict(os.environ, {"FAIL_GOAL": "compile"}):
            self.assertEqual(1, self.execute())
        self.assertEqual("baseline-compile", self.status()["events"][-1]["stage"])
        self.assertEqual(19, self.status()["events"][-1]["returncode"])
        self.assertNotIn(":runNoFork", self.trace.read_text())

    def test_post_apply_failure_stops_without_rollback(self):
        with patch.dict(os.environ, {"FAIL_GOAL": "compile", "FAIL_AFTER_APPLY": "1"}):
            self.assertEqual(1, self.execute())
        self.assertEqual("recipe-000-compile", self.status()["events"][-1]["stage"])
        self.assertIn("return 2", (self.root / "widgets/A.java").read_text())

    def test_replay_change_is_failure(self):
        self.plan["recipes"][0]["name"] = "test.Drift"
        self.save_plan()
        self.assertEqual(1, self.execute())
        self.assertEqual("replay-000", self.status()["events"][-1]["stage"])
        self.assertEqual("FAILED", self.status()["status"])

    def test_noop_is_not_claimed_as_application(self):
        self.plan["recipes"][0]["name"] = "test.Noop"
        self.save_plan()
        self.assertEqual(1, self.execute())
        self.assertIn("no source transformation", self.status()["error"])

    def test_source_mutating_verification_stops(self):
        with patch.dict(os.environ, {"MUTATE_VALIDATE": "1"}):
            self.assertEqual(1, self.execute())
        self.assertEqual("baseline-validate", self.status()["events"][-1]["stage"])

    def test_scope_escape_plan_drift_and_index_drift(self):
        for name, reason in [("test.Escape", "protected owner/root"), ("test.PlanDrift", "plan changed"), ("test.IndexDrift", "index changed")]:
            with self.subTest(name=name):
                self.plan["recipes"][0]["name"] = name
                self.save_plan()
                self.assertEqual(1, self.execute())
                self.assertIn(reason, self.status()["error"])
                # Test fixture teardown/reset only, never production runner behavior.
                self.run_git("reset", "--hard", "HEAD")
                shutil.rmtree(self.out)

    def test_missing_executable_retains_failure(self):
        with patch.dict(os.environ, {"M3_MVN": str(self.parent / "absent-maven")}):
            self.assertEqual(1, self.execute())
        event = self.status()["events"][-1]
        self.assertEqual("baseline-validate", event["stage"])
        self.assertIsNone(event["returncode"])
        self.assertIn("error", event)

    def test_protected_branch_and_dirty_checkout(self):
        self.run_git("branch", "-m", "master")
        with self.assertRaisesRegex(RuntimeError, "protected branch"):
            self.execute()
        self.assertFalse(self.out.exists())
        self.run_git("branch", "-m", "candidate")
        (self.root / "untracked.txt").write_text("must not discard")
        with self.assertRaisesRegex(RuntimeError, "must be clean"):
            self.execute()
        self.assertFalse(self.out.exists())

    def test_output_overlap_and_symlink_rejected(self):
        self.out = self.root / ".." / "repo" / "evidence"
        with self.assertRaisesRegex(RuntimeError, "overlaps checkout"):
            self.execute()
        link = self.parent / "link"
        link.symlink_to(self.parent, target_is_directory=True)
        self.out = link / "evidence"
        with self.assertRaisesRegex(RuntimeError, "symlink"):
            self.execute()

    def test_invalid_plan_rows(self):
        bad_rows = [{"name": "test.Apply;exit", "artifacts": []},
                    {"name": "test.Apply", "artifacts": ["a:b:LATEST"]},
                    {"name": "test.Apply", "artifacts": ["a:b:[1,2)"]},
                    {"name": "test.Apply", "artifacts": [], "config": "../escape.yml"}]
        for row in bad_rows:
            with self.subTest(row=row):
                self.plan["recipes"] = [row]
                self.save_plan()
                with self.assertRaises(RuntimeError):
                    self.execute()
                self.assertFalse(self.out.exists())

    def test_ignored_yaml_is_not_an_admitted_input(self):
        (self.root / ".gitignore").write_text("ignored.yml\n")
        self.run_git("add", ".gitignore")
        self.run_git("commit", "-m", "fixture ignore policy")
        self.plan["base_revision"] = self.run_git("rev-parse", "HEAD").strip()
        (self.root / "ignored.yml").write_text("type: specs.openrewrite.org/v1beta/recipe\n")
        self.plan["recipes"][0]["config"] = "ignored.yml"
        self.save_plan()
        with self.assertRaises(subprocess.CalledProcessError):
            self.execute()
        self.assertFalse(self.out.exists())

    def test_duplicate_json_keys_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "duplicate JSON"):
            json.loads('{"schema":1,"schema":2}', object_pairs_hook=module.no_duplicate_keys)

    def test_deletion_and_mode_change_rejected(self):
        before = {"widgets/A.java": {"sha256": "a", "mode": 0o644}}
        with self.assertRaisesRegex(RuntimeError, "deletion"):
            module.changed(before, {}, ["widgets/**"])
        with self.assertRaisesRegex(RuntimeError, "mode"):
            module.changed(before, {"widgets/A.java": {"sha256": "a", "mode": 0o755}}, ["widgets/**"])

    def test_legacy_body_byte_identity(self):
        after = (HERE / "verify.sh").read_bytes()
        start = after.index(b"# Explicit application")
        end = after.index(b'SYNEXIA_ROOT=', start)
        original = after[:start] + after[end:]
        actual = hashlib.sha1(b"blob " + str(len(original)).encode() + b"\0" + original).hexdigest()
        self.assertEqual("9b811f683de78b318d9968e63b211134e7f8ff06", actual)

    def test_launcher_dispatch_arity(self):
        result = subprocess.run(["bash", str(self.root / "m3/convergence/verify.sh"), "--apply-catalogue"], capture_output=True)
        self.assertEqual(2, result.returncode)
        self.assertIn(b"usage:", result.stderr)

    def test_local_recipe_artifact_uses_existing_reactor(self):
        self.plan["recipes"][0]["artifacts"] = [module.LOCAL_ARTIFACT]
        self.save_plan()
        self.assertEqual(0, self.execute())
        event = self.status()["events"][1]
        self.assertEqual("recipe-tool-build", event["stage"])
        self.assertIn(str(self.root / "m3/reactor.xml"), event["argv"])
        self.assertEqual("install", event["argv"][-1])


if __name__ == "__main__":
    unittest.main(verbosity=2)
