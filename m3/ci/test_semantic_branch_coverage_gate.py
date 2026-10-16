# SPDX-License-Identifier: EPL-2.0
"""Protect the M3 recipe atom semantic coverage policy without running Maven."""

from __future__ import annotations

from decimal import Decimal, InvalidOperation
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
POM = ROOT / "recipe-first" / "pom.xml"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
EXPECTED_CLASSES = (
    "NebulaM3Java21ConvergenceRecipe",
    "NebulaM3AtomizePureIntReturnRecipe",
    "NebulaM3PatternizePureIntAtomRecipe",
    "NebulaM3DocumentPureIntAtomRecipe",
    "NebulaM3PureIntConvergenceRecipe",
    "NebulaM3OrchestratorPlans",
)


def text(node: ET.Element | None) -> str:
    return "" if node is None or node.text is None else node.text.strip()


def find_profile(root: ET.Element, profile_id: str) -> ET.Element:
    matches = [
        profile
        for profile in root.findall("m:profiles/m:profile", NS)
        if text(profile.find("m:id", NS)) == profile_id
    ]
    if len(matches) != 1:
        raise AssertionError(f"expected exactly one Maven profile {profile_id}")
    return matches[0]


def find_plugin(profile: ET.Element, group: str, artifact: str) -> ET.Element:
    matches = [
        plugin
        for plugin in profile.findall("m:build/m:plugins/m:plugin", NS)
        if text(plugin.find("m:groupId", NS)) == group
        and text(plugin.find("m:artifactId", NS)) == artifact
    ]
    if len(matches) != 1:
        raise AssertionError(f"expected exactly one {group}:{artifact} plugin")
    return matches[0]


def validate_policy(xml_body: str) -> dict[str, Decimal]:
    """Demand 99% line AND branch proof in the same canonical Maven profile."""
    root = ET.fromstring(xml_body)
    semantic = find_profile(root, "semantic-coverage")
    plugin = find_plugin(semantic, "org.jacoco", "jacoco-maven-plugin")
    if plugin.find("m:configuration/m:excludes", NS) is not None:
        raise AssertionError("JaCoCo coverage exclusions are forbidden")
    includes = [
        text(item)
        for item in plugin.findall("m:configuration/m:includes/m:include", NS)
    ]
    for required in EXPECTED_CLASSES:
        if not any(required in item for item in includes):
            raise AssertionError(f"missing semantic recipe owner: {required}")

    check = [
        execution
        for execution in plugin.findall("m:executions/m:execution", NS)
        if text(execution.find("m:id", NS)) == "m3-semantic-coverage-check"
    ]
    if len(check) != 1:
        raise AssertionError("JaCoCo semantic check execution must be unique")
    goals = [text(node) for node in check[0].findall("m:goals/m:goal", NS)]
    if goals != ["check"]:
        raise AssertionError("JaCoCo check goal is missing or altered")
    rule = check[0].find("m:configuration/m:rules/m:rule", NS)
    if rule is None or text(rule.find("m:element", NS)) != "BUNDLE":
        raise AssertionError("semantic coverage must enforce the canonical BUNDLE rule")

    counters: dict[str, Decimal] = {}
    for limit in rule.findall("m:limits/m:limit", NS):
        counter = text(limit.find("m:counter", NS))
        value = text(limit.find("m:value", NS))
        if counter in counters or value != "COVEREDRATIO":
            raise AssertionError("coverage limit is duplicated or not a covered ratio")
        try:
            counters[counter] = Decimal(text(limit.find("m:minimum", NS)))
        except InvalidOperation as exc:
            raise AssertionError("invalid JaCoCo minimum") from exc
    if set(counters) != {"LINE", "BRANCH"}:
        raise AssertionError("both LINE and BRANCH covered-ratio limits are required")
    if any(value < Decimal("0.99") for value in counters.values()):
        raise AssertionError("semantic coverage must be at least 99%")

    candidate = find_profile(root, "m3-semantic-branch-coverage-recipe")
    recipe_plugin = find_plugin(
        candidate, "org.openrewrite.maven", "rewrite-maven-plugin"
    )
    if recipe_plugin.find("m:executions", NS) is not None:
        raise AssertionError("candidate source mutation cannot bind a Maven lifecycle")
    active = [
        text(node)
        for node in recipe_plugin.findall(
            "m:configuration/m:activeRecipes/m:recipe", NS
        )
    ]
    if active != [
        "org.eclipse.nebula.m3.rewrite.exact.NebulaM3SemanticCoverageGateRecipe"
    ]:
        raise AssertionError("candidate profile must bind the reviewed exact recipe")
    return counters


class SemanticBranchCoverageGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.pom = POM.read_text(encoding="utf-8")

    def test_both_thresholds_are_enforced(self) -> None:
        self.assertEqual(
            validate_policy(self.pom),
            {"LINE": Decimal("0.99"), "BRANCH": Decimal("0.99")},
        )

    def test_removing_branch_fails_closed(self) -> None:
        cut = """                        <limit>
                          <counter>BRANCH</counter>
                          <value>COVEREDRATIO</value>
                          <minimum>0.99</minimum>
                        </limit>
"""
        self.assertIn(cut, self.pom)
        with self.assertRaises(AssertionError):
            validate_policy(self.pom.replace(cut, "", 1))

    def test_weakening_branch_fails_closed(self) -> None:
        old = "<counter>BRANCH</counter>\n                          <value>COVEREDRATIO</value>\n                          <minimum>0.99</minimum>"
        self.assertIn(old, self.pom)
        with self.assertRaises(AssertionError):
            validate_policy(
                self.pom.replace(old, old.replace("0.99", "0.98"), 1)
            )

    def test_disabling_checked_recipe_fails_closed(self) -> None:
        original = "org.eclipse.nebula.m3.rewrite.exact.NebulaM3SemanticCoverageGateRecipe"
        self.assertIn(original, self.pom)
        with self.assertRaises(AssertionError):
            validate_policy(self.pom.replace(original, "other.Recipe", 1))


if __name__ == "__main__":
    unittest.main()
