import React, { useState, useEffect } from 'react';
import { WifiOff, X } from 'lucide-react';

export const OfflineBanner: React.FC = () => {
  const [isOffline, setIsOffline] = useState<boolean>(() => {
    return typeof navigator !== 'undefined' ? !navigator.onLine : false;
  });
  const [isFromCache, setIsFromCache] = useState<boolean>(false);
  const [dismissed, setDismissed] = useState<boolean>(false);

  useEffect(() => {
    const handleOffline = () => {
      setIsOffline(true);
      setDismissed(false);
    };

    const handleOnline = () => {
      setIsOffline(false);
      setIsFromCache(false);
      setDismissed(false);
    };

    const handleCachedResponse = () => {
      setIsFromCache(true);
      setDismissed(false);
    };

    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnline);
    window.addEventListener('gatelms:cached-response', handleCachedResponse);

    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('gatelms:cached-response', handleCachedResponse);
    };
  }, []);

  const shouldShow = (isOffline || isFromCache) && !dismissed;

  if (!shouldShow) {
    return null;
  }

  return (
    <aside
      role="status"
      aria-live="polite"
      data-testid="offline-banner"
      className="bg-ds-surface-raised border-b border-ds-warning/40 px-4 py-2 text-xs text-ds-primary flex items-center justify-between gap-3 shadow-sm transition-all"
    >
      <div className="flex items-center gap-2">
        <WifiOff className="w-4 h-4 text-ds-warning shrink-0" />
        <span className="font-medium">
          Offline — showing cached content
        </span>
        <span className="text-[11px] text-ds-muted font-mono hidden sm:inline">
          (Read-only mode · new submissions will resume when reconnected)
        </span>
      </div>

      <button
        type="button"
        data-testid="offline-dismiss-btn"
        onClick={() => setDismissed(true)}
        aria-label="Dismiss offline notification"
        className="text-ds-muted hover:text-ds-primary p-1 rounded-ds transition-colors"
      >
        <X className="w-3.5 h-3.5" />
      </button>
    </aside>
  );
};
