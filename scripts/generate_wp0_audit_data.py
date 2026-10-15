# scripts/generate_wp0_audit_data.py
import json
import re
from collections import defaultdict

prefixes = [
    r'for\s+[^:]+:\s*',
    r'evaluate\s+for\s+[^:]+:\s*',
    r'with\s+[^:]+parameters:\s*',
    r'from\s+[^:]+concepts:\s*',
    r'on\s+[^:]+:\s*',
    r'in\s+[^:]+formulation:\s*',
    r'as\s+applied\s+to\s+[^:]+:\s*',
    r'under\s+the\s+principles\s+of\s+[^:]+:\s*',
    r'reviewing\s+[^:]+:\s*',
    r'in\s+the\s+domain\s+of\s+[^:]+:\s*'
]
prefix_re = re.compile('^(' + '|'.join(prefixes) + ')', re.IGNORECASE)

def normalize(stmt):
    stmt = prefix_re.sub('', stmt)
    return re.sub(r'\s+', ' ', stmt).strip().lower()

def pattern(stmt):
    return re.sub(r'\d+', '#', normalize(stmt))

# 1. Load questions
pilot_questions = []
for path in ['seed-data/questions.jsonl', 'seed-data/pyp-bank.jsonl', 'seed-data/so-gate-bank.jsonl']:
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            if not line.strip() or line.strip().startswith('#'): continue
            d = json.loads(line)
            if d.get('topicSlug') == 'process-synchronization':
                pilot_questions.append((path, d))

print(f"Total pilot questions in seed banks: {len(pilot_questions)}")

# Check duplicate normalized statements
seen = {}
duplicates = []
for path, q in pilot_questions:
    norm = normalize(q['statement'])
    eid = q['externalId']
    if norm in seen:
        duplicates.append((eid, seen[norm]))
    else:
        seen[norm] = eid

print(f"Normalized duplicates count: {len(duplicates)}")
assert len(duplicates) == 0, f"Duplicates found: {duplicates}"

# Group questions by concept
# For newly authored questions, curationNote contains: Phase 8 WP0 pilot question authored for concept '<concept>'.
# For existing questions, mapped per spec §6.2:
# SYN-OS-2024-Q16 -> critical-section-requirements
# AUTH-OS-PSYNC-VAR1, VAR2 -> semaphore-state-tracing
# SO-OS-0001 -> peterson-software-solutions, critical-section-requirements
# SO-OS-0005 -> semaphore-operations
# SO-OS-0009, 0029, 0049, 0069, 0089, 0109 -> semaphore-state-tracing, semaphore-operations
# SO-OS-0017 -> semaphore-operations
# SO-OS-0021 -> critical-section-requirements

concept_map = defaultdict(list)
for path, q in pilot_questions:
    eid = q['externalId']
    note = q.get('curationNote', '')
    if "authored for concept '" in note:
        concept = note.split("authored for concept '")[1].split("'")[0]
        concept_map[concept].append(q)
    elif eid == 'SYN-OS-2024-Q16':
        concept_map['critical-section-requirements'].append(q)
    elif eid in ('AUTH-OS-PSYNC-VAR1', 'AUTH-OS-PSYNC-VAR2'):
        concept_map['semaphore-state-tracing'].append(q)
    elif eid == 'SO-OS-0001':
        concept_map['peterson-software-solutions'].append(q)
        concept_map['critical-section-requirements'].append(q)
    elif eid == 'SO-OS-0005':
        concept_map['semaphore-operations'].append(q)
    elif eid in ('SO-OS-0009', 'SO-OS-0029', 'SO-OS-0049', 'SO-OS-0069', 'SO-OS-0089', 'SO-OS-0109'):
        concept_map['semaphore-state-tracing'].append(q)
        concept_map['semaphore-operations'].append(q)
    elif eid == 'SO-OS-0017':
        concept_map['semaphore-operations'].append(q)
    elif eid == 'SO-OS-0021':
        concept_map['critical-section-requirements'].append(q)
    else:
        print(f"Unmapped item: {eid}")

canonical_concepts = [
    "critical-section-requirements",
    "peterson-software-solutions",
    "hardware-atomic-primitives",
    "semaphore-operations",
    "semaphore-state-tracing",
    "producer-consumer-bounded-buffer",
    "readers-writers",
    "dining-philosophers",
    "monitors-condition-variables",
    "race-interleavings"
]

print("\n--- PER-CONCEPT INVENTORY (WP0 Acceptance Gate 1) ---")
print("| Concept Slug | Total Questions | Unique Statements | Distinct Patterns | Saturation Ratio | Gate (>=12) |")
print("|---|---|---|---|---|---|")
for slug in canonical_concepts:
    c_qs = concept_map[slug]
    unique_stmts = set(normalize(x['statement']) for x in c_qs)
    distinct_pats = set(pattern(x['statement']) for x in c_qs)
    ratio = f"{len(unique_stmts)/len(distinct_pats):.1f}:1" if distinct_pats else "N/A"
    gate = "PASS" if len(unique_stmts) >= 12 else "FAIL"
    print(f"| `{slug}` | {len(c_qs)} | {len(unique_stmts)} | {len(distinct_pats)} | {ratio} | **{gate}** |")

# Removals check
with open('seed-data/removals.jsonl', 'r', encoding='utf-8') as f:
    rem_lines = [l for l in f if l.strip() and not l.startswith('#')]
print(f"\nRemovals count in removals.jsonl: {len(rem_lines)}")

# Select 10 diverse audit sample questions across different concepts
samples = []
for slug in canonical_concepts:
    samples.append(concept_map[slug][0])

print(f"\n--- AUDITOR VERIFICATION SAMPLES (10 Questions across 10 concepts) ---")
for i, s in enumerate(samples, 1):
    note = s.get('curationNote', '')
    concept = slug
    diff = s.get('difficulty', 'N/A')
    print(f"### Sample {i}: [{s['externalId']}] ({s['type']}, {s['marks']} Mark, {diff})")
    print(f"**Statement:** {s['statement']}")
    if s.get('options'):
        for k, v in s['options'].items():
            print(f"  - ({k}) {v}")
        print(f"**Correct Option(s):** {s['correctOptions']}")
    else:
        print(f"**Numeric Range:** [{s['numericMin']}, {s['numericMax']}]")
    print(f"**Explanation:** {s['explanation']}\n")
