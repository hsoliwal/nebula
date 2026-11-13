# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-13): setters that repaint (redraw, update, InvalidateRect, RedrawWindow, repaint) with no
equality guard (==, !=, equals) in their body: setting the value in use repaints the widget. Usage: noop_repaint_scan.py <root> <out>"""
import os, re, sys
root, out = sys.argv[1], sys.argv[2]
BS = chr(92)
setter = re.compile(r'public\s+void\s+(set\w+)\s*\(\s*(?:final\s+)?([\w<>\[\]]+)\s+(\w+)\s*\)\s*\{')
paint = re.compile(r'\b(redraw|InvalidateRect|RedrawWindow|repaint)\s*\(')
guard = re.compile(r'==|!=|\.equals\(|Objects\.equals')
rows = []
for d, _, fs in os.walk(root):
    for f in fs:
        if not f.endswith('.java'):
            continue
        p = os.path.join(d, f)
        t = open(p, encoding='utf-8', errors='replace').read()
        for m in setter.finditer(t):
            i = j = m.end(); depth = 1
            while depth and j < len(t):
                depth += {'{': 1, '}': -1}.get(t[j], 0); j += 1
            b = t[i:j - 1]
            if paint.search(b) and not guard.search(b) and b.count('\n') <= 25:
                ln = t[:m.start()].count('\n') + 1
                rows.append(f"{os.path.relpath(p, root).replace(BS, '/')}:{ln}\t{m.group(1)}({m.group(2)})")
open(out, 'w', encoding='utf-8', newline='').write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
