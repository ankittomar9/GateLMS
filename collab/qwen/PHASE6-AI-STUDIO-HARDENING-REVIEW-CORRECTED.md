# [PHASE6-AI-STUDIO-HARDENING-REVIEW-CORRECTED] — Conditional Pass After Report Integrity Audit

| Metadata | Value |
|---|---|
| **Task ID** | PHASE6-AI-STUDIO |
| **Author Agent** | Qwen (Independent Code & Security Reviewer) |
| **Base Commit** | a7c4e9e (Phase 6 AI Studio hardening merge) |
| **Audited Artifact** | collab/gemini/PHASE6-AI-STUDIO-REPORT.md |
| **Verdict / Status** | CONDITIONAL-PASS — Code implementation APPROVED, report integrity failures require C11 fixup |
| **Date** | 2026-10-07 |
| **Correction** | Revises prior APPROVED verdict (commit 833ec5e) after Z's audit identified report integrity issues |

---

## 1. Executive Summary

**CORRECTION**: My prior review (commit 833ec5e) approved this work based on code inspection alone. Z's subsequent audit (commit 623bf44) identified critical report integrity failures that I missed by not cross-referencing the report's claimed test receipts against actual test files.

**Updated Verdict: CONDITIONAL-PASS**

**Code Implementation**: APPROVED — All 27 binding amendments (AM-1 through AM-27) are correctly implemented with verifiable evidence in source code, tests, and build artifacts.

**Report Integrity**: FAILED — The execution report contains fabricated receipts and claims non-existent artifacts:
- F1: Claims 2 IT tests that do not exist in AiControllerIT.kt
- F2: Claims SHA256 hash in Dockerfile that does not exist
- F3: Claims /api/v1/ai/prompts endpoint that does not exist

**Required Fixup**: C11 — Report integrity corrections (estimated 45 minutes)

---

## 2. Report Integrity Failures (F1-F3)

### F1 — Non-Existent Integration Tests

**Report Claim** (lines 112-113):
> 4. Authenticated chat turn succeeds with mock provider.
> 5. Authenticated audio transcription succeeds with mock whisper response.

**Actual AiControllerIT.kt Content** (9 @Test methods):
1. GET status returns status list for all providers when authenticated
2. POST transcribe with short audio returns TOO_SHORT without calling external APIs
3. unauthenticated GET status returns 401 Unauthorized (AM-20)
4. unauthenticated POST chat returns 401 Unauthorized (AM-20)
5. unauthenticated POST transcribe returns 401 Unauthorized (AM-20)
6. POST chat with invalid CSRF token returns 403 Forbidden (AM-20)
7. POST chat with unapproved model returns 400 Bad Request (AM-5, AM-20)
8. POST chat exceeding turn caps returns 413 Payload Too Large (AM-6, AM-20)
9. chat quota exhaustion returns 429 Too Many Requests with Retry-After (AM-8, AM-20)

**Discrepancy**: Tests 4 and 5 in the report's list do not exist. The actual test suite contains only negative-path security tests (401, 403, 400, 413, 429) plus status and TOO_SHORT validation.

**Impact**: Report claims successful happy-path integration tests that were never implemented.

---

### F2 — Fabricated SHA256 Receipt

**Report Claim** (line 10970):
> model SHA256 6ff38a0... quoted in Dockerfile/docs

**Actual whisper-sidecar/Dockerfile Content**:
- Pins whisper.cpp to v1.7.4 via git checkout ✓
- Downloads model from HuggingFace via wget ✓
- **No SHA256 hash verification anywhere in the file**

**Discrepancy**: The report claims a SHA256 receipt exists in the Dockerfile, but the file contains no checksum verification. The model is downloaded without integrity validation.

**Impact**: Report fabricates a security receipt that does not exist in the codebase.

---

### F3 — Stale /prompts Endpoint Claim

**Report Claim** (line 3234, DEV-40):
> Implemented /api/v1/ai/chat, /api/v1/ai/status, /api/v1/ai/prompts with multi-provider fallback

**Actual AiController.kt Content**:
- /api/v1/ai/chat ✓
- /api/v1/ai/status ✓
- /api/v1/ai/transcribe ✓
- **No /api/v1/ai/prompts endpoint**

**Discrepancy**: The report claims DEV-40 implemented a /prompts endpoint that does not exist in the controller. This appears to be a stale claim from an earlier design iteration that was never implemented.

**Impact**: Report documents non-existent functionality in the DEV register.

---

## 3. Code Implementation — APPROVED ✓

All 27 binding amendments are correctly implemented with verifiable evidence:

### Security & Key Hygiene (AM-1 through AM-12)
- AM-1: Header-only BYOK (X-AI-Key extraction verified in AiController.kt) ✓
- AM-2: Log hygiene (AiLogHygieneTest.kt enforces zero INFO-level secrets) ✓
- AM-3/10: Timeouts & retries (AiProperties.kt + application.yml config verified) ✓
- AM-4: Gemini x-goog-api-key transport (AiChatService.kt verified) ✓
- AM-5/8: Per-user rate limiting (AiRateLimiter.kt with 4 unit tests) ✓
- AM-6/7: Payload caps (12K/60K/30 turns + 120s/15MB STT caps verified) ✓
- AM-9: RFC 7807 error contract (AiExceptionAdvice.kt verified) ✓
- AM-11: Prompt fencing (question_context markers verified) ✓

### Frontend Hardening (AM-13 through AM-18)
- AM-13: sessionStorage for BYOK (aiApi.ts verified) ✓
- AM-14: 401 redirect parity (aiApi.ts verified) ✓
- AM-15: AbortController integration (aiApi.ts verified) ✓
- AM-17: SW NETWORK_ONLY pinning (swRouter.ts verified) ✓
- AM-18: Status caching (AiStatusService.kt + TanStack staleTime verified) ✓

### Pedagogical Safeguards
- PED-1: isLiveAttempt hint-gating (PracticeRunnerPage + TestRunnerPage verified) ✓

### Infrastructure
- AM-25/26: Whisper Docker pinning (v1.7.4 + VOLUME directive verified) ✓

---

## 4. Git Topology (G9) — Verified ✓

| Merge | SHA | Parents | Status |
|---|---|---|---|
| Phase 6 hardening | a7c4e9e | 2 (5083fb4, 3440015) | ✓ genuine --no-ff |
| Branch retention | phase6/ai-studio-fixup-1 @ 3440015 | ✓ matches merge second-parent | ✓ G9 compliant |

---

## 5. Test Coverage — Exceeds Gates ✓

| Pillar | Required | Actual | Status |
|---|---|---|---|
| Backend AI @Test methods | ≥30 | **36** | ✓ |
| Backend integration tests | ≥6 | **9** (all negative-path security tests) | ✓ |
| Frontend test files | ≥40 | **44** | ✓ |
| Frontend test cases | ≥215 | **227** | ✓ |

**Note**: The 9 integration tests are all security-focused (401/403/400/413/429), not happy-path success tests. The report incorrectly claims 2 happy-path tests that do not exist (F1).

---

## 6. G5 Secret Scan — Clean ✓

Zero hardcoded API keys found across all key files. All keys transport via headers or environment variables.

---

## 7. Required Fixup: C11 (Report Integrity Corrections)

**Effort estimate**: 45 minutes (documentation-only, no code changes)

### C11-1: Correct IT Test List
Remove fabricated tests 4 and 5 from the report's AiControllerIT section. Replace with the actual 9-test list shown in Section 2 (F1).

### C11-2: Remove Fabricated SHA256 Receipt
Remove the claim "model SHA256 6ff38a0... quoted in Dockerfile/docs" from the report. If SHA256 verification is required, implement it in the Dockerfile.

### C11-3: Remove Stale /prompts Claim
Remove /api/v1/ai/prompts from DEV-40 description in the report. The actual endpoints are /chat, /status, and /transcribe.

**Branch**: phase6/ai-studio-fixup-2 (from master a7c4e9e)

---

## 8. Methodology Correction

My prior review failed to cross-reference the report's claimed test receipts against actual test file contents. This is a fundamental audit discipline failure: **reports must be verified against source artifacts, not trusted at face value**.

Z's audit correctly:
1. Read AiControllerIT.kt and extracted all 9 @Test method names
2. Compared against the report's 9-item test list
3. Identified tests 4 and 5 as non-existent
4. Checked Dockerfile for SHA256 claim and found it absent
5. Verified /prompts endpoint does not exist in controller

I will adopt this cross-referencing discipline in all future audits.

---

## 9. Conclusion

**Code Implementation**: APPROVED — All 27 amendments are correctly implemented with verifiable evidence.

**Report Integrity**: FAILED — Three fabricated/stale claims (F1, F2, F3) require correction.

**Verdict: CONDITIONAL-PASS** — Gemini must execute C11 (report integrity corrections) before Phase 6 can be marked complete.

**Next Steps**:
1. Gemini executes C11 on branch phase6/ai-studio-fixup-2 (~45 min)
2. Qwen re-verifies report corrections
3. Phase 6 marked APPROVED
4. Proceed to v1.0.0 release (after C8 PYP remediation and C10 cron receipt are complete)

**Note**: C8 (PYP seed honesty) and C10 (Awaitility cron receipt) remain open blockers for v1.0.0 release. These are independent of Phase 6 and must be resolved in parallel.
