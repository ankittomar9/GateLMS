# scripts/merge_and_validate_wp0.py
import json
import re
from collections import defaultdict

# 1. Load Part 1 (Q1 - Q15)
with open("scripts/temp_psync_part1.json", "r", encoding="utf-8") as f:
    part1 = json.load(f)

# 2. Load Part 2 (Q16 - Q60)
import questions_part2_rw_dp_monitors
part2 = questions_part2_rw_dp_monitors.get_questions()

# 3. Load Part 3 (Q61 - Q108)
import questions_part3_hw_races_peterson
part3 = questions_part3_hw_races_peterson.get_questions()

all_new = part1 + part2 + part3
print(f"Total new questions: {len(all_new)}")
assert len(all_new) == 108, f"Expected 108, got {len(all_new)}"

# Prefix stripping regex
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
    stmt = re.sub(r'\s+', ' ', stmt).strip().lower()
    return stmt

def get_pattern(stmt):
    n = normalize(stmt)
    return re.sub(r'\d+', '#', n)

# Validate uniqueness among new questions
seen_norm = {}
for q in all_new:
    n = normalize(q['statement'])
    eid = q['externalId']
    assert n not in seen_norm, f"Duplicate statement found: {eid} matches {seen_norm[n]}"
    seen_norm[n] = eid

print("All 108 new questions have 100% unique normalized statements!")

# Read current pyp-bank.jsonl
for pyp_path in ['seed-data/pyp-bank.jsonl', 'backend/seed-data/pyp-bank.jsonl']:
    existing_lines = [line.strip() for line in open(pyp_path, encoding='utf-8') if line.strip() and not line.startswith('#')]
    # Check no ID collisions
    existing_ids = set(json.loads(line)['externalId'] for line in existing_lines)
    for q in all_new:
        assert q['externalId'] not in existing_ids, f"Collision on ID {q['externalId']}"
    
    # Append
    with open(pyp_path, 'w', encoding='utf-8') as f:
        for line in existing_lines:
            f.write(line + '\n')
        for q in all_new:
            f.write(json.dumps(q, ensure_ascii=False) + '\n')
    print(f"Updated {pyp_path}: was {len(existing_lines)} -> now {len(existing_lines) + len(all_new)} rows")

# Now check complete pilot bank for process-synchronization across all 3 files
all_psync = []
for fpath in ['backend/seed-data/questions.jsonl', 'seed-data/pyp-bank.jsonl', 'seed-data/so-gate-bank.jsonl']:
    for line in open(fpath, encoding='utf-8'):
        if line.strip() and not line.startswith('#'):
            row = json.loads(line)
            if row.get('topicSlug') == 'process-synchronization':
                all_psync.append(row)

print(f"\n==========================================")
print(f"Total Process Synchronization Bank Post-Remediation: {len(all_psync)} rows")
print(f"==========================================")

# Check uniqueness across all psync questions
overall_seen = {}
duplicates = []
for r in all_psync:
    n = normalize(r['statement'])
    eid = r['externalId']
    if n in overall_seen:
        duplicates.append((eid, overall_seen[n]))
    else:
        overall_seen[n] = eid

print(f"Unique normalized statements in psync: {len(overall_seen)}")
print(f"Duplicates: {len(duplicates)}")
if duplicates:
    print("Duplicates detail:", duplicates)
assert len(duplicates) == 0, "Expected 0 duplicate normalized statements!"

# Print breakdown
type_counts = defaultdict(int)
diff_counts = defaultdict(int)
source_counts = defaultdict(int)
for r in all_psync:
    type_counts[r['type']] += 1
    diff_counts[r.get('difficulty', 'UNSET')] += 1
    source_counts[r.get('source', 'UNSET')] += 1

print("\nType breakdown:", dict(type_counts))
print("Difficulty breakdown:", dict(diff_counts))
print("Source breakdown:", dict(source_counts))
