#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Reject duplicated or truncated final M3 recipe/Java-grade transfer workflows.

This is a source-structure admission guard, not Maven/JUnit/Tycho/native execution proof.
"""

from __future__ import annotations

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "m3-final-proven-transfer.yml"

REQUIRED_STEPS = (
    "2 - Compile and test complete M3 source-first reactor",
    "3 - Prove canonical convergence and transfer contract tests",
    "4 - Enforce 99 percent semantic recipe coverage",
    "5 - Export scheduler-neutral DAG and portable transfer receipt",
    "6 - Assert transfer and orchestration authority",
    "7 - Inventory complete Nebula Java checkout",
    "8 - Require zero Java parse failures",
    "9 - Install the tested Nebula recipe artifact",
    "10 - Apply FILE-local recipe DAG and capture first candidate",
    "11 - Prove complete recipe DAG fixed point",
    "12 - Original Nebula Tycho reactor on transformed candidate tree",
    "13 - Preserve candidate-only authority",
    "14 - Bind successful hosted proof into manual-review receipt",
    "Upload final proof and transfer artifacts",
)

HEREDOC_MARKERS = ("PYQUALIFICATION", "PYPROOF", "PYRECEIPT", "PYJAVAGRADE")


def verify_final_workflow(text: str) -> None:
    """Fail closed on proof-stage, script or YAML-structure damage."""
    if "\r" in text:
        raise ValueError("final-transfer workflow must use LF")
    if "jobs:\n" not in text:
        raise ValueError("final-transfer workflow has no jobs mapping")

    jobs = text.split("jobs:\n", 1)[1]
    job_names = re.findall(r"^  ([a-z][a-z0-9-]*):\s*$", jobs, re.MULTILINE)
    if job_names != ["prove"]:
        raise ValueError(f"expected exactly one final proof job: {job_names}")

    # A column-zero fragment after the jobs mapping is not a valid shell step
    # and would end the YAML mapping, potentially hiding duplicated proof steps.
    for line in jobs.splitlines():
        if line and not line[0].isspace() and not line.startswith("#"):
            raise ValueError(f"orphaned unindented final-proof fragment: {line[:80]}")

    steps = re.findall(r"^      - name: (.+)$", jobs, re.MULTILINE)
    if len(steps) != len(set(steps)):
        raise ValueError("duplicate final-transfer stage names")
    for name in REQUIRED_STEPS:
        if steps.count(name) != 1:
            raise ValueError(f"missing or repeated final-transfer stage: {name}")
    if steps.count("Upload final proof and transfer artifacts") != 1:
        raise ValueError("proof artifacts may be uploaded only once")

    for marker in HEREDOC_MARKERS:
        opening = f"<<'{marker}'"
        closing = f"          {marker}"
        if text.count(opening) != 1 or len(
            re.findall(rf"^{re.escape(closing)}$", text, re.MULTILINE)
        ) != 1:
            raise ValueError(f"incomplete or duplicated proof script: {marker}")

    required = (
        "m3/reactor.xml",
        "NebulaM3JavaGradeQualificationTest",
        "NebulaM3JavaGradeQualificationCli",
        "NebulaM3BackportBundleTest",
        "-Psemantic-coverage",
        "rewrite:runNoFork",
        "rewrite:dryRunNoFork",
        "xvfb-run -a mvn",
        "backport-bundle.sha256",
        "java-grade-qualification.tsv",
        "MANUAL_TRANSFER_REVIEW_REQUIRED",
        "mutationAuthority",
        "promotionAuthority",
    )
    for marker in required:
        if marker not in text:
            raise ValueError(f"missing final-transfer proof requirement: {marker}")

    if text.count("maven-version: 3.9.11") != 1:
        raise ValueError("final-transfer Maven version must match portable and one-build proof")



class FinalTransferWorkflowTest(unittest.TestCase):
    def test_current_final_workflow_has_one_complete_stage_chain(self) -> None:
        verify_final_workflow(WORKFLOW.read_text(encoding="utf-8"))

    def test_duplicate_stage_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_final_workflow(valid + "\n      - name: 7 - Inventory complete Nebula Java checkout\n")

    def test_orphaned_powershell_or_shell_fragment_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_final_workflow(valid + "\nbackportBundleRoot stale-fragment\n")

    def test_maven_version_drift_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_final_workflow(
                valid.replace("maven-version: 3.9.11", "maven-version: 3.9.6", 1)
            )

    def test_missing_tycho_acceptance_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_final_workflow(valid.replace("xvfb-run -a mvn", "mvn", 1))

    def test_missing_java_grade_receipt_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_final_workflow(
                valid.replace("NebulaM3JavaGradeQualificationCli", "RemovedCli", 1)
            )

    def test_duplicated_receipt_script_is_rejected(self) -> None:
        valid = WORKFLOW.read_text(encoding="utf-8")
        with self.assertRaises(ValueError):
            verify_final_workflow(valid + "\n          PYJAVAGRADE\n")


if __name__ == "__main__":
    unittest.main()
