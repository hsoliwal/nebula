# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-10): setters of widgets/figures that store a field read by paint code (paint*, fillShape,
outlineShape, paintControl handlers) but neither repaint, redraw, revalidate, refresh, layout, update nor fire anything:
the new value shows only at the next unrelated repaint. Usage: stale_setter_scan.py <root> <out>"""
import os, re, sys
root, out = sys.argv[1], sys.argv[2]
BS = chr(92)
ext = re.compile(r'extends\s+\w*(Figure|Label|Shape|Canvas|Composite|Control|Widget|Button|Polyline|Ellipse|Panel)\b')
setter = re.compile(r'public\s+void\s+(set\w+)\s*\(\s*(?:final\s+)?[\w<>\[\],. ]+\s+(\w+)\s*\)\s*\{')
paint = re.compile(r'(?:protected|public|private)?\s*void\s+(paint\w*|fillShape|outlineShape|drawFigure|onPaint\w*)\s*\([^)]*\)\s*\{')
act = re.compile(r'repaint|redraw|revalidate|refresh|layout|update|fire|invalidate|notify|super\.set|markDirty|dirty|set[A-Z]\w*\(')
def body(t, i):
    depth, j = 1, i
    while depth and j < len(t):
        depth += {'{': 1, '}': -1}.get(t[j], 0); j += 1
    return t[i:j - 1]
rows = []
for d, _, fs in os.walk(root):
    dl = d.replace(BS, '/').lower()
    if '.test' in dl or 'snippet' in dl or 'example' in dl:
        continue
    for f in fs:
        if not f.endswith('.java'):
            continue
        p = os.path.join(d, f)
        t = open(p, encoding='utf-8', errors='replace').read()
        if not ext.search(t):
            continue
        painted = " ".join(body(t, m.end()) for m in paint.finditer(t))
        if not painted:
            continue
        for m in setter.finditer(t):
            b = body(t, m.end())
            fields = re.findall(r'(?:this\.)?(\w+)\s*=\s*' + re.escape(m.group(2)) + r'\b', b)
            if not fields or act.search(b) or b.count('\n') > 8:
                continue
            hit = [x for x in fields if re.search(r'\b' + re.escape(x) + r'\b', painted)]
            if hit:
                ln = t[:m.start()].count('\n') + 1
                rel = os.path.relpath(p, root).replace(BS, '/')
                rows.append(f"{rel}:{ln}\t{m.group(1)}\t{','.join(hit)}")
open(out, 'w', encoding='utf-8', newline='').write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
