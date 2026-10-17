#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compile exact before/received Grid sources and retain source-bound GTK native gates.

Run under a caller-owned Xvfb with GTK_OVERLAY_SCROLLING=0. This focused gate
does not replace Maven/Tycho, other native platforms, or resource budget gates.
An existing output directory is refused so earlier proof is never overwritten.
"""
import argparse
import gzip
import hashlib
import json
import os
from pathlib import Path
import re
import resource
import shutil
import subprocess
import time

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

parser = argparse.ArgumentParser(description=__doc__)
for name in ("target", "baseline", "output", "java-home", "swt-overlay", "swt-classes",
             "swt-native", "junit", "hamcrest"):
    parser.add_argument("--" + name, required=True, type=Path)
args = parser.parse_args()
assert os.environ.get("DISPLAY"), "Launch this gate under a live native X11 display"
assert os.environ.get("GTK_OVERLAY_SCROLLING") == "0", "GTK_OVERLAY_SCROLLING=0 is required"
out = args.output.resolve()
out.mkdir(parents=True, exist_ok=False)
commands = []
source_root = Path("widgets/grid/org.eclipse.nebula.widgets.grid/src")
test_root = Path("widgets/grid/org.eclipse.nebula.widgets.grid.test/src")
test_package = test_root / "org/eclipse/nebula/widgets/grid"
test_names = ["GridTopIndexStartup_Test", "GridSelectionOccurrence_Test", "GridSelectionList_Test",
              "GridCellSelectionList_Test", "GridSwtScreenshotCapture"]
classpath = os.pathsep.join(map(str, (args.swt_overlay, args.swt_classes, args.junit, args.hamcrest)))

def execute(label, command, required=True):
    started = time.monotonic()
    before = resource.getrusage(resource.RUSAGE_CHILDREN)
    with (out / (label + ".log")).open("w") as log:
        run = subprocess.Popen(list(map(str, command)), stdout=log, stderr=subprocess.STDOUT)
        _, status, usage = os.wait4(run.pid, 0)
        run.returncode = os.waitstatus_to_exitcode(status)
        result = {"exit": run.returncode, "wall_seconds": time.monotonic() - started,
                  "cpu_user_seconds": usage.ru_utime, "cpu_system_seconds": usage.ru_stime,
                  "max_rss_kib": usage.ru_maxrss}
        log.write("\nMEASUREMENTS\t" + json.dumps(result, sort_keys=True) + "\n")
    commands.append({"stage": label, "argv": list(map(str, command)), **result})
    (out / "commands.json").write_text(json.dumps(commands, indent=2) + "\n")
    if required and result["exit"]:
        raise RuntimeError(label + " failed; inspect " + str(out / (label + ".log")))
    return result

inputs = {"driver_sha256": sha(__file__), "runner_sha256": sha(Path(__file__).with_name("RunGridNativeProof.java.txt")),
          "environment": {"GTK_OVERLAY_SCROLLING": "0", "display": "caller-owned GTK3/X11 Xvfb", "heap_cap_mib": 512},
          "java": str(args.java_home), "junit": {"path": str(args.junit), "sha256": sha(args.junit)},
          "hamcrest": {"path": str(args.hamcrest), "sha256": sha(args.hamcrest)}, "widget_sources": {}, "test_sources": {},
          "swt_overlay": {}, "swt_classes_and_resources": {}, "native_libraries": {}}
execute("java-version", [args.java_home / "bin/java", "-version"])
for key, root, pattern in (("swt_overlay", args.swt_overlay, "*"),
                           ("swt_classes_and_resources", args.swt_classes, "*"),
                           ("native_libraries", args.swt_native, "*.so")):
    inputs[key] = {str(path.relative_to(root)): sha(path) for path in sorted(root.rglob(pattern)) if path.is_file()}

for phase, root in (("before", args.baseline), ("after", args.target)):
    paths = sorted(path for path in (root / source_root / "org/eclipse/nebula/widgets").rglob("*.java"))
    assert len(paths) == 105, (phase, "unexpected core source count", len(paths))
    inputs["widget_sources"][phase] = {str(path.relative_to(root)): sha(path) for path in paths}
    classes = out / phase / "classes"
    classes.mkdir(parents=True)
    execute(phase + "-compile", [args.java_home / "bin/javac", "-encoding", "UTF-8", "-cp", classpath,
                               "-d", classes, *paths])
    execute(phase + "-api", [args.java_home / "bin/javap", "-public", "-s", "-classpath", classes,
                           "org.eclipse.nebula.widgets.grid.Grid"])

def api_text(phase):
    return (out / (phase + "-api.log")).read_text().split("\nMEASUREMENTS\t")[0]
assert api_text("before") == api_text("after"), "Grid public JVM descriptors changed"
support = out / "support"
support.mkdir()
runner = support / "RunGridNativeProof.java"
shutil.copyfile(Path(__file__).with_name("RunGridNativeProof.java.txt"), runner)
test_paths = [args.target / test_package / (name + ".java") for name in test_names]
inputs["test_sources"] = {str(path.relative_to(args.target)): sha(path) for path in test_paths}
test_classes = support / "classes"
test_classes.mkdir()
execute("test-compile", [args.java_home / "bin/javac", "-encoding", "UTF-8", "-cp",
                         classpath + os.pathsep + str(out / "after/classes"), "-d", test_classes, runner, *test_paths])
with (out / "inputs.json.gz").open("wb") as raw:
    with gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as compressed:
        compressed.write((json.dumps(inputs, indent=2, sort_keys=True) + "\n").encode())

def native_run(label, phase, mode, names, count):
    evidence = out / label
    evidence.mkdir()
    runtime = os.pathsep.join(map(str, (args.swt_overlay, args.swt_classes, out / phase / "classes",
                                      test_classes, args.target / source_root, args.junit, args.hamcrest)))
    command = [args.java_home / "bin/java", "-Xmx512m", "-Djava.library.path=" + str(args.swt_native),
               "-Dorg.eclipse.nebula.widgets.grid.look=classic", "-Dnebula.topindex.evidence.dir=" + str(evidence),
               "-Dnebula.selection.evidence.dir=" + str(evidence)]
    if mode == "original":
        command.append("-Dswt.widget.mode.Grid=original")
    command.extend(["-cp", runtime, "RunGridNativeProof", str(count), *["org.eclipse.nebula.widgets.grid." + n for n in names]])
    metrics = execute(label, command, required=False)
    log = (out / (label + ".log")).read_text()
    records = re.findall(r"^METHOD\t([^\t]+)\t([^\n]+)$", log, re.M)
    counts = re.findall(r"^COUNTS\t(\d+)\t(\d+)\t(\d+)\t(\d+)\t(false|true)$", log, re.M)
    assert len(counts) == 1 and len(records) == count and len(dict(records)) == count, label + " incomplete or duplicate evidence"
    run, failed, ignored, assumptions, duplicate = counts[0]
    assert int(run) == count and int(ignored) == 0 and int(assumptions) == 0 and duplicate == "false", label + " skipped gate"
    return {"tests": int(run), "failures": int(failed), "ignored": int(ignored), "assumptions": int(assumptions),
            "methods": dict(records), "metrics": metrics}

results = {}
results["startup_before"] = native_run("startup-before", "before", "default", ["GridTopIndexStartup_Test"], 20)
assert results["startup_before"]["failures"] == 19, "The exact negative baseline changed"
for mode in ("default", "original"):
    key = "startup_" + mode
    results[key] = native_run("startup-" + mode, "after", mode, ["GridTopIndexStartup_Test"], 20)
    assert results[key]["failures"] == 0 and results[key]["metrics"]["exit"] == 0, key + " failed"
results["selection_default"] = native_run("selection-default", "after", "default",
    ["GridSelectionOccurrence_Test", "GridSelectionList_Test", "GridCellSelectionList_Test"], 11)
assert results["selection_default"]["failures"] == 0 and results["selection_default"]["metrics"]["exit"] == 0
result = {"format": "nebula-startup-scroll-native-v1", "status": "PASSED_FOCUSED_GATE", "public_api": "UNCHANGED",
          "core_widget_sources_per_side": 105, "results": results,
          "unqualified": ["10 JFace adapter sources", "full Maven/Tycho", "Windows", "Cocoa", "GTK4",
                          "independent heap and native memory budgets", "combined SWT/Nebula resource and pixel gate"],
          "inputs_sha256": sha(out / "inputs.json.gz"), "commands_sha256": sha(out / "commands.json")}
(out / "result.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps({"status": result["status"], "output": str(out), "runs":
    {k: {"tests": v["tests"], "failures": v["failures"]} for k, v in results.items()}}, sort_keys=True))
