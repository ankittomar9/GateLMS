import { request } from './apiClient';
import {
  JourneyOverviewSubject,
  JourneyTopicSummary,
  TopicPrimerResponse,
  JourneyContinue,
  TopicPrimer,
  SubjectNotebookResponse,
  SimilarQuestionsResponse,
  TopicConceptCoverageResponse,
  ConceptLessonResponse,
  ConceptLesson,
  CurationLessonUpdateRequest,
} from '../types/journey';
import { PageResponse } from './anatomyApi';

export interface CurationPrimerUpdateRequest {
  status?: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  motivation?: string;
  coreIdea?: string;
  invariant?: string;
  workedExample?: string;
  traps?: string;
}

export const journeyApi = {
  getOverview: async (): Promise<JourneyOverviewSubject[]> => {
    return request<JourneyOverviewSubject[]>('/journey/overview');
  },

  getSubjectTopics: async (subjectId: string): Promise<JourneyTopicSummary[]> => {
    return request<JourneyTopicSummary[]>(`/journey/subjects/${subjectId}/topics`);
  },

  getTopicPrimer: async (topicId: string): Promise<TopicPrimerResponse> => {
    return request<TopicPrimerResponse>(`/journey/topics/${topicId}/primer`);
  },

  generateTopicPrimer: async (
    topicId: string,
    options?: { provider?: string; model?: string; apiKey?: string; force?: boolean }
  ): Promise<TopicPrimerResponse> => {
    const headers: Record<string, string> = {};
    if (options?.apiKey) {
      headers['x-api-key'] = options.apiKey;
    }
    const params: Record<string, string> = {};
    if (options?.provider) params.provider = options.provider;
    if (options?.model) params.model = options.model;
    if (options?.force) params.force = 'true';

    return request<TopicPrimerResponse>(`/journey/topics/${topicId}/primer/generate`, {
      method: 'POST',
      headers,
      params,
    });
  },

  updateTopicPrimer: async (
    topicId: string,
    req: CurationPrimerUpdateRequest
  ): Promise<TopicPrimer> => {
    return request<TopicPrimer>(`/journey/topics/${topicId}/primer`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
  },

  deleteTopicPrimer: async (topicId: string): Promise<{ status: string; topicId: string }> => {
    return request<{ status: string; topicId: string }>(`/journey/topics/${topicId}/primer`, {
      method: 'DELETE',
    });
  },

  getContinue: async (): Promise<JourneyContinue | null> => {
    return request<JourneyContinue | null>('/journey/continue');
  },

  getCurationPrimers: async (page = 0, size = 20): Promise<PageResponse<TopicPrimer>> => {
    return request<PageResponse<TopicPrimer>>('/curation/primers', {
      params: { page, size },
    });
  },

  updateCurationPrimer: async (
    id: string,
    req: CurationPrimerUpdateRequest
  ): Promise<TopicPrimer> => {
    return request<TopicPrimer>(`/curation/primers/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(req),
    });
  },

  getSubjectNotebook: async (
    subjectId: string,
    starredOnly = false,
    topicId?: string
  ): Promise<SubjectNotebookResponse> => {
    const params: Record<string, string> = { starredOnly: String(starredOnly) };
    if (topicId) params.topicId = topicId;
    return request<SubjectNotebookResponse>(`/journey/subjects/${subjectId}/notebook`, { params });
  },

  getSimilarQuestions: async (
    questionId: string,
    limit = 5
  ): Promise<SimilarQuestionsResponse> => {
    return request<SimilarQuestionsResponse>(`/questions/${questionId}/similar`, {
      params: { limit: String(limit) },
    });
  },

  getTopicConcepts: async (topicId: string): Promise<TopicConceptCoverageResponse> => {
    return request<TopicConceptCoverageResponse>(`/journey/topics/${topicId}/concepts`);
  },

  getConceptLesson: async (slug: string): Promise<ConceptLessonResponse> => {
    return request<ConceptLessonResponse>(`/journey/concepts/${slug}/lesson`);
  },

  generateConceptLesson: async (
    slug: string,
    options?: { provider?: string; model?: string; apiKey?: string; force?: boolean }
  ): Promise<ConceptLessonResponse> => {
    const headers: Record<string, string> = {};
    if (options?.apiKey) {
      headers['x-api-key'] = options.apiKey;
    }
    const params: Record<string, string> = {};
    if (options?.provider) params.provider = options.provider;
    if (options?.model) params.model = options.model;
    if (options?.force) params.force = 'true';

    return request<ConceptLessonResponse>(`/journey/concepts/${slug}/lesson/generate`, {
      method: 'POST',
      headers,
      params,
    });
  },

  updateConceptLesson: async (
    slug: string,
    req: CurationLessonUpdateRequest
  ): Promise<ConceptLesson> => {
    return request<ConceptLesson>(`/curation/concepts/${slug}/lesson`, {
      method: 'PUT',
      body: JSON.stringify(req),
    });
  },

  deleteConceptLesson: async (slug: string): Promise<{ status: string; slug: string }> => {
    return request<{ status: string; slug: string }>(`/curation/concepts/${slug}/lesson`, {
      method: 'DELETE',
    });
  },

  getCurationLessons: async (page = 0, size = 20): Promise<PageResponse<ConceptLesson>> => {
    return request<PageResponse<ConceptLesson>>('/curation/lessons', {
      params: { page, size },
    });
  },
};

