# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-10): dispose() overrides of SWT controls and figures; SWT does not call dispose() on
children when an ancestor is disposed, so what such an override does outside the subtree (unhook listeners or
filters from long-lived owners, free resources, dispose other shells) is skipped then: move it to SWT.Dispose.
Modes: unhook (remove*Listener/removeFilter/unregister/deleteObserver calls), free (.dispose() calls), all (any call).
Usage: dispose_override_scan.py <root> <out> [unhook|free|all]"""
import os, re, sys
root, out = sys.argv[1], sys.argv[2]
mode = sys.argv[3] if len(sys.argv) > 3 else 'unhook'
BS = chr(92)
pat = re.compile(r'public\s+void\s+dispose\s*\(\s*\)\s*\{')
ext = re.compile(r'extends\s+\w*(Composite|Canvas|Control|Scrollable|Shell|Widget|Renderer|Button|Label|Text)\b')
CALLS = {'unhook': r'(remove\w*Listener|removeFilter|unregister\w*|deleteObserver|removeObserver)', 'free': r'(dispose)', 'all': r'(\w+)'}
call = re.compile(r'([\w.()]+?)\s*\.\s*' + CALLS[mode] + r'\s*\(')
rows = []
for d, _, fs in os.walk(root):
    dl = d.replace(BS, '/').lower()
    if '.tests' in dl or 'snippet' in dl or 'example' in dl:
        continue
    for f in fs:
        if not f.endswith('.java'):
            continue
        p = os.path.join(d, f)
        t = open(p, encoding='utf-8', errors='replace').read()
        if not ext.search(t):
            continue
        for m in pat.finditer(t):
            i = j = m.end(); depth = 1
            while depth and j < len(t):
                depth += {'{': 1, '}': -1}.get(t[j], 0); j += 1
            calls = call.findall(t[i:j - 1])
            calls = [c for c in calls if c[0] != 'super']
            if calls:
                ln = t[:m.start()].count('\n') + 1
                rel = os.path.relpath(p, root).replace(BS, '/')
                rows.append(f"{rel}:{ln}\t" + ", ".join(f"{a}.{b}" for a, b in calls))
open(out, 'w', encoding='utf-8', newline='').write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
