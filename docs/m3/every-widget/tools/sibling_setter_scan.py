# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-10): widgets and figures where some setters damage (redraw/repaint/refresh/revalidate)
and sibling style setters (Color, Font, Image, boolean, int, float, double params) only assign: the latter show at the
next unrelated repaint. --swing scans Swing components (repaint, revalidate, firePropertyChange; Icon, Paint, Border params).
Usage: sibling_setter_scan.py <root> <out> [--swing]"""
import os, re, sys
root, out = sys.argv[1], sys.argv[2]
SWING = '--swing' in sys.argv[3:]
BS = chr(92)
ext = re.compile(r'extends\s+\w*(Component|Panel|Label|Button|Pane|Field|Area|List|Table|Tree|Bar|Box|Picker|View|Editor|Painter)\b' if SWING else r'extends\s+\w*(Figure|Label|Shape|Canvas|Composite|Control|Button|Polyline|Ellipse|Panel)\b')
setter = re.compile(r'public\s+void\s+(set\w+)\s*\(\s*(?:final\s+)?(Color|Font|Image|Icon|Paint|Painter|Border|Insets|boolean|int|float|double)\s+(\w+)\s*\)\s*\{' if SWING else r'public\s+void\s+(set\w+)\s*\(\s*(?:final\s+)?(Color|Font|Image|boolean|int|float|double|RGB)\s+(\w+)\s*\)\s*\{')
damage = re.compile(r'\b(redraw|repaint|refresh|revalidate|layout|update\w*|fire\w*|invalidate\w*|setDirty|firePropertyChange)\s*\(' if SWING else r'\b(redraw|repaint|refresh|revalidate|layout|update\w*|fire\w*|invalidate\w*)\s*\(')
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
        good, bare = [], []
        for m in setter.finditer(t):
            b = body(t, m.end())
            if damage.search(b) or re.search(r'\bset[A-Z]\w*\(', b) or 'super.' in b:
                good.append(m.group(1))
            elif re.fullmatch(r'\s*(checkWidget\(\);\s*)?(this\.)?\w+\s*=\s*' + re.escape(m.group(3)) + r';\s*', b):
                bare.append(m.group(1))
        if good and bare:
            rel = os.path.relpath(p, root).replace(BS, '/')
            rows.append(f"{rel}\tbare={len(bare)}:{','.join(bare)}\tdamaging={len(good)}")
open(out, 'w', encoding='utf-8', newline='').write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
