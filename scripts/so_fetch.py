#!/usr/bin/env python3
"""
scripts/so_fetch.py
Fetches high-vote questions from Stack Exchange API 2.2 mapped to GATE CSE subjects.
Caches raw JSON responses in seed-data/so-raw/<subject>.json (gitignored).
"""

import os
import sys
import json
import time
import argparse
import urllib.request
import urllib.parse
import urllib.error

SUBJECT_TAG_MAP = {
    "algorithms": ["algorithm", "complexity-theory"],
    "programming-and-ds": ["data-structures", "sorting"],
    "databases": ["database", "sql", "dbms"],
    "operating-systems": ["operating-system", "process", "concurrency", "deadlock"],
    "computer-networks": ["networking", "tcp", "computer-networks"],
    "theory-of-computation": ["automata", "turing-machine"],
    "digital-and-compilers": ["compiler-construction", "parsing", "boolean-algebra"],
    "computer-organization": ["assembly", "cpu-architecture", "memory-management"],
    "discrete-mathematics": ["discrete-mathematics", "graph-theory", "combinatorics"],
    "general-aptitude": []  # Skipped, authored directly with APT- prefix
}

API_BASE = "https://api.stackexchange.com/2.2/questions"

def fetch_subject_questions(subject: str, tags: list[str], max_pages: int = 2, api_key: str | None = None, dry_run: bool = False):
    if not tags:
        print(f"[{subject}] No tags mapped (authored directly); skipping fetch.")
        return []

    cache_dir = os.path.join("seed-data", "so-raw")
    os.makedirs(cache_dir, exist_ok=True)
    cache_path = os.path.join(cache_dir, f"{subject}.json")

    all_items = []
    print(f"[{subject}] Fetching questions for tags: {tags}...")

    for tag in tags:
        if dry_run:
            print(f"  [DRY-RUN] Would fetch tag '{tag}' from StackExchange API.")
            continue

        for page in range(1, max_pages + 1):
            params = {
                "order": "desc",
                "sort": "votes",
                "tagged": tag,
                "site": "stackoverflow",
                "pagesize": 50,
                "page": page,
                "filter": "!9_bDDxJY5"  # Includes body and tags
            }
            if api_key:
                params["key"] = api_key

            url = f"{API_BASE}?{urllib.parse.urlencode(params)}"
            req = urllib.request.Request(url, headers={"User-Agent": "GateLMS-SeedPipeline/1.0"})

            try:
                with urllib.request.urlopen(req, timeout=15) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    items = data.get("items", [])
                    all_items.extend(items)
                    quota_rem = data.get("quota_remaining", "unknown")
                    print(f"  Fetched tag '{tag}' page {page}: {len(items)} items. Quota remaining: {quota_rem}")

                    backoff = data.get("backoff", 0)
                    if backoff > 0:
                        print(f"  StackExchange backoff requested: sleeping {backoff}s...")
                        time.sleep(backoff)
                    else:
                        time.sleep(0.5)

                    if not data.get("has_more", False):
                        break
            except urllib.error.HTTPError as e:
                print(f"  HTTP error {e.code} fetching tag '{tag}': {e.reason}")
                break
            except Exception as e:
                print(f"  Error fetching tag '{tag}': {e}")
                break

    if not dry_run and all_items:
        with open(cache_path, "w", encoding="utf-8") as f:
            json.dump(all_items, f, indent=2)
        print(f"[{subject}] Saved {len(all_items)} raw items to {cache_path}")

    return all_items

def main():
    parser = argparse.ArgumentParser(description="Fetch Stack Overflow questions for GateLMS seed pipeline.")
    parser.add_argument("--subject", choices=list(SUBJECT_TAG_MAP.keys()) + ["all"], default="all", help="Specific subject to fetch or 'all'")
    parser.add_argument("--max-pages", type=int, default=2, help="Max API pages to fetch per tag (default: 2)")
    parser.add_argument("--dry-run", action="store_true", help="Log planned requests without querying API")
    args = parser.parse_args()

    api_key = os.environ.get("SO_API_KEY")
    subjects_to_fetch = list(SUBJECT_TAG_MAP.keys()) if args.subject == "all" else [args.subject]

    print("=== GateLMS Stack Overflow Question Fetcher ===")
    print(f"Target subjects: {len(subjects_to_fetch)}")
    print(f"API key configured: {'Yes' if api_key else 'No (using anonymous quota)'}")

    for subj in subjects_to_fetch:
        tags = SUBJECT_TAG_MAP.get(subj, [])
        fetch_subject_questions(subj, tags, max_pages=args.max_pages, api_key=api_key, dry_run=args.dry_run)

    print("Fetch execution completed.")

if __name__ == "__main__":
    main()
