# SPDX-License-Identifier: EPL-2.0
"""Source-level census of public/protected declarations, base vs head (CPU only, no JVM).

usage: python -I api_census_source.py <nebula-repo> <base> <head> <api_evidence.tsv> <out.tsv>

Bundles: xygraph, visualization widgets, oscilloscope (main src only). A declaration is a line whose first token is
public/protected (comments stripped), plus abstract-looking method lines inside interfaces. Interim for the javap
listing (api_coverage.py), which is authoritative once it can be run.
"""
import re
import subprocess
import sys

repo, base, head, evid, out = sys.argv[1:6]
ROOTS = ['widgets/visualization/org.eclipse.nebula.visualization.xygraph/src/',
         'widgets/visualization/org.eclipse.nebula.visualization.widgets/src/',
         'widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope/src/']

def git(*a):
    return subprocess.run(['git', '-C', repo] + list(a), check=True, capture_output=True).stdout.decode('utf-8', 'replace')

def files(rev):
    out = {}
    for root in ROOTS:
        for line in git('ls-tree', '-r', rev, root).splitlines():
            meta, path = line.split('\t', 1)
            if path.endswith('.java'):
                out[path] = meta.split()[2]
    return out

def strip_comments(text):
    text = re.sub(r'/\*.*?\*/', lambda m: '\n' * m.group(0).count('\n'), text, flags=re.S)
    text = re.sub(r'//[^\n]*', '', text)
    text = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
    return text

DECL = re.compile(r'^\s*(?:@\w+(?:\([^)]*\))?\s+)*((?:public|protected)\b[^{;=]*)')
IFACE_M = re.compile(r'^\s*(?:default\s+|static\s+)?[\w<>\[\],.? ]+\s+\w+\s*\([^)]*\)\s*(?:throws [\w., ]+)?;')

def decls(text):
    text = strip_comments(text)
    is_iface = re.search(r'\b(public\s+)?interface\s+\w+', text) is not None and not re.search(r'\bclass\s+\w+', text)
    res = set()
    for line in text.splitlines():
        m = DECL.match(line)
        if m:
            sig = re.sub(r'\s+', ' ', m.group(1)).strip()
            res.add(sig)
        elif is_iface and IFACE_M.match(line):
            res.add('(interface) ' + re.sub(r'\s+', ' ', line.strip().rstrip(';')))
    return res

def type_of(path):
    return path.split('/src/', 1)[1][:-5].replace('/', '.')

ev = {}
for line in open(evid, encoding='utf-8'):
    if line.startswith('#') or not line.strip():
        continue
    k, v = line.rstrip('\n').split('\t', 1)
    ev[k] = v

fb, fh = files(base), files(head)
rows = []
for path in sorted(set(fb) | set(fh)):
    t = type_of(path)
    if path in fb and path in fh and fb[path] == fh[path]:
        for d in sorted(decls(git('show', head + ':' + path))):
            rows.append((t, d, 'UNCHANGED_SOURCE', 'git blob ' + fh[path][:12] + ' identical base vs head'))
        continue
    b = decls(git('show', base + ':' + path)) if path in fb else set()
    h = decls(git('show', head + ':' + path)) if path in fh else set()
    e = ev.get(t, 'NO_EVIDENCE')
    for d in sorted(b | h):
        if d in b and d in h:
            rows.append((t, d, 'EXISTING_IN_CHANGED_TYPE', e))
        elif d in h:
            name = re.findall(r'(\w+)\s*\(', d)
            key = t + '#' + name[0] if name else t
            rows.append((t, d, 'NEW_ADDITIVE', ev.get(key, e)))
        else:
            rows.append((t, d, 'REMOVED', 'API BREAK'))
with open(out, 'w', encoding='utf-8', newline='\n') as f:
    f.write('type\tdeclaration\tstatus\tevidence\n')
    for r in rows:
        f.write('\t'.join(c.replace('\t', ' ') for c in r) + '\n')
counts = {}
for r in rows:
    counts[r[2]] = counts.get(r[2], 0) + 1
print('rows', len(rows), counts, 'types', len(set(r[0] for r in rows)))
