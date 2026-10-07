import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiError } from '../apiClient';
import { readinessApi, ReadinessResponse, HeatmapResponse, TrendsResponse, ExamCountdown } from '../readinessApi';

describe('Readiness and Analytics API Layer', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('honest-judge PASS - getReadiness returns readiness score and per-subject component shapes', async () => {
    const mockReadiness: ReadinessResponse = {
      readiness: 72,
      perSubject: [
        {
          subjectId: 'subj_em',
          name: 'Engineering Mathematics',
          weight: 15.0,
          m: 0.8,
          a: 0.75,
          c: 0.6,
          readiness: 0.745,
        },
      ],
      computedAt: '2026-10-07T08:00:00Z',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockReadiness,
    });

    const result = await readinessApi.getReadiness();
    expect(result.readiness).toBe(72);
    expect(result.perSubject).toHaveLength(1);
    expect(result.perSubject[0].name).toBe('Engineering Mathematics');
    expect(result.perSubject[0].readiness).toBe(0.745);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/stats/readiness',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('honest-judge PASS - getHeatmap returns subjects with topic mastery and due flags', async () => {
    const mockHeatmap: HeatmapResponse = {
      subjects: [
        {
          subjectId: 'subj_os',
          name: 'Operating Systems',
          topics: [
            {
              topicId: 'top_virt_mem',
              name: 'Virtual Memory & Paging',
              mastery: 4,
              due: true,
              answered: 12,
              correct: 10,
            },
          ],
        },
      ],
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockHeatmap,
    });

    const result = await readinessApi.getHeatmap();
    expect(result.subjects).toHaveLength(1);
    expect(result.subjects[0].topics[0].mastery).toBe(4);
    expect(result.subjects[0].topics[0].due).toBe(true);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/stats/heatmap',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('honest-judge PASS - getTrends propagates days parameter and returns daily rollups with rolling7', async () => {
    const mockTrends: TrendsResponse = {
      days: [
        {
          date: '2026-10-06',
          questionsAnswered: 15,
          correct: 12,
          minutesPracticed: 35,
          minutesMocked: 0,
        },
      ],
      rolling7: {
        accuracy: 80.0,
        volume: 15,
      },
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockTrends,
    });

    const result = await readinessApi.getTrends(30);
    expect(result.days).toHaveLength(1);
    expect(result.rolling7.accuracy).toBe(80.0);
    const calledUrl = (globalThis.fetch as any).mock.calls[0][0];
    expect(calledUrl).toBe('/api/v1/stats/trends?days=30');

    // Also test default days=90
    await readinessApi.getTrends();
    const calledUrlDefault = (globalThis.fetch as any).mock.calls[1][0];
    expect(calledUrlDefault).toBe('/api/v1/stats/trends?days=90');
  });

  it('honest-judge PASS - getCountdown returns exam date, daysRemaining and weeksRemaining', async () => {
    const mockCountdown: ExamCountdown = {
      examDate: '2028-02-12',
      daysRemaining: 493,
      weeksRemaining: 70,
      source: 'config',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockCountdown,
    });

    const result = await readinessApi.getCountdown();
    expect(result.examDate).toBe('2028-02-12');
    expect(result.daysRemaining).toBe(493);
    expect(result.weeksRemaining).toBe(70);
    expect(result.source).toBe('config');
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/exam/countdown',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('honest-judge FAIL - getTrends throws ApiError with fieldErrors on invalid days parameter', async () => {
    const problemDetail = {
      type: 'https://gatelms.dev/errors/validation-failed',
      title: 'Bad Request',
      status: 400,
      detail: 'Invalid days parameter',
      fieldErrors: {
        days: 'Days must be between 7 and 365',
      },
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => problemDetail,
    });

    await expect(readinessApi.getTrends(5)).rejects.toThrow(ApiError);

    try {
      await readinessApi.getTrends(5);
    } catch (err: any) {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(400);
      expect(err.fieldErrors?.days).toBe('Days must be between 7 and 365');
    }
  });
});
