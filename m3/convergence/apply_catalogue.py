#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Explicit, serial Maven/OpenRewrite application; not a source transformer."""
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import subprocess
import sys

PLUGIN = "org.openrewrite.maven:rewrite-maven-plugin:6.46.1:runNoFork"
LOCAL_ARTIFACT = "org.eclipse.nebula.m3:nebula-m3-recipe-first:1.0.0-SNAPSHOT"
WIRING = frozenset("m3/convergence/" + name for name in (
    "verify.sh", "apply_catalogue.py", "test_catalogue_application.py",
    "catalogue-plan.json", "CATALOGUE_APPLICATION.md"))


def require(condition: bool, reason: str) -> None:
    if not condition:
        raise RuntimeError(reason)


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, sort_keys=True, indent=2) + "\n", encoding="utf-8")


def no_duplicate_keys(pairs: list[tuple[str, object]]) -> dict:
    result: dict = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key: " + key)
        result[key] = value
    return result


def no_symlink(path: Path) -> None:
    require(not any(p.is_symlink() for p in (path, *path.parents)), "symlink path: " + str(path))


def load_plan(path: Path, root: Path) -> dict:
    no_symlink(path)
    plan = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=no_duplicate_keys)
    require(isinstance(plan, dict), "plan must be an object")
    require(set(plan) == {"schema", "base_revision", "allowed_paths", "recipes"}, "unknown or missing plan fields")
    require(type(plan["schema"]) is int and plan["schema"] == 1, "schema must be 1")
    require(isinstance(plan["base_revision"], str) and re.fullmatch(r"[0-9a-f]{40}", plan["base_revision"]), "exact base revision required")
    globs = plan["allowed_paths"]
    require(isinstance(globs, list) and bool(globs), "explicit allowed_paths required")
    for glob in globs:
        require(isinstance(glob, str) and "/" in glob and not glob.startswith(("/", ".", "*", "?")), "non-root namespace glob required")
        require("\\" not in glob and ".." not in glob.split("/") and "\0" not in glob, "unsafe namespace glob")
    require(isinstance(plan["recipes"], list) and bool(plan["recipes"]), "ordered recipe rows required")
    names: set[str] = set()
    for row in plan["recipes"]:
        require(isinstance(row, dict) and {"name", "artifacts"} <= row.keys() and row.keys() <= {"name", "artifacts", "config"}, "invalid recipe row")
        name = row["name"]
        require(isinstance(name, str) and re.fullmatch(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+", name), "fully qualified recipe name required")
        require(name not in names, "duplicate recipe: " + name)
        names.add(name)
        require(isinstance(row["artifacts"], list), "artifacts must be a list")
        for gav in row["artifacts"]:
            require(isinstance(gav, str) and re.fullmatch(r"[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[0-9][A-Za-z0-9_.-]*", gav), "pinned group:artifact:version required")
        require(len(set(row["artifacts"])) == len(row["artifacts"]), "duplicate artifact coordinate")
        if "config" in row:
            value = row["config"]
            require(isinstance(value, str), "config must be a relative path")
            config = root / value
            no_symlink(config)
            require(not Path(value).is_absolute() and config.resolve().is_relative_to(root), "config escapes checkout")
            require(config.is_file() and config.suffix in {".yml", ".yaml"}, "recipe YAML configuration missing")
    return plan


def git(root: Path, *args: str) -> bytes:
    return subprocess.check_output(["git", "-C", str(root), *args], stderr=subprocess.PIPE)


def snapshot(root: Path) -> dict:
    """All tracked and non-ignored untracked files, including new atom files."""
    paths = sorted(set(git(root, "ls-files", "--cached", "--others", "--exclude-standard", "-z").split(b"\0")) - {b""})
    result = {}
    for raw in paths:
        name = raw.decode("utf-8")
        path = root / name
        no_symlink(path)
        require(path.is_file(), "missing/non-file repository input: " + name)
        hasher = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                hasher.update(chunk)
        result[name] = {"sha256": hasher.hexdigest(), "mode": stat.S_IMODE(path.stat().st_mode)}
    return result


def changed(before: dict, after: dict, allowed: list[str]) -> list[str]:
    require(before.keys() <= after.keys(), "source deletion is not admitted")
    paths = sorted(p for p in after if before.get(p) != after[p])
    for path in paths:
        require(path not in before or before[path]["mode"] == after[path]["mode"], "source mode changed: " + path)
        require("/" in path and not path.startswith(("m3/", ".m3/", ".git/")), "protected owner/root path changed: " + path)
        require(any(fnmatch.fnmatchcase(path, pattern) for pattern in allowed), "recipe escaped selected paths: " + path)
    return paths


def execute(root: Path, plan_path: Path, output: Path) -> int:
    no_symlink(root)
    no_symlink(output)
    root, output, plan_path = root.resolve(), output.resolve(), plan_path.absolute()
    require(not output.exists() and output.parent.is_dir(), "output must be absent under an existing directory")
    require(output.parent != Path(output.anchor), "filesystem-root output is not admitted")
    require(not output.is_relative_to(root) and not root.is_relative_to(output), "output overlaps checkout")
    require(Path(git(root, "rev-parse", "--show-toplevel").decode().strip()).resolve() == root, "root must be the repository root")
    branch = git(root, "symbolic-ref", "--short", "HEAD").decode().strip()
    require(branch not in {"master", "main", "develop"}, "protected branch: create a candidate branch first")
    require(not git(root, "status", "--porcelain", "--untracked-files=all"), "candidate checkout must be clean")
    remote = git(root, "remote", "get-url", "origin").decode().strip().removesuffix(".git")
    require(remote in {"https://github.com/hsoliwal/nebula", "git@github.com:hsoliwal/nebula"}, "unexpected repository origin")
    plan = load_plan(plan_path, root)
    require(plan_path.resolve().is_relative_to(root), "plan must be checked into this candidate")
    git(root, "ls-files", "--error-unmatch", str(plan_path.resolve().relative_to(root)))
    for row in plan["recipes"]:
        if "config" in row:
            git(root, "ls-files", "--error-unmatch", str((root / row["config"]).resolve().relative_to(root)))
    git(root, "merge-base", "--is-ancestor", plan["base_revision"], "HEAD")
    prior = set(git(root, "diff", "--name-only", "-z", plan["base_revision"], "HEAD").decode().split("\0")) - {""}
    require(prior <= WIRING, "candidate differs from reviewed base outside convergence wiring")
    head = git(root, "rev-parse", "HEAD")
    index = git(root, "ls-files", "--stage", "-z")
    inputs = snapshot(root)
    plan_hash = digest(plan_path.read_bytes())
    output.mkdir()
    write_json(output / "before.json", inputs)
    write_json(output / "plan.json", plan)
    events: list[dict] = []
    expected = inputs
    state = {"status": "RUNNING", "head": head.decode().strip(), "branch": branch,
             "plan_sha256": plan_hash, "events": events, "production_promoted": False}
    write_json(output / "status.json", state)

    def identity() -> None:
        require(git(root, "rev-parse", "HEAD") == head, "HEAD changed during execution")
        require(git(root, "symbolic-ref", "--short", "HEAD").decode().strip() == branch, "branch changed during execution")
        require(git(root, "ls-files", "--stage", "-z") == index, "index changed during execution")
        require(digest(plan_path.read_bytes()) == plan_hash, "plan changed during execution")

    def run(stage: str, argv: list[str], mutating: bool = False) -> dict:
        identity()
        require(snapshot(root) == expected, "source drift before " + stage)
        logfile = output / (f"{len(events):03d}-" + stage + ".log")
        event = {"stage": stage, "argv": argv, "returncode": None}
        events.append(event)
        write_json(output / "status.json", state)
        try:
            with logfile.open("wb") as log:
                done = subprocess.run(argv, cwd=root, stdout=log, stderr=subprocess.STDOUT, timeout=3600)
            event["returncode"] = done.returncode
        except (OSError, subprocess.TimeoutExpired) as error:
            event["error"] = str(error)
            raise
        finally:
            event["log_sha256"] = digest(logfile.read_bytes())
            write_json(output / "status.json", state)
        require(done.returncode == 0, stage + " failed; see " + logfile.name)
        identity()
        current = snapshot(root)
        if not mutating:
            require(current == expected, "verification changed source at " + stage)
        return current

    mvn = [os.environ.get("M3_MVN", "mvn"), "-B", "-ntp", "-f", str(root / "pom.xml"),
           "-DskipTests=false", "-Dmaven.test.skip=false", "-Dtycho.localArtifacts=ignore"]
    display = [] if sys.platform != "linux" or os.environ.get("DISPLAY") else ["xvfb-run", "-a"]

    def gates(prefix: str) -> None:
        run(prefix + "-diff-hygiene", ["git", "diff", "--check"])
        run(prefix + "-validate", [*mvn, "validate"])
        run(prefix + "-compile", [*mvn, "compile"])
        run(prefix + "-test", [*display, *mvn, "test"])
        run(prefix + "-runtime-verify", [*display, *mvn, "verify"])

    def invocation(row: dict) -> list[str]:
        argv = [*mvn, PLUGIN, "-Drewrite.activeRecipes=" + row["name"],
                "-Drewrite.failOnInvalidActiveRecipes=true", "-Drewrite.exportDatatables=true",
                "-Drewrite.runPerSubmodule=false", "-Drewrite.skip=false",
                "-Drewrite.exclusions=pom.xml,m3/**,.m3/**,.git/**"]
        if row["artifacts"]:
            argv.append("-Drewrite.recipeArtifactCoordinates=" + ",".join(row["artifacts"]))
        if "config" in row:
            argv.append("-Drewrite.configLocation=" + str(root / row["config"]))
        return argv

    try:
        run("launcher-syntax", ["bash", "-n", str(root / "m3/convergence/verify.sh")])
        if any(LOCAL_ARTIFACT in row["artifacts"] for row in plan["recipes"]):
            run("recipe-tool-build", [mvn[0], "-B", "-ntp", "-f", str(root / "m3/reactor.xml"),
                                       "-DskipTests=false", "-Dmaven.test.skip=false", "install"])
        gates("baseline")
        for number, row in enumerate(plan["recipes"]):
            prefix = f"recipe-{number:03d}"
            current = run(prefix + "-apply", invocation(row), True)
            paths = changed(expected, current, plan["allowed_paths"])
            write_json(output / (prefix + "-changes.json"), {p: {"before": expected.get(p), "after": current[p]} for p in paths})
            expected = current
            write_json(output / (prefix + "-after.json"), expected)
            gates(prefix)
        require(inputs != expected, "no source transformation occurred")
        write_json(output / "after.json", expected)
        with (output / "candidate.patch").open("wb") as patch:
            patch.write(git(root, "diff", "--binary", "HEAD", "--"))
            for path in sorted(expected.keys() - inputs.keys()):
                result = subprocess.run(["git", "diff", "--no-index", "--binary", "--", "/dev/null", path],
                                        cwd=root, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
                require(result.returncode == 1, "new-file patch export failed: " + path)
                patch.write(result.stdout)
        for number, row in enumerate(plan["recipes"]):
            # Non-mutating expectation checks each replay, not just final net diff.
            run(f"replay-{number:03d}", invocation(row))
        state["status"] = "CANDIDATE_PROJECT_GATES_PASSED"
        state["changed_paths"] = changed(inputs, expected, plan["allowed_paths"])
        state["api_equivalence"] = "NOT_ESTABLISHED_BY_LAUNCHER"
        write_json(output / "status.json", state)
        print(json.dumps({"status": state["status"], "changed_paths": state["changed_paths"]}))
        return 0
    except Exception as error:
        state.update(status="FAILED", error=str(error))
        try:
            write_json(output / "failure-source.json", snapshot(root))
        except Exception as snapshot_error:
            state["snapshot_error"] = str(snapshot_error)
        write_json(output / "status.json", state)
        print("FAILED: " + str(error), file=sys.stderr)
        return 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--plan", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    try:
        return execute(args.root, args.plan, args.out)
    except (RuntimeError, ValueError, OSError, subprocess.SubprocessError) as error:
        print("ADMISSION FAILED: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
