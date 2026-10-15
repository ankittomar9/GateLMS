# scripts/append_22_questions.py
import json
import re

import generate_remaining_wp0_questions
new_qs = generate_remaining_wp0_questions.questions
print(f"Loaded {len(new_qs)} new questions")

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

# Check against existing bank
existing_stmts = {}
for path in ['seed-data/questions.jsonl', 'seed-data/pyp-bank.jsonl', 'seed-data/so-gate-bank.jsonl']:
    with open(path, 'r', encoding='utf-8') as f:
        for line in f:
            if not line.strip() or line.strip().startswith('#'): continue
            d = json.loads(line)
            norm = normalize(d['statement'])
            existing_stmts[norm] = d['externalId']

print(f"Total existing normalized statements: {len(existing_stmts)}")

for q in new_qs:
    norm = normalize(q['statement'])
    eid = q['externalId']
    assert norm not in existing_stmts, f"Collision detected: {eid} matches existing {existing_stmts[norm]}"
    existing_stmts[norm] = eid

print("All 22 new questions are 100% unique!")

# Append to both pyp-bank.jsonl locations
for path in ['seed-data/pyp-bank.jsonl', 'backend/seed-data/pyp-bank.jsonl', '../seed-data/pyp-bank.jsonl']:
    try:
        with open(path, 'r', encoding='utf-8') as f:
            lines = [l for l in f if l.strip()]
        
        # Check no ID collisions
        ext_ids = set(json.loads(l)['externalId'] for l in lines)
        for q in new_qs:
            assert q['externalId'] not in ext_ids, f"ID collision: {q['externalId']} already in {path}"
        
        with open(path, 'a', encoding='utf-8') as f:
            for q in new_qs:
                f.write(json.dumps(q, ensure_ascii=False) + '\n')
        print(f"Appended 22 questions to {path} (new line count: {len(lines) + 22})")
    except FileNotFoundError:
        pass

print("Done appending!")
