#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Template-first generation of one hash-pinned recipe pass for a Nebula widget.

Usage: python -I make_crate_pass.py <nebula-root> <base-commit> <widget> <synexia-root> <pass-name>

Reads every path under widgets/<widget>/ that differs between <base-commit> and HEAD, writes the
exact preimage (base blob, or ABSENT) and postimage (HEAD blob) as UTF-8 resources, and emits the
sorted manifests consumed by the canonical engines:
  *.java  -> hash-pinned-java/<pass>/  (M3HashPinnedJavaSnapshotRecipe)
  other   -> hash-pinned-text/<pass>/  (M3HashPinnedTextSnapshotRecipe: pom.xml, *.properties, MANIFEST.MF)
Row: path <TAB> preimage-sha256|ABSENT <TAB> postimage-sha256 <TAB> resource-name.
Preimages go to <plane>/<pass>/pre/<resource-name> for the replay test. Deleted paths are refused.
"""
import hashlib
import os
import subprocess
import sys


def git(root, *args):
    return subprocess.run(["git", "-C", root, *args], check=True, capture_output=True).stdout


def sha(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def emit(nebula, base, synexia, pass_name, plane, changes):
    res = os.path.join(synexia, "synexia-openrewrite-recipes", "src", "main", "resources", "com", "synexia",
                       "rewrite", "hash-pinned-" + plane, pass_name)
    rows, names = [], set()
    for kind, path in changes:
        if path.endswith(".java") != (plane == "java"):
            continue
        if kind == "D":
            raise SystemExit("refusing deletion: " + path)
        os.makedirs(os.path.join(res, "pre"), exist_ok=True)
        after = git(nebula, "show", "HEAD:" + path).decode("utf-8")
        before = None if kind == "A" else git(nebula, "show", base + ":" + path).decode("utf-8")
        name = os.path.basename(path) + ".txt"
        if name in names or plane == "text":
            name = path.split("/")[2] + "." + name
        names.add(name)
        with open(os.path.join(res, name), "w", encoding="utf-8", newline="") as fh:
            fh.write(after)
        if before is not None:
            with open(os.path.join(res, "pre", name), "w", encoding="utf-8", newline="") as fh:
                fh.write(before)
        rows.append((path, "ABSENT" if before is None else sha(before), sha(after), name))
    if not rows:
        return
    rows.sort()
    with open(os.path.join(res, "manifest.tsv"), "w", encoding="utf-8", newline="\n") as fh:
        for r in rows:
            fh.write("\t".join(r) + "\n")
    print(pass_name, plane, len(rows), "targets")
    for r in rows:
        print("  " + r[1][:12] + " -> " + r[2][:12] + "  " + r[0])


def main():
    nebula, base, widget, synexia, pass_name = sys.argv[1:6]
    status = git(nebula, "diff", "--name-status", "--no-renames", base, "HEAD", "--", "widgets/" + widget)
    changes = [tuple(line.split("\t", 1)) for line in status.decode().splitlines()]
    for plane in ("java", "text"):
        emit(nebula, base, synexia, pass_name, plane, changes)


if __name__ == "__main__":
    main()
