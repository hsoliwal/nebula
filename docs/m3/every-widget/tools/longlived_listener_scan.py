# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-10): listeners added to long-lived owners (Display listeners and filters, JFace colour/font
registries, preference stores, PlatformUI, static singletons) with no matching removal in the same file.
Usage: longlived_listener_scan.py <root> <out>"""
import os, re, sys
root, out = sys.argv[1], sys.argv[2]
BS = chr(92)
add = re.compile(r'([\w.()]*(?:[Dd]isplay|Registry|PreferenceStore|getPreferenceStore\(\)|PlatformUI[\w.()]*|JFaceResources[\w.()]*)(?:\(\))?)\s*\.\s*(add(\w*Listener|Filter))\s*\(')
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
        for m in add.finditer(t):
            kind = m.group(3)
            remove = 'remove' + kind
            if remove not in t:
                ln = t[:m.start()].count('\n') + 1
                rel = os.path.relpath(p, root).replace(BS, '/')
                rows.append(f"{rel}:{ln}\t{m.group(1)}.{m.group(2)}")
open(out, 'w', encoding='utf-8', newline='').write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
