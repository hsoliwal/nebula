#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Regression guard for the read-only three-host M3 OpenRewrite transfer workflow.

Checks the known YAML duplication/truncation failure mechanically without requiring
third-party Python modules or claiming that the hosted build has executed.
"""

from __future__ import annotations

from pathlib import Path
import re
import unittest


WORKFLOW = (
    Path(__file__).resolve().parents[2]
    / ".github"
    / "workflows"
    / "m3-portable-recipe-dag-java21.yml"
)


def verify_workflow(source: str) -> None:
    """Reject missing/duplicated proof jobs and unsafe or truncated handoffs."""
    if "\r" in source:
        raise ValueError("noncanonical portable workflow line endings")

    required_singletons = (
        "  host-neutral-recipe-proof:",
        "  compare-host-receipts:",
        "      - name: 2 - Prove Java 21 recipe with full Maven dependency closure",
        "      - name: 3 - Verify roots, real JUnit reports and source immutability",
        "      - name: 4 - Export host-independent receipt",
        "      - name: Require all three hosts and identical content roots",
        "          $receipt = $header + [char]10 + $values + [char]10",
    )
    for marker in required_singletons:
        if source.count(marker) != 1:
            raise ValueError(f"portable workflow marker must occur once: {marker}")

    required = (
        "  pull_request:",
        "  workflow_dispatch:",
        "-pl recipe-first -am",
        "m3/reactor.xml",
        "NebulaM3HostPortabilityTest",
        "NebulaM3BackportPlanTest",
        "NebulaM3BackportBundleTest",
        "NebulaM3TransferContractTest",
        "NebulaM3JavaGradeQualificationTest",
        "java-grade-qualification.tsv",
        "backport-bundle.tsv",
        "backport-bundle.sha256",
        "HOLD_UNTIL_PROVEN",
        "orchestrator-plans.sha256",
        "backport-plan.tsv",
        "transfer.sha256",
        "git status --porcelain --untracked-files=normal",
        "actions/upload-artifact@v4",
        "actions/download-artifact@v4",
        "if-no-files-found: error",
    )
    for marker in required:
        if marker not in source:
            raise ValueError(f"portable workflow proof element missing: {marker}")

    if len(re.findall(r"^  [A-Za-z][A-Za-z-]*:\s*$", source, re.MULTILINE)) != 2:
        # The only job keys indented by two spaces are the worker and reducer.
        raise ValueError("unexpected portable workflow job count")

    for host in ("linux", "windows", "macos"):
        if source.count(f"          - label: {host}") != 1:
            raise ValueError(f"missing or duplicate host: {host}")
        if source.count(f"'receipts/nebula-m3-dag-{host}'") > 1:
            raise ValueError(f"duplicated host receipt: {host}")

    # The worker and reducer must agree on all five roots. A 3-/4-field
    # partial receipt must not be accepted as a Java-grade backport proof.
    schema = (
        "'dagRoot', 'plansRoot', 'backportRoot', "
        "'bundleRoot', 'transferRoot'"
    )
    if source.count(schema) != 2:
        raise ValueError("portable worker/reducer five-root schemas diverge")
    if "if ($fields.Count -ne 5)" not in source:
        raise ValueError("five-root reducer field validation is missing")
    if "$bundle -cne $actualBundle" not in source:
        raise ValueError("backport bundle SHA-256 content check missing")
    if "disposition' + [char]9 + 'HOLD'" not in source:
        raise ValueError("portable Java-grade state must remain HOLD")

    if "rewrite:runNoFork" in source or "rewrite:run" in source:
        raise ValueError("portability proof must never mutate product source")
    if source.count("          $receipt = $header + [char]10 + $values + [char]10") != 1:
        raise ValueError("portable root handoff is incomplete")

    head, reducer = source.split("  compare-host-receipts:", 1)
    if "WriteAllText(" not in head or "portable-roots.tsv" not in head:
        raise ValueError("worker did not write a portable root receipt")
    if "foreach ($hostName in @('linux', 'windows', 'macos'))" not in reducer:
        raise ValueError("reducer does not require all three platforms")
    if "Host-dependent DAG/scheduler/transfer identity" not in reducer:
        raise ValueError("reducer does not reject divergent roots")


class PortableRecipeWorkflowTest(unittest.TestCase):
    def test_current_workflow_has_one_worker_and_one_fanin(self) -> None:
        verify_workflow(WORKFLOW.read_text(encoding="utf-8"))

    def test_duplicate_fanin_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_workflow(valid + "\n  compare-host-receipts:\n")

    def test_truncated_handoff_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        broken = valid.replace(
            "          $receipt = $header + [char]10 + $values + [char]10",
            "          $receipt = $header +",
        )
        with self.assertRaises(ValueError):
            verify_workflow(broken)

    def test_partial_root_schema_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        broken = valid.replace(
            "'backportRoot', 'bundleRoot', 'transferRoot'",
            "'transferRoot'",
            1,
        )
        with self.assertRaises(ValueError):
            verify_workflow(broken)

    def test_unverified_backport_bundle_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_workflow(
                valid.replace(
                    "$bundle -cne $actualBundle",
                    "$bundle -eq $actualBundle",
                )
            )

    def test_source_mutation_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_workflow(valid + "\n# rewrite:runNoFork\n")


if __name__ == "__main__":
    unittest.main()
