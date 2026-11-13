# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-13): composites that never paint themselves (no Paint listener on this, no paintControl
of their own) but call their own redraw() from setters. Control.redraw() does not reach children, so the children that
do the painting keep the old pixels (Carousel.setSelection, Nebula #384). Lists the class and the setters.
Usage: composite_self_redraw_scan.py <root> <out>"""
import os
import re
import sys

root, out = sys.argv[1], sys.argv[2]
BS = chr(92)
ext = re.compile(r"class\s+\w+(?:<[^>]*>)?\s+extends\s+(Composite|Group|ScrolledComposite|SashForm)\b")
self_paint = re.compile(r"(?<![\w.])(?:this\.)?add(?:Paint)?Listener\s*\(\s*(?:SWT\.Paint\b|new PaintListener|this::|\w+ ->|e ->|event ->)"
                        r"|(?<![\w.])addPaintListener\s*\(")
setter = re.compile(r"public\s+void\s+(set\w+)\s*\([^)]*\)\s*\{")
own_redraw = re.compile(r"(?<![\w.])redraw\s*\(\s*\)\s*;")
rows = []
for d, _, fs in os.walk(root):
    dl = d.replace(BS, "/").lower()
    if ".test" in dl or "snippet" in dl or "example" in dl:
        continue
    for f in fs:
        if not f.endswith(".java"):
            continue
        p = os.path.join(d, f)
        t = open(p, encoding="utf-8", errors="replace").read()
        if not ext.search(t) or self_paint.search(t):
            continue
        hits = []
        for m in setter.finditer(t):
            i = j = m.end()
            depth = 1
            while depth and j < len(t):
                depth += {"{": 1, "}": -1}.get(t[j], 0)
                j += 1
            if own_redraw.search(t[i:j - 1]):
                hits.append(m.group(1))
        if hits:
            rows.append(f"{os.path.relpath(p, root).replace(BS, '/')}\t{','.join(hits)}")
open(out, "w", encoding="utf-8", newline="").write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
