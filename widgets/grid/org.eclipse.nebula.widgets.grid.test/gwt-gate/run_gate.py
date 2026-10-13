# Copyright (c) 2026 Contributors to the Eclipse Foundation.
# SPDX-License-Identifier: EPL-2.0
"""GWT compile gate for the Grid model package (pattern of the SWT viewport gate, SWT #117).

Copies org.eclipse.nebula.widgets.grid.model into a scratch GWT module, compiles it with the GWT
compiler in strict mode, runs the same seeded workload in the JVM and in the generated JavaScript
(Node), and fails unless every output line is identical.

Usage: python run_gate.py --nebula-root REPO --gwt-lib DIR --work NEW_DIR [--java-home DIR] [--node EXE]
DIR holds gwt-dev, gwt-user and their dependencies (ecj-*.jar is ignored; gwt-dev bundles its JDT).
"""
import argparse
import os
import shutil
import subprocess
import sys

MODEL = os.path.join("widgets", "grid", "org.eclipse.nebula.widgets.grid", "src", "org", "eclipse", "nebula",
                     "widgets", "grid", "model")
PKG = os.path.join("org", "eclipse", "nebula", "widgets", "grid", "model")
MODULE = "org.eclipse.nebula.widgets.grid.model.GridModel"


def run(cmd, **kw):
    print("+", " ".join(cmd[:4]), "...", flush=True)
    return subprocess.run(cmd, check=False, capture_output=True, text=True, **kw)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--nebula-root", required=True)
    ap.add_argument("--gwt-lib", required=True)
    ap.add_argument("--work", required=True)
    ap.add_argument("--java-home", default=os.environ.get("JAVA_HOME", ""))
    ap.add_argument("--node", default="node")
    a = ap.parse_args()
    here = os.path.dirname(os.path.abspath(__file__))
    if os.path.exists(a.work):
        sys.exit("work directory must not exist: " + a.work)
    tool = lambda n: os.path.join(a.java_home, "bin", n) if a.java_home else n
    src = os.path.join(a.work, "src")
    dst = os.path.join(src, PKG)
    os.makedirs(dst)
    model = os.path.join(a.nebula_root, MODEL)
    sources = sorted(f for f in os.listdir(model) if f.endswith(".java"))
    for name in sources:
        shutil.copy(os.path.join(model, name), dst)
    gate = os.path.join(here, "src", PKG)
    for name in os.listdir(gate):
        if os.path.isfile(os.path.join(gate, name)):
            shutil.copy(os.path.join(gate, name), dst)
    shutil.copytree(os.path.join(gate, "super"), os.path.join(dst, "super"))
    jars = sorted(f for f in os.listdir(a.gwt_lib) if f.endswith(".jar") and not f.startswith("ecj-"))
    jars.sort(key=lambda f: (not f.startswith("gwt-dev"), not f.startswith("gwt-user"), f))
    cp = os.pathsep.join([src] + [os.path.join(a.gwt_lib, j) for j in jars])
    war, workdir = os.path.join(a.work, "war"), os.path.join(a.work, "gwt-work")
    os.makedirs(workdir)
    gwt = run([tool("java"), "-Xmx1400m", "-XX:+UseSerialGC", "-cp", cp, "com.google.gwt.dev.Compiler", "-strict",
               "-sourceLevel", "17", "-style", "PRETTY", "-war", war, "-workDir", workdir, "-XnoclassMetadata",
               "-nocompileReport", MODULE])
    print(gwt.stdout[-3000:])
    if gwt.returncode != 0 or "Compile of permutations succeeded" not in gwt.stdout:
        print(gwt.stderr[-3000:])
        sys.exit("GWT_GATE_FAIL: GWT compile did not succeed")
    classes = os.path.join(a.work, "classes")
    os.makedirs(classes)
    java_files = [os.path.join(dst, n) for n in sources] + [os.path.join(dst, n) for n in ("GateWorkload.java", "GateJava.java")]
    jc = run([tool("javac"), "-d", classes] + java_files)
    if jc.returncode != 0:
        print(jc.stdout, jc.stderr)
        sys.exit("GWT_GATE_FAIL: javac failed")
    jvm = run([tool("java"), "-cp", classes, "org.eclipse.nebula.widgets.grid.model.GateJava"])
    js = run([a.node, os.path.join(here, "run_node.mjs"), os.path.join(war, MODULE, MODULE + ".nocache.js")])
    if jvm.returncode != 0 or js.returncode != 0:
        print(jvm.stderr, js.stderr)
        sys.exit("GWT_GATE_FAIL: workload did not run")
    jvm_lines, js_lines = jvm.stdout.strip().splitlines(), js.stdout.strip().splitlines()
    print("JVM:", *jvm_lines, sep="\n  ")
    print("JS:", *js_lines, sep="\n  ")
    if jvm_lines != js_lines or not jvm_lines:
        sys.exit("GWT_GATE_FAIL: JVM and GWT JavaScript disagree")
    print("GWT_GATE_OK", len(jvm_lines), "workloads identical in the JVM and in GWT JavaScript;",
          len(sources), "model classes compiled by GWT")


if __name__ == "__main__":
    main()
