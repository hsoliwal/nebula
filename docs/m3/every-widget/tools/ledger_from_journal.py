# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""Every-widget viewport ledger from the journal of an inventory workflow run (CPU only).

Reads the workflow journal (one JSON object per line; "result" lines carry either a unit's card set
{unit, unit_notes, widgets[]} or a verifier's verdicts {unit, verdicts[]}), applies each verifier
correction to its card (status, aspects present, next leaf), and writes:
  LEDGER.tsv      one row per widget class: unit, class, file, shape, wraps, required, present, status,
                  verdict, size, hidden_work, redraw_full, redraw_rect, tests, next_leaf, verifier_note
  UNITS.tsv       one row per unit: unit, cards, verified (yes/no), notes
Usage: python -I ledger_from_journal.py <journal.jsonl> <out dir> [unit ids expected, comma separated]
"""
import json
import sys


def first_line(text, limit=240):
    text = (text or "").strip().replace("\t", " ").replace("\r", " ")
    line = text.split("\n", 1)[0]
    return line[:limit]


def main():
    journal, out = sys.argv[1], sys.argv[2]
    expected = sys.argv[3].split(",") if len(sys.argv) > 3 else []
    cards, verdicts = {}, {}
    with open(journal, encoding="utf-8") as f:
        for raw in f:
            raw = raw.strip()
            if not raw:
                continue
            rec = json.loads(raw)
            if rec.get("type") != "result" or not isinstance(rec.get("result"), dict):
                continue
            r = rec["result"]
            if "widgets" in r:
                cards[r["unit"]] = r
            elif "verdicts" in r:
                verdicts[r["unit"]] = {v["cls"]: v for v in r["verdicts"]}
    rows, units = [], []
    for unit in sorted(cards):
        card = cards[unit]
        vs = verdicts.get(unit)
        units.append((unit, len(card["widgets"]), "yes" if vs is not None else "no", first_line(card.get("unit_notes"), 400)))
        for w in card["widgets"]:
            v = (vs or {}).get(w["cls"])
            verdict = v["verdict"] if v else "UNVERIFIED"
            status, present, leaf = w["status"], w.get("aspects_present", []), w["next_leaf"]["title"]
            if v and verdict == "CORRECTED":
                status = v.get("corrected_status") or status
                present = v.get("corrected_aspects_present") or present
                leaf = v.get("corrected_next_leaf") or leaf
            if verdict == "REFUTED":
                status = "REFUTED"
            hw = w.get("hidden_work") or []
            rows.append([
                unit, w["cls"].split(".")[-1], w["file"], w["shape"], first_line(w.get("wraps"), 120),
                "+".join(w.get("aspects_required", [])), "+".join(present), status, verdict,
                w["next_leaf"].get("size", ""), str(len(hw)) + (": " + first_line(hw[0]["what"], 100) if hw else ""),
                str(w.get("redraw_full", "")), str(w.get("redraw_rect", "")), str(len(w.get("tests_existing") or [])),
                first_line(leaf), first_line(v["notes"] if v else "", 200),
            ])
    head = ["unit", "class", "file", "shape", "wraps", "required", "present", "status", "verdict", "size",
            "hidden_work", "redraw_full", "redraw_rect", "tests", "next_leaf", "verifier_note"]
    with open(out + "/LEDGER.tsv", "w", encoding="utf-8", newline="") as f:
        f.write("\t".join(head) + "\n")
        for row in rows:
            f.write("\t".join(c.replace("\t", " ").replace("\n", " ") for c in row) + "\n")
    missing = [u for u in expected if u not in cards]
    with open(out + "/UNITS.tsv", "w", encoding="utf-8", newline="") as f:
        f.write("unit\tcards\tverified\tnotes\n")
        for u in units:
            f.write("\t".join(str(c) for c in u) + "\n")
        for u in missing:
            f.write(u + "\t0\tno\tinventory not run (account session limit, 2026-10-24 12:00 UTC); resume the workflow\n")
    by_status = {}
    for row in rows:
        by_status[row[7]] = by_status.get(row[7], 0) + 1
    print("cards %d in %d units (%d verified), missing units %d: %s" % (
        len(rows), len(cards), len(verdicts), len(missing), ", ".join(missing)))
    print("by status: " + ", ".join("%s %d" % kv for kv in sorted(by_status.items())))


if __name__ == "__main__":
    main()
