# [PHASE5-RELEASE-SPEC] — Release Readiness: Security Headers, Rate Limiting, Backups, Export, Docs, v1.0.0

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE5-RELEASE` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase5/performance` (SPEC 2) — final SPEC of the roadmap program |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | G5/G8 posture; PHASE1-FIXUP-AUDIT A2 (loopback posture — final sweep); PHASE5-AUTH security surface; Operator as release owner |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase5/release-readiness` · **No migration** · Effort 6–9 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

The final work order: everything between "works" and "shippable" — nginx security headers + login rate limiting, a **backup/restore drill with receipts**, a **data export escape hatch**, the user guide + final runbook + changelog, and the release checklist. The **v1.0.0 tag itself is Operator-executed** (§6) — Gemini prepares everything and proves it with the drill; the Operator pulls the trigger.

## 2. Security Hardening (binding)

### 2.1 Nginx headers (binding — every response)

- `Content-Security-Policy`: `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; object-src 'self'` (KaTeX injects inline styles — `'unsafe-inline'` on style-src is the documented, minimal concession; no `unsafe-eval` anywhere; `object-src 'self'` required by the notes PDF `<embed>`). Receipt: a curl table — header present on `/`, `/api/v1/subjects`, and a notes file response.
- `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (the app is never framed), `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=()`.
- **HSTS is deliberately NOT set** — the default deployment is loopback HTTP; setting HSTS on HTTP breaks local re-access. A commented, documented template block ships ready for the TLS deployment (documented in runbook §TLS).
- TLS itself is out of scope (Operator's reverse proxy/domain decision — runbook documents a Caddy/nginx-SSL fronting recipe with the PHASE5-AUTH `Secure=true` cookie toggle).

### 2.2 Rate limiting (binding)

- nginx `limit_req_zone` on `/api/v1/auth/login` — `rate=10r/m` with `burst=5 nodelay` → 429 (complements the backend's per-username counter: nginx is per-IP, the backend is per-account — belt and braces, both tested).
- A generic zone caps `/api/` at `50r/s` burst 100 per IP (protects the Testcontainers-measured p95 from accidental floods).

### 2.3 Final loopback posture sweep (closes A2 formally)

- `docker-compose.yaml`: **every** published port — backend, frontend, postgres, prometheus, grafana, tempo, loki — bound `127.0.0.1:`. Receipt: `docker compose config | grep -A3 ports:` quoted; a table port × binding × justification. External exposure = documented operator action (SSH tunnel / reverse proxy recipe in the runbook).

## 3. Backup & Restore (binding, drill-proven)

- **`scripts/backup.sh`**: `docker compose exec -T postgres pg_dump -U gatelms -Fc gatelms > backups/gatelms-$(date +%Y%m%d-%H%M%S).dump` + `tar czf` the `docker-data/notes` volume dir into the same timestamped set; writes `backups/manifest-<ts>.txt` (sizes, db row counts via `pg_restore --list`); `backups/` gitignored; `backup.sh` prints the verify command for the operator. Retention note documented (no auto-pruning v1).
- **`scripts/restore.sh`**: takes a dump file, **requires interactive confirmation** (typed `RESTORE`), stops the app stack, recreates the DB volume, `pg_restore`, restarts, runs a health + row-count smoke, prints a verification table.
- **The drill (binding receipt)**: on the executor machine — seed ≥100 questions + a few attempts, run backup, **destroy the volumes** (`docker compose down -v`), run restore, verify counts identical + the app boots + one question round-trips. Receipt quoted verbatim (commands + outputs). A backup script that has never been restored is a finding, not a feature.

## 4. Data Export (binding — the escape hatch)

- `GET /api/v1/export` (ADMIN; stream, `Content-Disposition: attachment; filename="gatelms-export-<date>.json"`): a single JSON document — subjects/topics, full question bank (statements, options, answers, explanations), all user-specific state for the requesting user (attempts with answers, mistakes, notes metadata + content_md, plans, rollups, achievements), and curation decisions. Size-bounded: paginated `?part=` if > 100 MB (honest chunking, documented).
- **Not** for the notes' binary files (the backup covers those); export carries `content_md` and file metadata with a note explaining where binaries live. Documented in the user guide.

## 5. Documentation & Release Artifacts (binding)

- **`docs/USER-GUIDE.md`**: the student-facing manual — install (compose up), login/bootstrap, the full loop (practice → mistakes → revision → mocks → PYP → planner → analytics), design/layout switching, PWA install, data export, FAQ. Written for a **new user**, not the Operator's shorthand.
- **`docker/observability/RUNBOOK.md` final pass**: append §TLS recipe, §tunnel exposure, §backup/restore, §export, §rate-limits, §session-survival (JDBC sessions), §query-timeout behavior. Every prior runbook section verified current.
- **`CHANGELOG.md`**: generated from `git log --merges --format=...` grouped by phase — every merge SHA + one-line summary; rev-1 audited programs noted.
- **`README.md`** refresh: quickstart (5 commands), feature matrix linking the user guide, the collab/ multi-agent governance one-paragraph explainer, license note for SO-derived/PYP content (attribution links, non-rehosting policy from PHASE2-SEED-BULK/PHASE3-PYP).
- **`RELEASE-CHECKLIST.md`**: the §6 drill as a literal operator checklist + the tag command block.

## 6. Release Drill & Tag (Operator-executed; Gemini stages everything)

1. `git pull origin master` → merge of `phase5/release-readiness` audited PASS.
2. Fresh-volume boot: `docker compose down -v && docker compose up -d --build` → seed import receipt → login bootstrap → **full student loop** (practice → wrong answer → mistake log → revision drill → mock submit → score review → PYP attempt → plan today card → insights readiness) — all green.
3. Backup → down -v → restore drill → identical counts.
4. Observability profile boot → dashboards live → `BackendDown` alert fires on `docker compose stop backend`.
5. Security receipt spot-checks: CSP header present; anonymous Grafana fails; 11 rapid logins → 429; `curl` from another machine fails (loopback).
6. Tag: `git tag -a v1.0.0 -m "GateLMS v1.0.0 — full study platform"` && `git push origin v1.0.0`. **The tag is the Operator's act** (Gemini never tags).

## 7. Verification & Test Gates

- **Backend delta** ≥ +1 file / +3 cases (target ≥ 47 / ≥ 159): `ExportApiIT` ≥ 3 (export shape: bank present + user state present for requester only; part-paging boundary; content-disposition header).
- **Frontend delta** ≥ +1 file / +3 cases (target ≥ 37 / ≥ 190): release-mode smoke (build with release config: login → dashboard renders → no console errors — a Playwright-free vitest DOM smoke against the built bundle or a structural config test, executor's choice, documented).
- Full `./mvnw test` + `npm run build` + vitest green; SPEC 0 grep gates green; **zero-migration diff gate** (`backend/src/main/resources/db/` unchanged).
- **Receipts**: §2 header table, §2.2 rate-limit proof (11 requests → 429s observed), §3 drill transcript, §4 export sample (first 50 lines + size), §5 doc diff summary.

## 8. Report & Acceptance

Report `collab/gemini/PHASE5-RELEASE-REPORT.md`: WP register; G3 per-file receipts; the drill transcript (§3) as the centerpiece; header/rate-limit receipt tables; docs inventory; honest-judge register (DEV-N). Operator acceptance = §6 in full, ending in the tag. **On the tag, the roadmap program (Phases 0–5) closes**; Z.ai publishes a final program-close audit summarizing the whole DAG (every branch retained per G9, every merge audited, cumulative test trajectory 9/27+8/36 → ≥47/159+≥37/190 — the program's real, measured growth).

**Out of scope**: CI/CD pipelines, multi-environment configs, CDN/TLS termination (recipe only), auto-scheduled backups (cron recipe documented, not scheduled), telemetry beyond the existing metrics.

**Work packages**: WP1 nginx headers + rate limits + loopback sweep (1.5 h) · WP2 backup/restore scripts + drill (2 h) · WP3 export endpoint (1 h) · WP4 docs: user guide + runbook + changelog + README + checklist (2–2.5 h) · WP5 tests + report (1 h).
