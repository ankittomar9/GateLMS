import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { ChatMessage, QuestionContext } from '../api/aiApi';

interface AiStudioContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  toggleOpen: () => void;
  width: number;
  setWidth: (width: number) => void;
  activeQuestion: QuestionContext | null;
  registerActiveQuestion: (question: QuestionContext | null) => void;
  selectedProvider: string;
  setSelectedProvider: (provider: string) => void;
  selectedModel: string;
  setSelectedModel: (model: string) => void;
  byokKeys: Record<string, string>;
  setByokKey: (provider: string, key: string) => void;
  messages: ChatMessage[];
  addMessage: (message: ChatMessage) => void;
  clearMessages: () => void;
}

const STORAGE_KEY_WIDTH = 'gatelms_ai_studio_width';
const STORAGE_KEY_PROVIDER = 'gatelms_ai_provider';
const STORAGE_KEY_MODEL = 'gatelms_ai_model';
const STORAGE_KEY_BYOK = 'gatelms_byok_keys';

const DEFAULT_WIDTH = 420;
const MIN_WIDTH = 340;
const MAX_WIDTH = 800;

const DEFAULT_PROVIDER_MODELS: Record<string, string> = {
  GROQ: 'llama-3.3-70b-versatile',
  GEMINI: 'gemini-2.0-flash',
  OPENAI: 'gpt-4o-mini',
  OLLAMA: 'llama3',
};

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
        if (!isNaN(parsed) && parsed >= MIN_WIDTH && parsed <= MAX_WIDTH) {
          return parsed;
        }
      }
    }
    return DEFAULT_WIDTH;
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
      return localStorage.getItem(STORAGE_KEY_MODEL) || DEFAULT_PROVIDER_MODELS['GROQ'];
    }
    return DEFAULT_PROVIDER_MODELS['GROQ'];
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

  const setWidth = useCallback((newWidth: number) => {
    const clamped = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, newWidth));
    setWidthState(clamped);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_WIDTH, String(clamped));
    }
  }, []);

  const setSelectedProvider = useCallback((provider: string) => {
    setSelectedProviderState(provider);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_PROVIDER, provider);
    }
    const defaultModel = DEFAULT_PROVIDER_MODELS[provider] || '';
    setSelectedModelState(defaultModel);
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY_MODEL, defaultModel);
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

  const registerActiveQuestion = useCallback((q: QuestionContext | null) => {
    setActiveQuestion(q);
  }, []);

  const addMessage = useCallback((msg: ChatMessage) => {
    setMessages((prev) => [...prev, msg]);
  }, []);

  const clearMessages = useCallback(() => {
    setMessages([
      {
        role: 'assistant',
        content: "Chat cleared. What problem or topic would you like to explore next?",
      },
    ]);
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

  return (
    <AiStudioContext.Provider
      value={{
        isOpen,
        setIsOpen,
        toggleOpen,
        width,
        setWidth,
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
    };
  }
  return context;
};
