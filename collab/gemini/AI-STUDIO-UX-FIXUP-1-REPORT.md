# [AI-STUDIO-UX-FIXUP-1] Maintenance Fixup & Audit Discharge Report

- **Work Package:** AI Studio UX Fixup 1 (Conditions C1–C4 Discharged)
- **Executor:** Gemini Antigravity
- **Base Commit:** `5449736` (`origin/master` post-audit)
- **Branch:** `ai/studio-ux-fixup-1`
- **Audit Reference:** `collab/z/AI-STUDIO-UX-AUDIT.md` (Discharging Conditions C1, C2, C3, C4)
- **Date:** 2026-10-30

---

## 1. Executive Summary & Delivered Fixes

### Condition C1 (Finding F-1) — Rules-of-Hooks Order & `node` Prop Leak in `RichMarkdownText.tsx`
- **Hook Order:** Moved `const processedContent = React.useMemo(...)` unconditionally above `if (!content) return null;`. When content transitions between truthy and falsy states on a mounted instance, the hook count remains consistent, strictly complying with the Rules of Hooks.
- **Prop Leak:** Destructured `node` in `a: ({ node, href, children, ...props })` so the hast Element AST object is stripped before forwarding remaining HTML props to `<a />`, mirroring the existing clean pattern in `code`.

### Condition C2 (Finding F-2) — Context-Wired FAB In-Flight Pulse Indicator
- **State Lifted to Context:** Added `isLoading: boolean` and `setIsLoading: (loading: boolean) => void` to `AiStudioContextValue` and `AiStudioProvider` in `frontend/src/context/AiStudioContext.tsx`. Fallback defaults provided in `useAiStudio()`.
- **Drawer Consumption:** In `frontend/src/components/ai/AiStudioDrawer.tsx`, removed drawer-local `useState(false)` and consumed `isLoading, setIsLoading` directly from `useAiStudio()`. In-flight generation automatically reflects in global studio context.
- **FAB Consumption:** In `frontend/src/components/ai/AiStudioFab.tsx`, consumed `const { isOpen, setIsOpen, isLoading: contextLoading } = useAiStudio();` and computed `const isLoading = propIsLoading ?? contextLoading;`. The production `<AiStudioFab />` mount in `Layout.tsx` (rendered without props) now pulses in real-time when generation is in progress in the background while the drawer is closed.
- **Test Updated:** In `frontend/src/components/ai/__tests__/AiStudioWindowing.test.tsx`, updated Test 6 to drive the pulse via context `setIsLoading(true)` and verify that `<AiStudioFab />` with zero props renders `data-testid="ai-studio-fab-pulse"`.

### Condition C3 (Finding F-3) — Lazy Code-Splitting of `RichMarkdownText` & Honest Build Receipt
- **Dynamic Import:** In `frontend/src/components/ai/AiStudioDrawer.tsx`, replaced static import with:
  ```tsx
  const RichMarkdownText = React.lazy(() => import('./RichMarkdownText'));
  ```
- **Suspense Boundary:** Wrapped assistant markdown message rendering in `<React.Suspense fallback={<p className="whitespace-pre-wrap">{msg.content}</p>}>`.
- **Bundle Split:** Production build (`npm run build`) generates separate chunk `dist/assets/RichMarkdownText-xpflyPcI.js` (184.22 kB │ gzip: 56.58 kB) and reduces main chunk `dist/assets/index-CAIuIPmS.js` to 1,627.99 kB (down from 1,812.72 kB), achieving the expected lazy-loading threshold.

### Condition C4 (Finding F-4) — Restored `key.trim()` in `setByokKey`
- **Freeze Breach Reversal:** In `frontend/src/context/AiStudioContext.tsx`, restored `key.trim()`:
  ```tsx
  const next = { ...prev, [provider]: key.trim() };
  ```
  Prevents accidental trailing whitespace/newlines from pasted keys propagating into `Authorization` headers.

---

## 2. Verification Matrix & Test Receipts

| Suite | Scope | Target | Result | Status |
|---|---|---|---|---|
| Backend Unit Suite (`*Test`) | Full Backend Unit Regression | 167 tests | **167 run, 0 failures, 0 errors, 1 skipped** (elapsed 2:25 min) | **PASS** |
| Frontend Vitest | Full Frontend Regression | 55 files / 299 tests | **55 files passed, 299 tests passed** (0 failures) | **PASS** |
| Frontend Build | Production Code-Split | 2 chunks | **RichMarkdownText-*.js 184.22 kB, index-*.js 1,627.99 kB** | **PASS** |
| Whitespace / CRLF | Git diff hygiene check | All touched files | **0 errors (`git diff --check`)** | **PASS** |

### Detailed Receipts

#### 1. Frontend Vitest Receipt (`npm.cmd test -- --run`)
```
 Test Files  55 passed (55)
      Tests  299 passed (299)
   Start at  22:19:04
   Duration  80.91s
```
*(Arithmetic: 289 baseline + 4 RichMarkdownText + 6 AiStudioWindowing = 299 passed).*

#### 2. Frontend Production Build Receipt (`npm.cmd run build`)
```
vite v6.4.4 building for production...
transforming...
✓ 2935 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                                               1.61 kB │ gzip:   0.81 kB
...
dist/assets/RichMarkdownText-xpflyPcI.js                    184.22 kB │ gzip:  56.58 kB
dist/assets/index-CAIuIPmS.js                             1,627.99 kB │ gzip: 422.62 kB
✓ built in 22.57s
```

#### 3. Backend Unit Regression Receipt (`./mvnw.cmd test -Dtest="*Test"`)
```
[INFO] Results:
[INFO] 
[WARNING] Tests run: 167, Failures: 0, Errors: 0, Skipped: 1
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  02:25 min
[INFO] Finished at: 2026-10-30T22:24:05+05:30
```

---

## 3. Git & Branch Hygiene Compliance

All actions executed per `AGENTS.md` G9 Branch-per-Work-Package Protocol:
1. Created dedicated branch `ai/studio-ux-fixup-1` off `origin/master` (`5449736`).
2. Atomic commit on feature branch.
3. Pushed branch to `origin/ai/studio-ux-fixup-1`.
4. Merged into `master` via `--no-ff` merge commit.
5. Pushed `master` to remote `origin`.
