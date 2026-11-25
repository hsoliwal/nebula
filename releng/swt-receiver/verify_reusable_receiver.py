#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Fail-closed verification of a pinned SWT receiver from a prior CI artifact.

The original receipt binds source revision, SWT GC class, JNI library bytes
and all nine platform bundles. A valid GitHub artifact cannot replace the
pinned source build unless the published p2 jar bytes and composite endpoints
match the original receipt.
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile

# Must equal PIN/GC_SHA256, the jar version and the composite children written by
# prepare_swt_receiver.py, which produces the receipt verified here.
PIN = "99577bf0d9399c7e02decf1b13ec2a9dafaa647a"
GC_SOURCE_SHA256 = "51ccf2acd0c803258f1c9cee09f42f6806bd05eddae8cd176e9178414768c8c9"
SWT_BUNDLE_SUFFIX = "-3.138.0-SNAPSHOT.jar"
PLATFORM_IBUILD = "https://download.eclipse.org/eclipse/updates/4.42-I-builds/I20261112-2300/"
PLATFORM_UPDATE = "https://download.eclipse.org/eclipse/updates/latest"
# Name used by the Terminal/VNC alignment for the same pinned 4.42 I-build child.
TERMINAL_UPDATE = PLATFORM_IBUILD
RECEIVER_BACKEND = "GTK3/X11 only; GTK4 payload is not qualified"
REQUIRED_NATIVES = ("libswt-gtk-", "libswt-pi3-gtk-", "libswt-cairo-gtk-", "libswt-atk-gtk-")
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def fail(message: str) -> None:
    raise ValueError("SWT artifact qualification failed: " + message)


def verify(target: Path) -> dict:
    """Verify artifact against pinned source and actual published p2 payload."""
    root = target.resolve(strict=True)
    binding = root / "binding"
    repo = root / "repository"
    manifest = json.loads((binding / "expected.json").read_text(encoding="utf-8"))

    if manifest.get("sourceCommit") != PIN or manifest.get("sourceWorktreeHead") != PIN:
        fail("source commit differs from the pinned SWT receiver")
    if manifest.get("gcSourceSha256") != GC_SOURCE_SHA256:
        fail("SWT GC.java source checksum differs")
    if manifest.get("qualifiedBackend") != RECEIVER_BACKEND:
        fail("runtime/backend differs from qualified GTK3/X11")
    if manifest.get("runtimeQualification") != "PENDING":
        fail("unexpected runtime qualification claim")

    class_digest = manifest.get("gcClassSha256")
    if not isinstance(class_digest, str) or not HEX64.fullmatch(class_digest):
        fail("invalid pinned GC.class checksum")

    bundle_manifest = manifest.get("bundleSha256")
    if not isinstance(bundle_manifest, dict) or len(bundle_manifest) != 9:
        fail("expected exactly nine source-built SWT and platform-fragment bundles")
    if "org.eclipse.swt" + SWT_BUNDLE_SUFFIX not in bundle_manifest or \
            "org.eclipse.swt.gtk.linux.x86_64" + SWT_BUNDLE_SUFFIX not in bundle_manifest:
        fail("required SWT core/GTK fragment absent")
    for filename, value in bundle_manifest.items():
        if Path(filename).name != filename or not filename.endswith(SWT_BUNDLE_SUFFIX):
            fail("unexpected bundle filename in proof")
        if not isinstance(value, str) or not HEX64.fullmatch(value):
            fail("invalid bundle checksum in proof")

    plugin_dir = repo / "plugins"
    if not plugin_dir.is_dir():
        fail("published p2 plugins are absent")
    # The p2 publisher may rename a jar, but may not mutate its bytes.
    published = {digest(path.read_bytes()): path for path in plugin_dir.rglob("*.jar")}
    for filename, expected in bundle_manifest.items():
        if expected not in published:
            fail("source-built bundle missing or modified in p2 repository: " + filename)

    gtk_digest = bundle_manifest["org.eclipse.swt.gtk.linux.x86_64" + SWT_BUNDLE_SUFFIX]
    gtk_jar = published[gtk_digest]
    with ZipFile(gtk_jar) as archive:
        if digest(archive.read("org/eclipse/swt/graphics/GC.class")) != class_digest:
            fail("native GTK fragment does not have qualified GC.class")
        expected_natives = manifest.get("nativeSha256")
        if not isinstance(expected_natives, dict) or not expected_natives:
            fail("qualified native library manifest absent")
        actual_natives = {name: digest(archive.read(name)) for name in archive.namelist()
                          if name.endswith(".so")}
        if actual_natives != expected_natives:
            fail("native .so payload hashes differ from the qualification receipt")
        if not all(any(name.startswith(prefix) and archive.read(name).startswith(b"\x7fELF")
                       for name in actual_natives) for prefix in REQUIRED_NATIVES):
            fail("required GTK3/PI3/Cairo/ATK native ELF libraries missing")

    for index_name in ("content", "artifacts"):
        if not any((repo / (index_name + extension)).is_file()
                   for extension in (".xml", ".jar", ".xml.xz")):
            fail("p2 repository does not contain " + index_name + " metadata")

    expected_composite_locations = {repo.as_uri(), PLATFORM_IBUILD, PLATFORM_UPDATE}
    composite = binding / "composite"
    for filename in ("compositeContent.xml", "compositeArtifacts.xml"):
        tree = ET.parse(composite / filename)
        locations = [child.attrib.get("location") for child in tree.findall("./children/child")]
        if len(locations) != len(expected_composite_locations) or \
                set(locations) != expected_composite_locations:
            fail("composite points to unqualified repositories: " + filename)

    env_lines = (binding / "receiver.env").read_text(encoding="utf-8").splitlines()
    expected_lines = {"M3_SWT_GC_SHA256=" + class_digest,
                      "M3_SWT_PLATFORM=" + composite.as_uri()}
    if set(env_lines) != expected_lines or len(env_lines) != len(expected_lines):
        fail("receiver.env does not match checked source GC/composite location")

    return {"qualifiedBackend": RECEIVER_BACKEND, "sourceCommit": PIN,
            "bundleCount": len(bundle_manifest), "nativeCount": len(actual_natives),
            "gcClassSha256": class_digest}


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: verify_reusable_receiver.py REHYDRATED_TARGET_DIR")
    verified = verify(Path(sys.argv[1]))
    print("NEBULA_PINNED_SWT_ARTIFACT=VERIFIED " + json.dumps(verified, sort_keys=True))


if __name__ == "__main__":
    main()
