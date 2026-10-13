#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Template-first generation of one hash-pinned recipe pass for a Nebula widget.

Usage: python -I make_crate_pass.py <nebula-root> <base-commit> <widget> <synexia-root> <pass-name>

Reads every *.java path under widgets/<widget>/ that differs between <base-commit> and HEAD,
writes the exact preimage (base blob, or ABSENT) and postimage (HEAD blob) as UTF-8 resources,
and emits the sorted manifest consumed by M3HashPinnedJavaSnapshotRecipe:
    path <TAB> preimage-sha256|ABSENT <TAB> postimage-sha256 <TAB> resource-name
Preimages go to <crate>/pre/<resource-name> for the replay test. Deleted paths are refused.
"""
import hashlib
import os
import subprocess
import sys


def git(root, *args):
    return subprocess.run(["git", "-C", root, *args], check=True, capture_output=True).stdout


def sha(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def main():
    nebula, base, widget, synexia, pass_name = sys.argv[1:6]
    status = git(nebula, "diff", "--name-status", "--no-renames", base, "HEAD", "--", "widgets/" + widget)
    rows, names = [], set()
    res = os.path.join(synexia, "synexia-openrewrite-recipes", "src", "main", "resources", "com", "synexia",
                       "rewrite", "hash-pinned-java", pass_name)
    os.makedirs(os.path.join(res, "pre"), exist_ok=True)
    for line in status.decode().splitlines():
        kind, path = line.split("\t", 1)
        if not path.endswith(".java"):
            continue
        if kind == "D":
            raise SystemExit("refusing deletion: " + path)
        after = git(nebula, "show", "HEAD:" + path).decode("utf-8")
        before = None if kind == "A" else git(nebula, "show", base + ":" + path).decode("utf-8")
        name = os.path.basename(path) + ".txt"
        if name in names:
            name = path.split("/")[2] + "." + name
        names.add(name)
        with open(os.path.join(res, name), "w", encoding="utf-8", newline="") as fh:
            fh.write(after)
        if before is not None:
            with open(os.path.join(res, "pre", name), "w", encoding="utf-8", newline="") as fh:
                fh.write(before)
        rows.append((path, "ABSENT" if before is None else sha(before), sha(after), name))
    rows.sort()
    with open(os.path.join(res, "manifest.tsv"), "w", encoding="utf-8", newline="\n") as fh:
        for r in rows:
            fh.write("\t".join(r) + "\n")
    print(pass_name, len(rows), "targets")
    for r in rows:
        print("  " + r[1][:12] + " -> " + r[2][:12] + "  " + r[0])


if __name__ == "__main__":
    main()
