import { request } from './apiClient';
import { QuestionSummary } from './types';

export interface TrapAnalysisItem {
  option: string;
  verdict: string;
  why_plausible: string;
  misconception: string;
}

export interface PrerequisiteItem {
  concept_slug: string;
  concept_label: string;
  topic_slug?: string | null;
  must_read: boolean;
}

export interface QuestionAnatomyDto {
  id: string;
  questionId: string;
  testedConcept: string;
  invariantRule: string | null;
  trapAnalysis: TrapAnalysisItem[];
  prerequisites: PrerequisiteItem[];
  similarPatternHint: string | null;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  parseError: string | null;
  generatorModel: string | null;
  generatedAt: string;
  updatedAt: string | null;
}

export interface RetrievalCardDto {
  id: string;
  prompt: string;
  answer: string;
  cardOrder: number;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
}

export interface ConceptUnitDto {
  id: string;
  conceptTagSlug: string;
  title: string;
  motivation: string;
  coreIdea: string;
  invariant: string | null;
  workedExample: string;
  traps: string | null;
  deepLinkRef: string | null;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  parseError: string | null;
  generatorModel: string | null;
  generatedAt: string;
  cards: RetrievalCardDto[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CurationAnatomyUpdateRequest {
  status?: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  testedConcept?: string;
  invariantRule?: string;
  similarPatternHint?: string;
}

export const anatomyApi = {
  getQuestionAnatomy: async (questionId: string): Promise<QuestionAnatomyDto> => {
    return request<QuestionAnatomyDto>(`/questions/${questionId}/anatomy`);
  },

  generateQuestionAnatomy: async (
    questionId: string,
    options?: { provider?: string; model?: string; apiKey?: string; force?: boolean }
  ): Promise<QuestionAnatomyDto> => {
    const headers: Record<string, string> = {};
    if (options?.apiKey) {
      headers['x-api-key'] = options.apiKey;
    }
    const query = options?.force ? '?force=true' : '';
    return request<QuestionAnatomyDto>(`/questions/${questionId}/anatomy/generate${query}`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        provider: options?.provider,
        model: options?.model,
      }),
    });
  },

  getConceptUnit: async (slug: string): Promise<ConceptUnitDto> => {
    return request<ConceptUnitDto>(`/concepts/${slug}`);
  },

  generateConceptUnit: async (
    slug: string,
    options?: { provider?: string; model?: string; apiKey?: string }
  ): Promise<ConceptUnitDto> => {
    const headers: Record<string, string> = {};
    if (options?.apiKey) {
      headers['x-api-key'] = options.apiKey;
    }
    return request<ConceptUnitDto>(`/concepts/${slug}/generate`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        provider: options?.provider,
        model: options?.model,
      }),
    });
  },

  getConceptQuestions: async (slug: string, page = 0, size = 20): Promise<PageResponse<QuestionSummary>> => {
    return request<PageResponse<QuestionSummary>>(`/concepts/${slug}/questions`, {
      params: { page, size },
    });
  },

  getCurationDrafts: async (page = 0, size = 20): Promise<PageResponse<QuestionAnatomyDto>> => {
    return request<PageResponse<QuestionAnatomyDto>>(`/curation/anatomy`, {
      params: { page, size },
    });
  },

  updateCurationAnatomy: async (
    id: string,
    req: CurationAnatomyUpdateRequest
  ): Promise<QuestionAnatomyDto> => {
    return request<QuestionAnatomyDto>(`/curation/anatomy/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(req),
    });
  },
};
