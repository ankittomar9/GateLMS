/**
 * GateLMS Service Worker Registration Utility
 * Handles PROD registration, update listeners, and controllerchange events
 */

export interface RegisterSwOptions {
  isProd?: boolean;
  onUpdate?: (registration: ServiceWorkerRegistration) => void;
  onSuccess?: (registration: ServiceWorkerRegistration) => void;
}

export function registerServiceWorker(options: RegisterSwOptions = {}): Promise<ServiceWorkerRegistration | null> {
  const isProd = options.isProd !== undefined ? options.isProd : import.meta.env.PROD;

  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
    return Promise.resolve(null);
  }

  // Strictly avoid service worker registration in DEV to prevent cache poisoning
  if (!isProd) {
    return Promise.resolve(null);
  }

  // Listen for controller changes to signal update available
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    window.dispatchEvent(new CustomEvent('gatelms:sw-update'));
  });

  return navigator.serviceWorker
    .register('/sw.js')
    .then((registration) => {
      registration.addEventListener('updatefound', () => {
        const installingWorker = registration.installing;
        if (!installingWorker) return;

        installingWorker.addEventListener('statechange', () => {
          if (installingWorker.state === 'installed') {
            if (navigator.serviceWorker.controller) {
              // New content is available; notify user
              if (options.onUpdate) {
                options.onUpdate(registration);
              }
              window.dispatchEvent(
                new CustomEvent('gatelms:sw-update', { detail: { registration } })
              );
            } else {
              // Content cached for offline use
              if (options.onSuccess) {
                options.onSuccess(registration);
              }
            }
          }
        });
      });

      return registration;
    })
    .catch((error) => {
      console.error('Service worker registration failed:', error);
      return null;
    });
}
