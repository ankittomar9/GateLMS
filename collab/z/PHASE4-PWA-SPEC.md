# [PHASE4-PWA-SPEC] — Installable PWA: Manifest, Service Worker, Offline Shell & Read-Only Review

| Metadata | Value |
|---|---|
| **Task ID** | `PHASE4-PWA` |
| **Author Agent** | Z.ai (Architect & Quality Auditor) |
| **Base Commit** | merge of `phase4/study-planner` (SPEC 1); low-coupling — may follow SPEC 2 |
| **Revision** | Rev 1 (2026-10-07) |
| **Governing Inputs** | Operator goal: study anywhere (mobile/tablet); current SPA is browser-only; nginx static serving already in place; **no new runtime dependency policy** (DEV-02 precedent — deviations must be declared) |
| **Executor** | Gemini / Antigravity |
| **Branch** | `phase4/pwa-mobile` · **No migration** · Effort 4–6 h |
| **Status** | APPROVED_FOR_EXECUTION |

---

## 1. Goal

Make GateLMS **installable** (add-to-home-screen, standalone window, app icon, theme color following the active design preset) and **usefully offline**: the app shell and previously-visited question pages load without a network, with an explicit offline banner — honest read-only capability, no fake offline answering. Hand-rolled service worker (no `vite-plugin-pwa` — avoids a new dependency; a deviation to that effect is legitimate only if the executor demonstrates the plugin is required, recorded as DEV-N).

## 2. Architecture (binding)

### 2.1 Manifest — `frontend/public/manifest.webmanifest`

- `name: "GateLMS"`, `short_name: "GateLMS"`, `start_url: "/"`, `display: "standalone"`, `orientation: "any"`, `background_color: "#0F1115"` (matches tuf default), `theme_color` follows the **active design preset** (injected at runtime by the existing design boot script — a small `<meta name="theme-color">` updater; manifest static value = tuf default, runtime meta = live token).
- Icons: `public/icons/icon-192.png`, `icon-512.png`, `maskable-512.png` — executor-authored simple token-styled "G" mark (flat, works on dark/light); PNGs committed; `purpose: "any maskable"` on the maskable variant.
- `<link rel="manifest">`, `apple-touch-icon`, and `theme-color` meta wired in `index.html`.

### 2.2 Service Worker — `frontend/public/sw.js` (hand-rolled, ES5-safe plain JS)

- **Versioning**: `const CACHE_VERSION = 'gatelms-v<build>'` — bumped via a build-time define (Vite `define` or a generated `sw-version.js`); old caches deleted on activate.
- **Install**: pre-cache the app shell ( `/`, `index.html`, manifest, icons). Vite's hashed `/assets/*` are **runtime-cached**, not precached (no build-manifest coupling).
- **Fetch strategy (binding)**:
  - `GET /assets/*` (hashed static) → **cache-first** (immutable by hash).
  - `GET /api/v1/questions*`, `/subjects*`, `/topics*`, `/stats/*`, `/mistakes*` → **network-first with cache fallback**; fallback responses stamped `X-From-Cache: 1`; non-GET `/api/*` → **network-only** (never cache mutations; a failed mutation surfaces the existing error banner).
  - Navigations (route `GET` document requests) → network-first, fallback to cached `index.html` (SPA shell), then offline page.
  - Everything else (fonts, KaTeX assets — already local-bundled) → stale-while-revalidate.
- **Offline page**: `public/offline.html` — token-styled static shell with "You're offline — cached content only" and a retry button (no build tooling needed).
- **Update flow**: `self.skipWaiting()` on `message {type: 'SKIP_WAITING'}`; the app shows a soft "Update available — reload" toast when `controllerchange` fires (toast only; never hard-reloads under the user).
- **Registration**: `main.tsx` — `if ('serviceWorker' in navigator && import.meta.env.PROD) navigator.serviceWorker.register('/sw.js')` — dev environment never registers (Vite dev server + SW = cache poisoning; documented in the report).

### 2.3 Frontend surface

- **OfflineBanner** component: `navigator.onLine` + `online`/`offline` events + SW fallback detection (`X-From-Cache: 1` on API responses via TanStack Query meta) → top banner "Offline — showing cached content" (token `--ds-*` alert styling, `role="status"`); auto-dismiss on reconnect.
- **InstallPrompt**: `beforeinstallprompt` captured → "Install GateLMS" button in the header profile menu (tuf/all designs); iOS fallback hint (Safari Share → Add to Home Screen) when the event never fires; after `appinstalled` the button disappears.
- **Update toast**: SW `controllerchange` → "Update available" toast with Reload action (above).
- Dashboard: no layout changes (chrome untouched) — PWA work is additive only.

## 3. Verification & Test Gates

- **Backend delta**: none (zero backend diff gate: `git diff origin/master...phase4/pwa-mobile -- backend/` empty).
- **Frontend delta** ≥ +2 files / +10 cases (target ≥ 33 / ≥ 171): `OfflineBanner.test` ≥ 4 (banner on `offline` event; dismiss on `online`; cached-response detection via meta; a11y role), `InstallPrompt.test` ≥ 3 (button shows when beforeinstallprompt fires; hidden after appinstalled; iOS hint path), `swRegistration.test` ≥ 3 (registers in PROD mock; never in DEV; update toast on controllerchange).
- **SW logic tests**: the fetch-strategy matrix is extracted into a testable pure module (`sw-router.js`, imported by `sw.js` via a classic-script-safe pattern or duplicated thin dispatcher — executor's choice, documented) tested with mocked `Request`/`Response`/`caches`: cache-first for `/assets/`, network-first-with-fallback for the five GET families, network-only for POST/PATCH, navigation fallback to cached shell. ≥ 4 cases (may live in the 10 above).
- `npm run build` + vitest green; SPEC 0 grep gates green; **no new dependency** in `package.json` (diff gate — unless a DEV-N justifies it).
- **Build receipt** (report-verbatim): `npm run build` output sizes; `sw.js` served size < 5 KB gzipped target (hand-rolled discipline check); manifest passes `node --check`-style JSON validation.

## 4. Report & Acceptance

Report `collab/gemini/PHASE4-PWA-REPORT.md`: WP register; G3 per-file receipts; fetch-strategy matrix table (request class → strategy → test name); update-flow proof; dev-registration guard proof; honest-judge register (DEV-N — including any deviation on the no-plugin rule). Operator acceptance (host with the built stack, e.g. `docker compose up`): install from Chrome (beforeinstallprompt path), launch standalone, browse a question page, kill the network (offline in DevTools or disconnect), reload — the shell + previously-visited question data render with the offline banner; a practice POST while offline shows the error banner, no phantom success; flip connectivity back and the banner clears; trigger an SW update and see the soft reload toast.

**Out of scope**: offline answering/sync-queue (answers require the server's grading — deferred deliberately, honest offline = read-only), web-push notifications (needs VAPID + Phase 5 auth), background sync, per-platform store packaging.

**Work packages**: WP1 manifest + icons + index.html wiring (1 h) · WP2 service worker + versioning + offline page (1.5 h) · WP3 registration + banner + install/update UX (1.5 h) · WP4 tests + report (1 h).
