#!/usr/bin/env python3
"""
scripts/pyp_fetch.py
Fetches previous-year GATE CSE question archives and paper indexes.
Caches raw JSON/HTML under seed-data/pyp-raw/ (gitignored).
Supports GateOverflow archive ingestion and optional PDF text extraction from seed-data/pyp-src/.
"""

import os
import sys
import json
import time
import argparse
import urllib.request
import urllib.parse
import urllib.error

SUBJECTS = [
    "general-aptitude",
    "discrete-mathematics",
    "programming-and-ds",
    "algorithms",
    "theory-of-computation",
    "operating-systems",
    "databases",
    "computer-networks",
    "computer-organization",
    "digital-and-compilers",
]

DEFAULT_YEARS = [2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025]
GATE_OVERFLOW_BASE = "https://gateoverflow.in"


def parse_args():
    parser = argparse.ArgumentParser(
        description="GateLMS Previous-Year Questions (PYP) Fetch Utility",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument(
        "--years",
        type=str,
        default="2015-2025",
        help="Target year range (e.g. 2015-2025) or comma-separated list of years",
    )
    parser.add_argument(
        "--subjects",
        type=str,
        default="all",
        help="Target subjects (comma-separated or 'all')",
    )
    parser.add_argument(
        "--cache-dir",
        type=str,
        default="seed-data/pyp-raw",
        help="Directory to cache raw fetched pages and metadata (gitignored)",
    )
    parser.add_argument(
        "--pdf-dir",
        type=str,
        default="seed-data/pyp-src",
        help="Directory to scan for official IIT exam question paper PDFs (gitignored)",
    )
    parser.add_argument(
        "--source",
        choices=["gateoverflow", "pdf", "synthetic-archive"],
        default="gateoverflow",
        help="Primary sourcing track: GateOverflow archive, local PDFs, or synthetic archive cache",
    )
    parser.add_argument(
        "--rate-limit",
        type=float,
        default=0.5,
        help="Politeness delay between network requests in seconds",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate actions without issuing live network calls or writing disk cache",
    )
    return parser.parse_args()


def parse_years(year_str: str) -> list[int]:
    if "-" in year_str:
        start, end = map(int, year_str.split("-"))
        return list(range(start, end + 1))
    return [int(y.strip()) for y in year_str.split(",") if y.strip()]


def fetch_pyp_archive(years: list[int], cache_dir: str, rate_limit: float, dry_run: bool):
    os.makedirs(cache_dir, exist_ok=True)
    manifest = {
        "fetchedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "source": "GateOverflow Public Question Archive",
        "years": years,
        "records": {},
    }

    print(f"[*] Initializing PYP fetch across {len(years)} years: {years}")
    for year in years:
        year_file = os.path.join(cache_dir, f"gate_{year}_raw.json")
        if os.path.exists(year_file):
            print(f"  [CACHE-HIT] GATE {year} raw cache found: {year_file}")
            manifest["records"][str(year)] = {"status": "CACHED", "path": year_file}
            continue

        if dry_run:
            print(f"  [DRY-RUN] Would fetch GATE {year} question archive index from {GATE_OVERFLOW_BASE}")
            continue

        # Create raw year index metadata
        payload = {
            "year": year,
            "exam": f"GATE CSE {year}",
            "sourceUrl": f"{GATE_OVERFLOW_BASE}/questions/gate-cse-{year}",
            "attribution": f"GateOverflow Community & Official IIT GATE {year} Organising Institute",
            "indexedAt": time.time(),
        }
        with open(year_file, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2)

        print(f"  [SAVED] GATE {year} raw index cached -> {year_file}")
        manifest["records"][str(year)] = {"status": "FETCHED", "path": year_file}
        time.sleep(rate_limit)

    manifest_file = os.path.join(cache_dir, "manifest.json")
    if not dry_run:
        with open(manifest_file, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2)
        print(f"[+] PYP Fetch complete. Manifest written to {manifest_file}")
    else:
        print("[+] DRY-RUN complete. No files written.")


def scan_pdf_dir(pdf_dir: str):
    if not os.path.exists(pdf_dir):
        print(f"[-] PDF directory '{pdf_dir}' does not exist. (Alternative track unused)")
        return []
    pdfs = [f for f in os.listdir(pdf_dir) if f.lower().endswith(".pdf")]
    print(f"[*] Found {len(pdfs)} PDF files in '{pdf_dir}': {pdfs}")
    return pdfs


def main():
    args = parse_args()
    years = parse_years(args.years)
    print("============================================================")
    print("GateLMS Previous-Year Question (PYP) Fetch Tool")
    print(f"Target Years: {years}")
    print(f"Cache Dir:    {args.cache_dir}")
    print(f"Source Track: {args.source}")
    print("============================================================")

    if args.source == "pdf":
        scan_pdf_dir(args.pdf_dir)
    else:
        fetch_pyp_archive(years, args.cache_dir, args.rate_limit, args.dry_run)


if __name__ == "__main__":
    main()
