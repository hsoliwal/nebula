"""BLLSE: parse javac / Maven / JUnit logs into one file:line:severity table.

usage: python parse_logs.py OUT.tsv LOG...
Columns: log, tool, file, line, col, severity, rule, message.
javac "path:line: error|warning: msg", Maven "[ERROR] /path:[line,col] msg",
JUnit 4 "N) method(class)" failures, surefire "[ERROR]   Class.method:line" summaries and "Tests run/OK" summaries. Unparsed
javac notes are kept as NOTE rows without a location.
"""
import re
import sys

JAVAC = re.compile(r"^(?P<file>[A-Za-z]:[^:]+|/[^:]+):(?P<line>\d+): (?P<sev>error|warning): (?P<msg>.*)$")
MAVEN = re.compile(r"^\[(?P<sev>ERROR|WARNING)\] (?P<file>/?[A-Za-z]:?[^\[]*\.java):\[(?P<line>\d+),(?P<col>\d+)\] (?P<msg>.*)$")
JUNIT_FAIL = re.compile(r"^\d+\) (?P<method>[^(]+)\((?P<cls>[^)]+)\)$")
JUNIT_SUM = re.compile(r"^(OK \((?P<ok>\d+) tests?\)|Tests run: (?P<run>\d+),\s+Failures: (?P<fail>\d+))$")
LINT = re.compile(r"\[(?P<rule>[a-z-]+)\] ")
SUREFIRE = re.compile(r"^\[ERROR\]\s+(?P<cls>\w+)\.(?P<method>\w+):(?P<line>\d+) (?P<msg>.*)$")
AT = re.compile(r"^\s+at (?P<cls>[\w.$]+)\.(?P<method>[\w$<>]+)\((?P<file>[\w$]+\.java):(?P<line>\d+)\)$")


def rows(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = [l.rstrip("\r\n") for l in f]
    pending = None
    for i, line in enumerate(lines):
        m = JAVAC.match(line)
        if m:
            rule = LINT.search(m["msg"])
            yield ("javac", m["file"], m["line"], "", m["sev"].upper(), rule["rule"] if rule else "", m["msg"])
            continue
        m = MAVEN.match(line)
        if m:
            yield ("maven", m["file"], m["line"], m["col"], m["sev"], "", m["msg"])
            continue
        if line.startswith("Note: "):
            yield ("javac", "", "", "", "NOTE", "", line[6:])
            continue
        m = SUREFIRE.match(line)
        if m:
            yield ("surefire", m["cls"] + ".java", m["line"], "", "FAIL", m["method"], m["msg"][:300])
            continue
        m = JUNIT_FAIL.match(line)
        if m:
            pending = (m["cls"], m["method"], lines[i + 1] if i + 1 < len(lines) else "")
            continue
        if pending:
            a = AT.match(line)
            if a and a["cls"] == pending[0]:
                yield ("junit", pending[0].replace(".", "/") + ".java", a["line"], "", "FAIL",
                       pending[1], pending[2])
                pending = None
            continue
        m = JUNIT_SUM.match(line)
        if m:
            if m["ok"]:
                yield ("junit", "", "", "", "PASS", "summary", "OK (%s tests)" % m["ok"])
            else:
                yield ("junit", "", "", "", "FAIL", "summary", line)


def main():
    out = sys.argv[1]
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("log\ttool\tfile\tline\tcol\tseverity\trule\tmessage\n")
        for log in sys.argv[2:]:
            for r in rows(log):
                w.write("\t".join([log] + [str(x).replace("\t", " ") for x in r]) + "\n")


if __name__ == "__main__":
    main()
