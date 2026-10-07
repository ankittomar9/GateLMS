// GateLMS Hand-Rolled Service Worker
// Cache Versioning
var CACHE_VERSION = '2026.10.07-v2';
var CACHE_NAME = 'gatelms-' + CACHE_VERSION;

var PRECACHE_ASSETS = [
  '/',
  '/index.html',
  '/offline.html',
  '/manifest.webmanifest',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/maskable-512.png'
];

var CACHED_API_FAMILIES = [
  '/questions',
  '/subjects',
  '/topics',
  '/stats',
  '/mistakes'
];

// 1. Install Phase: Precache core application shell
self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(CACHE_NAME).then(function (cache) {
      return cache.addAll(PRECACHE_ASSETS);
    })
  );
});

// 2. Activate Phase: Purge obsolete cache versions
self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (cacheNames) {
      return Promise.all(
        cacheNames.map(function (name) {
          if (name !== CACHE_NAME) {
            return caches.delete(name);
          }
        })
      );
    }).then(function () {
      return self.clients.claim();
    })
  );
});

// 3. Skip Waiting on client message
self.addEventListener('message', function (event) {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// 4. Fetch Event Dispatcher
self.addEventListener('fetch', function (event) {
  var request = event.request;
  var url = new URL(request.url);
  var pathname = url.pathname;
  var method = request.method.toUpperCase();

  // Auth routes (/login, /api/v1/auth/*) -> strictly network-only (never cached or intercepted by SW)
  if (
    pathname === '/login' ||
    pathname.indexOf('/login/') === 0 ||
    pathname.indexOf('/api/v1/auth/') === 0 ||
    pathname.indexOf('/auth/') === 0
  ) {
    event.respondWith(fetch(request));
    return;
  }

  // Navigation requests: document / page load
  if (method === 'GET' && (request.mode === 'navigate' || request.destination === 'document')) {
    event.respondWith(
      fetch(request).catch(function () {
        return caches.match('/index.html').then(function (res) {
          return res || caches.match('/offline.html');
        });
      })
    );
    return;
  }

  // Non-GET requests (mutations) are strictly network-only
  if (method !== 'GET') {
    return;
  }

  // Static assets (/assets/*): Cache-First
  if (pathname.indexOf('/assets/') === 0 || pathname.indexOf('/icons/') === 0 || pathname === '/manifest.webmanifest') {
    event.respondWith(
      caches.match(request).then(function (cached) {
        if (cached) return cached;
        return fetch(request).then(function (response) {
          if (response && response.status === 200) {
            var copy = response.clone();
            caches.open(CACHE_NAME).then(function (cache) {
              cache.put(request, copy);
            });
          }
          return response;
        });
      })
    );
    return;
  }

  // Read-only API whitelist: Network-First with Cache Fallback
  var normalizedApiPath = pathname.replace(/^\/api\/v1/, '').replace(/^\/api/, '');
  var isWhitelistedApi = CACHED_API_FAMILIES.some(function (family) {
    return normalizedApiPath.indexOf(family) === 0;
  });

  if (isWhitelistedApi) {
    event.respondWith(
      fetch(request).then(function (response) {
        if (response && response.status === 200) {
          var copy = response.clone();
          caches.open(CACHE_NAME).then(function (cache) {
            cache.put(request, copy);
          });
        }
        return response;
      }).catch(function () {
        return caches.match(request).then(function (cached) {
          if (cached) {
            var newHeaders = new Headers(cached.headers);
            newHeaders.set('X-From-Cache', '1');
            return new Response(cached.body, {
              status: cached.status,
              statusText: cached.statusText,
              headers: newHeaders
            });
          }
          return new Response(JSON.stringify({ error: 'Offline - cached content not available' }), {
            status: 503,
            headers: { 'Content-Type': 'application/json' }
          });
        });
      })
    );
    return;
  }

  // Non-whitelisted GET APIs -> network only
  if (pathname.indexOf('/api/') === 0 || pathname.indexOf('/api') === 0) {
    return;
  }

  // Everything else: Stale-While-Revalidate
  event.respondWith(
    caches.match(request).then(function (cached) {
      var fetchPromise = fetch(request).then(function (networkResponse) {
        if (networkResponse && networkResponse.status === 200) {
          var copy = networkResponse.clone();
          caches.open(CACHE_NAME).then(function (cache) {
            cache.put(request, copy);
          });
        }
        return networkResponse;
      }).catch(function () {
        return cached;
      });

      return cached || fetchPromise;
    })
  );
});
