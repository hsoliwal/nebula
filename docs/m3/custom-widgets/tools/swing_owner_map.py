#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""swing_owner_map.py <WIDGET_PLATFORM_MATRIX.tsv> <swingx class list> [--check]

One owner per concept for the Swing column of the widget platform matrix (no-sprawl rule, 2026-11-08: no new public
JDK classes without the user's admission; no second owner of a concept):
1. A row whose Swing cell is ADAPTER_NEEDED and whose widget already has a Swing implementation of the same name in the
   SwingX fork (hsoliwal/swingx, org.jdesktop.swingx) becomes AVAILABLE with that class: the planned
   javax.swing.M3<Name> twin would be a second owner.
2. Every remaining ADAPTER_NEEDED row keeps its gap, but its Swing target moves from a new javax.swing.M3<Name> class in
   java.desktop to the SwingX fork (org.jdesktop.swingx, over the shared model): the Swing counterparts of custom widgets
   grow a library the programme owns, not the JDK's public API.
Only the swing_m3jdk, swing_class and gap_plan cells change; no cell names a javax.swing.M3 class afterwards. Idempotent; --check writes nothing and exits 1 on change."""
import csv
import io
import re
import sys
from pathlib import Path

MATRIX, CLASSES = Path(sys.argv[1]), Path(sys.argv[2])
CHECK = "--check" in sys.argv
text = MATRIX.read_text(encoding="utf-8")
rows = list(csv.reader(text.splitlines(), delimiter="\t"))
head, body = rows[0], rows[1:]
ix = {c: i for i, c in enumerate(head)}
swingx = {w for line in CLASSES.read_text(encoding="utf-8").splitlines() if not line.startswith("#") for w in line.split()}
available = retargeted = 0
for r in body:
    cell = r[ix["swing_m3jdk"]]
    if not cell.startswith("ADAPTER_NEEDED"):
        continue
    widget = r[ix["widget"]]
    owner = widget if widget in swingx else ("JX" + widget if "JX" + widget in swingx else None)
    if owner:
        r[ix["swing_m3jdk"]] = f"AVAILABLE (SwingX org.jdesktop.swingx.{owner})"
        r[ix["swing_class"]] = f"org.jdesktop.swingx.{owner}"
        plan = re.sub(r"javax\.swing\.M3\w+", f"org.jdesktop.swingx.{owner}", r[ix["gap_plan"]])
        r[ix["gap_plan"]] = (plan + " | one owner per concept (2026-11-08): the SwingX fork already "
                             f"implements {owner}; no new JDK twin")
        available += 1
    else:
        new_cell = re.sub(r"javax\.swing\.M3(\w+)", r"org.jdesktop.swingx (SwingX fork) \1", cell)
        new_plan = re.sub(r"javax\.swing\.M3(\w+)", r"org.jdesktop.swingx \1 (SwingX fork)", r[ix["gap_plan"]])
        if (new_cell, new_plan) != (cell, r[ix["gap_plan"]]) or "no new JDK class" not in new_plan:
            r[ix["swing_m3jdk"]] = new_cell
            r[ix["swing_class"]] = re.sub(r"^javax\.swing\.M3(\w+)$", r"org.jdesktop.swingx.\g<1> (planned, SwingX fork)",
                                          r[ix["swing_class"]])
            r[ix["gap_plan"]] = new_plan + ("" if "no new JDK class" in new_plan else
                                            " | Swing target: the SwingX fork, no new JDK class (no-sprawl 2026-11-08)")
            retargeted += 1
out = io.StringIO()
writer = csv.writer(out, delimiter="\t", lineterminator="\n", quoting=csv.QUOTE_MINIMAL)
writer.writerows([head] + body)
new = out.getvalue()
if not text.endswith("\n"):
    new = new.rstrip("\n")
changed = new != text
if changed and not CHECK:
    MATRIX.write_text(new, encoding="utf-8", newline="")
print(f"SWING_OWNER_MAP available={available} retargeted={retargeted} changed={changed} check={CHECK}")
sys.exit(1 if CHECK and changed else 0)
