import { request } from './apiClient';
import {
  JourneyOverviewSubject,
  JourneyTopicSummary,
  TopicPrimerResponse,
  JourneyContinue,
  TopicPrimer,
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
    options?: { provider?: string; model?: string; apiKey?: string }
  ): Promise<TopicPrimerResponse> => {
    const headers: Record<string, string> = {};
    if (options?.apiKey) {
      headers['x-api-key'] = options.apiKey;
    }
    const params: Record<string, string> = {};
    if (options?.provider) params.provider = options.provider;
    if (options?.model) params.model = options.model;

    return request<TopicPrimerResponse>(`/journey/topics/${topicId}/primer/generate`, {
      method: 'POST',
      headers,
      params,
    });
  },

  getContinue: async (): Promise<JourneyContinue> => {
    return request<JourneyContinue>('/journey/continue');
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
};
