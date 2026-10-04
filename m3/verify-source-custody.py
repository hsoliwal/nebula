# SPDX-License-Identifier: EPL-2.0
"""Read-only exact-byte custody for the five previously admitted owner paths."""
from hashlib import sha256
from pathlib import Path
import subprocess
import sys


def blob(revision, path):
    return subprocess.check_output(["git", "show", revision + ":" + path])


def require(condition, reason):
    if not condition:
        raise SystemExit(reason)


def verify(base):
    rows = Path(__file__).with_name("source-custody.tsv").read_text(encoding="utf-8").splitlines()
    require(len(rows) == 4, "exactly four copyright paths required")
    for row in rows:
        path, before_hash, after_hash = row.split("\t")
        before, after = blob(base, path), blob("HEAD", path)
        require(sha256(after).hexdigest() == after_hash, "copyright postimage drift: " + path)
        if sha256(before).hexdigest() == after_hash:
            require(before == after, "already-admitted copyright bytes changed: " + path)
        else:
            require(sha256(before).hexdigest() == before_hash, "copyright preimage drift: " + path)
            require(before.count(b"\xa9") == 1 and all(byte < 128 or byte == 169 for byte in before),
                    "copyright input must contain exactly one non-ASCII copyright byte: " + path)
            require(b"// \xa9 Copyright IBM Corp. 2005" in before, "copyright context absent: " + path)
            require(before.replace(b"\xa9", b"\xc2\xa9") == after, "copyright bytes escaped header-only replacement: " + path)
            require(before.decode("latin-1") == after.decode("utf-8"), "copyright decoded text changed: " + path)
        print(path + "\tPASS\t" + sha256(before).hexdigest() + "\t" + sha256(after).hexdigest())
    parent = "releng/org.eclipse.nebula.nebula-parent/pom.xml"
    pre, post = blob(base, parent), blob("HEAD", parent)
    expected = "1b30a4af10a3a3b5731e45ae36afa7c07faaf7d721c4b7bf15f88c3a481fc6f4"
    require(sha256(post).hexdigest() == expected, "admitted parent aggregation postimage drift")
    require(sha256(pre).hexdigest() in {"255bd1e72e6e6368fdfbd76c6cdcf392d28857a97678290acb8ebbefd2766103", expected},
            "parent aggregation preimage drift")
    print(parent + "\tPASS\t" + sha256(pre).hexdigest() + "\t" + expected)


if __name__ == "__main__":
    require(len(sys.argv) == 2, "BASE_REVISION required")
    verify(sys.argv[1])
