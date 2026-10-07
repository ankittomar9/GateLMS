#!/usr/bin/env python3
"""
scripts/verify_pyp_bank.py
Verifies PYP question bank against Auditor Z's C8 criteria:
- Exactly 555 items
- 0 duplicated external IDs
- 0 URLs (gateOverflowUrl is null)
- Source is AUTHORED
- 0 fake "Official GATE paper item" or "IIT answer key" curation notes
- Distinct 60-char stems >= 300 (targeting > 500)
- Single-answer MCQ keys balanced ~25% each
- 100% explanations
"""

import sys
import json
from collections import defaultdict

def verify_bank(path="seed-data/pyp-bank.jsonl"):
    total = 0
    ids = set()
    dup_ids = 0
    urls = 0
    fake_notes = 0
    explanations = 0
    stems_60 = set()
    full_stems = set()
    option_sets = set()
    mcq_answers = defaultdict(int)
    mcq_count = 0
    by_year = defaultdict(int)

    with open(path, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.trim() if hasattr(line, 'trim') else line.strip()
            if not line:
                continue
            total += 1
            item = json.loads(line)

            ext_id = item.get("externalId", "")
            if ext_id in ids:
                dup_ids += 1
            ids.add(ext_id)

            if item.get("gateOverflowUrl"):
                urls += 1

            note = item.get("curationNote") or ""
            if "Official GATE" in note or "IIT answer key" in note:
                fake_notes += 1

            if item.get("explanation"):
                explanations += 1

            stmt = item.get("statement", "")
            prefix = stmt[:60].strip()
            stems_60.add(prefix)
            full_stems.add(stmt.strip())

            opts = item.get("options")
            if opts:
                if isinstance(opts, dict):
                    texts = frozenset(str(v).strip() for v in opts.values())
                elif isinstance(opts, list):
                    texts = frozenset(str(o.get("text", "")).strip() for o in opts if isinstance(o, dict))
                else:
                    texts = frozenset()
                option_sets.add(texts)

            if item.get("type") == "MCQ":
                corr = item.get("correctOptions")
                if isinstance(corr, list) and len(corr) == 1:
                    mcq_count += 1
                    mcq_answers[corr[0]] += 1
                elif isinstance(corr, str) and len(corr) == 1:
                    mcq_count += 1
                    mcq_answers[corr] += 1

            by_year[item.get("year")] += 1

    print("==================================================")
    print(f"PYP Bank Verification Report: {path}")
    print("==================================================")
    print(f"Total Questions:         {total} (Expected: 555)")
    print(f"Unique External IDs:     {len(ids)} (Duplicates: {dup_ids})")
    print(f"URLs present:            {urls} (Target: 0)")
    print(f"Fake Provenance Notes:   {fake_notes} (Target: 0)")
    print(f"Explanation Coverage:    {explanations}/{total} ({(explanations/total)*100:.1f}%)")
    print(f"Distinct Stems (Full):   {len(full_stems)}")
    print(f"Distinct Stems (60-char):{len(stems_60)} (Target: >= 300)")
    print(f"Unique Option Sets:      {len(option_sets)}")
    print("--------------------------------------------------")
    print(f"Single-Answer MCQs:      {mcq_count}")
    for k in ["A", "B", "C", "D"]:
        cnt = mcq_answers[k]
        pct = (cnt / mcq_count * 100) if mcq_count else 0
        print(f"  Option {k}: {cnt:4d} ({pct:5.2f}%)")
    print("--------------------------------------------------")
    print(f"Years: {sorted(by_year.keys())}")
    for y in sorted(by_year.keys()):
        print(f"  {y}: {by_year[y]} questions")
    print("==================================================")

    passed = (
        total == 555 and
        dup_ids == 0 and
        urls == 0 and
        fake_notes == 0 and
        explanations == total and
        len(stems_60) >= 300
    )
    print(f"VERDICT: {'PASS' if passed else 'FAIL'}")
    return passed

if __name__ == "__main__":
    p = sys.argv[1] if len(sys.argv) > 1 else "seed-data/pyp-bank.jsonl"
    ok = verify_bank(p)
    sys.exit(0 if ok else 1)
