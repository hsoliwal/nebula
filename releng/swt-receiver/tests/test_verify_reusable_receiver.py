#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Synthetic tamper and cross-owner checks; these do not qualify native SWT."""

import ast
import importlib.util
import itertools
import json
import re
import tempfile
import unittest
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile

HERE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("reuse", HERE / "verify_reusable_receiver.py")
reuse = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reuse)

# Independent source-bound expectations: a verifier edit cannot silently update
# both sides of this fixture. The unchanged prepare owner is checked below.
EXPECTED_PIN = "c25cb33505465b28980e563acc99097ce3e33fee"
EXPECTED_GC = "51ccf2acd0c803258f1c9cee09f42f6806bd05eddae8cd176e9178414768c8c9"
EXPECTED_VERSION = "3.138.0-SNAPSHOT"
EXPECTED_UPDATES = (
    "https://download.eclipse.org/eclipse/updates/4.42-I-builds/I20261112-2300/",
    "https://download.eclipse.org/eclipse/updates/latest",
)
OLD_PIN = "25e637e40c5243f5210586007a74206d8dce7953"
OLD_GC = "b68e4dc52b4d009972b01b46eb191eb1a30269345a3044ad2d02c850fce53c76"
EXPECTED_CACHE = "nebula-swt3.138-gtk3-c25cb335-20261124-v2"
PLATFORMS = (
    "cocoa.macosx.aarch64", "cocoa.macosx.x86_64", "gtk.linux.aarch64",
    "gtk.linux.ppc64le", "gtk.linux.riscv64", "win32.win32.aarch64",
    "win32.win32.x86_64",
)


def build_fixture(root: Path, *, pin=EXPECTED_PIN, gc_source=EXPECTED_GC,
                  version=EXPECTED_VERSION, updates=EXPECTED_UPDATES) -> Path:
    target = root / "target"
    binding = target / "binding"
    repo = target / "repository"
    plugins = repo / "plugins"
    plugins.mkdir(parents=True)
    composite = binding / "composite"
    composite.mkdir(parents=True)
    names = ["org.eclipse.swt-" + version + ".jar",
             "org.eclipse.swt.gtk.linux.x86_64-" + version + ".jar"]
    names += ["org.eclipse.swt." + p + "-" + version + ".jar" for p in PLATFORMS]
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
    receipt = {"sourceCommit": pin, "sourceWorktreeHead": pin,
               "gcSourceSha256": gc_source,
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
        tree = ET.Element("repository")
        children = ET.SubElement(tree, "children", {"size": str(1 + len(updates))})
        for location in (repo.resolve().as_uri(), *updates):
            ET.SubElement(children, "child", {"location": location})
        (composite / ("composite" + family + ".xml")).write_text(
            ET.tostring(tree, encoding="unicode"))
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
        self.assertEqual(result["sourceCommit"], EXPECTED_PIN)

    def test_wrong_source_pin_fails(self):
        path = self.target / "binding/expected.json"
        data = json.loads(path.read_text())
        data["sourceCommit"] = "0" * 40
        path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_modified_published_bundle_fails(self):
        path = self.target / "repository/plugins/org.eclipse.swt-3.138.0-SNAPSHOT.jar"
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

    def test_bad_env_fails(self):
        path = self.target / "binding/receiver.env"
        path.write_text("M3_SWT_PLATFORM=file:///tmp/fake\n")
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_only_complete_current_contract_tuple_is_accepted(self):
        # All 16 combinations of current/old source, GC, bundle version and
        # composite endpoints. Exactly one tuple belongs to this source owner.
        accepted = []
        for current in itertools.product((False, True), repeat=4):
            with self.subTest(current=current), tempfile.TemporaryDirectory() as root:
                target = build_fixture(
                    Path(root), pin=EXPECTED_PIN if current[0] else OLD_PIN,
                    gc_source=EXPECTED_GC if current[1] else OLD_GC,
                    version=EXPECTED_VERSION if current[2] else "3.136.0-SNAPSHOT",
                    updates=EXPECTED_UPDATES if current[3] else EXPECTED_UPDATES[1:])
                if all(current):
                    self.assertEqual(reuse.verify(target)["sourceCommit"], EXPECTED_PIN)
                    accepted.append(current)
                else:
                    with self.assertRaises(ValueError):
                        reuse.verify(target)
        self.assertEqual(accepted, [(True, True, True, True)])

    def test_wrong_worktree_head_fails_even_when_source_commit_matches(self):
        path = self.target / "binding/expected.json"
        data = json.loads(path.read_text())
        data["sourceWorktreeHead"] = OLD_PIN
        path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)

    def test_duplicate_composite_child_fails(self):
        for filename in ("compositeContent.xml", "compositeArtifacts.xml"):
            with self.subTest(filename=filename):
                path = self.target / "binding/composite" / filename
                original = path.read_bytes()
                tree = ET.fromstring(original)
                children = tree.find("children")
                ET.SubElement(children, "child", {"location": EXPECTED_UPDATES[0]})
                children.set("size", "4")
                path.write_text(ET.tostring(tree, encoding="unicode"))
                with self.assertRaises(ValueError):
                    reuse.verify(self.target)
                path.write_bytes(original)

    def test_preparer_and_reuse_share_the_source_bound_contract(self):
        # Read the real preparation owner without executing Git/JDK/native I/O.
        tree = ast.parse((HERE / "prepare_swt_receiver.py").read_text())
        constants = {target.id: node.value.value
                     for node in tree.body if isinstance(node, ast.Assign)
                     and isinstance(node.value, ast.Constant)
                     for target in node.targets if isinstance(target, ast.Name)}
        self.assertEqual(constants["PIN"], EXPECTED_PIN)
        self.assertEqual(constants["GC_SHA256"], EXPECTED_GC)
        self.assertEqual(reuse.PIN, constants["PIN"])
        self.assertEqual(reuse.GC_SOURCE_SHA256, constants["GC_SHA256"])
        literals = [node.value for node in ast.walk(tree)
                    if isinstance(node, ast.Constant) and isinstance(node.value, str)]
        versions = {match for value in literals for match in
                    re.findall(r"(3\.[0-9]+\.0-SNAPSHOT)\.jar", value)}
        self.assertEqual(versions, {EXPECTED_VERSION})
        self.assertEqual({value for value in literals if value.startswith("https://")},
                         set(EXPECTED_UPDATES))

    def test_workflow_source_cache_and_fragments_match_the_preparer(self):
        workflow = (HERE.parents[1] / ".github/workflows/terminal-vnc-contracts.yml").read_text()
        source_step = workflow.split("      - name: Checkout exact SWT receiver source\n", 1)[1]
        source_step = source_step.split("      - name:", 1)[0]
        self.assertIn("          ref: " + EXPECTED_PIN + "\n", source_step)
        self.assertEqual(workflow.count("          key: " + EXPECTED_CACHE + "\n"), 2)
        self.assertNotIn(OLD_PIN, workflow)
        self.assertNotIn("gh run download", workflow)
        self.assertNotIn("44986161156", workflow)
        fragment_line = next(line for line in workflow.splitlines() if "swt_fragments='" in line)
        selected = set(fragment_line.split("'", 2)[1].split(","))
        self.assertEqual(selected, {":org.eclipse.swt." + platform for platform in
                                    (*PLATFORMS, "gtk.linux.x86_64")})
        self.assertIn('-pl "$swt_fragments" -am package', workflow)
        self.assertIn("python3 releng/swt-receiver/prepare_eclipse_platform_parent.py", workflow)

    def test_receipt_cannot_claim_native_runtime_qualification(self):
        path = self.target / "binding/expected.json"
        data = json.loads(path.read_text())
        data["runtimeQualification"] = "PASSED"
        path.write_text(json.dumps(data))
        with self.assertRaises(ValueError):
            reuse.verify(self.target)


if __name__ == "__main__":
    unittest.main()
