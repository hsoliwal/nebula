# SPDX-License-Identifier: EPL-2.0
"""Builds NEBULA_VIZ_API_COVERAGE.tsv from javap -protected listings of the base and after class trees.

usage: python -I api_coverage.py <base.javap.txt> <after.javap.txt> <base-classes> <after-classes> <evidence.tsv> <out.tsv>

Rows: one per public/protected member (and per type) of every top-level or nested type in the
xygraph, widgets and oscilloscope bundles (examples excluded). Columns:
type, member, status (UNCHANGED_BYTECODE / EXISTING_IN_CHANGED_TYPE / NEW_ADDITIVE / REMOVED), evidence.
"""
import hashlib
import os
import re
import sys

def parse(path):
    types = {}
    cur = None
    for line in open(path, encoding='utf-8', errors='replace'):
        line = line.rstrip('\n')
        m = re.match(r'^(?:public |protected |abstract |final |static |sealed |non-sealed |strictfp )*(?:class|interface|enum|@interface|record) ([\w.$]+)', line)
        if m and line.endswith('{'):
            cur = m.group(1)
            types[cur] = set()
            continue
        if line.startswith('Compiled from') or line == '}' or cur is None:
            continue
        t = line.strip()
        if t.endswith(';'):
            types[cur].add(t[:-1])
    return types

def class_hashes(root):
    out = {}
    for d, _, files in os.walk(root):
        for f in files:
            if f.endswith('.class'):
                p = os.path.join(d, f)
                rel = os.path.relpath(p, root).replace(os.sep, '/')[:-6].replace('/', '.')
                out[rel] = hashlib.sha256(open(p, 'rb').read()).hexdigest()
    return out

def main():
    base = parse(sys.argv[1])
    after = parse(sys.argv[2])
    hb = class_hashes(sys.argv[3])
    ha = class_hashes(sys.argv[4])
    evidence = {}
    for line in open(sys.argv[5], encoding='utf-8'):
        if line.startswith('#') or not line.strip():
            continue
        k, v = line.rstrip('\n').split('\t', 1)
        evidence[k] = v
    rows = []
    for t in sorted(set(base) | set(after)):
        b = base.get(t, set())
        a = after.get(t, set())
        same_bytes = hb.get(t) is not None and hb.get(t) == ha.get(t)
        top = t.split('$')[0]
        ev_type = evidence.get(t) or evidence.get(top) or ''
        if t not in base:
            rows.append((t, '(type)', 'NEW_ADDITIVE', ev_type or 'new type'))
        for m in sorted(a | b):
            if m in a and m in b:
                status = 'UNCHANGED_BYTECODE' if same_bytes else 'EXISTING_IN_CHANGED_TYPE'
                ev = 'class file SHA-256 identical base vs after' if same_bytes else (ev_type or 'NO_EVIDENCE')
            elif m in a:
                status = 'NEW_ADDITIVE'
                ev = evidence.get(t + '#' + m.split('(')[0].split(' ')[-1], ev_type or 'new member')
            else:
                status = 'REMOVED'
                ev = 'API BREAK'
            rows.append((t, m, status, ev))
    with open(sys.argv[6], 'w', encoding='utf-8', newline='\n') as f:
        f.write('type\tmember\tstatus\tevidence\n')
        for r in rows:
            f.write('\t'.join(x.replace('\t', ' ') for x in r) + '\n')
    counts = {}
    for r in rows:
        counts[r[2]] = counts.get(r[2], 0) + 1
    print('rows', len(rows), counts)

main()
