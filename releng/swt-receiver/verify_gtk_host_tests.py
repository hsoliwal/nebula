#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Bind original GTK AllTests reports and JVM native loads to the receiver bytes.

Capture requires the pinned, unmodified test sources. Verification rechecks the
retained raw evidence on cache reuse. This does not qualify non-host platforms,
the optional resource-budget suite, or downstream Terminal/VNC runtime tests.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile

PIN = "a1a0471ee8b30bd31d54ff6bc6758da5d9bd31b1"
SCHEMA = "NEBULA_SWT_GTK_HOST_ALLTESTS_V1"
GTK_JAR = "org.eclipse.swt.gtk.linux.x86_64-3.138.0-SNAPSHOT.jar"
TEST_ROOT = Path("tests/org.eclipse.swt.tests")
NATIVE_ROOT = Path("binaries/org.eclipse.swt.gtk.linux.x86_64")
REQUIRED = {
    "Test_ViewportBoundsGtk",
    "Test_ItemBitSetDifferential",
    "Test_ItemBitSetMaximumPosition",
}
GTK_CASES = {
    "tableZeroWidthThenReopenPreservesItemsAndSelection",
    "tableHiddenAncestorSuppressesRowsButExplicitDataAccessRemains",
    "treeZeroWidthThenReopenPreservesIdentityInBothModels",
    "treeHiddenAncestorCannotResurrectAllocatedPage",
    "treeZeroHeightRejectsStalePageAndRestoresDemandCallbacks",
}
NATIVE_CLASSES = {
    "Test_org_eclipse_swt_widgets_Display",
    "Test_org_eclipse_swt_graphics_GC",
}
NATIVE_PREFIXES = ("libswt-gtk-", "libswt-pi3-gtk-", "libswt-cairo-gtk-", "libswt-atk-gtk-")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def checked_file(path: Path) -> bytes:
    if path.is_symlink() or not path.is_file():
        raise ValueError("Missing or linked evidence: " + str(path))
    return path.read_bytes()


def case_name(name: str) -> str:
    return name.split("(", 1)[0].split("[", 1)[0]


PACKAGE = "org.eclipse.swt.tests.junit"
JAVA_ROOT = Path("tests/org.eclipse.swt.tests/JUnit Tests")
IDENT = r"[A-Za-z_$][A-Za-z0-9_$]*"
QUALIFIED = IDENT + r"(?:\." + IDENT + r")*"


def _blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()


def _views(text: str) -> tuple[str, str]:
    """Keep offsets; erase comments, then erase quoted literals for delimiters."""
    clean, mask = list(text), list(text)
    i = 0
    while i < len(text):
        if text.startswith("//", i):
            end = text.find("\n", i)
            end = len(text) if end < 0 else end
            for k in range(i, end):
                clean[k] = mask[k] = " "
            i = end
        elif text.startswith("/*", i):
            end = text.find("*/", i + 2)
            if end < 0:
                raise ValueError("Unterminated source comment")
            end += 2
            for k in range(i, end):
                if text[k] != "\n":
                    clean[k] = mask[k] = " "
            i = end
        elif text[i] in "\"'":
            if text.startswith('"""', i):
                end = i + 3
                while True:
                    closing = text.find('"""', end)
                    if closing < 0:
                        raise ValueError("Unterminated source text block")
                    escaped, previous = 0, closing - 1
                    while previous >= i and text[previous] == "\\":
                        escaped += 1
                        previous -= 1
                    if escaped % 2 == 0:
                        end = closing + 3
                        break
                    end = closing + 1
            else:
                quote, end = text[i], i + 1
                while end < len(text):
                    if text[end] == "\\":
                        end += 2
                    elif text[end] == quote:
                        end += 1
                        break
                    else:
                        end += 1
                else:
                    raise ValueError("Unterminated source literal")
            for k in range(i, end):
                if text[k] != "\n":
                    mask[k] = " "
            i = end
        else:
            i += 1
    return "".join(clean), "".join(mask)


def _close(mask: str, start: int, opening: str, closing: str) -> int:
    if mask[start] != opening:
        raise ValueError("Invalid source delimiter")
    depth = 0
    for index in range(start, len(mask)):
        if mask[index] == opening:
            depth += 1
        elif mask[index] == closing:
            depth -= 1
            if depth == 0:
                return index
    raise ValueError("Unbalanced source delimiter")


def _annotations(clean: str, mask: str, annotation: str) -> list[tuple[str, str]]:
    result = []
    pattern = r"@(?:" + QUALIFIED + r"\.)?" + re.escape(annotation) + r"\b"
    for match in re.finditer(pattern, mask):
        index = match.end()
        while index < len(mask) and mask[index].isspace():
            index += 1
        if index < len(mask) and mask[index] == "(":
            end = _close(mask, index, "(", ")")
            result.append((clean[index + 1:end], mask[index + 1:end]))
        else:
            result.append(("", ""))
    return result


def _read(source: Path, full_name: str) -> dict:
    if not re.fullmatch(re.escape(PACKAGE) + r"\." + IDENT, full_name):
        raise ValueError("Unknown source-owned test class: " + full_name)
    relative = JAVA_ROOT / Path(*full_name.split(".")).with_suffix(".java")
    path = source / relative
    if path.is_symlink() or not path.is_file():
        raise ValueError("Missing or linked original class: " + full_name)
    raw = path.read_bytes()
    clean, mask = _views(raw.decode("utf-8"))
    declaration = re.search(r"\bclass\s+" + re.escape(full_name.rsplit(".", 1)[1]) + r"\b", mask)
    if declaration is None:
        raise ValueError("Original test class declaration is missing")
    start = mask.find("{", declaration.end())
    if start < 0:
        raise ValueError("Original class body is missing")
    end = _close(mask, start, "{", "}")
    imports = dict((m.group(2), m.group(1)) for m in re.finditer(
        r"\bimport\s+((?:" + IDENT + r"\.)+(" + IDENT + r"))\s*;", mask))
    return {"class": full_name, "sourcePath": relative.as_posix(), "sourceGitBlob": _blob(raw),
            "clean": clean, "mask": mask, "declaration": declaration.start(), "start": start,
            "end": end, "imports": imports}


def _resolve(owner: dict, name: str) -> str:
    if "." in name:
        return name
    return owner["imports"].get(name, PACKAGE + "." + name)


def _selected_inventory(source: Path) -> dict:
    """Resolve literal registrations recursively, including wrapper suites."""
    leaves, suites, visited, active = {}, {}, set(), set()

    def visit(full_name: str) -> None:
        if full_name in active:
            raise ValueError("Cyclic original suite selection: " + full_name)
        if full_name in visited:
            return
        active.add(full_name)
        owner = _read(source, full_name)
        selectors = set(re.findall(r"@(?:" + IDENT + r"\.)*(Select[A-Za-z0-9_]*)\b", owner["mask"]))
        if selectors - {"SelectClasses"}:
            raise ValueError("Unsupported original suite selector: " + full_name)
        selections = _annotations(owner["clean"], owner["mask"], "SelectClasses")
        if selections:
            if len(selections) != 1 or not full_name.rsplit(".", 1)[1].startswith("All"):
                raise ValueError("Ambiguous original suite registration: " + full_name)
            _, selected = selections[0]
            selected = selected.strip()
            if selected.startswith("{") and selected.endswith("}"):
                selected = selected[1:-1]
            parts = [part.strip() for part in selected.split(",") if part.strip()]
            if not parts:
                raise ValueError("Empty original suite registration")
            suites[full_name] = {key: owner[key] for key in ("class", "sourcePath", "sourceGitBlob")}
            for part in parts:
                match = re.fullmatch(r"(" + QUALIFIED + r")\s*\.\s*class", part)
                if match is None:
                    raise ValueError("Nonliteral original class selector")
                visit(_resolve(owner, match.group(1)))
        elif _annotations(owner["clean"], owner["mask"], "Suite") or full_name.rsplit(".", 1)[1].startswith("All"):
            raise ValueError("Unsupported original suite selection: " + full_name)
        else:
            leaves[full_name] = {key: owner[key] for key in ("class", "sourcePath", "sourceGitBlob")}
        active.remove(full_name)
        visited.add(full_name)

    visit(PACKAGE + ".AllTests")
    if not leaves:
        raise ValueError("No original AllTests leaf classes")
    return {"leaves": [leaves[name] for name in sorted(leaves)],
            "suiteSources": [suites[name] for name in sorted(suites)]}


def selected_leaf_classes(source: Path) -> list[dict]:
    return _selected_inventory(source)["leaves"]


def selected_suite_sources(source: Path) -> list[dict]:
    return _selected_inventory(source)["suiteSources"]


def _methods(owner: dict) -> dict[str, list[dict]]:
    """Recognize direct method bodies; reject ambiguous overloaded test names."""
    clean, mask = owner["clean"], owner["mask"]
    result = {}
    index = member_start = owner["start"] + 1
    parentheses = 0
    while index < owner["end"]:
        if mask[index] == "(":
            parentheses += 1
        elif mask[index] == ")":
            parentheses -= 1
        elif mask[index] == ";" and parentheses == 0:
            member_start = index + 1
        elif mask[index] == "{" and parentheses == 0:
            end = _close(mask, index, "{", "}")
            header = mask[member_start:index]
            # Locate the last parameter list, allowing a throws clause.
            ending = re.search(r"\)\s*(?:throws\s+" + QUALIFIED + r"(?:\s*,\s*" + QUALIFIED + r")*)?\s*$", header)
            if ending:
                close_index = member_start + ending.start()
                depth, opening = 1, close_index - 1
                while opening >= member_start:
                    if mask[opening] == ")":
                        depth += 1
                    elif mask[opening] == "(":
                        depth -= 1
                        if not depth:
                            break
                    opening -= 1
                name_match = re.search(r"(" + IDENT + r")\s*$", mask[member_start:opening])
                prefix = mask[member_start:member_start + name_match.start()] if name_match else ""
                level, assigned = 0, False
                for token in prefix:
                    if token in "([":
                        level += 1
                    elif token in ")]":
                        level -= 1
                    elif token == "=" and level == 0:
                        assigned = True
                if name_match and not assigned:
                    name = name_match.group(1)
                    method = {"name": name, "cleanHeader": clean[member_start:index], "maskHeader": header,
                              "cleanBody": clean[index + 1:end], "maskBody": mask[index + 1:end]}
                    result.setdefault(name, []).append(method)
            index = end
            member_start = end + 1
        index += 1
    return result


def _chain(source: Path, full_name: str) -> list[dict]:
    result, visited = [], set()
    while full_name:
        if full_name in visited:
            raise ValueError("Cyclic source-owned test inheritance")
        visited.add(full_name)
        owner = _read(source, full_name)
        owner["methods"] = _methods(owner)
        result.append(owner)
        header = owner["mask"][owner["declaration"]:owner["start"]]
        parent = re.search(r"\bextends\s+(" + QUALIFIED + r")\b", header)
        if parent is None or parent.group(1) in ("Object", "java.lang.Object"):
            break
        full_name = _resolve(owner, parent.group(1))
    return result


def _literal(value: str) -> str | None:
    value = value.strip()
    if not re.fullmatch(r'"(?:[^"\\]|\\.)*"', value):
        return None
    try:
        result = json.loads(value)
    except json.JSONDecodeError:
        return None
    return result if isinstance(result, str) else None


def _annotation_permission(clean: str, mask: str, message: str) -> str | None:
    for value, _ in _annotations(clean, mask, "Disabled"):
        value = re.sub(r"^\s*value\s*=", "", value)
        if _literal(value) == message:
            return "original-source-declared-condition"
    for annotation in ("DisabledOnOs", "EnabledOnOs"):
        for clean_args, masked_args in _annotations(clean, mask, annotation):
            # Only the actually observed value and disabledReason attributes.
            attributes, unsupported = {}, False
            for value, masked in _argument_pairs(clean_args, masked_args):
                named = re.match(r"\s*(" + IDENT + r")\s*=", masked)
                key = named.group(1) if named else "value"
                if key not in ("value", "disabledReason") or key in attributes:
                    unsupported = True
                    break
                attributes[key] = value[named.end():].strip() if named else value.strip()
            if unsupported or "value" not in attributes:
                continue
            value = attributes["value"].removeprefix("{").removesuffix("}").strip()
            selectors = [item.strip() for item in value.split(",") if item.strip()]
            os_pattern = r"(?:org\.junit\.jupiter\.api\.condition\.)?OS\.([A-Z0-9_]+)"
            parsed = [re.fullmatch(os_pattern, item) for item in selectors]
            if not parsed or not all(parsed):
                continue
            systems = {item.group(1) for item in parsed}
            disabled = (annotation == "DisabledOnOs" and "LINUX" in systems) or (
                annotation == "EnabledOnOs" and "LINUX" not in systems)
            custom = _literal(attributes["disabledReason"]) if "disabledReason" in attributes else ""
            if custom is None:
                continue
            expected = "Disabled on operating system: Linux"
            if custom.strip():
                expected += " ==> " + custom
            if disabled and message == expected:
                return "original-source-os-condition"
    return None


def _argument_pairs(clean: str, mask: str) -> list[tuple[str, str]]:
    depth, begin, result = 0, 0, []
    for index in range(len(mask) + 1):
        token = mask[index] if index < len(mask) else ","
        if token in "([{":
            depth += 1
        elif token in ")]}":
            depth -= 1
        elif token == "," and depth == 0:
            result.append((clean[begin:index], mask[begin:index]))
            begin = index + 1
    return result


def _platform_assumption_permission(source: Path, owner: dict, clean: str, mask: str, message: str) -> dict:
    """Only pinned direct Cocoa/Win32 assumptions known false on the GTK host."""
    if message != "assumption is not true":
        return {}
    for match in re.finditer(r"\bassumeTrue\s*\(", mask):
        start = mask.find("(", match.start())
        end = _close(mask, start, "(", ")")
        expression = re.sub(r"\s+", "", clean[start + 1:end])
        witness = {"runtimePlatform": "gtk", "sourceExpression": expression,
                   "conditionValue": False, "requiredAssumption": True}
        if expression in ('"cocoa".equals(SWT.getPlatform())', '"win32".equals(SWT.getPlatform())'):
            if owner["imports"].get("SWT") != "org.eclipse.swt.SWT" and not re.search(
                    r"\bimport\s+org\.eclipse\.swt\.\*\s*;", owner["mask"]):
                continue
            return {"platformWitness": witness}
        if expression == "SwtTestUtil.isCocoa":
            utility = _read(source, PACKAGE + ".SwtTestUtil")
            declaration = re.findall(r"\bboolean\s+isCocoa\s*=([^;]+);", utility["clean"])
            if len(declaration) != 1 or re.sub(r"\s+", "", declaration[0]) != 'SWT.getPlatform().startsWith("cocoa")':
                continue
            if utility["imports"].get("SWT") != "org.eclipse.swt.SWT" and not re.search(
                    r"\bimport\s+org\.eclipse\.swt\.\*\s*;", utility["mask"]):
                continue
            witness.update({"definitionSourcePath": utility["sourcePath"],
                            "definitionSourceGitBlob": utility["sourceGitBlob"]})
            return {"platformWitness": witness}
    return {}


def _assumption_permission(clean: str, mask: str, message: str) -> bool:
    for match in re.finditer(r"\b(assumeTrue|assumeFalse|assumeThat)\s*\(", mask):
        start = mask.find("(", match.start())
        end = _close(mask, start, "(", ")")
        # A message must be a direct, complete argument of this assumption.
        # A matching literal in a predicate or another method does not qualify.
        depth = 0
        arguments = []
        begin = start + 1
        for index in range(begin, end + 1):
            token = mask[index] if index < end else ","
            if token in "([{":
                depth += 1
            elif token in ")]}":
                depth -= 1
            elif token == "," and depth == 0:
                arguments.append(clean[begin:index])
                begin = index + 1
        if match.group(1) == "assumeThat":
            # The two-argument overload has an actual value and matcher, no message.
            candidates = arguments[:1] if len(arguments) == 3 else []
        else:
            # JUnit 4 uses (message, boolean...); Jupiter uses (boolean, message).
            candidates = arguments[:1] if len(arguments) >= 2 else []
            if len(arguments) == 2:
                candidates += arguments[1:]
        if any(_literal(argument) == message for argument in candidates):
            return True
    return False


def permitted_skip(source: Path, full_name: str, testcase_name: str, reason: str) -> dict:
    """Bind a skip to its exact method or applicable original fixture scope."""
    name = testcase_name.split("(", 1)[0].split("[", 1)[0]
    if not re.fullmatch(IDENT, name) or not reason:
        raise ValueError("Unknown original skipped testcase identity")
    message = reason.removeprefix("Assumption failed: ")
    if not message:
        raise ValueError("Unexplained original skipped testcase")
    chain = _chain(source, full_name)
    first = chain[0]
    scopes = []
    # Class annotations on an ancestor are not assumed to be inherited.
    class_clean = first["clean"][:first["declaration"]]
    class_mask = first["mask"][:first["declaration"]]
    scopes.append((first, "<class>", class_clean, class_mask, False))
    found_method = False
    for owner in chain:
        methods = owner["methods"].get(name, [])
        if methods:
            if len(methods) != 1:
                raise ValueError("Ambiguous overloaded skipped testcase")
            method = methods[0]
            scopes.append((owner, name, method["cleanHeader"], method["maskHeader"], False))
            scopes.append((owner, name, method["cleanBody"], method["maskBody"], True))
            found_method = True
            break
    if not found_method:
        raise ValueError("Skipped testcase method absent from original source hierarchy")
    shadowed = set()
    for owner in chain:
        for method_name, methods in owner["methods"].items():
            if method_name in shadowed:
                continue
            if len(methods) != 1:
                continue
            method = methods[0]
            if any(_annotations(method["cleanHeader"], method["maskHeader"], annotation)
                   for annotation in ("BeforeAll", "BeforeEach")):
                scopes.append((owner, method_name, method["cleanBody"], method["maskBody"], True))
        shadowed.update(owner["methods"])
    for owner, condition_method, clean, mask, assumption in scopes:
        extra = {}
        rule = ("original-source-declared-condition" if _assumption_permission(clean, mask, message) else None) \
            if assumption else _annotation_permission(clean, mask, message)
        if assumption and rule is None:
            extra = _platform_assumption_permission(source, owner, clean, mask, message)
            if extra:
                rule = "original-source-os-condition"
        if rule:
            return {"class": full_name, "name": testcase_name, "reason": reason, "rule": rule,
                    "sourcePath": first["sourcePath"], "sourceGitBlob": first["sourceGitBlob"],
                    "conditionOwner": owner["class"], "conditionMethod": condition_method,
                    "conditionSourcePath": owner["sourcePath"], "conditionSourceGitBlob": owner["sourceGitBlob"], **extra}
    raise ValueError("No applicable original source condition for skipped test: " + full_name + "." + testcase_name)


def report_summary(proof: Path, native_directory: str, permitted: list[dict] | None,
                   source: Path | None = None, selected: dict | None = None) -> dict:
    if source is not None:
        selected = _selected_inventory(source)
    if not isinstance(selected, dict) or not selected.get("leaves") or not selected.get("suiteSources"):
        raise ValueError("Original AllTests selection inventory is missing")
    selected_names = set()
    for row in selected["leaves"] + selected["suiteSources"]:
        name = row.get("class", "")
        if not re.fullmatch(re.escape(PACKAGE) + r"\." + IDENT, name) or not re.fullmatch(r"[0-9a-f]{40}", row.get("sourceGitBlob", "")):
            raise ValueError("Malformed original source selection identity")
        expected_path = (JAVA_ROOT / Path(*name.split(".")).with_suffix(".java")).as_posix()
        if row.get("sourcePath") != expected_path:
            raise ValueError("Original source selection path differs")
    for row in selected["leaves"]:
        if row["class"] in selected_names:
            raise ValueError("Duplicate original leaf class")
        selected_names.add(row["class"])
    if PACKAGE + ".AllTests" not in {row["class"] for row in selected["suiteSources"]}:
        raise ValueError("Original AllTests suite root is absent")
    reports = sorted((proof / "reports").glob("TEST-*.xml"))
    if not reports:
        raise ValueError("Original SWT AllTests reports are missing")
    counts = {"passed": 0, "skipped": 0, "failures": 0, "errors": 0}
    classes = {}
    observed = set()
    gtk_cases = set()
    skips = []
    for path in reports:
        root = ET.fromstring(checked_file(path))
        if root.tag != "testsuite":
            raise ValueError("Unsupported original report structure: " + path.name)
        properties = {p.get("name"): p.get("value") for p in root.findall("properties/property")}
        if properties.get("swt.library.path") != native_directory or properties.get("os.name") != "Linux" \
                or properties.get("os.arch") not in ("amd64", "x86_64"):
            raise ValueError("Report does not identify the source-built Linux GTK native path")
        cases = root.findall("testcase")
        actual = {"tests": len(cases), "failures": 0, "errors": 0, "skipped": 0}
        for case in cases:
            full_name = case.get("classname", "")
            name = case.get("name", "")
            if not full_name or not name:
                raise ValueError("Unnamed original testcase")
            leaf = full_name.split("$", 1)[0]
            if leaf not in selected_names:
                raise ValueError("Testcase is outside original AllTests selection: " + full_name)
            observed.add(leaf)
            short = full_name.rsplit(".", 1)[-1]
            stats = classes.setdefault(short, {"passed": 0, "skipped": 0})
            if case.find("failure") is not None:
                actual["failures"] += 1
            if case.find("error") is not None:
                actual["errors"] += 1
            skipped = case.find("skipped")
            if skipped is not None:
                actual["skipped"] += 1
                stats["skipped"] += 1
                reason = skipped.get("message", "").strip()
                if short in REQUIRED or not reason:
                    raise ValueError("Required or unexplained skipped original testcase")
                if source is not None:
                    skips.append(permitted_skip(source, full_name, name, reason))
                else:
                    matches = [row for row in permitted or [] if
                               (row.get("class"), row.get("name"), row.get("reason")) == (full_name, name, reason)]
                    if len(matches) != 1 or matches[0].get("rule") not in (
                            "original-source-declared-condition", "original-source-os-condition"):
                        raise ValueError("Skipped testcase lacks its explicit source-bound permission")
                    skips.append(matches[0])
            elif case.find("failure") is None and case.find("error") is None:
                counts["passed"] += 1
                stats["passed"] += 1
                if short == "Test_ViewportBoundsGtk":
                    gtk_cases.add(case_name(name))
        for key, value in actual.items():
            if int(root.get(key, "-1")) != value:
                raise ValueError("Report summary disagrees with testcase evidence: " + path.name)
            if key != "tests":
                counts[key] += value
    if counts["passed"] <= 0 or counts["failures"] or counts["errors"]:
        raise ValueError("Original GTK AllTests did not pass")
    if observed != selected_names:
        raise ValueError("Original AllTests selected leaf classes are missing: " + ", ".join(sorted(selected_names - observed)))
    for name in REQUIRED | NATIVE_CLASSES:
        if classes.get(name, {}).get("passed", 0) <= 0:
            raise ValueError("Required original native test class did not execute: " + name)
    if any(classes[name]["skipped"] for name in REQUIRED) or gtk_cases != GTK_CASES:
        raise ValueError("Incomplete original GTK viewport or sparse-state execution")
    if permitted is not None and sorted(skips, key=lambda row: (row["class"], row["name"])) != permitted:
        raise ValueError("Skip permission inventory disagrees with reports")
    return {"reportCount": len(reports), "testcases": counts,
            "selectedSourceInventory": selected, "observedLeafClasses": sorted(observed),
            "requiredClasses": sorted(REQUIRED | NATIVE_CLASSES),
            "gtkViewportCases": sorted(gtk_cases),
            "permittedSkips": sorted(skips, key=lambda row: (row["class"], row["name"]))}


def loaded_natives(proof: Path, native_directory: str, expected: dict, source: Path | None = None) -> dict:
    logs = sorted((proof / "native").glob("native-library-*.log"))
    if not logs:
        raise ValueError("JVM native-library load evidence is missing")
    loaded = {}
    for log in logs:
        for line in checked_file(log).decode("utf-8").splitlines():
            match = re.search(r"\[library\]\s+Loaded library (.+), handle 0x[0-9a-fA-F]+", line)
            if not match:
                continue
            path = Path(match.group(1))
            if not path.name.startswith("libswt"):
                continue
            if str(path.parent) != native_directory or path.name not in expected:
                raise ValueError("JVM loaded SWT natives outside the exact source-built receiver")
            if source is not None and digest(checked_file(source / NATIVE_ROOT / path.name)) != expected[path.name]:
                raise ValueError("Loaded native bytes differ from the published receiver")
            loaded[path.name] = expected[path.name]
    if not all(any(name.startswith(prefix) for name in loaded) for prefix in NATIVE_PREFIXES):
        raise ValueError("Original suite did not load all required GTK3 JNI libraries")
    return dict(sorted(loaded.items()))


def evidence_hashes(proof: Path) -> dict:
    result = {}
    for folder, pattern in (("reports", "TEST-*.xml"), ("native", "native-library-*.log")):
        for path in sorted((proof / folder).glob(pattern)):
            result[path.relative_to(proof).as_posix()] = digest(checked_file(path))
    sparse = checked_file(proof / "original-sparse-coverage.json")
    coverage = json.loads(sparse)
    if coverage.get("schema") != "SWT_M3_JUNIT_EXECUTION_V1" or coverage.get("passed") is not True:
        raise ValueError("Original sparse-state execution gate did not pass")
    result["original-sparse-coverage.json"] = digest(sparse)
    return result


def receiver_identity(target: Path) -> dict:
    expected = json.loads(checked_file(target / "binding/expected.json"))
    if (expected.get("sourceCommit"), expected.get("sourceWorktreeHead")) != (PIN, PIN):
        raise ValueError("GTK host evidence is not for the pinned source")
    gtk_digest = expected.get("bundleSha256", {}).get(GTK_JAR)
    matches = [path for path in (target / "repository/plugins").rglob("*.jar")
               if digest(checked_file(path)) == gtk_digest]
    # p2 is allowed to rename the Maven input, never to change its bytes.
    if len(matches) != 1:
        raise ValueError("GTK receiver is missing, changed or ambiguous")
    with ZipFile(matches[0]) as archive:
        natives = {name: digest(archive.read(name)) for name in archive.namelist() if name.endswith(".so")}
        if not natives or natives != expected.get("nativeSha256"):
            raise ValueError("Published GTK native bytes disagree with the receiver manifest")
        if not all(any(name.startswith(prefix) and archive.read(name).startswith(b"\x7fELF")
                       for name in natives) for prefix in NATIVE_PREFIXES):
            raise ValueError("Published receiver lacks original GTK3 JNI payloads")
    return expected


def capture(source: Path, target: Path) -> dict:
    source = source.resolve()
    expected = receiver_identity(target)
    if subprocess.check_output(["git", "-C", str(source), "rev-parse", "HEAD"], text=True).strip() != PIN:
        raise ValueError("Original AllTests checkout is not the pinned source")
    subprocess.run(["git", "-C", str(source), "diff", "--quiet", PIN, "--", str(TEST_ROOT),
                    "pom.xml", "binaries/pom.xml", "bundles/org.eclipse.swt"], check=True)
    if subprocess.check_output(["git", "-C", str(source), "ls-files", "--others", "--exclude-standard", "--", str(TEST_ROOT)]).strip():
        raise ValueError("Untracked original SWT test sources")
    proof = target / "binding/gtk-host-tests"
    reports = proof / "reports"
    reports.mkdir(parents=True, exist_ok=False)
    for report in sorted((source / TEST_ROOT / "target/surefire-reports").glob("TEST-*.xml")):
        (reports / report.name).write_bytes(checked_file(report))
    native_directory = str(source / NATIVE_ROOT)
    summary = report_summary(proof, native_directory, None, source)
    natives = loaded_natives(proof, native_directory, expected["nativeSha256"], source)
    receipt = {"schema": SCHEMA, "sourceCommit": PIN, "suite": "AllTests",
               "backend": "GTK3/X11 Linux x86_64", "nativeDirectory": native_directory,
               "gtkBundleSha256": expected["bundleSha256"][GTK_JAR],
               "loadedNativeSha256": natives, "reports": summary,
               "rawEvidenceSha256": evidence_hashes(proof),
               "nonHostPlatforms": "NOT_RUN", "optionalResourceBudgets": "NOT_QUALIFIED"}
    (proof / "receipt.json").write_text(json.dumps(receipt, sort_keys=True, indent=2) + "\n")
    return verify(target)


def verify(target: Path) -> dict:
    expected = receiver_identity(target)
    proof = target / "binding/gtk-host-tests"
    receipt = json.loads(checked_file(proof / "receipt.json"))
    if (receipt.get("schema"), receipt.get("sourceCommit"), receipt.get("suite"), receipt.get("backend")) != (
            SCHEMA, PIN, "AllTests", "GTK3/X11 Linux x86_64"):
        raise ValueError("Missing original GTK AllTests qualification identity")
    if receipt.get("gtkBundleSha256") != expected["bundleSha256"][GTK_JAR]:
        raise ValueError("GTK test receipt belongs to different receiver bytes")
    native_directory = receipt.get("nativeDirectory", "")
    if not Path(native_directory).is_absolute() or not native_directory.endswith("/.m3-swt/" + NATIVE_ROOT.as_posix()):
        raise ValueError("GTK native load owner is not the exact source checkout")
    if receipt.get("rawEvidenceSha256") != evidence_hashes(proof):
        raise ValueError("Raw original GTK test evidence changed")
    summary = report_summary(proof, native_directory, receipt.get("reports", {}).get("permittedSkips", []),
                             selected=receipt.get("reports", {}).get("selectedSourceInventory"))
    if receipt.get("reports") != summary:
        raise ValueError("Original GTK report summary changed")
    if receipt.get("loadedNativeSha256") != loaded_natives(proof, native_directory, expected["nativeSha256"]):
        raise ValueError("Original GTK native load identity changed")
    return {"sourceCommit": PIN, "gtkBundleSha256": receipt["gtkBundleSha256"],
            "gtkHostAllTests": "PASS", "passed": summary["testcases"]["passed"],
            "permittedSkips": summary["testcases"]["skipped"],
            "nonHostPlatforms": "NOT_RUN", "terminalVncRuntime": "PENDING"}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=("capture", "verify"))
    parser.add_argument("target", type=Path)
    parser.add_argument("--source", type=Path)
    args = parser.parse_args()
    if args.operation == "capture" and args.source is None:
        parser.error("capture requires --source")
    result = capture(args.source, args.target) if args.operation == "capture" else verify(args.target)
    print(json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
