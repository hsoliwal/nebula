"""Render a GitHub Actions job's `run:` steps as one local bash script (ubuntu-24.04 emulation).

Usage: wf2bash.py WORKFLOW.yml JOB BASE_SHA > job.sh
- `uses:` steps are skipped (setup-java/maven/upload-artifact are provided by the host).
- `${{ github.event.pull_request.base.sha }}` -> BASE_SHA, `${{ secrets.* }}` -> empty,
  `${{ runner.temp }}` -> $RUNNER_TEMP, `${{ github.workspace }}` -> $GITHUB_WORKSPACE.
- Each step runs in its own bash with `set -eo pipefail` (Actions default for shell: bash);
  the script records STEP_PASS / STEP_FAIL lines and stops at the first failure.
"""
import re
import shlex
import sys

import yaml


def expr(text, base):
    text = text.replace("${{ github.event.pull_request.base.sha }}", base)
    text = text.replace("${{ runner.temp }}", "$RUNNER_TEMP")
    text = text.replace("${{ github.workspace }}", "$GITHUB_WORKSPACE")
    text = re.sub(r"\$\{\{\s*secrets\.[A-Z0-9_]+\s*\}\}", "", text)
    text = re.sub(r"\$\{\{\s*env\.([A-Z0-9_]+)\s*\}\}", r"${\1}", text)
    if "${{" in text:
        raise SystemExit("unhandled expression: " + text)
    return text


def main(path, job_name, base):
    workflow = yaml.safe_load(open(path, encoding="utf-8"))
    job = workflow["jobs"][job_name]
    out = ["#!/usr/bin/env bash", "cd \"$GITHUB_WORKSPACE\""]
    for key, value in (workflow.get("env") or {}).items():
        out.append("export %s=%s" % (key, shlex.quote(str(value))))
    for key, value in (job.get("env") or {}).items():
        out.append("export %s=%s" % (key, shlex.quote(expr(str(value), base))))
    for index, step in enumerate(job["steps"], 1):
        if "run" not in step:
            continue
        name = step.get("name", "step %d" % index)
        if step.get("if") and "pull_request" not in str(step["if"]):
            out.append("echo 'STEP_SKIP_CONDITION %d %s'" % (index, name.replace("'", "")))
            continue
        envs = " ".join("%s=%s" % (k, shlex.quote(expr(str(v), base)))
                        for k, v in (step.get("env") or {}).items())
        body = expr(step["run"], base)
        out.append("echo '::STEP %d %s'" % (index, name.replace("'", "")))
        out.append("if env %s bash -eo pipefail -c %s; then echo 'STEP_PASS %d'; else rc=$?; "
                   "echo \"STEP_FAIL %d rc=$rc\"; exit $rc; fi"
                   % (envs, shlex.quote(body), index, index))
    out.append("echo JOB_PASS")
    print("\n".join(out))


if __name__ == "__main__":
    main(*sys.argv[1:4])
