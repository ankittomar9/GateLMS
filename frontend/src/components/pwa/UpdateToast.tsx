import React, { useState, useEffect } from 'react';
import { RefreshCw, X } from 'lucide-react';

export const UpdateToast: React.FC = () => {
  const [showUpdate, setShowUpdate] = useState(false);

  useEffect(() => {
    const handleUpdate = () => {
      setShowUpdate(true);
    };

    window.addEventListener('gatelms:sw-update', handleUpdate);
    return () => {
      window.removeEventListener('gatelms:sw-update', handleUpdate);
    };
  }, []);

  const handleReload = () => {
    if (typeof window !== 'undefined') {
      window.location.reload();
    }
  };

  if (!showUpdate) {
    return null;
  }

  return (
    <div
      role="alert"
      data-testid="pwa-update-toast"
      className="fixed bottom-5 right-5 z-50 bg-ds-surface border border-ds-border rounded-ds p-4 shadow-2xl flex items-center gap-3.5 max-w-sm animate-in fade-in slide-in-from-bottom-2 duration-200"
    >
      <div className="w-8 h-8 rounded-full bg-ds-accent/15 border border-ds-accent/30 flex items-center justify-center shrink-0 text-ds-accent">
        <RefreshCw className="w-4 h-4" />
      </div>

      <div className="flex-1 min-w-0">
        <h4 className="text-xs font-bold text-ds-primary leading-tight">Update Available</h4>
        <p className="text-[11px] text-ds-muted leading-tight mt-0.5">
          A new version of GateLMS is ready.
        </p>
      </div>

      <div className="flex items-center gap-1.5 shrink-0">
        <button
          type="button"
          data-testid="pwa-reload-btn"
          onClick={handleReload}
          className="px-2.5 py-1.5 rounded-ds bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-bold transition-all shadow-sm"
        >
          Reload
        </button>
        <button
          type="button"
          data-testid="pwa-dismiss-toast-btn"
          onClick={() => setShowUpdate(false)}
          className="p-1 rounded-ds text-ds-muted hover:text-ds-primary transition-colors"
          aria-label="Dismiss update notification"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
