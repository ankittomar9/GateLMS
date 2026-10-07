import { request } from './apiClient';
import { PracticeSessionResponse, QuestionDetail } from './types';

export type RevisionDrillSource = 'DUE_MISTAKES' | 'PASS2' | 'BOOKMARKED' | 'TOPIC';

export interface RevisionOverviewDto {
  dueMistakes: number;
  dueTopics: number;
  pass2Pending: number;
  bookmarked: number;
  drillsCompleted: number;
  generatedAt: string;
}

export interface RevisionDrillRequest {
  source: RevisionDrillSource;
  subjectId?: string;
  topicId?: string;
  count?: number;
}

export const revisionApi = {
  getOverview: (): Promise<RevisionOverviewDto> => {
    return request<RevisionOverviewDto>('/revision/overview');
  },

  createDrill: (req: RevisionDrillRequest): Promise<PracticeSessionResponse> => {
    return request<PracticeSessionResponse>('/revision/drills', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  redrillMistake: (mistakeId: string): Promise<PracticeSessionResponse> => {
    return request<PracticeSessionResponse>(`/revision/mistakes/${mistakeId}/redrill`, {
      method: 'POST',
    });
  },

  toggleBookmark: (questionId: string, bookmarked: boolean): Promise<QuestionDetail> => {
    return request<QuestionDetail>(`/questions/${questionId}/bookmark`, {
      method: 'PATCH',
      body: JSON.stringify({ bookmarked }),
    });
  },
};
