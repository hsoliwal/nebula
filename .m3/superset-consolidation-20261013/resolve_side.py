"""Resolve git conflict blocks in place by keeping one side, byte-exact.

usage: python resolve_side.py ours|theirs|union FILE...

Each block '<<<<<<< ... [||||||| ...] ======= ... >>>>>>> ...' is replaced by
the chosen side's lines (union: ours then theirs); all other bytes (including CR/LF) are kept as is.
Prints, per file, the number of blocks resolved.
"""
import sys


def resolve(data, side):
    out = []
    state = 0  # 0 outside, 1 ours, 2 base, 3 theirs
    blocks = 0
    for line in data.splitlines(keepends=True):
        bare = line.rstrip(b"\r\n")
        if state == 0 and bare.startswith(b"<<<<<<< "):
            state = 1
            blocks += 1
            continue
        if state == 1 and bare.startswith(b"||||||| "):
            state = 2
            continue
        if state in (1, 2) and bare == b"=======":
            state = 3
            continue
        if state == 3 and bare.startswith(b">>>>>>> "):
            state = 0
            continue
        if state == 0 or (state == 1 and side in ("ours", "union")) or (state == 3 and side in ("theirs", "union")):
            out.append(line)
    if state != 0:
        raise SystemExit("unterminated conflict block")
    return b"".join(out), blocks


def main():
    side = sys.argv[1]
    for path in sys.argv[2:]:
        with open(path, "rb") as f:
            data = f.read()
        res, blocks = resolve(data, side)
        with open(path, "wb") as f:
            f.write(res)
        print("%d\t%s" % (blocks, path))


if __name__ == "__main__":
    main()
