#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Guard the M3 one-build recipe proof against module-only Maven invocations.

These structural tests do not substitute for Maven, OpenRewrite, JUnit, SWT or Tycho execution.
They fail before CI attempts source mutation when the source-first reactor is bypassed.
"""

from __future__ import annotations

from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "m3-one-build-openrewrite-convergence.yml"
REACTOR = ROOT / "m3" / "reactor.xml"
RECIPE_POM = ROOT / "m3" / "recipe-first" / "pom.xml"
PORTABLE = ROOT / ".github" / "workflows" / "m3-portable-recipe-dag-java21.yml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def step(source: str, ordinal: int) -> str:
    """Extract exactly one numbered shell-step body."""
    heading = rf"^      - name: {ordinal} - .+$"
    matches = list(re.finditer(heading, source, flags=re.MULTILINE))
    if len(matches) != 1:
        raise ValueError(f"missing/duplicate one-build step {ordinal}")
    remaining = source[matches[0].end():]
    end = re.search(r"^      - (?:name:|uses:)", remaining, flags=re.MULTILINE)
    return remaining[:end.start()] if end is not None else remaining


def verify_source_first_workflow(source: str, portable: str) -> None:
    """Both proof and reproof must build dependencies through the same reactor."""
    if "\r" in source:
        raise ValueError("noncanonical workflow line endings")
    if "-f m3/recipe-first/pom.xml" in source:
        raise ValueError("direct recipe module invocation bypasses source-first dependencies")
    expected = "mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am"
    if source.count(expected) != 2:
        raise ValueError("both proof stages must use the reactor with dependency closure")
    for ordinal in (3, 8):
        text = step(source, ordinal)
        if text.count(expected) != 1 or "-DskipTests" in text or "-Dmaven.test.skip" in text:
            raise ValueError(f"step {ordinal} does not prove its Maven dependency closure")

    if "-Psemantic-coverage,dag-manifest clean install" not in step(source, 3):
        raise ValueError("initial recipe proof must install a tested DAG artifact")
    if "-Psemantic-coverage verify" not in step(source, 8):
        raise ValueError("candidate reproof must execute semantic coverage")

    for ordinal in (1, 2, 4, 5, 6, 7, 9, 10, 11):
        step(source, ordinal)
    if "rewrite:runNoFork" not in step(source, 6):
        raise ValueError("candidate application step missing")
    if "rewrite:dryRunNoFork" not in step(source, 7):
        raise ValueError("fixed-point replay missing")
    if "xvfb-run -a mvn" not in step(source, 9):
        raise ValueError("original Tycho/SWT product proof missing")
    if "candidate-after-tycho.patch" not in step(source, 10):
        raise ValueError("post-Tycho candidate custody missing")

    pin = "maven-version: 3.9.11"
    if source.count(pin) != 1 or portable.count(pin) != 1:
        raise ValueError("one-build and portability matrix require Maven 3.9.11")


def verify_reactor(reactor_xml: str, recipe_xml: str) -> None:
    """The selected recipe module must have its actual parser donor in this reactor."""
    root = ET.fromstring(reactor_xml)
    modules = [node.text for node in root.findall("./m:modules/m:module", NS)]
    if modules != ["openrewrite-line-terminators", "recipe-first"]:
        raise ValueError(f"unexpected source-first reactor topology: {modules}")
    recipe = ET.fromstring(recipe_xml)
    pinned = [
        dep
        for dep in recipe.findall("./m:dependencies/m:dependency", NS)
        if dep.findtext("m:groupId", namespaces=NS) == "com.synexia.recipe"
        and dep.findtext("m:artifactId", namespaces=NS) == "openrewrite-line-terminators"
    ]
    if len(pinned) != 1 or pinned[0].findtext("m:scope", namespaces=NS) != "runtime":
        raise ValueError("recipe parser dependency is missing or scope has drifted")


class OneBuildSourceFirstReactorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.source = WORKFLOW.read_text(encoding="utf-8")
        cls.portable = PORTABLE.read_text(encoding="utf-8")

    def test_one_build_uses_pinned_source_first_closure(self) -> None:
        verify_source_first_workflow(self.source, self.portable)
        verify_reactor(
            REACTOR.read_text(encoding="utf-8"),
            RECIPE_POM.read_text(encoding="utf-8"),
        )

    def test_module_only_invocation_is_rejected(self) -> None:
        broken = self.source.replace(
            "-f m3/reactor.xml", "-f m3/recipe-first/pom.xml", 1
        )
        with self.assertRaises(ValueError):
            verify_source_first_workflow(broken, self.portable)

    def test_missing_dependency_closure_is_rejected(self) -> None:
        broken = self.source.replace(" -pl recipe-first -am", " -pl recipe-first", 1)
        with self.assertRaises(ValueError):
            verify_source_first_workflow(broken, self.portable)

    def test_skipped_recipe_junit_is_rejected(self) -> None:
        broken = self.source.replace(
            "-Psemantic-coverage,dag-manifest clean install",
            "-Psemantic-coverage,dag-manifest -DskipTests clean verify install",
            1,
        )
        with self.assertRaises(ValueError):
            verify_source_first_workflow(broken, self.portable)

    def test_redundant_maven_verify_install_lifecycle_is_rejected(self) -> None:
        broken = self.source.replace(
            "-Psemantic-coverage,dag-manifest clean install",
            "-Psemantic-coverage,dag-manifest clean verify install",
            1,
        )
        with self.assertRaises(ValueError):
            verify_source_first_workflow(broken, self.portable)

    def test_missing_fixed_point_step_is_rejected(self) -> None:
        broken = self.source.replace(
            "rewrite:dryRunNoFork\n          if find . -path",
            "rewrite:runNoFork\n          if find . -path",
            1,
        )
        with self.assertRaises(ValueError):
            verify_source_first_workflow(broken, self.portable)

    def test_unrelated_reactor_topology_is_rejected(self) -> None:
        altered = REACTOR.read_text(encoding="utf-8").replace(
            "<module>openrewrite-line-terminators</module>", ""
        )
        with self.assertRaises(ValueError):
            verify_reactor(altered, RECIPE_POM.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
