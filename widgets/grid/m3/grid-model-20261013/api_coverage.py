"""GRID_MODEL_API_COVERAGE.tsv: every public/protected member of the Grid bundle, pre vs post.

usage: api_coverage.py <pre-src-root> <post-src-root> <out.tsv>
A member is a public/protected method, constructor or field of a top-level type. For each:
status = UNCHANGED (same signature, same body text) | BODY_CHANGED (same signature, body text
differs) | ADDED (new) | REMOVED (must not occur). Evidence names the tests that exercise the
changed/added members (curated map below; unchanged members are covered by the existing suite
whose failure set is compared pre vs post).
"""
import os
import re
import sys

pre_root, post_root, out = sys.argv[1:4]
SIG = re.compile(r'^\t(public|protected)\b[^;{=]*?\b([A-Za-z_][A-Za-z0-9_]*)\s*\(([^)]*)\)')
FIELD = re.compile(r'^\t(public|protected)\b[^(]*?\b([A-Za-z_][A-Za-z0-9_]*)\s*(=|;)')

EVIDENCE = {
    'Grid': 'GridRowPermutation_Test, GridColumnWindow_Test, GridSetDataRedraw_Test, GridPinnedRows_Test, '
            'GridCheckLanes_Test, GridResidency_Test, GridBodyDefer_Test, GridModelJobs_Test, GridIssue700_Test, '
            'existing suite (failure set pre == post)',
    'GridItem': 'GridSetDataRedraw_Test, GridCheckLanes_Test, GridResidency_Test, GridIssue700_Test, existing suite',
    'GridItemDataVisualizer': 'GridResidency_Test, existing suite',
    'GridModelJobs': 'GridModelJobs_Test',
}


def members(root):
    result = {}
    for dirpath, _, files in os.walk(root):
        for f in files:
            if not f.endswith('.java'):
                continue
            path = os.path.join(dirpath, f)
            cls = f[:-5]
            lines = open(path, encoding='utf-8').read().split('\n')
            for i, line in enumerate(lines):
                m = SIG.match(line)
                if m:
                    sig = re.sub(r'\s+', ' ', line.strip().split('{')[0]).strip()
                    params = re.sub(r'\b(final)\b\s*', '', m.group(3))
                    key = (cls, m.group(2) + '(' + ','.join(p.strip().rsplit(' ', 1)[0] for p in params.split(',') if p.strip()) + ')')
                    result[key] = (sig, body(lines, i), os.path.relpath(path, root).replace('\\', '/'))
                    continue
                m = FIELD.match(line)
                if m and '(' not in line.split('=')[0]:
                    key = (cls, m.group(2))
                    result[key] = (line.strip(), line.strip(), os.path.relpath(path, root).replace('\\', '/'))
    return result


def body(lines, start):
    depth = 0
    text = []
    began = False
    for line in lines[start:]:
        text.append(line)
        depth += line.count('{') - line.count('}')
        if '{' in line:
            began = True
        if began and depth <= 0:
            break
        if not began and line.rstrip().endswith(';'):
            break
    return '\n'.join(text)


pre = members(pre_root)
post = members(post_root)
rows = []
for key in sorted(set(pre) | set(post)):
    cls, member = key
    if key in pre and key not in post:
        status = 'REMOVED'
    elif key not in pre:
        status = 'ADDED'
    elif pre[key][1] == post[key][1]:
        status = 'UNCHANGED'
    else:
        status = 'BODY_CHANGED'
    file = (post.get(key) or pre.get(key))[2]
    evidence = 'existing suite (failure set pre == post)' if status == 'UNCHANGED' else EVIDENCE.get(
        cls, 'model tests: RowPermutation_Test, RowGrouping_Test, RowBitLanes_Test, RowStateLanes_Test, HeapGrowth_Test'
        if '/model/' in file else 'existing suite')
    rows.append((cls, member, status, file, evidence))
with open(out, 'w', encoding='utf-8', newline='\n') as fh:
    fh.write('type\tmember\tstatus\tfile\tevidence\n')
    for r in rows:
        fh.write('\t'.join(r) + '\n')
counts = {}
for r in rows:
    counts[r[2]] = counts.get(r[2], 0) + 1
print(counts)
for r in rows:
    if r[2] in ('BODY_CHANGED', 'REMOVED'):
        print(r[2], r[0], r[1])
