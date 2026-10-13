# SPDX-License-Identifier: EPL-2.0
"""Unit tests for vendored_closure.py using synthetic receiver and aggregator poms."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("vendored_closure", HERE / "vendored_closure.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

POM = ('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>'
       '{body}</project>')


def dep(group, artifact, version, scope=""):
    scope_xml = "<scope>%s</scope>" % scope if scope else ""
    return ("<dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>%s"
            "</dependency>" % (group, artifact, version, scope_xml))


def gav(group, artifact, version):
    return "<groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version>" % (
        group, artifact, version)


class VendoredClosureTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.vendor = self.root / "third_party"
        modules = {
            "rewrite-core": gav("org.openrewrite", "rewrite-core", "8.17.1-m3-source-1"),
            "slf4j-api-1.7.36": gav("org.slf4j", "slf4j-api", "1.7.36-m3-source-1"),
            "slf4j-api-2.0.9": gav("org.slf4j", "slf4j-api", "2.0.9-m3-source-1"),
        }
        for name, coordinates in modules.items():
            (self.vendor / name).mkdir(parents=True)
            (self.vendor / name / "pom.xml").write_text(POM.format(body=coordinates), encoding="utf-8")
        listed = "".join("<module>%s</module>" % name for name in modules)
        (self.vendor / "pom.xml").write_text(
            POM.format(body=gav("com.m3.vendor", "third-party-sources", "1.0.0-SNAPSHOT")
                       + "<modules>" + listed + "</modules>"), encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def receiver(self, *deps):
        pom = self.root / "receiver.xml"
        pom.write_text(POM.format(body="<properties><rw>8.17.1</rw></properties><dependencies>"
                                  + "".join(deps) + "</dependencies>"), encoding="utf-8")
        return pom

    def test_selects_exact_version_owner_and_skips_test_and_central_deps(self):
        pom = self.receiver(
            dep("org.openrewrite", "rewrite-core", "${rw}-m3-source-1"),
            dep("org.slf4j", "slf4j-api", "1.7.36-m3-source-1"),
            dep("org.junit.jupiter", "junit-jupiter", "5.10.2", "test"),
            dep("org.example", "only-in-tests", "1.0-m3-source-1", "test"))
        self.assertEqual(module.resolve(pom, self.vendor), ["rewrite-core", "slf4j-api-1.7.36"])

    def test_missing_vendored_owner_fails(self):
        pom = self.receiver(dep("org.openrewrite", "rewrite-java", "8.17.1-m3-source-1"))
        with self.assertRaises(SystemExit) as raised:
            module.resolve(pom, self.vendor)
        self.assertIn("rewrite-java", str(raised.exception))

    def test_version_drift_fails(self):
        pom = self.receiver(dep("org.slf4j", "slf4j-api", "2.0.12-m3-source-1"))
        with self.assertRaises(SystemExit):
            module.resolve(pom, self.vendor)

    def test_receiver_without_source_built_dependency_fails(self):
        pom = self.receiver(dep("org.slf4j", "slf4j-api", "1.7.36"))
        with self.assertRaises(SystemExit):
            module.resolve(pom, self.vendor)


if __name__ == "__main__":
    unittest.main(verbosity=2)
