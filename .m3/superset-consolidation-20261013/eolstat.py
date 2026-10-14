"""Print line-ending counts: python eolstat.py FILE... (or '-' for stdin).

Columns: lines, crlf, lf_only, path.
"""
import sys


def stat(data):
    lines = data.count(b"\n")
    crlf = data.count(b"\r\n")
    return lines, crlf, lines - crlf


for path in sys.argv[1:]:
    if path == "-":
        data = sys.stdin.buffer.read()
    else:
        with open(path, "rb") as f:
            data = f.read()
    print("%d\t%d\t%d\t%s" % (stat(data) + (path,)))
