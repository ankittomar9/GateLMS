import { apiClient } from './apiClient';

export interface SubjectReadiness {
  subjectId: string;
  name: string;
  weight: number;
  m: number;
  a: number;
  c: number;
  readiness: number;
}

export interface ReadinessResponse {
  readiness: number;
  perSubject: SubjectReadiness[];
  computedAt: string;
}

export interface TopicHeatmap {
  topicId: string;
  name: string;
  mastery: number;
  due: boolean;
  answered: number;
  correct: number;
}

export interface SubjectHeatmap {
  subjectId: string;
  name: string;
  topics: TopicHeatmap[];
}

export interface HeatmapResponse {
  subjects: SubjectHeatmap[];
}

export interface DayRollup {
  date: string;
  questionsAnswered: number;
  correct: number;
  minutesPracticed: number;
  minutesMocked: number;
}

export interface Rolling7Stats {
  accuracy: number;
  volume: number;
}

export interface TrendsResponse {
  days: DayRollup[];
  rolling7: Rolling7Stats;
}

export interface ExamCountdown {
  examDate: string;
  daysRemaining: number;
  weeksRemaining: number;
  source: string;
}

export const readinessApi = {
  getReadiness: async (): Promise<ReadinessResponse> => {
    return apiClient.get<ReadinessResponse>('/stats/readiness');
  },

  getHeatmap: async (): Promise<HeatmapResponse> => {
    return apiClient.get<HeatmapResponse>('/stats/heatmap');
  },

  getTrends: async (days: number = 90): Promise<TrendsResponse> => {
    return apiClient.get<TrendsResponse>('/stats/trends', { params: { days } });
  },

  getCountdown: async (): Promise<ExamCountdown> => {
    return apiClient.get<ExamCountdown>('/exam/countdown');
  },
};
