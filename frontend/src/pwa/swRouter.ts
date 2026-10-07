/**
 * Service Worker Strategy Router
 * Pure functional routing and response transformation logic for GateLMS PWA
 */

export type FetchStrategy =
  | 'CACHE_FIRST'
  | 'NETWORK_FIRST_CACHE_FALLBACK'
  | 'NETWORK_ONLY'
  | 'NAVIGATION'
  | 'STALE_WHILE_REVALIDATE';

export interface RouteMatchOptions {
  method?: string;
  mode?: string;
  destination?: string;
}

/**
 * Determine the caching strategy for a given URL, method, and request metadata
 */
export function matchStrategy(url: string, options: RouteMatchOptions = {}): FetchStrategy {
  const method = (options.method || 'GET').toUpperCase();
  const parsedUrl = new URL(url, 'http://localhost');
  const pathname = parsedUrl.pathname;
  const isGet = method === 'GET';

  // 1. Auth routes (/login, /api/v1/auth/*) -> strictly network-only (never cached by SW)
  if (
    pathname === '/login' ||
    pathname.startsWith('/login/') ||
    pathname.startsWith('/api/v1/auth/') ||
    pathname.startsWith('/auth/')
  ) {
    return 'NETWORK_ONLY';
  }

  // 1b. AI endpoints (/api/v1/ai/*) -> strictly network-only (never cached by SW, AM-17)
  if (pathname.startsWith('/api/v1/ai/') || pathname.startsWith('/api/ai/')) {
    return 'NETWORK_ONLY';
  }

  // 2. Navigation requests (document/page transition)
  if (isGet && (options.mode === 'navigate' || options.destination === 'document')) {
    return 'NAVIGATION';
  }

  // 2. Non-GET API calls (mutations) -> strictly network-only
  if (!isGet && (pathname.startsWith('/api/') || pathname.startsWith('/api'))) {
    return 'NETWORK_ONLY';
  }

  // 3. Static assets (/assets/*) -> cache-first (Vite output is immutable by hash)
  if (isGet && pathname.startsWith('/assets/')) {
    return 'CACHE_FIRST';
  }

  // 4. Offline shell / manifest / static icons -> cache-first
  if (isGet && (pathname === '/offline.html' || pathname === '/manifest.webmanifest' || pathname.startsWith('/icons/'))) {
    return 'CACHE_FIRST';
  }

  // 5. Read-only API queries -> network-first with cache fallback
  // Binding families: /questions*, /subjects*, /topics*, /stats/*, /mistakes*
  const normalizedApiPath = pathname.replace(/^\/api\/v1/, '').replace(/^\/api/, '');
  const isCachedApiFamily = [
    '/questions',
    '/subjects',
    '/topics',
    '/stats',
    '/mistakes',
  ].some((prefix) => normalizedApiPath.startsWith(prefix));

  if (isGet && isCachedApiFamily) {
    return 'NETWORK_FIRST_CACHE_FALLBACK';
  }

  // Non-whitelisted GET API endpoints (e.g. notifications/polling) -> network-only
  if (isGet && (pathname.startsWith('/api/') || pathname.startsWith('/api'))) {
    return 'NETWORK_ONLY';
  }

  // 6. Local fonts, katex, images, scripts -> stale-while-revalidate
  return 'STALE_WHILE_REVALIDATE';
}

/**
 * Stamp response with X-From-Cache header when served from offline cache
 */
export function stampFromCacheResponse(cachedResponse: Response): Response {
  const newHeaders = new Headers(cachedResponse.headers);
  newHeaders.set('X-From-Cache', '1');
  return new Response(cachedResponse.body, {
    status: cachedResponse.status,
    statusText: cachedResponse.statusText,
    headers: newHeaders,
  });
}
