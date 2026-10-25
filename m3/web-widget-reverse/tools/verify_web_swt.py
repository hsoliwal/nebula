#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Read-only fail-closed Web/SWT/Nebula/RWT inventory gate; not runtime proof."""
import argparse
import csv
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[3]
BASE = "m3/web-widget-reverse/"
VALID_ROUTES = {"RAP_NATIVE", "CANVAS_GC_REPLAY", "REBUILD_AS_PAINTER",
                "BLOCKED(draw2d absent on RWT)",
                "BLOCKED(org.eclipse.swt.printing absent in RWT)"}
VALID_FORWARD = {"RWT_COMMON_API", "RWT_REBUILD", "RWT_PORT", "RWT_PAINT", "RWT_HOLD"}
VALID_ALLOCATION = {"PARENT_OWNS_CONTROL", "OWNER_ITEM_VIEWPORT", "PAINT_VISIBLE_ONLY"}


def need(value, message):
    if not value:
        raise ValueError(message)


def load(path, key):
    with path.open(encoding="utf-8", newline="") as stream:
        rdr = csv.DictReader(stream, delimiter="\t")
        need(rdr.fieldnames and key in rdr.fieldnames, "missing " + key)
        data = list(rdr)
    ids = [item[key] for item in data]
    need(bool(data) and len(ids) == len(set(ids)) and all(ids),
         "duplicate/blank/missing key: " + str(path))
    need(all(all(value is not None for value in item.values())
             for item in data), "broken TSV: " + str(path))
    return data


def audit(native, rwt, mapping, extra, frameworks):
    n = {x["concept_id"]: x for x in native}
    r = {x["concept_id"]: x for x in rwt}
    e = {x["concept_id"]: x for x in extra}
    need(len(n) == len(native) and len(r) == len(rwt) and len(e) == len(extra),
         "duplicate concept")
    need(set(n).issubset(r), "native matrix not represented in RWT")
    need(set(e) == set(r).difference(n), "undeclared RWT-only delta")
    for id_, item in r.items():
        need(item["rwt_route"] in VALID_ROUTES, "unknown RWT route: " + id_)
        need("NOT_RUN" in item["rwt_status"] or "BLOCKED" in item["rwt_status"],
             "unverified RWT PASS: " + id_)
    for id_, item in e.items():
        need(item["rwt_route"] == r[id_]["rwt_route"]
             and item["rwt_status"] == r[id_]["rwt_status"]
             and item["disposition"] == "EXPLICIT_RWT_ONLY_HOLD"
             and item["native_matrix_status"] == "NOT_IN_66_CONCEPT_BASELINE",
             "RWT-only delta drift: " + id_)
    seen = set()
    for item in mapping:
        id_ = item["web_widget"]
        need(bool(id_) and id_ not in seen, "duplicate web binding")
        seen.add(id_)
        concept = item["rwt_concept_id"]
        route = item["rwt_route"]
        need((route == "SWT_RWT_COMMON" if concept == "-"
              else concept in r and route == r[concept]["rwt_route"]),
             "route drift: " + id_)
        need(item["forward_route"] in VALID_FORWARD, "unknown forward lane")
        need(item["allocation_policy"] in VALID_ALLOCATION, "unbounded controls")
        need(item["status"] == "HOLD_UNQUALIFIED", "unproven PASS")
        need(item["swt_java"] and item["semantic_gate"], "unbound owner/behavior")
        if item["forward_route"] == "RWT_PAINT":
            need(route == "CANVAS_GC_REPLAY", "paint route mismatch")
        if item["forward_route"] == "RWT_REBUILD":
            need(route == "REBUILD_AS_PAINTER", "rebuild route mismatch")
        if item["forward_route"] == "RWT_PORT":
            need(route == "RAP_NATIVE", "port route mismatch")
        if item["allocation_policy"] == "OWNER_ITEM_VIEWPORT":
            need(item["translation_mode"] == "OWNER_VIEWPORT",
                 "logical row must stay widget owned")
    need(bool(seen) and bool(frameworks), "empty bindings")
    need(all(x["proof_status"] == "HOLD_UNQUALIFIED"
             and x["actual_direction"] and x["reverse_swt_decision"]
             for x in frameworks), "unqualified framework")
    return len(native), len(rwt), len(extra), len(mapping), len(frameworks)


class AuditTests(unittest.TestCase):
    def setUp(self):
        self.n = [{"concept_id": "GRID"}]
        self.r = [{"concept_id": "GRID", "rwt_route": "RAP_NATIVE",
                   "rwt_status": "NOT_RUN"},
                  {"concept_id": "EXTRA", "rwt_route": "RAP_NATIVE",
                   "rwt_status": "NOT_RUN"}]
        self.e = [{"concept_id": "EXTRA", "rwt_route": "RAP_NATIVE",
                   "rwt_status": "NOT_RUN", "disposition": "EXPLICIT_RWT_ONLY_HOLD",
                   "native_matrix_status": "NOT_IN_66_CONCEPT_BASELINE"}]
        self.m = [{"web_widget": "grid", "rwt_concept_id": "GRID",
                   "rwt_route": "RAP_NATIVE", "forward_route": "RWT_PORT",
                   "translation_mode": "OWNER_VIEWPORT",
                   "allocation_policy": "OWNER_ITEM_VIEWPORT",
                   "status": "HOLD_UNQUALIFIED", "swt_java": "Grid",
                   "semantic_gate": "SetData"}]
        self.f = [{"proof_status": "HOLD_UNQUALIFIED",
                   "actual_direction": "JAVA_SERVER_TO_BROWSER",
                   "reverse_swt_decision": "NOT_REVERSE_TRANSPILER"}]

    def check(self):
        return audit(self.n, self.r, self.m, self.e, self.f)

    def test_valid(self):
        self.assertEqual((1, 2, 1, 1, 1), self.check())

    def test_unlisted_extra(self):
        self.e.clear()
        with self.assertRaises(ValueError):
            self.check()

    def test_route_drift(self):
        self.m[0]["rwt_route"] = "CANVAS_GC_REPLAY"
        with self.assertRaises(ValueError):
            self.check()

    def test_unproven_pass(self):
        self.m[0]["status"] = "PASSED"
        with self.assertRaises(ValueError):
            self.check()

    def test_per_row_control(self):
        self.m[0]["allocation_policy"] = "CONTROL_PER_ROW"
        with self.assertRaises(ValueError):
            self.check()

    def test_bad_item_mode(self):
        self.m[0]["translation_mode"] = "STATIC_FORM"
        with self.assertRaises(ValueError):
            self.check()

    def test_rwt_pass_without_proof(self):
        self.r[0]["rwt_status"] = "PASSED"
        with self.assertRaises(ValueError):
            self.check()

    def test_duplicate_web_widget(self):
        self.m.append(dict(self.m[0]))
        with self.assertRaises(ValueError):
            self.check()


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--self-test", action="store_true")
    args = p.parse_args()
    if args.self_test:
        tests = unittest.defaultTestLoader.loadTestsFromTestCase(AuditTests)
        return 0 if unittest.TextTestRunner(verbosity=2).run(tests).wasSuccessful() else 1
    try:
        result = audit(
            load(ROOT / "docs/m3/custom-widgets/WIDGET_MATRIX.tsv", "concept_id"),
            load(ROOT / "docs/m3/rwt/RWT_COLUMN.tsv", "concept_id"),
            load(ROOT / BASE / "WEB_SWT_BINDINGS.tsv", "web_widget"),
            load(ROOT / BASE / "RWT_EXTRA_CONCEPTS.tsv", "concept_id"),
            load(ROOT / BASE / "WEB_FRAMEWORK_LANES.tsv", "framework"))
        print("PASS INVENTORY ONLY: native=%d rwt=%d extra=%d bindings=%d frameworks=%d"
              % result)
        print("HOLD: OpenRewrite SDK, real SWT/Tycho, RAP/RWT and native runtime")
        return 0
    except (OSError, ValueError) as failure:
        print("FAIL INVENTORY: " + str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
