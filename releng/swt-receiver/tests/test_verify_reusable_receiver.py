#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Synthetic tamper/regression checks for the pinned SWT artifact reuse gate."""

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

HERE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("reuse", HERE / "verify_reusable_receiver.py")
reuse = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reuse)


def build_fixture(root: Path) -> Path:
    target = root / "target"
    binding = target / "binding"
    repo = target / "repository"
    plugins = repo / "plugins"
    plugins.mkdir(parents=True)
    composite = binding / "composite"
    composite.mkdir(parents=True)
    suffix = reuse.SWT_BUNDLE_SUFFIX
    names = ["org.eclipse.swt" + suffix,
             "org.eclipse.swt.gtk.linux.x86_64" + suffix]
    names += ["org.eclipse.swt." + p + suffix for p in (
        "cocoa.macosx.aarch64", "cocoa.macosx.x86_64", "gtk.linux.aarch64",
        "gtk.linux.ppc64le", "gtk.linux.riscv64", "win32.win32.aarch64",
        "win32.win32.x86_64")]
    native = {prefix + "123.so": b"\x7fELF" + bytes([i])
              for i, prefix in enumerate(reuse.REQUIRED_NATIVES)}
    gc_class = b"qualified GC.class fixture"
    bundles = {}
    for i, name in enumerate(names):
        path = plugins / name
        with ZipFile(path, "w") as archive:
            archive.writestr("dummy/%d.class" % i, bytes([i]))
            if "gtk.linux.x86_64" in name:
                archive.writestr("org/eclipse/swt/graphics/GC.class", gc_class)
                for native_name, payload in native.items():
                    archive.writestr(native_name, payload)
        bundles[name] = reuse.digest(path.read_bytes())
    receipt = {"sourceCommit": reuse.PIN, "sourceWorktreeHead": reuse.PIN,
               "gcSourceSha256": reuse.GC_SOURCE_SHA256,
               "gcClassSha256": reuse.digest(gc_class),
               "bundleSha256": bundles,
               "nativeSha256": {k: reuse.digest(v) for k, v in native.items()},
               "qualifiedBackend": reuse.RECEIVER_BACKEND,
               "runtimeQualification": "PENDING"}
    (binding / "expected.json").write_text(json.dumps(receipt))
    (binding / "receiver.env").write_text(
        "M3_SWT_GC_SHA256=" + reuse.digest(gc_class) + "\n"
        "M3_SWT_PLATFORM=" + composite.resolve().as_uri() + "\n")
    for family, local in (("Content", "content"), ("Artifacts", "artifacts")):
        (repo / (local + ".xml")).write_text("<repository/>")
        (composite / ("composite" + family + ".xml")).write_text(
            '<repository><children size="3"><child location="'
            + repo.resolve().as_uri() + '"/><child location="'
            + reuse.PLATFORM_IBUILD + '"/><child location="'
            + reuse.PLATFORM_UPDATE + '"/></children></repository>')
    return target


class ReusableReceiverTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.target = build_fixture(Path(self.tmp.name))

    def test_valid_qualified_fixture_passes(self):
        result = reuse.verify(self.target)
        self.assertEqual(result["bundleCount"], 9)
        self.assertEqual(result["nativeCount"], 4)

    def test_wrong_source_pin_fails(self):
        path = self.target / "binding/expected.json"
        data = json.loads(path.read_text())
        data["sourceCommit"] = "0" * 40
        path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_modified_published_bundle_fails(self):
        path = self.target / ("repository/plugins/org.eclipse.swt" + reuse.SWT_BUNDLE_SUFFIX)
        path.write_bytes(path.read_bytes() + b"bad")
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_fake_native_receipt_fails(self):
        path = self.target / "binding/expected.json"
        data = json.loads(path.read_text())
        data["nativeSha256"]["libswt-gtk-123.so"] = "0" * 64
        path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_unapproved_composite_child_fails(self):
        path = self.target / "binding/composite/compositeContent.xml"
        path.write_text(path.read_text().replace(
            reuse.PLATFORM_UPDATE, "https://example.invalid/untrusted"))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_missing_pinned_ibuild_child_fails(self):
        path = self.target / "binding/composite/compositeContent.xml"
        path.write_text(path.read_text().replace(
            '<child location="' + reuse.PLATFORM_IBUILD + '"/>', ""))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_superseded_swt_receipt_fails(self):
        path = self.target / "binding/expected.json"
        data = json.loads(path.read_text())
        superseded = "25e637e40c5243f5210586007a74206d8dce7953"
        data["sourceCommit"] = data["sourceWorktreeHead"] = superseded
        path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_bad_env_fails(self):
        path = self.target / "binding/receiver.env"
        path.write_text("M3_SWT_PLATFORM=file:///tmp/fake\n")
        with self.assertRaises(ValueError):
            reuse.verify(self.target)


if __name__ == "__main__":
    unittest.main()
