# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget scan (2026-11-10): Swing components that add listeners to long-lived owners (UIManager,
KeyboardFocusManager, Toolkit, static models) with no matching removal in the same file.
Usage: swing_longlived_listener_scan.py <root> <out>"""
import os, re, sys
root, out = sys.argv[1], sys.argv[2]
BS = chr(92)
add = re.compile(r'((?:UIManager|KeyboardFocusManager[\w.()]*|Toolkit[\w.()]*|getDefaultToolkit\(\)|LookAndFeelAddons|FocusManager[\w.()]*))\s*\.\s*(add(\w*(?:Listener|AWTEventListener)))\s*\(')
rows = []
for d, _, fs in os.walk(root):
    for f in fs:
        if not f.endswith('.java'):
            continue
        p = os.path.join(d, f)
        t = open(p, encoding='utf-8', errors='replace').read()
        for m in add.finditer(t):
            remove = 'remove' + m.group(3)
            ln = t[:m.start()].count('\n') + 1
            rel = os.path.relpath(p, root).replace(BS, '/')
            weak = 'Weak' in t[max(0, m.start() - 400):m.end() + 200]
            rows.append(f"{rel}:{ln}\t{m.group(1)}.{m.group(2)}\tremove={'yes' if remove in t else 'NO'}\tweak={'yes' if weak else 'no'}")
open(out, 'w', encoding='utf-8', newline='').write("\n".join(rows) + "\n")
print(len(rows), "rows ->", out)
