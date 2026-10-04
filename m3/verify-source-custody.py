# SPDX-License-Identifier: EPL-2.0
"""Read-only exact-byte custody for the five previously admitted owner paths."""
from hashlib import sha256
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


def blob(revision, path):
    return subprocess.check_output(["git", "show", revision + ":" + path])


def require(condition, reason):
    if not condition:
        raise SystemExit(reason)


def profile_shape(raw):
    ns = "{http://maven.apache.org/POM/4.0.0}"
    root = ET.fromstring(raw)
    profiles = [p for p in root.findall(ns + "profiles/" + ns + "profile")
                if p.findtext(ns + "id") == "m3-offline-p2"]
    require(len(profiles) == 1, "exactly one admitted p2 profile required")
    profile = profiles[0]
    require(not profile.attrib and [c.tag for c in profile] == [ns + "id", ns + "properties"],
            "p2 profile must retain exact opt-in shape")
    properties = profile[1]
    names = ["target-platform-platform", "target-platform-gef", "target-platform-swtbot"]
    require(not properties.attrib and [c.tag for c in properties] == [ns + x for x in names],
            "p2 profile property shape drift")
    require(all(not c.attrib and len(c) == 0 and c.text == "${m3.p2.mirror.url}" for c in properties),
            "p2 mirror properties drift")


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
    profile_hash = "7bd62f0f528e3955a14aaa79fa693f758d6e2f779ec569d837aa410cec9ab408"
    post_hash = sha256(post).hexdigest()
    require(post_hash in {expected, profile_hash}, "admitted parent postimage drift")
    if post_hash == profile_hash:
        profile_shape(post)
    require(sha256(pre).hexdigest() in {"255bd1e72e6e6368fdfbd76c6cdcf392d28857a97678290acb8ebbefd2766103", expected, profile_hash},
            "parent aggregation preimage drift")
    print(parent + "\tPASS\t" + sha256(pre).hexdigest() + "\t" + post_hash)


if __name__ == "__main__":
    require(len(sys.argv) == 2, "BASE_REVISION required")
    verify(sys.argv[1])
