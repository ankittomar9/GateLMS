import { request } from './apiClient';
import { PracticeSessionResponse, QuestionDetail } from './types';

export type RevisionDrillSource = 'DUE_MISTAKES' | 'PASS2' | 'BOOKMARKED' | 'TOPIC';

export interface RevisionOverviewDto {
  dueMistakes: number;
  dueTopics: number;
  pass2Pending: number;
  dueCards: number;
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

export type CardGrade = 0 | 1 | 2;

export interface DueCardDto {
  cardId: string;
  prompt: string;
  answer: string;
  conceptSlug: string;
  conceptLabel: string;
  unitTitle: string;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
}

export interface DueCardsResponse {
  dueCount: number;
  cards: DueCardDto[];
}

export interface CardReviewResponse {
  nextDueAt: string;
  intervalDays: number;
  ease: number;
}

export const revisionApi = {
  getOverview: (): Promise<RevisionOverviewDto> => {
    return request<RevisionOverviewDto>('/revision/overview');
  },

  getDueCards: (): Promise<DueCardsResponse> => {
    return request<DueCardsResponse>('/revision/cards/due');
  },

  reviewCard: (cardId: string, grade: CardGrade): Promise<CardReviewResponse> => {
    return request<CardReviewResponse>(`/revision/cards/${cardId}/review`, {
      method: 'POST',
      body: JSON.stringify({ grade }),
    });
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
