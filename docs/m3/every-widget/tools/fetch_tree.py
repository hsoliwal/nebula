# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Fetch the files under one or more path prefixes of a GitHub ref into a local folder (same relative paths), through
the git trees API (one recursive call) and the blobs API, with gh. For clean master copies when a local worktree is
stale or a shared blobless clone must not be checked out. Skips files whose local copy already has the blob's git hash.
Usage: fetch_tree.py <owner/repo> <ref> <outdir> <prefix>..."""
import base64, hashlib, json, os, subprocess, sys
from concurrent.futures import ThreadPoolExecutor

repo, ref, out, prefixes = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]


def gh(path):
    return json.loads(subprocess.run(["gh", "api", path], capture_output=True, check=True).stdout)


def git_hash(data):
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


tree = gh(f"repos/{repo}/git/trees/{ref}?recursive=1")
if tree.get("truncated"):
    sys.exit("tree truncated: fetch narrower prefixes through sub-tree shas")
want = [e for e in tree["tree"] if e["type"] == "blob" and any(e["path"].startswith(p) for p in prefixes)]


def fetch(e):
    dest = os.path.join(out, e["path"])
    if os.path.exists(dest) and git_hash(open(dest, "rb").read()) == e["sha"]:
        return "same"
    data = base64.b64decode(gh(f"repos/{repo}/git/blobs/{e['sha']}")["content"])
    if git_hash(data) != e["sha"]:
        raise SystemExit(f"hash mismatch {e['path']}")
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    open(dest, "wb").write(data)
    return "fetched"


with ThreadPoolExecutor(8) as pool:
    results = list(pool.map(fetch, want))
print(f"{len(want)} files: fetched={results.count('fetched')} same={results.count('same')} -> {out}")
