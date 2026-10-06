import React, { createContext, useContext, useState, useEffect } from 'react';

export type DashboardLayoutVariant = 'minimalist' | 'editorial' | 'bento';

export interface LayoutContextType {
  currentLayout: DashboardLayoutVariant;
  setLayout: (layout: DashboardLayoutVariant) => void;
}

export const STORAGE_KEY = 'gatelms_dashboard_layout';
export const DEFAULT_LAYOUT: DashboardLayoutVariant = 'minimalist';

const LayoutContext = createContext<LayoutContextType | undefined>(undefined);

export const LayoutProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentLayout, setCurrentLayout] = useState<DashboardLayoutVariant>(() => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const saved = window.localStorage.getItem(STORAGE_KEY);
        if (saved === 'minimalist' || saved === 'editorial' || saved === 'bento') {
          return saved;
        }
      }
    } catch {
      // Ignore localStorage access failures (e.g. private browsing or sandbox)
    }
    return DEFAULT_LAYOUT;
  });

  const setLayout = (layout: DashboardLayoutVariant) => {
    setCurrentLayout(layout);
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.setItem(STORAGE_KEY, layout);
      }
    } catch {
      // Ignore localStorage write failures
    }
  };

  return (
    <LayoutContext.Provider value={{ currentLayout, setLayout }}>
      {children}
    </LayoutContext.Provider>
  );
};

export const useDashboardLayout = (): LayoutContextType => {
  const context = useContext(LayoutContext);
  if (!context) {
    return {
      currentLayout: DEFAULT_LAYOUT,
      setLayout: () => {},
    };
  }
  return context;
};
