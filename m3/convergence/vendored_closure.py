# SPDX-License-Identifier: EPL-2.0
"""Map a receiver pom's source-built dependencies to M3jdk21 vendored module directories.

The public M3JDK21 custody receiver pins coordinates such as
org.openrewrite:rewrite-core:8.17.1-m3-source-1. Those versions are never published to
Maven Central; they exist only after M3jdk21's m3/vendor/third_party reactor installs them.
This resolver prints the comma-separated module directories (for ``mvn -pl ... -am``) that
own every non-test ``-m3-source-1`` dependency of the receiver, and fails when any one is
missing, duplicated or version-drifted. It never substitutes a binary artifact.

Usage: vendored_closure.py RECEIVER_POM VENDOR_AGGREGATOR_DIR
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"
SUFFIX = "-m3-source-1"


def _text(node, name):
    value = node.findtext(NS + name)
    return value.strip() if value else ""


def _properties(root):
    props = root.find(NS + "properties")
    return {} if props is None else {child.tag[len(NS):]: (child.text or "").strip() for child in props}


def _expand(value, props):
    for key, replacement in props.items():
        value = value.replace("${" + key + "}", replacement)
    return value


def source_built_dependencies(receiver_pom):
    """Return sorted (groupId, artifactId, version) for non-test -m3-source-1 dependencies."""
    root = ET.parse(receiver_pom).getroot()
    props = _properties(root)
    wanted = set()
    for dep in root.findall(NS + "dependencies/" + NS + "dependency"):
        version = _expand(_text(dep, "version"), props)
        if version.endswith(SUFFIX) and _text(dep, "scope") != "test":
            wanted.add((_text(dep, "groupId"), _text(dep, "artifactId"), version))
    return sorted(wanted)


def vendored_modules(aggregator_dir):
    """Return {(groupId, artifactId, version): [module dir]} for modules listed by the aggregator."""
    aggregator = Path(aggregator_dir)
    root = ET.parse(aggregator / "pom.xml").getroot()
    owners = {}
    for module in root.findall(NS + "modules/" + NS + "module"):
        name = (module.text or "").strip()
        pom = ET.parse(aggregator / name / "pom.xml").getroot()
        parent = pom.find(NS + "parent")
        group = _text(pom, "groupId") or (_text(parent, "groupId") if parent is not None else "")
        version = _text(pom, "version") or (_text(parent, "version") if parent is not None else "")
        owners.setdefault((group, _text(pom, "artifactId"), version), []).append(name)
    return owners


def resolve(receiver_pom, aggregator_dir):
    wanted = source_built_dependencies(receiver_pom)
    if not wanted:
        raise SystemExit("receiver declares no " + SUFFIX + " dependency; closure binding drift")
    owners = vendored_modules(aggregator_dir)
    selected = []
    for gav in wanted:
        dirs = owners.get(gav, [])
        if len(dirs) != 1:
            raise SystemExit("vendored module for %s:%s:%s: expected exactly one, found %d"
                             % (gav + (len(dirs),)))
        selected.append(dirs[0])
    return selected


def main(argv):
    if len(argv) != 3:
        raise SystemExit("usage: vendored_closure.py RECEIVER_POM VENDOR_AGGREGATOR_DIR")
    print(",".join(resolve(argv[1], argv[2])))


if __name__ == "__main__":
    main(sys.argv)
