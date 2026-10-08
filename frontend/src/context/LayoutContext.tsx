import React, { createContext, useContext, useState, useEffect } from 'react';
import { registerLayoutSetter } from './DesignContext';

export type DashboardLayoutVariant = 'minimalist' | 'editorial' | 'bento';

export interface LayoutContextType {
  currentLayout: DashboardLayoutVariant;
  setLayout: (layout: DashboardLayoutVariant) => void;
}

export const STORAGE_KEY = 'gatelms_dashboard_layout';
export const RIGHT_SIDEBAR_STORAGE_KEY = 'gatelms_right_sidebar_collapsed';
export const LEFT_SIDEBAR_STORAGE_KEY = 'gatelms_left_sidebar_collapsed';
export const DEFAULT_LAYOUT: DashboardLayoutVariant = 'minimalist';

export interface LayoutContextType {
  currentLayout: DashboardLayoutVariant;
  setLayout: (layout: DashboardLayoutVariant) => void;
  rightSidebarCollapsed: boolean;
  setRightSidebarCollapsed: (collapsed: boolean | ((prev: boolean) => boolean)) => void;
  toggleRightSidebar: () => void;
  leftSidebarCollapsed: boolean;
  setLeftSidebarCollapsed: (collapsed: boolean | ((prev: boolean) => boolean)) => void;
  toggleLeftSidebar: () => void;
}

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

  const [rightSidebarCollapsed, setRightSidebarCollapsedState] = useState<boolean>(() => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        return window.localStorage.getItem(RIGHT_SIDEBAR_STORAGE_KEY) === 'true';
      }
    } catch {
      // Ignore
    }
    return false;
  });

  const [leftSidebarCollapsed, setLeftSidebarCollapsedState] = useState<boolean>(() => {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        return window.localStorage.getItem(LEFT_SIDEBAR_STORAGE_KEY) === 'true';
      }
    } catch {
      // Ignore
    }
    return false;
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

  const setRightSidebarCollapsed = (
    valueOrFn: boolean | ((prev: boolean) => boolean)
  ) => {
    setRightSidebarCollapsedState((prev) => {
      const next = typeof valueOrFn === 'function' ? valueOrFn(prev) : valueOrFn;
      try {
        if (typeof window !== 'undefined' && window.localStorage) {
          window.localStorage.setItem(RIGHT_SIDEBAR_STORAGE_KEY, String(next));
        }
      } catch {
        // Ignore
      }
      return next;
    });
  };

  const toggleRightSidebar = () => {
    setRightSidebarCollapsed((prev) => !prev);
  };

  const setLeftSidebarCollapsed = (
    valueOrFn: boolean | ((prev: boolean) => boolean)
  ) => {
    setLeftSidebarCollapsedState((prev) => {
      const next = typeof valueOrFn === 'function' ? valueOrFn(prev) : valueOrFn;
      try {
        if (typeof window !== 'undefined' && window.localStorage) {
          window.localStorage.setItem(LEFT_SIDEBAR_STORAGE_KEY, String(next));
        }
      } catch {
        // Ignore
      }
      return next;
    });
  };

  const toggleLeftSidebar = () => {
    setLeftSidebarCollapsed((prev) => !prev);
  };

  useEffect(() => {
    registerLayoutSetter(setLayout);
    return () => {
      registerLayoutSetter(null);
    };
  }, []);

  return (
    <LayoutContext.Provider
      value={{
        currentLayout,
        setLayout,
        rightSidebarCollapsed,
        setRightSidebarCollapsed,
        toggleRightSidebar,
        leftSidebarCollapsed,
        setLeftSidebarCollapsed,
        toggleLeftSidebar,
      }}
    >
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
      rightSidebarCollapsed: false,
      setRightSidebarCollapsed: () => {},
      toggleRightSidebar: () => {},
      leftSidebarCollapsed: false,
      setLeftSidebarCollapsed: () => {},
      toggleLeftSidebar: () => {},
    };
  }
  return context;
};
