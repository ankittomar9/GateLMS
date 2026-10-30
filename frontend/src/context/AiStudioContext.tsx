import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { ChatMessage, QuestionContext } from '../api/aiApi';

export interface WindowRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface AiStudioContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggleOpen: () => void;
  width: number;
  setWidth: (width: number, maxLimit?: number) => void;
  windowMode: 'docked' | 'floating';
  setWindowMode: (mode: 'docked' | 'floating') => void;
  toggleWindowMode: () => void;
  windowRect: WindowRect;
  setWindowRect: (rect: WindowRect) => void;
  activeQuestion: QuestionContext | null;
  registerActiveQuestion: (question: QuestionContext | null) => void;
  selectedProvider: string;
  setSelectedProvider: (provider: string, defaultModel?: string) => void;
  selectedModel: string;
  setSelectedModel: (model: string) => void;
  byokKeys: Record<string, string>;
  setByokKey: (provider: string, key: string) => void;
  messages: ChatMessage[];
  addMessage: (message: ChatMessage) => void;
  clearMessages: () => void;
  isLoading: boolean;
  setIsLoading: (loading: boolean) => void;
  pendingPrompt: { text: string; mode?: 'HINT' | 'EXPLANATION' | 'GENERAL' } | null;
  triggerPrompt: (text: string, mode?: 'HINT' | 'EXPLANATION' | 'GENERAL') => void;
  clearPendingPrompt: () => void;
}

const STORAGE_KEY_WIDTH = 'gatelms_ai_studio_width';
const STORAGE_KEY_PROVIDER = 'gatelms_ai_provider';
const STORAGE_KEY_MODEL = 'gatelms_ai_model';
const STORAGE_KEY_BYOK = 'gatelms_byok_keys';
const STORAGE_KEY_WINDOW_MODE = 'gatelms_ai_studio_window_mode';
const STORAGE_KEY_WINDOW_RECT = 'gatelms_ai_studio_window_rect';

export const DEFAULT_WIDTH = 420;
export const MIN_WIDTH = 340;

export function getMaxWidth(): number {
  if (typeof window !== 'undefined') {
    return Math.max(800, window.innerWidth - 32);
  }
  return 800;
}

export function getDefaultWindowRect(w: number): WindowRect {
  const vw = typeof window !== 'undefined' ? window.innerWidth : 1280;
  const vh = typeof window !== 'undefined' ? window.innerHeight : 800;
  return {
    x: Math.max(16, vw - w - 16),
    y: 16,
    width: Math.min(w, Math.max(MIN_WIDTH, vw - 32)),
    height: Math.max(400, vh - 32),
  };
}

export function clampWindowRect(rect: WindowRect): WindowRect {
  const vw = typeof window !== 'undefined' ? window.innerWidth : 1280;
  const vh = typeof window !== 'undefined' ? window.innerHeight : 800;
  const margin = 8;
  const minW = MIN_WIDTH;
  const minH = 400;

  const w = Math.max(minW, Math.min(vw - margin * 2, rect.width));
  const h = Math.max(minH, Math.min(vh - margin * 2, rect.height));
  const x = Math.max(margin, Math.min(vw - w - margin, rect.x));
  const y = Math.max(margin, Math.min(vh - h - margin, rect.y));

  return { x, y, width: w, height: h };
}

const AiStudioContext = createContext<AiStudioContextValue | undefined>(undefined);

export const AiStudioProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isOpen, setIsOpenState] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('gatelms_ai_studio_open') === 'true';
    }
    return false;
  });

  const [width, setWidthState] = useState<number>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem(STORAGE_KEY_WIDTH);
      if (saved) {
        const parsed = parseInt(saved, 10);
        const maxW = getMaxWidth();
        if (!isNaN(parsed) && parsed >= MIN_WIDTH && parsed <= maxW) {
          return parsed;
        }
      }
    }
    return DEFAULT_WIDTH;
  });

  const [windowMode, setWindowModeState] = useState<'docked' | 'floating'>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem(STORAGE_KEY_WINDOW_MODE);
      if (saved === 'floating' || saved === 'docked') return saved;
    }
    return 'docked';
  });

  const [windowRect, setWindowRectState] = useState<WindowRect>(() => {
    if (typeof window !== 'undefined') {
      try {
        const saved = localStorage.getItem(STORAGE_KEY_WINDOW_RECT);
        if (saved) {
          const parsed = JSON.parse(saved);
          if (
            typeof parsed.x === 'number' &&
            typeof parsed.y === 'number' &&
            typeof parsed.width === 'number' &&
            typeof parsed.height === 'number'
          ) {
            return clampWindowRect(parsed);
          }
        }
      } catch {}
    }
    return getDefaultWindowRect(DEFAULT_WIDTH);
  });

  const [activeQuestion, setActiveQuestion] = useState<QuestionContext | null>(null);

  const [selectedProvider, setSelectedProviderState] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem(STORAGE_KEY_PROVIDER) || 'GROQ';
    }
    return 'GROQ';
  });

  const [selectedModel, setSelectedModelState] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem(STORAGE_KEY_MODEL);
      if (saved && (saved === 'gemini-2.0-flash' || saved === 'gemini-2.5-flash')) {
        localStorage.setItem(STORAGE_KEY_MODEL, 'gemini-3.5-flash');
        return 'gemini-3.5-flash';
      }
      if (saved) return saved;
    }
    return 'llama-3.3-70b-versatile';
  });

  const [byokKeys, setByokKeys] = useState<Record<string, string>>(() => {
    if (typeof window !== 'undefined') {
      try {
        const saved = sessionStorage.getItem(STORAGE_KEY_BYOK);
        if (saved) return JSON.parse(saved);
      } catch {}
    }
    return {};
  });

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content:
        "Hello Aspirant! I'm your GateLMS AI Studio Tutor. Ask me any GATE CS concept, derivation, or click '**Attach Active Question**' to explore hints and step-by-step solutions without leaving your workspace.",
    },
  ]);

  const [isLoading, setIsLoading] = useState<boolean>(false);

  const setIsOpen = useCallback((open: boolean) => {
    setIsOpenState(open);
    if (typeof window !== 'undefined') {
      localStorage.setItem('gatelms_ai_studio_open', String(open));
    }
  }, []);

  const toggleOpen = useCallback(() => {
    setIsOpenState((prev) => {
      const next = !prev;
      if (typeof window !== 'undefined') {
        localStorage.setItem('gatelms_ai_studio_open', String(next));
      }
      return next;
    });
  }, []);

  const setWidth = useCallback((newWidth: number, maxLimit?: number) => {
    const maxW = maxLimit ?? 800;
    const clamped = Math.max(MIN_WIDTH, Math.min(maxW, newWidth));
    setWidthState(clamped);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_WIDTH, String(clamped));
    }
  }, []);

  const setWindowMode = useCallback((mode: 'docked' | 'floating') => {
    setWindowModeState(mode);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_WINDOW_MODE, mode);
    }
  }, []);

  const toggleWindowMode = useCallback(() => {
    setWindowModeState((prev) => {
      const next = prev === 'docked' ? 'floating' : 'docked';
      if (typeof window !== 'undefined') {
        localStorage.setItem(STORAGE_KEY_WINDOW_MODE, next);
      }
      return next;
    });
  }, []);

  const setWindowRect = useCallback((rect: WindowRect) => {
    const clamped = clampWindowRect(rect);
    setWindowRectState(clamped);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_WINDOW_RECT, JSON.stringify(clamped));
    }
  }, []);

  const setSelectedProvider = useCallback((provider: string, defaultModel?: string) => {
    setSelectedProviderState(provider);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_PROVIDER, provider);
    }
    if (defaultModel !== undefined) {
      setSelectedModelState(defaultModel);
      if (typeof window !== 'undefined') {
        localStorage.setItem(STORAGE_KEY_MODEL, defaultModel);
      }
    }
  }, []);

  const setSelectedModel = useCallback((model: string) => {
    setSelectedModelState(model);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_MODEL, model);
    }
  }, []);

  const setByokKey = useCallback((provider: string, key: string) => {
    setByokKeys((prev) => {
      const next = { ...prev, [provider]: key.trim() };
      if (typeof window !== 'undefined') {
        sessionStorage.setItem(STORAGE_KEY_BYOK, JSON.stringify(next));
      }
      return next;
    });
  }, []);

  const addMessage = useCallback((message: ChatMessage) => {
    setMessages((prev) => [...prev, message]);
  }, []);

  const clearMessages = useCallback(() => {
    setMessages([
      {
        role: 'assistant',
        content:
          'Chat session reset. Ask a question or click any quick action below to explore hints or detailed proofs.',
      },
    ]);
  }, []);

  const registerActiveQuestion = useCallback((question: QuestionContext | null) => {
    setActiveQuestion(question);
  }, []);

  const [pendingPrompt, setPendingPrompt] = useState<{
    text: string;
    mode?: 'HINT' | 'EXPLANATION' | 'GENERAL';
  } | null>(null);

  const triggerPrompt = useCallback((text: string, mode: 'HINT' | 'EXPLANATION' | 'GENERAL' = 'GENERAL') => {
    setPendingPrompt({ text, mode });
    setIsOpen(true);
  }, [setIsOpen]);

  const clearPendingPrompt = useCallback(() => {
    setPendingPrompt(null);
  }, []);

  // Global keyboard shortcut: Ctrl+J or Cmd+J to toggle AI Studio
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'j') {
        e.preventDefault();
        toggleOpen();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [toggleOpen]);

  // Window resize listener: re-clamp width & rect
  useEffect(() => {
    const handleResize = () => {
      setWidthState((prev) => {
        const maxW = getMaxWidth();
        if (prev > maxW) {
          const clamped = Math.max(MIN_WIDTH, maxW);
          if (typeof window !== 'undefined') {
            localStorage.setItem(STORAGE_KEY_WIDTH, String(clamped));
          }
          return clamped;
        }
        return prev;
      });
      setWindowRectState((prev) => {
        const clamped = clampWindowRect(prev);
        if (typeof window !== 'undefined') {
          localStorage.setItem(STORAGE_KEY_WINDOW_RECT, JSON.stringify(clamped));
        }
        return clamped;
      });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <AiStudioContext.Provider
      value={{
        isOpen,
        setIsOpen,
        toggleOpen,
        width,
        setWidth,
        windowMode,
        setWindowMode,
        toggleWindowMode,
        windowRect,
        setWindowRect,
        activeQuestion,
        registerActiveQuestion,
        selectedProvider,
        setSelectedProvider,
        selectedModel,
        setSelectedModel,
        byokKeys,
        setByokKey,
        messages,
        addMessage,
        clearMessages,
        isLoading,
        setIsLoading,
        pendingPrompt,
        triggerPrompt,
        clearPendingPrompt,
      }}
    >
      {children}
    </AiStudioContext.Provider>
  );
};

export const useAiStudio = (): AiStudioContextValue => {
  const context = useContext(AiStudioContext);
  if (!context) {
    return {
      isOpen: false,
      setIsOpen: () => {},
      toggleOpen: () => {},
      width: 420,
      setWidth: () => {},
      windowMode: 'docked',
      setWindowMode: () => {},
      toggleWindowMode: () => {},
      windowRect: { x: 16, y: 16, width: 420, height: 700 },
      setWindowRect: () => {},
      activeQuestion: null,
      registerActiveQuestion: () => {},
      selectedProvider: 'GROQ',
      setSelectedProvider: () => {},
      selectedModel: 'llama-3.3-70b-versatile',
      setSelectedModel: () => {},
      byokKeys: {},
      setByokKey: () => {},
      messages: [],
      addMessage: () => {},
      clearMessages: () => {},
      isLoading: false,
      setIsLoading: () => {},
      pendingPrompt: null,
      triggerPrompt: () => {},
      clearPendingPrompt: () => {},
    };
  }
  return context;
};

export default AiStudioContext;
