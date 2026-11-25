# SPDX-License-Identifier: EPL-2.0
"""Bind one exact upstream Eclipse Platform parent POM for source-built SWT."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

AGGREGATOR_COMMIT = "8c8c8d8ccaa2dab6954ae083fdd14c31d258ccec"
PARENT_BLOB = "c0b221e4b62f119e8a28fc61e9a44c568933dfa0"
PARENT_SHA256 = "121b6c26d986874391d26d114377e3beda91723b0d675146bdd194dbfe01e264"
SWT_COMMIT = "b774718b77e3932445e7e1f6d586804110669816"
NS = "{http://maven.apache.org/POM/4.0.0}"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def field(root, path):
    return root.findtext("/".join(NS + part for part in path.split("/")))


def validate_parent(data):
    root = ET.fromstring(data)
    for path, expected in (
        ("groupId", "org.eclipse"),
        ("artifactId", "eclipse-platform-parent"),
        ("version", "4.42.0-SNAPSHOT"),
        ("packaging", "pom"),
        ("properties/tycho.version", "5.0.4"),
    ):
        if field(root, path) != expected:
            raise ValueError("Eclipse parent metadata drift: " + path)
    if root.find(NS + "parent") is not None:
        raise ValueError("Unexpected nested Eclipse Platform parent")


def validate_receiver(data):
    root = ET.fromstring(data)
    for path, expected in (
        ("parent/groupId", "org.eclipse"),
        ("parent/artifactId", "eclipse-platform-parent"),
        ("parent/version", "4.42.0-SNAPSHOT"),
        ("parent/relativePath", "../eclipse-platform-parent"),
    ):
        if field(root, path) != expected:
            raise ValueError("SWT Maven parent binding drift: " + path)


def publish(target, data):
    if target.is_symlink():
        raise ValueError("Parent path must not be a symlink")
    if target.exists() and target.read_bytes() != data:
        raise ValueError("Refusing overwrite of a different Eclipse parent")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)
    if target.read_bytes() != data:
        raise ValueError("Eclipse parent differs after local publication")


def git(repo, *arguments):
    return subprocess.check_output(["git", "-C", str(repo), *arguments],
                                   text=True).strip()


def prepare(aggregator, receiver):
    aggregator = Path(aggregator).resolve()
    receiver = Path(receiver).resolve()
    if git(aggregator, "rev-parse", "HEAD") != AGGREGATOR_COMMIT:
        raise ValueError("Unexpected Eclipse aggregator source commit")
    if git(receiver, "rev-parse", "HEAD") != SWT_COMMIT:
        raise ValueError("Unexpected SWT fork source commit")
    source = aggregator / "eclipse-platform-parent" / "pom.xml"
    if source.is_symlink() or not source.is_file():
        raise ValueError("Missing regular Eclipse parent source")
    if git(aggregator, "hash-object", str(source)) != PARENT_BLOB:
        raise ValueError("Unreviewed Eclipse Platform parent Git blob")
    if git(aggregator, "status", "--porcelain", "--",
           "eclipse-platform-parent/pom.xml"):
        raise ValueError("Dirty Eclipse Platform parent source")
    data = source.read_bytes()
    if sha(data) != PARENT_SHA256:
        raise ValueError("Eclipse parent SHA-256 mismatch")
    validate_parent(data)
    validate_receiver((receiver / "pom.xml").read_bytes())
    target = receiver.parent / "eclipse-platform-parent" / "pom.xml"
    publish(target, data)
    return {"commit": AGGREGATOR_COMMIT, "gitBlob": PARENT_BLOB,
            "sha256": sha(data), "destination": str(target)}


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit(
            "Usage: prepare_eclipse_platform_parent.py AGGREGATOR_CHECKOUT SWT_CHECKOUT"
        )
    print(json.dumps(prepare(*sys.argv[1:]), sort_keys=True))
