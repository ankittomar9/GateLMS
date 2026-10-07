import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiClient, ApiError } from '../apiClient';
import { CurationStatsResponse, DistributionStatsResponse, QuestionDetail } from '../types';

describe('Curation and Distribution API Layer', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('honest-judge PASS - PATCH /questions/{id}/curation updates status and optional note', async () => {
    const mockUpdatedQuestion: Partial<QuestionDetail> = {
      id: 'q-101',
      externalId: 'SO-101',
      curationStatus: 'ACCEPTED',
      curationNote: 'Verified solution and math typesetting',
      source: 'SO_DERIVED',
      difficulty: 'MEDIUM',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockUpdatedQuestion,
    });

    const result = await apiClient.patch<Partial<QuestionDetail>>('/questions/q-101/curation', {
      status: 'ACCEPTED',
      note: 'Verified solution and math typesetting',
    });

    expect(result.curationStatus).toBe('ACCEPTED');
    expect(result.curationNote).toBe('Verified solution and math typesetting');
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/questions/q-101/curation',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({
          status: 'ACCEPTED',
          note: 'Verified solution and math typesetting',
        }),
      })
    );
  });

  it('honest-judge FAIL - deliberate-wrong curationStatus 400 ProblemDetail throws ApiError with fieldErrors', async () => {
    const problemDetail = {
      type: 'about:blank',
      title: 'Bad Request',
      status: 400,
      detail: 'Invalid curation status: INVALID_STATUS',
      fieldErrors: {
        curationStatus: 'Must be ACCEPTED, REJECTED, or UNREVIEWED',
      },
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => problemDetail,
    });

    await expect(
      apiClient.patch('/questions/q-101/curation', { status: 'INVALID_STATUS' })
    ).rejects.toThrow(ApiError);

    try {
      await apiClient.patch('/questions/q-101/curation', { status: 'INVALID_STATUS' });
    } catch (err: any) {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(400);
      expect(err.fieldErrors?.curationStatus).toBe('Must be ACCEPTED, REJECTED, or UNREVIEWED');
    }
  });

  it('honest-judge PASS - GET /stats/curation returns overall and per-subject counts', async () => {
    const mockCurationStats: CurationStatsResponse = {
      totalQuestions: 2550,
      byStatus: {
        UNREVIEWED: 2400,
        ACCEPTED: 120,
        REJECTED: 30,
      },
      bySubject: [
        {
          subjectId: 'subj-1',
          name: 'Operating Systems',
          total: 280,
          unreviewed: 260,
          accepted: 15,
          rejected: 5,
        },
      ],
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockCurationStats,
    });

    const result = await apiClient.get<CurationStatsResponse>('/stats/curation');
    expect(result.totalQuestions).toBe(2550);
    expect(result.byStatus.ACCEPTED).toBe(120);
    expect(result.bySubject[0].name).toBe('Operating Systems');
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/stats/curation', expect.objectContaining({
      method: 'GET',
    }));
  });

  it('honest-judge PASS - GET /stats/distribution returns multi-dimensional counts by subject, type, difficulty', async () => {
    const mockDistribution: DistributionStatsResponse = {
      total: 2550,
      distribution: [
        { subjectSlug: 'algorithms', subjectName: 'Algorithms', type: 'MCQ', difficulty: 'MEDIUM', count: 150 },
        { subjectSlug: 'algorithms', subjectName: 'Algorithms', type: 'MSQ', difficulty: 'HARD', count: 60 },
        { subjectSlug: 'algorithms', subjectName: 'Algorithms', type: 'NAT', difficulty: 'EASY', count: 70 },
      ],
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockDistribution,
    });

    const result = await apiClient.get<DistributionStatsResponse>('/stats/distribution');
    expect(result.total).toBe(2550);
    expect(result.distribution).toHaveLength(3);
    expect(result.distribution[0].subjectSlug).toBe('algorithms');
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/stats/distribution', expect.objectContaining({
      method: 'GET',
    }));
  });

  it('honest-judge PASS - GET /questions propagates curationStatus, source, difficulty query parameters', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        content: [],
        page: 0,
        size: 15,
        totalElements: 0,
        totalPages: 0,
      }),
    });

    await apiClient.get('/questions', {
      params: {
        curationStatus: 'UNREVIEWED',
        source: 'SO_DERIVED',
        difficulty: 'MEDIUM',
        page: 0,
        size: 15,
      },
    });

    const calledUrl = (globalThis.fetch as any).mock.calls[0][0];
    expect(calledUrl).toContain('curationStatus=UNREVIEWED');
    expect(calledUrl).toContain('source=SO_DERIVED');
    expect(calledUrl).toContain('difficulty=MEDIUM');
  });
});
