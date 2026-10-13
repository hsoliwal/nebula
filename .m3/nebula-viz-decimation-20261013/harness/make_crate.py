# SPDX-License-Identifier: Apache-2.0
"""Writes the hash-pinned recipe crate resources for the Nebula viz decimation leaf.

usage: python -I make_crate.py <nebula-repo> <base-commit> <head-commit> <resource-dir>

<resource-dir> receives manifest.tsv, 1.java .. N.java (postimages) and pre/<i>.java.txt (exact preimages of the
modified targets). Rows are ordered by path; preimage hash is ABSENT for added files. Text is the repository blob
(LF) decoded as UTF-8; hashes are SHA-256 of that text encoded as UTF-8, as M3HashPinnedJavaSnapshotRecipe expects.
"""
import hashlib
import os
import subprocess
import sys

repo, base, head, out = sys.argv[1:5]

def git(*a):
    return subprocess.run(['git', '-C', repo] + list(a), check=True, capture_output=True).stdout

rows = []
for line in git('diff', '--name-status', '--no-renames', base, head, '--', 'widgets').decode().splitlines():
    status, path = line.split('\t', 1)
    if path.endswith('.java'):
        rows.append((path, status))
rows.sort()
os.makedirs(os.path.join(out, 'pre'), exist_ok=True)
manifest = []
for i, (path, status) in enumerate(rows, 1):
    after = git('show', head + ':' + path).decode('utf-8')
    assert not after.startswith('﻿') and '\r' not in after, path
    open(os.path.join(out, '%d.java' % i), 'w', encoding='utf-8', newline='').write(after)
    ah = hashlib.sha256(after.encode('utf-8')).hexdigest()
    if status == 'A':
        bh = 'ABSENT'
    else:
        assert status == 'M', (path, status)
        before = git('show', base + ':' + path).decode('utf-8')
        assert not before.startswith('﻿') and '\r' not in before, path
        open(os.path.join(out, 'pre', '%d.java.txt' % i), 'w', encoding='utf-8', newline='').write(before)
        bh = hashlib.sha256(before.encode('utf-8')).hexdigest()
    manifest.append('\t'.join((path, bh, ah, '%d.java' % i)))
open(os.path.join(out, 'manifest.tsv'), 'w', encoding='utf-8', newline='\n').write('\n'.join(manifest) + '\n')
print(len(manifest), 'targets;', sum(1 for r in rows if r[1] == 'M'), 'modified;', sum(1 for r in rows if r[1] == 'A'), 'added')
