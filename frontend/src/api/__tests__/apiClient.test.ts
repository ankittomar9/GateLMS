import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiClient, ApiError } from '../apiClient';

describe('apiClient', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('honest-judge PASS - successful GET returns parsed JSON payload', async () => {
    const mockData = { id: 'subj_1', name: 'Operating Systems' };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockData,
    });

    const result = await apiClient.get<typeof mockData>('/subjects');
    expect(result).toEqual(mockData);
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/subjects', expect.objectContaining({
      method: 'GET',
    }));
  });

  it('honest-judge PASS - 204 No Content resolves to empty object without error', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      json: async () => ({}),
    });

    const result = await apiClient.delete('/topics/123');
    expect(result).toEqual({});
  });

  it('honest-judge FAIL - deliberate-wrong 400 ProblemDetail normalizes to ApiError with fieldErrors', async () => {
    const problemDetail = {
      title: 'Validation Failed',
      status: 400,
      detail: 'Field validation error',
      fieldErrors: {
        numericMin: 'numericMin cannot be greater than numericMax',
      },
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => problemDetail,
    });

    await expect(apiClient.post('/questions', { numericMin: 5, numericMax: 3 }))
      .rejects.toThrow(ApiError);

    try {
      await apiClient.post('/questions', { numericMin: 5, numericMax: 3 });
    } catch (err: any) {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(400);
      expect(err.message).toBe('Field validation error');
      expect(err.fieldErrors).toEqual({
        numericMin: 'numericMin cannot be greater than numericMax',
      });
    }
  });

  it('honest-judge FAIL - network failure normalizes to ApiError with status 0', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Failed to connect to backend'));

    await expect(apiClient.get('/health')).rejects.toThrow(ApiError);

    try {
      await apiClient.get('/health');
    } catch (err: any) {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(0);
      expect(err.message).toBe('Failed to connect to backend');
    }
  });

  it('honest-judge FAIL - deliberate-aborted request propagates AbortError cleanly', async () => {
    const controller = new AbortController();
    const abortErr = new Error('The operation was aborted');
    abortErr.name = 'AbortError';

    globalThis.fetch = vi.fn().mockRejectedValue(abortErr);

    controller.abort();
    await expect(
      apiClient.get('/questions', { signal: controller.signal })
    ).rejects.toThrow('The operation was aborted');
  });
});
