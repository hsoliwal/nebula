"""BLLSE extractor: GitHub Actions failed-run logs -> CI_ERROR_RECORDS.tsv (deduplicated).

Usage: extract_ci_error_records.py [INPUT_DIR] [CLASSES_TSV] [OUTPUT_TSV]
(defaults: current directory, INPUT_DIR/classes.tsv, ./CI_ERROR_RECORDS.tsv)

Inputs (in INPUT_DIR):
  failed_all.json   gh run list --status failure --json databaseId,workflowName,headBranch,headSha,createdAt,updatedAt
  jobs_steps.tsv    run_id, job_id, job, conclusion, failing steps ("n:name|n:name" or -)
  run_<id>.log      gh run view <id> --log-failed (optional per run)
  classes.tsv       rule/file regex -> class (first match wins)
Output: CI_ERROR_RECORDS.tsv
"""
import csv
import json
import re
import sys
from datetime import datetime
from pathlib import Path

NL, TAB = chr(10), chr(9)

HEADER = ["run_id", "workflow", "job", "step", "file", "line", "col", "severity", "rule",
          "message", "branch", "head_sha", "class", "master_status"]

# Records established on current master without a hosted run (Actions disabled since 2026-10-08).
EXTRA_ROWS = [
    ["local-repro-A", "Nebula M3 final proven transfer", "prove",
     "9a - Install exact bound Synexia FILE recipe", "m3/convergence/install_bound_synexia_recipe.sh",
     "165", "", "ERROR", "ARTIFACT_MISSING",
     "Could not find artifact org.openrewrite:rewrite-core:jar:8.17.1-m3-source-1 (also rewrite-java, "
     "rewrite-java-21, slf4j-api:1.7.36-m3-source-1); receiver M3jdk21@461b77e7 pom on a clean repo",
     "master", "eab2a65c451c", "WORKFLOW_CONFIG", "REPRODUCED_LOCALLY_FIXED_BY_PR160"],
    ["local-repro-A", "M3 Nebula recipe-first convergence", "recipe",
     "8a - Install exact bound Synexia FILE recipe", "m3/convergence/install_bound_synexia_recipe.sh",
     "165", "", "ERROR", "ARTIFACT_MISSING",
     "same installer and receiver as final proven transfer 9a", "master", "eab2a65c451c",
     "WORKFLOW_CONFIG", "REPRODUCED_LOCALLY_FIXED_BY_PR160"],
    ["static-master", "M3 Nebula recipe-first convergence", "recipe",
     "13 - Preserve candidate-only authority", ".github/workflows/m3-nebula-recipe-first.yml",
     "352", "", "ERROR", "STATIC_CONTRACT",
     "grep -F 8<TAB>FILE_FIXED_POINT m3/convergence/PLAN.tsv matches no row (PLAN has 9<TAB>FILE_FIXED_POINT)",
     "master", "eab2a65c451c", "WORKFLOW_CONFIG", "REPRODUCED_STATIC_FIXED_BY_PR158"],
]

PATTERNS = [
    # Maven/javac: [ERROR] /path/File.java:[12,5] message
    ("JAVAC", re.compile(r"\[ERROR\] (?P<file>\S+\.java):\[(?P<line>\d+),(?P<col>\d+)\] (?P<msg>.*)")),
    # surefire summary: [ERROR]   Class.method:123 message
    ("TEST_FAILURE", re.compile(
        r"\[ERROR\]\s{2,}(?P<file>[A-Z][\w$]*(?:\.[\w$]+)?):(?P<line>\d+)\s+(?P<msg>.*)")),
    # tycho-surefire/plain: org.x.Class.method -- Time elapsed ... <<< FAILURE!/ERROR!
    ("TEST_CASE", re.compile(
        r"^\s*(?:\[ERROR\] )?(?P<file>[a-z][\w.]*\.[A-Z][\w$]*\.[\w$]+) -- Time elapsed.*<<< (?P<msg>FAILURE|ERROR)!")),
    ("ASSERTION", re.compile(r"^\s*(?P<msg>(?:java\.lang\.AssertionError|org\.opentest4j\.AssertionFailedError):.*)")),
    ("NO_CLASS_DEF", re.compile(r"(?P<msg>(?:java\.lang\.)?NoClassDefFoundError: \S+)")),
    ("ARTIFACT_MISSING", re.compile(r"(?P<msg>Could not (?:find|resolve) artifact \S+)")),
    ("MOJO_FAILURE", re.compile(r"\[ERROR\] Failed to execute goal (?P<msg>.*)")),
    ("MAVEN_ERROR", re.compile(r"\[ERROR\] (?P<msg>(?:Non-resolvable|The build could not read|Unresolveable|Failed to read artifact|Invalid repository URI).*)")),
    ("DIFF_CHECK", re.compile(r"^(?P<file>[^\s:]+):(?P<line>\d+): (?P<msg>trailing whitespace\.|new blank line at EOF\.|space before tab in indent\.|leftover conflict marker)")),
    ("ECJ_LOCATION", re.compile(r"\[ERROR\] (?P<file>\S+\.java):\[(?P<line>\d+)\]\s*$")),
    ("SHELL_ASSERT", re.compile(r"^(?P<msg>(?:AssertionError|Traceback|.*drift.*|Nebula production/source path changed.*|FILE recipe escaped.*|missing .*binding.*))$")),
    ("JACOCO_RULE", re.compile(r"Rule violated for bundle (?P<msg>.*)")),
    ("SETUP_JAVA_CACHE", re.compile(r"##\[error\](?P<msg>No file in \S+ matched to \[\*\*/pom\.xml.*)")),
    ("GHA_ERROR", re.compile(r"##\[error\](?P<msg>.*)")),
]


WORKFLOW_FILES = {
    "CodeQL": ".github/workflows/codeql.yml",
    "Java CI with Maven": ".github/workflows/maven.yaml",
    "M3 Nebula recipe-first convergence": ".github/workflows/m3-nebula-recipe-first.yml",
    "Nebula M3 final proven transfer": ".github/workflows/m3-final-proven-transfer.yml",
    "Nebula M3 A3 mastery Java 21": ".github/workflows/m3-a3-mastery-java21.yml",
    "Nebula M3 declarative OpenRewrite convergence": ".github/workflows/m3-declarative-openrewrite.yml",
    "M3 Nebula full second-pass review DAG": ".github/workflows/m3-second-pass-review-dag.yml",
}


def ts_strip(text):
    return re.sub(r"^\d{4}-\d\d-\d\dT[\d:.]+Z ?", "", text)


def load_runs():
    runs = {}
    for row in json.loads(Path("failed_all.json").read_text(encoding="utf-8")):
        start = datetime.fromisoformat(row["createdAt"].replace("Z", "+00:00"))
        end = datetime.fromisoformat(row["updatedAt"].replace("Z", "+00:00"))
        row["seconds"] = (end - start).total_seconds()
        runs[str(row["databaseId"])] = row
    return runs


def load_jobs():
    jobs = {}
    path = Path("jobs_steps.tsv")
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            parts = line.split("\t")
            if len(parts) >= 4 and parts[3] == "failure":
                jobs.setdefault(parts[0], []).append(
                    (parts[2], parts[4] if len(parts) > 4 and parts[4] != "-" else ""))
    return jobs


def load_classes(path):
    """classes TSV: regex<TAB>class<TAB>master_status; matched against a run-level signature."""
    rules = []
    for row in csv.reader(Path(path).read_text(encoding="utf-8").splitlines(), delimiter=TAB):
        if row and not row[0].startswith("#"):
            rules.append((re.compile(row[0]), row[1], row[2]))
    return rules


def classify(rules, signature):
    return next(((cls, status) for regex, cls, status in rules if regex.search(signature)),
                ("UNCLASSIFIED", "UNVERIFIED"))


def parse_log(run_id, path, steps):
    records = []
    pending = None
    step = steps.get(path.name, "")
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        parts = raw.split("\t", 2)
        if len(parts) < 3:
            continue
        job, text = parts[0], ts_strip(parts[2])
        if "[36;1m" in text or "[36;1m" in text:
            continue  # echoed workflow script source, not output
        group = re.match(r"##\[group\]Run (.*)", text)
        if group:
            step = group.group(1)[:120]
            continue
        if pending is not None:
            follow = re.search(r"\[ERROR\] ([A-Za-z][^	]{8,})$", text)
            pending_lines += 1
            if follow and not follow.group(1).startswith(("1 problem", "-> [Help")):
                pending["message"] = follow.group(1).strip()[:300]
                pending = None
            elif pending_lines > 4:
                pending = None
        for rule, pattern in PATTERNS:
            match = pattern.search(text)
            if not match:
                continue
            found = match.groupdict()
            records.append({"run_id": run_id, "job": job, "step": step,
                            "file": found.get("file") or "", "line": found.get("line") or "",
                            "col": found.get("col") or "",
                            "severity": "ERROR", "rule": rule,
                            "message": re.sub(r"\s+", " ", found.get("msg") or "")[:300]})
            if rule == "ECJ_LOCATION":
                pending, pending_lines = records[-1], 0
            break
    return aggregate_diff_check(records)


def aggregate_diff_check(records):
    """Collapse git diff --check rows to one row per (job, step, file): first line + count."""
    kept, index = [], {}
    for record in records:
        if record["rule"] != "DIFF_CHECK":
            kept.append(record)
            continue
        key = (record["job"], record["step"], record["file"], record["message"])
        if key in index:
            index[key]["count"] += 1
            continue
        record = dict(record, count=1)
        index[key] = record
        kept.append(record)
    for record in index.values():
        record["message"] = "%s (%d lines)" % (record["message"], record.pop("count"))
    return kept


def main():
    import os
    args = sys.argv[1:]
    input_dir = Path(args[0] if args else ".").resolve()
    classes = Path(args[1]).resolve() if len(args) > 1 else input_dir / "classes.tsv"
    output = Path(args[2]).resolve() if len(args) > 2 else Path("CI_ERROR_RECORDS.tsv").resolve()
    os.chdir(input_dir)
    runs, jobs, rules = load_runs(), load_jobs(), load_classes(classes)
    by_run = {}
    for run_id, run in sorted(runs.items()):
        log = Path("run_%s.log" % run_id)
        if run["seconds"] < 5 and run_id not in jobs:
            rows = [{"run_id": run_id, "job": "", "step": "",
                     "file": WORKFLOW_FILES.get(run["workflowName"], ""), "line": "", "col": "",
                     "severity": "ERROR", "rule": "STARTUP_FAILURE_NO_JOBS",
                     "message": "run concluded failure in %ds with no jobs" % run["seconds"]}]
        elif log.exists() and log.stat().st_size > 0:
            rows = parse_log(run_id, log, {})
        else:
            rows = [{"run_id": run_id, "job": job, "step": failing, "file": "", "line": "",
                     "col": "", "severity": "ERROR", "rule": "JOB_STEP_FAILURE",
                     "message": "log not fetched; classified by sampled run of same workflow+branch+step"}
                    for job, failing in jobs.get(run_id, [("", "")])]
        named = dict(jobs.get(run_id, []))
        for row in rows:
            row.update(workflow=run["workflowName"], branch=run["headBranch"],
                       head_sha=run["headSha"][:12])
            if named.get(row["job"]):
                row["step"] = named[row["job"]]
        by_run[run_id] = rows

    def failing_steps(run_id):
        return tuple(sorted(step for _, step in jobs.get(run_id, [])))

    sample_class, step_class = {}, {}
    for run_id, rows in by_run.items():
        if rows and rows[0]["rule"] != "JOB_STEP_FAILURE":
            signature = "STEPS" + TAB + "|".join(failing_steps(run_id)) + NL + NL.join(TAB.join(r[k] for k in ("workflow", "branch", "step", "file",
                                                        "rule", "message")) for r in rows)
            verdict = classify(rules, signature)
            for row in rows:
                row["class"], row["master_status"] = verdict
            run = runs[run_id]
            sample_class.setdefault((run["workflowName"], run["headBranch"], failing_steps(run_id)),
                                    verdict)
            for step in failing_steps(run_id):
                step_class.setdefault((run["workflowName"], (step,)), verdict)
    for run_id, rows in by_run.items():
        if rows and rows[0]["rule"] == "JOB_STEP_FAILURE":
            run = runs[run_id]
            verdict = sample_class.get((run["workflowName"], run["headBranch"], failing_steps(run_id)))
            if verdict is None:
                verdict = step_class.get((run["workflowName"], failing_steps(run_id)),
                                         ("UNCLASSIFIED", "UNVERIFIED_NO_SAMPLE"))
            for row in rows:
                row["class"], row["master_status"] = verdict

    out, seen = [], set()
    for run_id in sorted(by_run):
        for row in by_run[run_id]:
            key = (row["workflow"], row["job"], row["file"], row["line"], row["rule"],
                   row["message"], row["head_sha"])
            if key not in seen:
                seen.add(key)
                out.append(row)
    for row in EXTRA_ROWS:
        out.append(dict(zip(HEADER, row)))

    with open(output, "w", encoding="utf-8", newline=NL) as handle:
        writer = csv.DictWriter(handle, HEADER, delimiter=TAB, lineterminator=NL,
                                quoting=csv.QUOTE_NONE, quotechar=None, escapechar=chr(92))
        writer.writeheader()
        writer.writerows(out)
    print(len(out), "records", file=sys.stderr)


if __name__ == "__main__":
    main()
