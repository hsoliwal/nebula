"""For a branch, measure how many of its added lines already exist in HEAD's version of each file.

usage: python absorbed.py REPO BRANCH_REF
Prints per file: added_lines, present_in_head, ratio, path; then a total.
Lines are compared after stripping trailing CR/whitespace; blank lines are ignored.
"""
import subprocess
import sys


def git(repo, *args):
    return subprocess.run(["git", "-C", repo, *args], capture_output=True, check=False).stdout


def main():
    repo, ref = sys.argv[1], sys.argv[2]
    mb = git(repo, "merge-base", "HEAD", ref).decode().strip()
    diff = git(repo, "diff", "--no-color", "-U0", mb, ref).decode("utf-8", "replace")
    added = {}
    path = None
    for line in diff.splitlines():
        if line.startswith("+++ "):
            path = line[6:] if line.startswith("+++ b/") else None
        elif line.startswith("+") and not line.startswith("+++") and path:
            text = line[1:].rstrip()
            if text.strip():
                added.setdefault(path, []).append(text)
    total_a = total_p = 0
    for p, lines in sorted(added.items()):
        head = git(repo, "show", "HEAD:" + p).decode("utf-8", "replace")
        have = set(l.rstrip() for l in head.splitlines())
        present = sum(1 for l in lines if l in have)
        total_a += len(lines)
        total_p += present
        print("%d\t%d\t%.2f\t%s" % (len(lines), present, present / len(lines), p))
    if total_a:
        print("TOTAL\t%d\t%d\t%.2f" % (total_a, total_p, total_p / total_a))


if __name__ == "__main__":
    main()
