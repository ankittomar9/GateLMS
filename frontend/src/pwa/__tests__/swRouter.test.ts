import { describe, it, expect } from 'vitest';
import { matchStrategy, stampFromCacheResponse } from '../swRouter';

describe('Service Worker Router Strategy Matrix', () => {
  it('1. cache-first strategy for Vite hashed assets, manifest, and icons', () => {
    expect(matchStrategy('/assets/index-DiYai3Pd.js', { method: 'GET' })).toBe('CACHE_FIRST');
    expect(matchStrategy('/assets/KaTeX_Main-Regular.woff2', { method: 'GET' })).toBe('CACHE_FIRST');
    expect(matchStrategy('/manifest.webmanifest', { method: 'GET' })).toBe('CACHE_FIRST');
    expect(matchStrategy('/icons/icon-192.png', { method: 'GET' })).toBe('CACHE_FIRST');
    expect(matchStrategy('/offline.html', { method: 'GET' })).toBe('CACHE_FIRST');
  });

  it('2. network-first with cache-fallback for all 5 whitelisted GET API families', () => {
    // 1. questions
    expect(matchStrategy('/api/v1/questions?page=0', { method: 'GET' })).toBe('NETWORK_FIRST_CACHE_FALLBACK');
    // 2. subjects
    expect(matchStrategy('/api/v1/subjects', { method: 'GET' })).toBe('NETWORK_FIRST_CACHE_FALLBACK');
    // 3. topics
    expect(matchStrategy('/api/v1/topics?subjectId=cs1', { method: 'GET' })).toBe('NETWORK_FIRST_CACHE_FALLBACK');
    // 4. stats
    expect(matchStrategy('/api/v1/stats/readiness', { method: 'GET' })).toBe('NETWORK_FIRST_CACHE_FALLBACK');
    expect(matchStrategy('/api/v1/stats/summary', { method: 'GET' })).toBe('NETWORK_FIRST_CACHE_FALLBACK');
    // 5. mistakes
    expect(matchStrategy('/api/v1/mistakes', { method: 'GET' })).toBe('NETWORK_FIRST_CACHE_FALLBACK');
  });

  it('3. network-only strategy for API mutations and non-cached endpoints', () => {
    // Non-GET API mutations (never cached)
    expect(matchStrategy('/api/v1/questions', { method: 'POST' })).toBe('NETWORK_ONLY');
    expect(matchStrategy('/api/v1/topics/top_1/mastery', { method: 'PATCH' })).toBe('NETWORK_ONLY');
    expect(matchStrategy('/api/v1/plans/generate', { method: 'POST' })).toBe('NETWORK_ONLY');
    expect(matchStrategy('/api/v1/notifications/1/read', { method: 'PATCH' })).toBe('NETWORK_ONLY');

    // Dynamic polling endpoints not in read-only offline whitelist
    expect(matchStrategy('/api/v1/notifications/unread-count', { method: 'GET' })).toBe('NETWORK_ONLY');

    // AI endpoints (AM-17: never cached by SW, strictly network-only)
    expect(matchStrategy('/api/v1/ai/status', { method: 'GET' })).toBe('NETWORK_ONLY');
    expect(matchStrategy('/api/v1/ai/chat', { method: 'POST' })).toBe('NETWORK_ONLY');
    expect(matchStrategy('/api/v1/ai/transcribe', { method: 'POST' })).toBe('NETWORK_ONLY');
  });

  it('4. navigation strategy for SPA routes and page transitions', () => {
    expect(matchStrategy('/', { method: 'GET', mode: 'navigate' })).toBe('NAVIGATION');
    expect(matchStrategy('/practice', { method: 'GET', destination: 'document' })).toBe('NAVIGATION');
    expect(matchStrategy('/syllabus', { method: 'GET', mode: 'navigate' })).toBe('NAVIGATION');
  });

  it('5. stampFromCacheResponse preserves response body and status while adding X-From-Cache header', async () => {
    const originalBody = JSON.stringify({ items: [1, 2, 3] });
    const originalResponse = new Response(originalBody, {
      status: 200,
      statusText: 'OK',
      headers: { 'Content-Type': 'application/json' },
    });

    const stampedResponse = stampFromCacheResponse(originalResponse);

    expect(stampedResponse.status).toBe(200);
    expect(stampedResponse.headers.get('Content-Type')).toBe('application/json');
    expect(stampedResponse.headers.get('X-From-Cache')).toBe('1');

    const text = await stampedResponse.text();
    expect(text).toBe(originalBody);
  });
});
