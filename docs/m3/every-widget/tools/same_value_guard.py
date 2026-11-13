# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""every-widget recipe (2026-11-13): give simple repainting setters a same-value guard. A setter matches only when its
body is exactly: [checkWidget();] [check<Name>(<param>); ...] [<owner>.]<field> = <param>; then one or more of
[<owner>.]redraw(); / repaint(); / update(); and nothing else. The guard goes after the checks (an invalid argument
still throws): primitives compare with ==, references with Objects.equals (java.util.Objects
is imported when missing). Setters of other shapes are left alone and listed with --list.
--mark "<note>" puts one //FLLDCIM: MARK — <note> anchor on the first guard (give the note without slashes).
Usage: same_value_guard.py <file.java> [--only name,name] [--list] [--mark "<note>"]"""
import re
import sys

src = sys.argv[1]
args = sys.argv[2:]
only = None
mark = None
if "--only" in args:
    only = set(args[args.index("--only") + 1].split(","))
if "--mark" in args:
    mark = args[args.index("--mark") + 1]
listing = "--list" in args
PRIMS = {"int", "boolean", "long", "float", "double", "char", "short", "byte"}
t = open(src, encoding="utf-8", newline="").read()
setter = re.compile(r"([ \t]*)public\s+void\s+(set\w+)\s*\(\s*(final\s+)?([\w<>\[\].]+)\s+(\w+)\s*\)\s*\{\n")
body_ok = re.compile(
    r"(?P<check>(?:\s*checkWidget\s*\(\s*\)\s*;\n)?(?:\s*check\w+\s*\(\s*\w+\s*\)\s*;\n)*)"
    r"(?P<assign>\s*(?P<owner>this\.|\w+\.)?(?P<field>\w+)\s*=\s*(?P<value>\w+)\s*;\n)"
    r"(?P<paint>(?:\s*(?:\w+\.)?(?:redraw|repaint|update)\s*\(\s*\)\s*;\n)+)\s*$")
out, pos, done, skipped = [], 0, [], []
for m in setter.finditer(t):
    i = j = m.end()
    depth = 1
    while depth and j < len(t):
        depth += {"{": 1, "}": -1}.get(t[j], 0)
        j += 1
    close = t.rindex("\n", i, j) + 1  # start of the line holding the closing brace
    body = t[i:close]
    name, typ, param = m.group(2), m.group(4), m.group(5)
    b = body_ok.fullmatch(body)
    if not b or b.group("value") != param or (only is not None and name not in only):
        if re.search(r"\b(redraw|repaint)\s*\(", body) and not re.search(r"==|!=|equals\(", body):
            skipped.append(name)
        continue
    field = b.group("field")
    owner = b.group("owner") or ""
    left = owner + field if owner not in ("", "this.") else (field if field != param else "this." + field)
    indent = re.match(r"\s*", b.group("assign")).group(0).lstrip("\n")
    cond = f"{left} == {param}" if typ in PRIMS else f"Objects.equals({left}, {param})"
    note = f" //FLLDCIM: MARK — {mark}" if mark and not done else ""
    guard = f"{indent}if ({cond}) {{{note}\n{indent}\treturn;\n{indent}}}\n"
    head = b.group("check") or ""
    out.append(t[pos:i] + head + guard + body[len(head):])
    pos = close
    done.append(name)
out.append(t[pos:])
t2 = "".join(out)
if done and "Objects.equals(" in t2 and "import java.util.Objects;" not in t2:
    imports = [x for x in re.finditer(r"^import [^;]+;\n", t2, re.M)]
    util = [x for x in imports if x.group(0).startswith("import java.util.")]
    at = (util[-1] if util else imports[0]).end() if imports else 0
    if util or not imports:
        t2 = t2[:at] + "import java.util.Objects;\n" + t2[at:]
    else:
        t2 = t2[:imports[0].start()] + "import java.util.Objects;\n\n" + t2[imports[0].start():]
if listing:
    print("would guard:", ", ".join(done) or "-")
    print("other unguarded repainting setters:", ", ".join(skipped) or "-")
else:
    open(src, "w", encoding="utf-8", newline="").write(t2)
    print(f"guarded {len(done)}: {', '.join(done) or '-'}; left {len(skipped)}: {', '.join(skipped) or '-'}")
