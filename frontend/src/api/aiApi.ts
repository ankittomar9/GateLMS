import { apiClient, getXsrfToken, ApiError } from './apiClient';

export interface TranscriptionResponse {
  text: string;
  status: string;
  provider: string;
  model?: string;
  latencyMs?: number;
  message?: string;
}

export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

export interface QuestionContext {
  id?: string | null;
  externalId?: string | null;
  statement?: string | null;
  type?: string | null;
  year?: number | null;
  marks?: number | null;
  subjectName?: string | null;
  topicName?: string | null;
  subjectId?: string | null;
  topicId?: string | null;
  optionsJson?: string | null;
  selectedAnswer?: string | null;
  isLiveAttempt?: boolean;
}

export interface AiChatRequest {
  messages: ChatMessage[];
  provider?: string;
  model?: string;
  mode?: 'HINT' | 'EXPLANATION' | 'GENERAL';
  questionContext?: QuestionContext;
}

export interface AiChatResponse {
  reply: string;
  provider: string;
  model: string;
  latencyMs: number;
}

export interface ProviderStatus {
  provider: string;
  configured: boolean;
  state: 'READY' | 'NOT_CONFIGURED' | 'UNREACHABLE' | 'ERROR';
  model?: string;
  reason?: string;
  label?: string;
}

export const aiApi = {
  transcribeAudio: async (
    audioBlob: Blob,
    options: {
      apiKey?: string;
      promptContext?: string;
      provider?: string;
      lang?: string;
      signal?: AbortSignal;
    } = {}
  ): Promise<TranscriptionResponse> => {
    const isWav = audioBlob.type.includes('wav');
    const filename = isWav ? 'audio.wav' : 'audio.webm';

    // AM-1: BYOK is header-only; no apiKey in multipart form-data
    const formData = new FormData();
    formData.append('file', audioBlob, filename);
    if (options.promptContext) formData.append('promptContext', options.promptContext);
    if (options.provider) formData.append('provider', options.provider);
    if (options.lang) formData.append('lang', options.lang);

    const headers: Record<string, string> = {};
    const xsrf = getXsrfToken();
    if (xsrf) headers['X-XSRF-TOKEN'] = xsrf;
    if (options.apiKey) headers['X-AI-Key'] = options.apiKey;

    const res = await fetch('/api/v1/ai/transcribe', {
      method: 'POST',
      body: formData,
      headers,
      credentials: 'include',
      signal: options.signal,
    });

    // AM-14: 401 parity with apiClient -> redirect to /login
    if (res.status === 401) {
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        const next = encodeURIComponent(window.location.pathname + window.location.search);
        window.location.href = `/login?next=${next}`;
      }
    }

    if (!res.ok) {
      let errJson: any = null;
      try {
        errJson = await res.json();
      } catch {}
      throw new ApiError(res.status, errJson?.detail || errJson?.message || 'Transcription failed', errJson?.fieldErrors, errJson?.detail);
    }

    return res.json();
  },

  chat: async (request: AiChatRequest, apiKey?: string, signal?: AbortSignal): Promise<AiChatResponse> => {
    const headers: Record<string, string> = {};
    if (apiKey) headers['X-AI-Key'] = apiKey;

    // AM-15: Support AbortSignal
    return apiClient.post<AiChatResponse>('/ai/chat', request, { headers, signal });
  },

  getProvidersStatus: async (apiKey?: string, provider?: string): Promise<ProviderStatus[]> => {
    const headers: Record<string, string> = {};
    if (apiKey) headers['X-AI-Key'] = apiKey;

    return apiClient.get<ProviderStatus[]>('/ai/status', {
      headers,
      params: provider ? { provider } : undefined,
    });
  },
};
