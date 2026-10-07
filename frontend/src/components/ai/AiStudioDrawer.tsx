import React, { useState, useRef, useEffect, useCallback } from 'react';
import { 
  Sparkles, 
  X, 
  Trash2, 
  Send, 
  Mic, 
  Square, 
  Settings, 
  Copy, 
  Check, 
  Lightbulb, 
  BookOpen, 
  AlertTriangle, 
  Cpu, 
  Maximize2, 
  Minimize2,
  ChevronRight,
  Loader2,
  KeyRound,
  ShieldAlert
} from 'lucide-react';
import { useAiStudio } from '../../context/AiStudioContext';
import { useAudioRecorder } from '../../hooks/useAudioRecorder';
import { aiApi, ChatMessage } from '../../api/aiApi';
import { MathText } from '../MathText';

const PROVIDER_OPTIONS = [
  { id: 'GROQ', label: 'Groq (Llama 3.3 70B)', defaultModel: 'llama-3.3-70b-versatile', speed: '~180ms' },
  { id: 'GEMINI', label: 'Google Gemini 2.0 Flash', defaultModel: 'gemini-2.0-flash', speed: '~350ms' },
  { id: 'OPENAI', label: 'OpenAI (GPT-4o Mini)', defaultModel: 'gpt-4o-mini', speed: '~450ms' },
  { id: 'OLLAMA', label: 'Local Ollama (Llama 3)', defaultModel: 'llama3', speed: 'Offline' },
];

export const AiStudioDrawer: React.FC = () => {
  const {
    isOpen,
    setIsOpen,
    width,
    setWidth,
    activeQuestion,
    selectedProvider,
    setSelectedProvider,
    selectedModel,
    setSelectedModel,
    byokKeys,
    setByokKey,
    messages,
    addMessage,
    clearMessages,
    pendingPrompt,
    clearPendingPrompt,
  } = useAiStudio();

  const [inputPrompt, setInputPrompt] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    return typeof window !== 'undefined' ? window.innerWidth < 768 : false;
  });

  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const activeApiKey = byokKeys[selectedProvider] || '';

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  }, []);

  // Responsive window resize listener (AM-16)
  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 768);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Esc key collapses drawer (AM-16)
  useEffect(() => {
    const handleGlobalKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        abortControllerRef.current?.abort();
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [isOpen, setIsOpen]);

  // Voice recording integration using audio-encoder and whisper sidecar
  const {
    isRecording,
    isTranscribing,
    durationSec,
    volumeLevel,
    errorMessage: audioError,
    startRecording,
    stopRecording,
    cancelRecording,
  } = useAudioRecorder({
    apiKey: activeApiKey,
    provider: selectedProvider,
    promptContext: activeQuestion ? `${activeQuestion.subjectName || ''} ${activeQuestion.topicName || ''} ${activeQuestion.statement || ''}`.slice(0, 300) : undefined,
    onTranscript: (text) => {
      setInputPrompt((prev) => (prev ? `${prev} ${text}` : text));
      showToast('Voice transcribed successfully!');
    },
    onError: (err) => {
      showToast(err);
    },
  });

  // Drag-to-resize sidecar drawer
  useEffect(() => {
    if (!isDragging || isMobile) return;

    const handleMouseMove = (e: MouseEvent) => {
      const newWidth = window.innerWidth - e.clientX;
      setWidth(newWidth);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging, isMobile, setWidth]);

  // Auto-scroll on new message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  const handleSendPrompt = async (promptOverride?: string, mode: 'HINT' | 'EXPLANATION' | 'GENERAL' = 'GENERAL') => {
    const textToSend = promptOverride || inputPrompt;
    if (!textToSend.trim() || isLoading) return;

    // AM-15: Abort any pending turn before starting a new one
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;

    const userMessage: ChatMessage = { role: 'user', content: textToSend.trim() };
    addMessage(userMessage);
    if (!promptOverride) {
      setInputPrompt('');
    }
    setIsLoading(true);

    try {
      const res = await aiApi.chat(
        {
          messages: [...messages, userMessage],
          provider: selectedProvider,
          model: selectedModel,
          mode,
          questionContext: activeQuestion || undefined,
        },
        activeApiKey || undefined,
        controller.signal
      );

      addMessage({ role: 'assistant', content: res.reply });
    } catch (err: any) {
      if (err.name === 'AbortError') return;
      const errMsg = err.message || 'AI generation failed. Please check your provider or key.';
      addMessage({
        role: 'assistant',
        content: `⚠️ **Error**: ${errMsg}\n\n*Tip: Click the Settings gear ⚙️ to configure your API key for ${selectedProvider}.*`,
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (pendingPrompt && isOpen) {
      const { text, mode } = pendingPrompt;
      clearPendingPrompt();
      handleSendPrompt(text, mode);
    }
  }, [pendingPrompt, isOpen, clearPendingPrompt]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendPrompt();
    }
  };

  const copyToClipboard = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const handleClose = () => {
    abortControllerRef.current?.abort();
    setIsOpen(false);
  };

  const handleClear = () => {
    abortControllerRef.current?.abort();
    clearMessages();
  };

  if (!isOpen) return null;

  const isLiveAttempt = Boolean(activeQuestion?.isLiveAttempt);

  return (
    <div
      role="complementary"
      aria-label="AI Studio"
      style={{ width: isMobile ? '100%' : `${width}px` }}
      data-testid="ai-studio-drawer"
      className="fixed right-0 top-0 bottom-0 z-40 bg-ds-surface border-l border-ds-border shadow-2xl flex flex-col transition-all duration-75 select-none"
    >
      {/* 1. Left border drag handle for resizing (disabled on mobile) */}
      {!isMobile && (
        <div
          onMouseDown={() => setIsDragging(true)}
          className={`absolute left-0 top-0 bottom-0 w-1.5 cursor-col-resize hover:bg-ds-accent/60 transition-colors z-50 ${
            isDragging ? 'bg-ds-accent' : 'bg-transparent'
          }`}
          title="Drag to resize AI Studio"
        />
      )}

      {/* 2. Top Header Bar */}
      <div className="h-14 px-4 border-b border-ds-border flex items-center justify-between bg-ds-surface shrink-0">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-ds-accent/10 border border-ds-accent/30 flex items-center justify-center text-ds-accent">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h2 className="text-xs font-bold text-ds-primary flex items-center gap-1.5">
              <span>AI Studio</span>
              <span className="text-[9px] font-mono px-1.5 py-0.2 rounded bg-ds-accent-soft text-ds-accent font-semibold">
                Sidecar
              </span>
            </h2>
          </div>
        </div>

        {/* Model Picker & Action Buttons */}
        <div className="flex items-center gap-1.5">
          {/* Provider Selector Dropdown */}
          <select
            value={selectedProvider}
            onChange={(e) => setSelectedProvider(e.target.value)}
            className="text-[11px] font-medium bg-ds-surface-input border border-ds-border rounded-ds px-2 py-1 text-ds-primary focus:outline-none focus:border-ds-accent cursor-pointer"
            aria-label="Select AI Provider"
          >
            {PROVIDER_OPTIONS.map((opt) => (
              <option key={opt.id} value={opt.id}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Settings / BYOK Modal Button */}
          <button
            type="button"
            onClick={() => setShowSettings((p) => !p)}
            className={`p-1.5 rounded-ds border transition-colors ${
              showSettings
                ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent'
                : 'text-ds-muted hover:text-ds-primary border-ds-border hover:bg-ds-surface-raised'
            }`}
            title="Configure API Keys & Providers"
            aria-label="Configure API Keys"
          >
            <Settings className="w-3.5 h-3.5" />
          </button>

          {/* Clear Chat Button */}
          <button
            type="button"
            onClick={handleClear}
            className="p-1.5 text-ds-muted hover:text-red-400 rounded-ds border border-ds-border hover:bg-ds-surface-raised transition-colors"
            title="Clear Chat History"
            aria-label="Clear Chat History"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>

          {/* Close / Collapse Drawer */}
          <button
            type="button"
            onClick={handleClose}
            className="p-1.5 text-ds-muted hover:text-ds-primary rounded-ds border border-ds-border hover:bg-ds-surface-raised transition-colors ml-1"
            title="Close AI Studio (Ctrl+J)"
            aria-label="Close AI Studio"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Toast Notification Banner */}
      {toastMessage && (
        <div aria-live="polite" className="bg-ds-accent/90 text-ds-accent-contrast px-3 py-1.5 text-xs font-semibold flex items-center justify-between animate-fadeIn shrink-0">
          <span>{toastMessage}</span>
          <button type="button" onClick={() => setToastMessage(null)} className="text-white/80 hover:text-white">
            <X className="w-3 h-3" />
          </button>
        </div>
      )}

      {/* 3. BYOK Settings Drawer/Overlay (AM-12 Privacy Notice, AM-13 sessionStorage) */}
      {showSettings && (
        <div className="p-4 bg-ds-surface-raised border-b border-ds-border space-y-3 shrink-0 animate-fadeIn text-xs">
          <div className="flex items-center justify-between font-bold text-ds-primary">
            <span className="flex items-center gap-1.5">
              <KeyRound className="w-3.5 h-3.5 text-ds-accent" />
              <span>Provider & Key Configuration</span>
            </span>
            <button
              type="button"
              onClick={() => setShowSettings(false)}
              className="text-ds-muted hover:text-ds-primary"
              aria-label="Close Settings"
            >
              ✕
            </button>
          </div>

          <div className="space-y-2">
            <div>
              <label className="block text-[10px] font-mono text-ds-muted uppercase mb-1">
                {selectedProvider} API Key
              </label>
              <input
                type="password"
                placeholder={
                  selectedProvider === 'OLLAMA'
                    ? 'No key needed for Local Ollama'
                    : `Paste ${selectedProvider} API key...`
                }
                value={byokKeys[selectedProvider] || ''}
                disabled={selectedProvider === 'OLLAMA'}
                onChange={(e) => setByokKey(selectedProvider, e.target.value)}
                className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-2.5 py-1 text-xs text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent disabled:opacity-50"
                aria-label={`${selectedProvider} API Key`}
              />
            </div>

            <div className="text-[10px] text-ds-muted leading-relaxed space-y-1">
              <p>
                Keys are temporarily stored in your browser's <code className="font-mono">sessionStorage</code> (cleared when browser session ends). If blank, server environment variables are used automatically.
              </p>
              <p className="text-[10px] text-amber-500/90 font-medium flex items-start gap-1">
                <ShieldAlert className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                <span>
                  <strong>Privacy Disclosure</strong>: Cloud providers receive your question text and prompts. LOCAL (Ollama / whisper.cpp) is 100% offline with zero egress.
                </span>
              </p>
            </div>
          </div>
        </div>
      )}

      {/* 4. Active Question Context Card (PED-1 Hint-gating during live attempts) */}
      {activeQuestion && (
        <div className="m-3 p-3 rounded-ds bg-ds-surface-raised border border-ds-border-strong space-y-2 shrink-0">
          <div className="flex items-center justify-between text-[11px]">
            <span className="font-bold text-ds-primary flex items-center gap-1">
              <BookOpen className="w-3.5 h-3.5 text-ds-accent" />
              <span>
                {activeQuestion.year ? `GATE CS ${activeQuestion.year}` : 'Active Problem'}
                {activeQuestion.marks ? ` · ${activeQuestion.marks} Marks` : ''}
              </span>
            </span>
            <span className="font-mono text-[9px] px-1.5 py-0.5 rounded bg-ds-accent/10 text-ds-accent border border-ds-accent/20">
              {activeQuestion.type || 'QUESTION'}
            </span>
          </div>

          {activeQuestion.statement && (
            <p className="text-[11px] text-ds-secondary line-clamp-2 leading-relaxed">
              {activeQuestion.statement}
            </p>
          )}

          {/* Quick Action Chips (PED-1) */}
          <div className="flex flex-wrap gap-1.5 pt-1 border-t border-ds-border/60">
            <button
              type="button"
              onClick={() => {
                const prompt = `Here is the question I'm solving: "${activeQuestion.statement || ''}". Could you give me a subtle conceptual hint without spoiling the answer?`;
                handleSendPrompt(prompt, 'HINT');
              }}
              className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-ds bg-ds-accent-soft text-ds-accent hover:bg-ds-accent hover:text-ds-accent-contrast transition-all"
            >
              <Lightbulb className="w-3 h-3" />
              <span>💡 Hint Only</span>
            </button>

            <button
              type="button"
              disabled={isLiveAttempt}
              onClick={() => {
                if (isLiveAttempt) return;
                const prompt = `Solve this problem step-by-step with complete formulas and proof: "${activeQuestion.statement || ''}". Options: ${activeQuestion.optionsJson || 'N/A'}`;
                handleSendPrompt(prompt, 'EXPLANATION');
              }}
              title={isLiveAttempt ? 'Unlocks after you submit the attempt' : 'Solve problem step-by-step'}
              className={`inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-ds border transition-all ${
                isLiveAttempt
                  ? 'bg-ds-surface border-ds-border text-ds-muted/60 opacity-50 cursor-not-allowed'
                  : 'bg-ds-surface border-ds-border text-ds-primary hover:border-ds-accent'
              }`}
            >
              <BookOpen className="w-3 h-3" />
              <span>📐 Step-by-Step</span>
            </button>

            <button
              type="button"
              onClick={() => {
                const prompt = `What are the most common student traps, subtleties, and edge cases to look out for in this topic (${activeQuestion.topicName || 'GATE CS'})?`;
                handleSendPrompt(prompt, 'GENERAL');
              }}
              className="inline-flex items-center gap-1 text-[10px] font-semibold px-2 py-0.5 rounded-ds bg-ds-surface border border-ds-border text-ds-muted hover:text-ds-primary transition-all"
            >
              <AlertTriangle className="w-3 h-3" />
              <span>⚠️ Traps</span>
            </button>
          </div>
        </div>
      )}

      {/* 5. Chat History Feed */}
      <div 
        aria-live="polite" 
        className="flex-1 p-4 overflow-y-auto space-y-4 select-text"
      >
        {messages.map((msg, index) => {
          const isUser = msg.role === 'user';
          return (
            <div
              key={index}
              className={`flex flex-col ${isUser ? 'items-end' : 'items-start'} space-y-1`}
            >
              <div
                className={`max-w-[90%] rounded-lg p-3 text-xs leading-relaxed ${
                  isUser
                    ? 'bg-ds-accent text-ds-accent-contrast font-medium rounded-tr-none'
                    : 'bg-ds-surface-raised border border-ds-border text-ds-primary rounded-tl-none shadow-sm'
                }`}
              >
                {isUser ? (
                  <p className="whitespace-pre-wrap">{msg.content}</p>
                ) : (
                  <div className="prose prose-xs dark:prose-invert max-w-none">
                    <MathText content={msg.content} />
                  </div>
                )}
              </div>

              {/* Message metadata & Copy button */}
              {!isUser && (
                <div className="flex items-center gap-2 text-[10px] text-ds-muted pl-1">
                  <button
                    type="button"
                    onClick={() => copyToClipboard(msg.content, index)}
                    className="flex items-center gap-1 hover:text-ds-primary transition-colors"
                  >
                    {copiedIndex === index ? (
                      <>
                        <Check className="w-2.5 h-2.5 text-ds-success" />
                        <span>Copied</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-2.5 h-2.5" />
                        <span>Copy</span>
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>
          );
        })}

        {isLoading && (
          <div className="flex items-center gap-2 p-3 rounded-lg bg-ds-surface-raised border border-ds-border text-xs text-ds-muted animate-pulse">
            <Loader2 className="w-4 h-4 animate-spin text-ds-accent" />
            <span>GateLMS AI is formulating response...</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* 6. Audio Waveform Indicator & Controls (when recording) */}
      {isRecording && (
        <div className="px-4 py-2 bg-red-500/10 border-t border-red-500/30 flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <span className="w-3 h-3 rounded-full bg-red-500 animate-ping motion-reduce:animate-none" />
            <span className="text-xs font-mono font-bold text-red-500">
              REC {String(Math.floor(durationSec / 60)).padStart(2, '0')}:
              {String(durationSec % 60).padStart(2, '0')}
            </span>

            {/* Live Volume Equalizer Bars */}
            <div className="flex items-center gap-0.5 h-4">
              {[0.4, 0.8, 1.0, 0.6, 0.9, 0.3].map((factor, i) => (
                <div
                  key={i}
                  style={{ height: `${Math.max(3, volumeLevel * 16 * factor)}px` }}
                  className="w-1 bg-red-500 rounded-full transition-all duration-75"
                />
              ))}
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={cancelRecording}
              className="px-2 py-1 text-[11px] rounded bg-ds-surface border border-ds-border text-ds-muted hover:text-ds-primary"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={stopRecording}
              className="px-2.5 py-1 text-[11px] font-bold rounded bg-red-500 text-white hover:bg-red-600 transition-all flex items-center gap-1"
            >
              <Check className="w-3 h-3" />
              <span>Finish & Transcribe</span>
            </button>
          </div>
        </div>
      )}

      {isTranscribing && (
        <div className="px-4 py-2 bg-ds-accent-soft border-t border-ds-accent/30 flex items-center gap-2 text-xs text-ds-accent font-medium shrink-0">
          <Loader2 className="w-3.5 h-3.5 animate-spin" />
          <span>Whisper neural transcription in progress (~180ms)...</span>
        </div>
      )}

      {/* 7. Input Textarea & Send Bar */}
      <div className="p-3 border-t border-ds-border bg-ds-surface shrink-0 space-y-2">
        <div className="relative flex items-end bg-ds-surface-input border border-ds-border rounded-lg focus-within:border-ds-accent transition-all">
          <textarea
            ref={textareaRef}
            rows={2}
            value={inputPrompt}
            onChange={(e) => setInputPrompt(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={isRecording ? 'Listening to speech...' : 'Ask question, explain approach, or dictate...'}
            disabled={isRecording}
            className="w-full bg-transparent px-3 py-2 text-xs text-ds-primary placeholder:text-ds-muted resize-none focus:outline-none leading-relaxed"
            aria-label="Ask AI Studio question"
          />

          <div className="flex items-center gap-1 p-1.5">
            {/* Whisper Microphone Recording Button */}
            <button
              type="button"
              onClick={isRecording ? stopRecording : startRecording}
              disabled={isTranscribing}
              title={isRecording ? 'Stop Recording' : 'Dictate with Whisper Speech-to-Text'}
              aria-label={isRecording ? 'Stop Recording' : 'Dictate with Whisper'}
              className={`p-1.5 rounded-md transition-all ${
                isRecording
                  ? 'bg-red-500 text-white animate-pulse motion-reduce:animate-none'
                  : 'text-ds-muted hover:text-ds-primary hover:bg-ds-surface-raised'
              }`}
            >
              <Mic className="w-4 h-4" />
            </button>

            {/* Send Button */}
            <button
              type="button"
              disabled={!inputPrompt.trim() || isLoading}
              onClick={() => handleSendPrompt()}
              className="p-1.5 rounded-md bg-ds-accent text-ds-accent-contrast disabled:opacity-40 hover:bg-ds-accent-hover transition-all"
              title="Send message (Enter)"
              aria-label="Send message"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="flex items-center justify-between text-[10px] text-ds-muted font-mono">
          <span>
            Provider: <strong className="text-ds-primary">{selectedProvider}</strong>
          </span>
          <span>Shift+Enter for newline</span>
        </div>
      </div>
    </div>
  );
};
