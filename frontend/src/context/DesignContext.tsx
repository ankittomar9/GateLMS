import React, { createContext, useContext, useState, useEffect } from 'react';
import { DashboardLayoutVariant, STORAGE_KEY as LAYOUT_STORAGE_KEY } from './LayoutContext';

export type DesignPreset = 'tuf' | 'ink' | 'aurora' | 'sky';
export const DESIGN_STORAGE_KEY = 'gatelms_design';
export const DEFAULT_DESIGN: DesignPreset = 'tuf';
export const VALID_DESIGNS: DesignPreset[] = ['tuf', 'ink', 'aurora', 'sky'];

export const DESIGN_META: Record<DesignPreset, {
  label: string;
  pairedLayout: DashboardLayoutVariant;
  swatch: [string, string, string];
}> = {
  tuf:    { label: 'Classic', pairedLayout: 'minimalist', swatch: ['#0d0d0d', '#161616', '#f59e0b'] },
  ink:    { label: 'Ink',     pairedLayout: 'minimalist', swatch: ['#0F1115', '#161920', '#3B82F6'] },
  aurora: { label: 'Aurora',  pairedLayout: 'editorial',  swatch: ['#0f111a', '#161b25', '#3b82f6'] },
  sky:    { label: 'Skyline', pairedLayout: 'bento',      swatch: ['#0F172A', '#1E293B', '#38BDF8'] },
};

export interface DesignContextType {
  currentDesign: DesignPreset;
  setDesign: (preset: DesignPreset) => void;
}

// Global layout listener bridge so DesignProvider can trigger setLayout
let globalLayoutSetter: ((layout: DashboardLayoutVariant) => void) | null = null;

export const registerLayoutSetter = (setter: ((layout: DashboardLayoutVariant) => void) | null) => {
  globalLayoutSetter = setter;
};

const DesignContext = createContext<DesignContextType | undefined>(undefined);

export const DesignProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentDesign, setCurrentDesignState] = useState<DesignPreset>(() => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const saved = window.localStorage.getItem(DESIGN_STORAGE_KEY);
        if (saved && (VALID_DESIGNS as string[]).includes(saved)) {
          return saved as DesignPreset;
        }
      }
    } catch {
      // Safe fallback on localStorage access failure
    }
    return DEFAULT_DESIGN;
  });

  // Keep documentElement data-design attribute in sync
  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.dataset.design = currentDesign;
      document.documentElement.setAttribute('data-design', currentDesign);
      const meta = document.querySelector('meta[name="theme-color"]');
      const themeColors: Record<DesignPreset, string> = {
        tuf: '#0F1115',
        ink: '#0F1115',
        aurora: '#0F111A',
        sky: '#0F172A',
      };
      if (meta && themeColors[currentDesign]) {
        meta.setAttribute('content', themeColors[currentDesign]);
      }
    }
  }, [currentDesign]);

  const setDesign = (preset: DesignPreset) => {
    const validPreset = (VALID_DESIGNS as string[]).includes(preset) ? preset : DEFAULT_DESIGN;
    setCurrentDesignState(validPreset);

    // 1. Sync data-design immediately
    if (typeof document !== 'undefined') {
      document.documentElement.dataset.design = validPreset;
      document.documentElement.setAttribute('data-design', validPreset);
    }

    // 2. Persist to localStorage
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.setItem(DESIGN_STORAGE_KEY, validPreset);
      }
    } catch {
      // Ignore localStorage write failure
    }

    // 3. Apply paired layout variant
    const paired = DESIGN_META[validPreset].pairedLayout;
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.setItem(LAYOUT_STORAGE_KEY, paired);
      }
    } catch {
      // Ignore
    }

    if (globalLayoutSetter) {
      globalLayoutSetter(paired);
    }
  };

  return (
    <DesignContext.Provider value={{ currentDesign, setDesign }}>
      {children}
    </DesignContext.Provider>
  );
};

export const useDesign = (): DesignContextType => {
  const context = useContext(DesignContext);
  if (!context) {
    return {
      currentDesign: DEFAULT_DESIGN,
      setDesign: () => {},
    };
  }
  return context;
};
