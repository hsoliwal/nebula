#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Mechanical public/protected API extractor for the Nebula tree/table family.

Usage: python -I api_surface.py <nebula-root> <out.tsv> [widget ...]

Scans each widget's main bundle sources (tests, examples, snippets, features
excluded), masks comments and string/char literals, tracks brace depth and the
enclosing type stack, and records every public/protected member of a public or
protected type. No body parsing: only declaration headers (IDL-LAW).
"""
import hashlib
import os
import re
import sys

WIDGETS = ["xviewer", "ctree", "ctreecombo", "tablecombo", "calendarcombo", "compositetable"]
SKIP = re.compile(r"(\.tests?|\.example|\.snippets|\.feature|\.integration|\.css)(\.|/|\\|$)")
TYPE_RE = re.compile(r"\b(class|interface|enum|record|@interface)\s+([A-Za-z_]\w*)")
MOD_RE = re.compile(r"^(?:@\w+(?:\([^)]*\))?\s+)*((?:public|protected|private|static|final|abstract|synchronized|native|default|transient|volatile|sealed|non-sealed|strictfp)\s+)*")


def mask(src):
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith("//", i):
            j = src.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i)); i = j
        elif src.startswith("/*", i):
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j])); i = j
        elif src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j < 0 else j + 3
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j])); i = j
        elif c in "\"'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            j = min(j + 1, n)
            out.append(c + " " * (j - i - 2) + c if j - i >= 2 else c); i = j
        else:
            out.append(c); i += 1
    return "".join(out)


def visibility(header):
    if re.search(r"\bpublic\b", header):
        return "public"
    if re.search(r"\bprotected\b", header):
        return "protected"
    if re.search(r"\bprivate\b", header):
        return "private"
    return "package"


def members(path):
    raw = open(path, encoding="utf-8", errors="replace").read()
    src = mask(raw)
    stack = []  # (name, depth_inside, exported, kind)
    depth, start, i, n = 0, 0, 0, len(src)
    rows = []
    while i < n:
        c = src[i]
        if c in "{;}":
            header = " ".join(src[start:i].split())
            line = src.count("\n", 0, start + (len(src[start:i]) - len(src[start:i].lstrip()))) + 1
            in_type = stack and stack[-1][1] == depth
            if c == "{":
                m = TYPE_RE.search(header)
                if m and "(" not in header.split(m.group(0))[0] and (not stack or in_type):
                    vis = visibility(header)
                    iface_parent = stack and stack[-1][3] in ("interface", "@interface")
                    exported = (vis in ("public", "protected") or (iface_parent and vis == "package")) and (not stack or stack[-1][2])
                    if exported:
                        rows.append((".".join([s[0] for s in stack] + [m.group(2)]), m.group(1), vis if vis != "package" else "public", header, line))
                    depth += 1
                    stack.append((m.group(2), depth, exported, m.group(1)))
                    start = i + 1; i += 1; continue
                if in_type and stack[-1][2] and header and not header.startswith(("static", "{")):
                    record(rows, stack, header, line)
                elif in_type and stack[-1][2] and stack[-1][3] == "enum" and header:
                    pass
                depth += 1
            elif c == ";":
                if in_type and stack[-1][2] and header:
                    if stack[-1][3] == "enum" and "(" not in header.split("=")[0] and not visibility(header) in ("public", "protected"):
                        for const in [x.strip().split("(")[0].strip() for x in header.split(",") if x.strip()]:
                            if re.match(r"^[A-Za-z_]\w*$", const):
                                rows.append((".".join(s[0] for s in stack), "enum_constant", "public", const, line))
                    else:
                        record(rows, stack, header, line)
            else:
                if stack and stack[-1][1] == depth:
                    stack.pop()
                depth -= 1
            start = i + 1
        i += 1
    return rows


def record(rows, stack, header, line):
    owner_kind = stack[-1][3]
    vis = visibility(header)
    if owner_kind in ("interface", "@interface") and vis == "package":
        vis = "public"
    if vis not in ("public", "protected"):
        return
    head = header.split("=")[0].strip() if "(" not in header.split("=")[0] else header
    if "(" in head:
        sig = head[: head.rfind(")") + 1] if ")" in head else head
        name = re.findall(r"([A-Za-z_]\w*)\s*\(", sig)
        kind = "constructor" if name and name[0] == stack[-1][0] else "method"
        rows.append((".".join(s[0] for s in stack), kind, vis, " ".join(sig.split()), line))
    else:
        rows.append((".".join(s[0] for s in stack), "field", vis, " ".join(head.split()), line))


def main():
    root, out = sys.argv[1], sys.argv[2]
    widgets = sys.argv[3:] or WIDGETS
    lines = ["api_id\twidget\tbundle\tpackage\ttype\tkind\tvisibility\tsignature\tfile\tline"]
    for w in widgets:
        base = os.path.join(root, "widgets", w)
        files = []
        for d, _, fs in os.walk(base):
            rel = os.path.relpath(d, base).replace("\\", "/")
            if SKIP.search(rel + "/") or "/src" not in "/" + rel:
                continue
            files += [os.path.join(d, f) for f in fs if f.endswith(".java")]
        for f in sorted(files):
            rel = os.path.relpath(f, root).replace("\\", "/")
            bundle = rel.split("/")[2]
            pkg = ""
            m = re.search(r"^\s*package\s+([\w.]+)\s*;", open(f, encoding="utf-8", errors="replace").read(), re.M)
            if m:
                pkg = m.group(1)
            for t, kind, vis, sig, line in members(f):
                key = "|".join([w, pkg, t, kind, sig])
                aid = hashlib.sha256(key.encode()).hexdigest()[:12]
                lines.append("\t".join([aid, w, bundle, pkg, t, kind, vis, sig, rel, str(line)]))
    with open(out, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    print(len(lines) - 1)


if __name__ == "__main__":
    main()
