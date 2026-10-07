import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { aiApi } from '../aiApi';
import { apiClient, ApiError } from '../apiClient';

describe('aiApi Client Suite', () => {
  const originalFetch = globalThis.fetch;
  const originalLocation = window.location;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    (window as any).location = originalLocation;
  });

  it('1 AM-1 transcribeAudio transports BYOK apiKey strictly via X-AI-Key header, not form body', async () => {
    let capturedHeaders: Record<string, string> = {};
    let capturedBody: FormData | null = null;

    globalThis.fetch = vi.fn().mockImplementation((_url, init) => {
      capturedHeaders = init.headers;
      capturedBody = init.body as FormData;
      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({ text: 'Transcribed audio content', status: 'SUCCESS', provider: 'GROQ' }),
      });
    });

    const mockBlob = new Blob(['dummy audio bytes'], { type: 'audio/wav' });
    const result = await aiApi.transcribeAudio(mockBlob, {
      apiKey: 'gsk_secret_12345',
      promptContext: 'Paging question context',
      provider: 'GROQ',
    });

    expect(result.text).toBe('Transcribed audio content');
    expect(capturedHeaders['X-AI-Key']).toBe('gsk_secret_12345');

    // Confirm apiKey is NOT present as form field
    const bodyFormData = capturedBody as any;
    expect(bodyFormData).not.toBeNull();
    expect(bodyFormData.get('apiKey')).toBeNull();
    expect(bodyFormData.get('provider')).toBe('GROQ');
    expect(bodyFormData.get('promptContext')).toBe('Paging question context');
  });

  it('2 AM-14 transcribeAudio triggers 401 redirect parity to /login', async () => {
    delete (window as any).location;
    window.location = {
      pathname: '/practice',
      search: '?subject=os',
      href: '',
    } as any;

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({ detail: 'Unauthorized session' }),
    });

    const mockBlob = new Blob(['bytes'], { type: 'audio/webm' });
    await expect(aiApi.transcribeAudio(mockBlob)).rejects.toThrow(ApiError);
    expect(window.location.href).toBe('/login?next=%2Fpractice%3Fsubject%3Dos');
  });

  it('3 AM-15 chat sends X-AI-Key header and supports AbortSignal', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValue({
      reply: 'AI Response',
      provider: 'GROQ',
      model: 'llama-3.3-70b-versatile',
      latencyMs: 120,
    });

    const controller = new AbortController();
    const req = {
      messages: [{ role: 'user' as const, content: 'Explain LRU cache' }],
      provider: 'GROQ',
      mode: 'HINT' as const,
    };

    const res = await aiApi.chat(req, 'gsk_key_555', controller.signal);
    expect(res.reply).toBe('AI Response');
    expect(postSpy).toHaveBeenCalledWith(
      '/ai/chat',
      req,
      expect.objectContaining({
        headers: { 'X-AI-Key': 'gsk_key_555' },
        signal: controller.signal,
      })
    );
  });

  it('4 getProvidersStatus queries status endpoint with provider filter and X-AI-Key', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue([
      { provider: 'GROQ', configured: true, state: 'READY' },
    ]);

    const res = await aiApi.getProvidersStatus('gsk_test', 'GROQ');
    expect(res).toHaveLength(1);
    expect(res[0].provider).toBe('GROQ');
    expect(getSpy).toHaveBeenCalledWith(
      '/ai/status',
      expect.objectContaining({
        headers: { 'X-AI-Key': 'gsk_test' },
        params: { provider: 'GROQ' },
      })
    );
  });
});
