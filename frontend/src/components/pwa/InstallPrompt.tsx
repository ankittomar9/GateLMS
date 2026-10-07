import React, { useState, useEffect } from 'react';
import { Download, Share, PlusSquare, Check } from 'lucide-react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

export const InstallPrompt: React.FC<{ inDropdown?: boolean }> = ({ inDropdown = false }) => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [canInstall, setCanInstall] = useState(false);
  const [isInstalled, setIsInstalled] = useState(false);
  const [showIosHint, setShowIosHint] = useState(false);

  // Detect iOS Safari environment where beforeinstallprompt doesn't fire
  const isIos =
    typeof window !== 'undefined' &&
    /iphone|ipad|ipod/.test(window.navigator.userAgent.toLowerCase()) &&
    !(window.navigator as any).standalone;

  useEffect(() => {
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setCanInstall(true);
    };

    const handleAppInstalled = () => {
      setCanInstall(false);
      setDeferredPrompt(null);
      setIsInstalled(true);
      setShowIosHint(false);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
      window.removeEventListener('appinstalled', handleAppInstalled);
    };
  }, []);

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      await deferredPrompt.prompt();
      const choice = await deferredPrompt.userChoice;
      if (choice.outcome === 'accepted') {
        setCanInstall(false);
        setDeferredPrompt(null);
      }
    } else if (isIos) {
      setShowIosHint(!showIosHint);
    }
  };

  if (isInstalled) {
    return null;
  }

  // If standard prompt is not available and not iOS, render nothing
  if (!canInstall && !isIos) {
    return null;
  }

  return (
    <div className="relative inline-block">
      {canInstall ? (
        <button
          type="button"
          data-testid="pwa-install-btn"
          onClick={handleInstallClick}
          className={`flex items-center gap-1.5 rounded-ds text-xs font-semibold transition-all shadow-sm ${
            inDropdown
              ? 'w-full px-3 py-2 text-ds-primary hover:bg-ds-surface-hover text-left'
              : 'px-2.5 py-1 sm:px-3 sm:py-1.5 bg-ds-surface hover:bg-ds-surface-hover text-ds-primary border border-ds-border'
          }`}
        >
          <Download className="w-3.5 h-3.5 text-ds-accent shrink-0" />
          <span>Install App</span>
        </button>
      ) : isIos ? (
        <button
          type="button"
          data-testid="ios-install-btn"
          onClick={() => setShowIosHint(!showIosHint)}
          className={`flex items-center gap-1.5 rounded-ds text-xs font-semibold transition-all ${
            inDropdown
              ? 'w-full px-3 py-2 text-ds-primary hover:bg-ds-surface-hover text-left'
              : 'px-2.5 py-1 text-ds-secondary hover:text-ds-primary text-xs'
          }`}
        >
          <Download className="w-3.5 h-3.5 text-ds-accent shrink-0" />
          <span>Install App</span>
        </button>
      ) : null}

      {/* iOS Safari Home Screen instructions */}
      {isIos && showIosHint && (
        <div
          data-testid="ios-install-hint"
          className="absolute right-0 mt-2 w-64 p-3 bg-ds-surface border border-ds-border rounded-ds shadow-xl text-xs text-ds-secondary space-y-2 z-50"
        >
          <div className="font-bold text-ds-primary flex items-center gap-1.5">
            <Share className="w-3.5 h-3.5 text-ds-accent" />
            <span>Install on iOS</span>
          </div>
          <p className="text-[11px] leading-relaxed">
            Tap the <strong className="text-ds-primary">Share</strong> button in Safari toolbar, then select <strong className="text-ds-primary">Add to Home Screen</strong>.
          </p>
        </div>
      )}
    </div>
  );
};
